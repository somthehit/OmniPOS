-- =============================================================================
-- OmniPOS / ShopFlow: Stored Procedures, Functions & Real-Time Reporting Views
-- Target: Microsoft SQL Server (Docker Container)
-- =============================================================================

USE OmniPOS;
GO

-- -----------------------------------------------------------------------------
-- 1. Scalar Function: fn_GetProductStockOnHand
-- Calculates real-time physical stock as SUM of stock movements
-- -----------------------------------------------------------------------------
CREATE OR ALTER FUNCTION dbo.fn_GetProductStockOnHand
(
    @org_id NVARCHAR(64),
    @branch_id NVARCHAR(64),
    @product_id NVARCHAR(64)
)
RETURNS INT
AS
BEGIN
    DECLARE @stock INT;
    SELECT @stock = ISNULL(SUM(quantity_delta), 0)
    FROM dbo.stock_movements
    WHERE org_id = @org_id
      AND branch_id = @branch_id
      AND product_id = @product_id;
    RETURN ISNULL(@stock, 0);
END;
GO

-- -----------------------------------------------------------------------------
-- 2. Scalar Function: fn_IsCustomerCreditAllowed
-- Verifies customer khata status and limits before authorizing credit tender
-- -----------------------------------------------------------------------------
CREATE OR ALTER FUNCTION dbo.fn_IsCustomerCreditAllowed
(
    @org_id NVARCHAR(64),
    @customer_id NVARCHAR(64),
    @charge_amount_cents BIGINT
)
RETURNS BIT
AS
BEGIN
    DECLARE @current_due BIGINT;
    DECLARE @credit_limit BIGINT;
    DECLARE @hold_status NVARCHAR(50);
    DECLARE @allowed BIT = 0;

    SELECT 
        @current_due = c.current_due_in_cents,
        @credit_limit = ISNULL(ct.credit_limit_minor, c.credit_limit_in_cents),
        @hold_status = ISNULL(ct.hold_status, N'ACTIVE')
    FROM dbo.customers c
    LEFT JOIN dbo.customer_credit_terms ct 
        ON c.id = ct.customer_id AND c.org_id = ct.org_id
    WHERE c.id = @customer_id AND c.org_id = @org_id;

    IF (@hold_status = N'ACTIVE' AND (@current_due + @charge_amount_cents) <= @credit_limit)
    BEGIN
        SET @allowed = 1;
    END

    RETURN @allowed;
END;
GO

-- -----------------------------------------------------------------------------
-- 3. Stored Procedure: sp_RecordCustomerLedgerEntry
-- Records debt/repayment and atomically updates customer current due balance
-- -----------------------------------------------------------------------------
CREATE OR ALTER PROCEDURE dbo.sp_RecordCustomerLedgerEntry
    @org_id NVARCHAR(64),
    @customer_id NVARCHAR(64),
    @entry_id NVARCHAR(64),
    @entry_type NVARCHAR(50), -- 'CHARGE', 'PAYMENT', 'SALE_CREDIT', 'DEBT_REPAYMENT', 'ADJUSTMENT'
    @amount_in_cents BIGINT,
    @sale_id NVARCHAR(64) = NULL,
    @reference_type NVARCHAR(50) = NULL,
    @reference_id NVARCHAR(100) = NULL,
    @idempotency_key NVARCHAR(128) = NULL,
    @notes NVARCHAR(1000) = N''
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;

    BEGIN TRY
        -- Check idempotency
        IF @idempotency_key IS NOT NULL AND EXISTS (
            SELECT 1 FROM dbo.customer_ledger 
            WHERE org_id = @org_id AND idempotency_key = @idempotency_key
        )
        BEGIN
            COMMIT TRANSACTION;
            RETURN;
        END

        DECLARE @current_balance BIGINT;
        SELECT @current_balance = current_due_in_cents 
        FROM dbo.customers WITH (UPDLOCK)
        WHERE id = @customer_id AND org_id = @org_id;

        IF @current_balance IS NULL
        BEGIN
            RAISERROR('Customer not found for specified organization.', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END

        DECLARE @new_balance BIGINT;
        IF @entry_type IN (N'CHARGE', N'SALE_CREDIT')
        BEGIN
            SET @new_balance = @current_balance + @amount_in_cents;
        END
        ELSE
        BEGIN
            SET @new_balance = @current_balance - @amount_in_cents;
            IF @new_balance < 0 SET @new_balance = 0; -- Floor at zero for retail khata
        END

        -- Insert ledger record
        INSERT INTO dbo.customer_ledger (
            id, org_id, customer_id, sale_id, entry_type, amount_in_cents,
            balance_after_in_cents, reference_type, reference_id, idempotency_key, notes
        ) VALUES (
            @entry_id, @org_id, @customer_id, @sale_id, @entry_type, @amount_in_cents,
            @new_balance, @reference_type, @reference_id, @idempotency_key, @notes
        );

        -- Update customer cache
        UPDATE dbo.customers
        SET current_due_in_cents = @new_balance,
            updated_at = SYSDATETIMEOFFSET()
        WHERE id = @customer_id AND org_id = @org_id;

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- -----------------------------------------------------------------------------
-- 4. Stored Procedure: sp_RegisterDevice
-- Handles hardware terminal fingerprint registration and heartbeat updates
-- -----------------------------------------------------------------------------
CREATE OR ALTER PROCEDURE dbo.sp_RegisterDevice
    @org_id NVARCHAR(64),
    @device_id NVARCHAR(64),
    @device_fingerprint NVARCHAR(255),
    @device_name NVARCHAR(255),
    @registered_by NVARCHAR(128)
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (SELECT 1 FROM dbo.devices WHERE org_id = @org_id AND device_fingerprint = @device_fingerprint)
    BEGIN
        UPDATE dbo.devices
        SET last_seen_at = SYSDATETIMEOFFSET(),
            device_name = @device_name,
            status = N'ACTIVE'
        WHERE org_id = @org_id AND device_fingerprint = @device_fingerprint;
    END
    ELSE
    BEGIN
        INSERT INTO dbo.devices (
            id, org_id, device_fingerprint, device_name, registered_by, status, last_seen_at
        ) VALUES (
            @device_id, @org_id, @device_fingerprint, @device_name, @registered_by, N'ACTIVE', SYSDATETIMEOFFSET()
        );
    END
END;
GO

-- -----------------------------------------------------------------------------
-- 5. Stored Procedure: sp_OpenShiftSession
-- Atomically initiates a cashier shift session with opening float validation
-- -----------------------------------------------------------------------------
CREATE OR ALTER PROCEDURE dbo.sp_OpenShiftSession
    @shift_id NVARCHAR(64),
    @org_id NVARCHAR(64),
    @branch_id NVARCHAR(64),
    @register_id NVARCHAR(64),
    @cashier_uid NVARCHAR(128),
    @cashier_name NVARCHAR(255),
    @business_date NVARCHAR(20),
    @opening_float_in_cents BIGINT,
    @device_id NVARCHAR(100) = N''
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;

    BEGIN TRY
        -- Ensure register does not have another active shift open
        IF EXISTS (
            SELECT 1 FROM dbo.shifts 
            WHERE org_id = @org_id AND register_id = @register_id AND status = N'OPEN'
        )
        BEGIN
            RAISERROR('Another shift is currently OPEN on this register. Close it before opening a new shift.', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- Insert Shift
        INSERT INTO dbo.shifts (
            id, org_id, branch_id, register_id, cashier_uid, cashier_name,
            business_date, opening_float_in_cents, status, last_device_id
        ) VALUES (
            @shift_id, @org_id, @branch_id, @register_id, @cashier_uid, @cashier_name,
            @business_date, @opening_float_in_cents, N'OPEN', @device_id
        );

        -- Update Register pointer
        UPDATE dbo.registers
        SET current_shift_id = @shift_id,
            updated_at = SYSDATETIMEOFFSET()
        WHERE id = @register_id AND org_id = @org_id;

        -- Record shift event log
        INSERT INTO dbo.shift_events (
            id, shift_id, org_id, action, event_type, actor_uid, actor_name, device_id, payload_json
        ) VALUES (
            CONCAT(N'evt_open_', @shift_id), @shift_id, @org_id, N'OPENED', N'OPENED',
            @cashier_uid, @cashier_name, @device_id,
            CONCAT(N'{"opening_float":', @opening_float_in_cents, N'}')
        );

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- -----------------------------------------------------------------------------
-- 6. Stored Procedure: sp_CloseShiftSession
-- Calculates expected drawer balance, compares counted cash, and finalizes shift
-- -----------------------------------------------------------------------------
CREATE OR ALTER PROCEDURE dbo.sp_CloseShiftSession
    @shift_id NVARCHAR(64),
    @counted_cash_in_cents BIGINT,
    @closing_float_in_cents BIGINT,
    @deposit_amount_in_cents BIGINT,
    @closed_by NVARCHAR(128),
    @notes NVARCHAR(1000) = N''
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;

    BEGIN TRY
        DECLARE @org_id NVARCHAR(64);
        DECLARE @register_id NVARCHAR(64);
        DECLARE @opening_float BIGINT;

        SELECT 
            @org_id = org_id,
            @register_id = register_id,
            @opening_float = opening_float_in_cents
        FROM dbo.shifts WITH (UPDLOCK)
        WHERE id = @shift_id;

        IF @org_id IS NULL
        BEGIN
            RAISERROR('Shift not found.', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- Sum cash sales on this shift
        DECLARE @cash_sales BIGINT = 0;
        SELECT @cash_sales = ISNULL(SUM(sp.amount_in_cents), 0)
        FROM dbo.sale_payments sp
        INNER JOIN dbo.sales s ON sp.sale_id = s.id AND sp.org_id = s.org_id
        WHERE s.org_id = @org_id 
          AND s.register_id = @register_id
          AND sp.payment_type = N'CASH'
          AND s.created_at >= (SELECT opened_at FROM dbo.shifts WHERE id = @shift_id);

        -- Sum cash movements (PAID_IN vs PAID_OUT / SAFE_DROP)
        DECLARE @paid_in BIGINT = 0;
        DECLARE @paid_out BIGINT = 0;

        SELECT 
            @paid_in = ISNULL(SUM(CASE WHEN movement_type = N'PAID_IN' THEN amount_in_cents ELSE 0 END), 0),
            @paid_out = ISNULL(SUM(CASE WHEN movement_type IN (N'PAID_OUT', N'SAFE_DROP') THEN amount_in_cents ELSE 0 END), 0)
        FROM dbo.cash_movements
        WHERE shift_id = @shift_id AND tender = N'CASH';

        DECLARE @expected_cash BIGINT = @opening_float + @cash_sales + @paid_in - @paid_out;
        DECLARE @variance BIGINT = @counted_cash_in_cents - @expected_cash;

        -- Update Shift
        UPDATE dbo.shifts
        SET closed_at = SYSDATETIMEOFFSET(),
            expected_cash_in_cents = @expected_cash,
            counted_cash_in_cents = @counted_cash_in_cents,
            closing_float_in_cents = @closing_float_in_cents,
            deposit_amount_in_cents = @deposit_amount_in_cents,
            variance_in_cents = @variance,
            status = CASE WHEN @variance = 0 THEN N'APPROVED' ELSE N'PENDING_REVIEW' END,
            closed_by = @closed_by,
            notes = @notes
        WHERE id = @shift_id;

        -- Clear current shift from Register
        UPDATE dbo.registers
        SET current_shift_id = NULL,
            updated_at = SYSDATETIMEOFFSET()
        WHERE id = @register_id AND org_id = @org_id;

        -- Log event
        INSERT INTO dbo.shift_events (
            id, shift_id, org_id, action, event_type, actor_uid, payload_json
        ) VALUES (
            CONCAT(N'evt_close_', @shift_id), @shift_id, @org_id, N'CLOSED', N'CLOSED', @closed_by,
            CONCAT(N'{"expected":', @expected_cash, N',"counted":', @counted_cash_in_cents, N',"variance":', @variance, N'}')
        );

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO

-- -----------------------------------------------------------------------------
-- 7. Real-Time View: vw_ProductInventoryStock
-- Instant query of products with category name, current physical stock, and margin
-- -----------------------------------------------------------------------------
CREATE OR ALTER VIEW dbo.vw_ProductInventoryStock
AS
SELECT 
    p.id AS product_id,
    p.org_id,
    p.name AS product_name,
    p.sku,
    p.barcode,
    c.name AS category_name,
    p.unit,
    p.cost_price_in_cents,
    p.selling_price_in_cents,
    (p.selling_price_in_cents - p.cost_price_in_cents) AS profit_margin_in_cents,
    tr.name AS tax_rate_name,
    tr.rate_percent AS tax_rate_percent,
    p.min_stock_alert,
    ISNULL(stock.total_stock, 0) AS current_stock_on_hand,
    CASE 
        WHEN ISNULL(stock.total_stock, 0) <= 0 THEN N'OUT_OF_STOCK'
        WHEN ISNULL(stock.total_stock, 0) <= p.min_stock_alert THEN N'LOW_STOCK'
        ELSE N'IN_STOCK'
    END AS stock_status,
    p.status AS product_status
FROM dbo.products p
LEFT JOIN dbo.categories c ON p.category_id = c.id AND p.org_id = c.org_id
LEFT JOIN dbo.tax_rates tr ON p.tax_rate_id = tr.id AND p.org_id = tr.org_id
LEFT JOIN (
    SELECT org_id, product_id, SUM(quantity_delta) AS total_stock
    FROM dbo.stock_movements
    GROUP BY org_id, product_id
) stock ON p.id = stock.product_id AND p.org_id = stock.org_id;
GO

-- -----------------------------------------------------------------------------
-- 8. Real-Time View: vw_DailySalesSummary
-- Instant aggregation of sales by day, payment status, and tender breakdown
-- -----------------------------------------------------------------------------
CREATE OR ALTER VIEW dbo.vw_DailySalesSummary
AS
SELECT 
    s.org_id,
    s.branch_id,
    CAST(s.created_at AS DATE) AS sale_date,
    COUNT(s.id) AS total_invoices_count,
    SUM(s.subtotal_in_cents) AS total_subtotal_in_cents,
    SUM(s.discount_in_cents) AS total_discount_in_cents,
    SUM(s.tax_in_cents) AS total_tax_in_cents,
    SUM(s.grand_total_in_cents) AS total_grand_total_in_cents,
    SUM(CASE WHEN sp.payment_type = N'CASH' THEN sp.amount_in_cents ELSE 0 END) AS cash_collected_in_cents,
    SUM(CASE WHEN sp.payment_type = N'CARD' THEN sp.amount_in_cents ELSE 0 END) AS card_collected_in_cents,
    SUM(CASE WHEN sp.payment_type = N'QR' THEN sp.amount_in_cents ELSE 0 END) AS qr_collected_in_cents,
    SUM(CASE WHEN sp.payment_type = N'CREDIT' THEN sp.amount_in_cents ELSE 0 END) AS credit_khata_in_cents
FROM dbo.sales s
LEFT JOIN dbo.sale_payments sp ON s.id = sp.sale_id AND s.org_id = sp.org_id
WHERE s.payment_status = N'COMPLETED'
GROUP BY s.org_id, s.branch_id, CAST(s.created_at AS DATE);
GO
