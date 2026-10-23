-- =============================================================================
-- OmniPOS / ShopFlow: Complete SQL Server Database Schema & Initial Seed
-- Target: Microsoft SQL Server 2022 / 2025 (Docker)
-- =============================================================================

USE master;
GO

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'OmniPOS')
BEGIN
    CREATE DATABASE OmniPOS COLLATE Latin1_General_100_CI_AS_SC_UTF8;
END
GO

USE OmniPOS;
GO

-- -----------------------------------------------------------------------------
-- 1. Organizations (Multi-Tenant Core)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.organizations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.organizations (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        display_name NVARCHAR(255) NOT NULL,
        legal_name NVARCHAR(255) NULL,
        business_type NVARCHAR(64) NOT NULL DEFAULT N'RETAIL',
        country_code NVARCHAR(10) NOT NULL DEFAULT N'NP',
        currency_code NVARCHAR(10) NOT NULL DEFAULT N'NPR',
        currency_symbol NVARCHAR(10) NOT NULL DEFAULT N'Rs.',
        currency_minor_digits INT NOT NULL DEFAULT 2,
        timezone_id NVARCHAR(100) NOT NULL DEFAULT N'Asia/Kathmandu',
        tax_id NVARCHAR(100) NULL,
        registration_number NVARCHAR(100) NULL,
        tax_mode NVARCHAR(20) NOT NULL DEFAULT N'EXCLUSIVE' CHECK (tax_mode IN (N'INCLUSIVE', N'EXCLUSIVE', N'NONE')),
        rounding_rule NVARCHAR(50) NOT NULL DEFAULT N'HALF_UP',
        invoice_prefix NVARCHAR(50) NOT NULL DEFAULT N'INV',
        owner_uid NVARCHAR(128) NOT NULL,
        status NVARCHAR(50) NOT NULL DEFAULT N'ACTIVE' CHECK (status IN (N'ACTIVE', N'SUSPENDED', N'ARCHIVED')),
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 2. Branches (Locations per Org)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.branches', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.branches (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_branches_org REFERENCES dbo.organizations(id),
        name NVARCHAR(255) NOT NULL,
        code NVARCHAR(50) NOT NULL,
        address NVARCHAR(500) NULL,
        phone NVARCHAR(50) NULL,
        receipt_header NVARCHAR(500) NULL,
        receipt_footer NVARCHAR(500) NULL,
        status NVARCHAR(50) NOT NULL DEFAULT N'ACTIVE' CHECK (status IN (N'ACTIVE', N'INACTIVE')),
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT UQ_branches_org_code UNIQUE (org_id, code)
    );
END
GO

-- -----------------------------------------------------------------------------
-- 3. Registers (Tills / POS Terminals)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.registers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.registers (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_registers_org REFERENCES dbo.organizations(id),
        branch_id NVARCHAR(64) NOT NULL CONSTRAINT FK_registers_branch REFERENCES dbo.branches(id),
        name NVARCHAR(100) NOT NULL,
        code NVARCHAR(50) NOT NULL,
        receipt_prefix NVARCHAR(50) NOT NULL,
        receipt_header NVARCHAR(500) NULL,
        receipt_footer NVARCHAR(500) NULL,
        current_shift_id NVARCHAR(64) NULL,
        is_active BIT NOT NULL DEFAULT 1,
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT UQ_registers_org_prefix UNIQUE (org_id, receipt_prefix)
    );
END
GO

-- -----------------------------------------------------------------------------
-- 4. Users / Profiles (Staff and Store Owners)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id NVARCHAR(128) NOT NULL PRIMARY KEY, -- Firebase Auth UID or Supabase UUID
        email NVARCHAR(255) NOT NULL,
        full_name NVARCHAR(255) NOT NULL,
        avatar_url NVARCHAR(1000) NULL,
        phone NVARCHAR(50) NULL,
        language NVARCHAR(10) NOT NULL DEFAULT N'en',
        terms_accepted_at DATETIMEOFFSET NULL,
        active_org_id NVARCHAR(64) NULL CONSTRAINT FK_users_active_org REFERENCES dbo.organizations(id),
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 5. Staff Members / Memberships (Role-Based Access Control)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.staff_members', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.staff_members (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_staff_org REFERENCES dbo.organizations(id),
        user_id NVARCHAR(128) NOT NULL,
        branch_id NVARCHAR(64) NULL CONSTRAINT FK_staff_branch REFERENCES dbo.branches(id),
        name NVARCHAR(255) NOT NULL,
        email NVARCHAR(255) NOT NULL,
        role NVARCHAR(50) NOT NULL CHECK (role IN (N'OWNER', N'ADMIN', N'MANAGER', N'STOREKEEPER', N'CASHIER', N'ACCOUNTANT')),
        pin_hash NVARCHAR(255) NOT NULL DEFAULT N'1234',
        status NVARCHAR(50) NOT NULL DEFAULT N'ACTIVE' CHECK (status IN (N'ACTIVE', N'SUSPENDED', N'REVOKED')),
        is_active BIT NOT NULL DEFAULT 1,
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT UQ_staff_org_user UNIQUE (org_id, user_id)
    );
END
GO

-- -----------------------------------------------------------------------------
-- 6. Registered Devices (Hardware Fleet Management)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.devices', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.devices (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_devices_org REFERENCES dbo.organizations(id),
        device_fingerprint NVARCHAR(255) NOT NULL,
        device_name NVARCHAR(255) NOT NULL,
        registered_by NVARCHAR(128) NOT NULL,
        status NVARCHAR(50) NOT NULL DEFAULT N'ACTIVE' CHECK (status IN (N'ACTIVE', N'REVOKED')),
        last_seen_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT UQ_devices_org_fingerprint UNIQUE (org_id, device_fingerprint)
    );
END
GO

-- -----------------------------------------------------------------------------
-- 7. Invitations (Staff Onboarding)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.invitations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.invitations (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_invitations_org REFERENCES dbo.organizations(id),
        branch_id NVARCHAR(64) NULL CONSTRAINT FK_invitations_branch REFERENCES dbo.branches(id),
        email NVARCHAR(255) NOT NULL,
        role NVARCHAR(50) NOT NULL CHECK (role IN (N'ADMIN', N'MANAGER', N'STOREKEEPER', N'CASHIER', N'ACCOUNTANT')),
        code NVARCHAR(64) NOT NULL UNIQUE,
        invited_by NVARCHAR(128) NOT NULL,
        status NVARCHAR(50) NOT NULL DEFAULT N'PENDING' CHECK (status IN (N'PENDING', N'ACCEPTED', N'EXPIRED', N'REVOKED')),
        expires_at DATETIMEOFFSET NOT NULL,
        used_at DATETIMEOFFSET NULL,
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 8. Categories (Catalog Taxonomy)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.categories', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.categories (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_categories_org REFERENCES dbo.organizations(id),
        name NVARCHAR(255) NOT NULL,
        color_hex NVARCHAR(20) NOT NULL DEFAULT N'#3B82F6',
        icon_name NVARCHAR(50) NOT NULL DEFAULT N'category',
        sort_order INT NOT NULL DEFAULT 0,
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT UQ_categories_org_name UNIQUE (org_id, name)
    );
END
GO

-- -----------------------------------------------------------------------------
-- 9. Tax Rates (Configurable Rates e.g. VAT 13%)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.tax_rates', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.tax_rates (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_tax_rates_org REFERENCES dbo.organizations(id),
        name NVARCHAR(100) NOT NULL,
        rate_bps INT NOT NULL, -- 1300 = 13.00%
        rate_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
        is_default BIT NOT NULL DEFAULT 0,
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 10. Products (SKUs, Pricing & Inventory Parameters)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.products', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.products (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_products_org REFERENCES dbo.organizations(id),
        category_id NVARCHAR(64) NULL CONSTRAINT FK_products_category REFERENCES dbo.categories(id),
        tax_rate_id NVARCHAR(64) NULL CONSTRAINT FK_products_tax REFERENCES dbo.tax_rates(id),
        name NVARCHAR(255) NOT NULL,
        sku NVARCHAR(100) NOT NULL,
        barcode NVARCHAR(100) NULL,
        brand NVARCHAR(100) NULL,
        description NVARCHAR(MAX) NULL,
        unit NVARCHAR(50) NOT NULL DEFAULT N'pcs',
        cost_price_in_cents BIGINT NOT NULL DEFAULT 0,
        selling_price_in_cents BIGINT NOT NULL DEFAULT 0,
        tax_rate_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
        min_stock_alert INT NOT NULL DEFAULT 5,
        track_stock BIT NOT NULL DEFAULT 1,
        image_path NVARCHAR(1000) NULL,
        status NVARCHAR(50) NOT NULL DEFAULT N'ACTIVE' CHECK (status IN (N'ACTIVE', N'ARCHIVED')),
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT UQ_products_org_sku UNIQUE (org_id, sku)
    );
    CREATE INDEX IX_products_org_barcode ON dbo.products(org_id, barcode);
END
GO

-- -----------------------------------------------------------------------------
-- 11. Item Barcodes (Multi-barcode & Pack Multipliers)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.item_barcodes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.item_barcodes (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_item_barcodes_org REFERENCES dbo.organizations(id),
        item_id NVARCHAR(64) NOT NULL CONSTRAINT FK_item_barcodes_item REFERENCES dbo.products(id),
        barcode NVARCHAR(100) NOT NULL,
        pack_qty INT NOT NULL DEFAULT 1,
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT UQ_item_barcodes_org_barcode UNIQUE (org_id, barcode)
    );
END
GO

-- -----------------------------------------------------------------------------
-- 12. Stock Movements (Append-Only Inventory Ledger)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.stock_movements', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.stock_movements (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_stock_movements_org REFERENCES dbo.organizations(id),
        branch_id NVARCHAR(64) NOT NULL CONSTRAINT FK_stock_movements_branch REFERENCES dbo.branches(id),
        product_id NVARCHAR(64) NOT NULL CONSTRAINT FK_stock_movements_product REFERENCES dbo.products(id),
        quantity_delta INT NOT NULL, -- Signed: negative for sales, positive for receipts
        type NVARCHAR(50) NOT NULL, -- SALE, RECEIPT, ADJUSTMENT, REFUND_RESTOCK, TRANSFER_OUT, TRANSFER_IN, COUNT_CORRECTION, OPENING
        reference_id NVARCHAR(100) NOT NULL DEFAULT N'',
        reason NVARCHAR(500) NULL,
        idempotency_key NVARCHAR(128) NULL,
        created_by NVARCHAR(128) NOT NULL DEFAULT N'',
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
    CREATE INDEX IX_stock_movements_search ON dbo.stock_movements(org_id, branch_id, product_id, created_at);
END
GO

-- -----------------------------------------------------------------------------
-- 13. Customers (CRM & Khata Accounts)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.customers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.customers (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_customers_org REFERENCES dbo.organizations(id),
        name NVARCHAR(255) NOT NULL,
        phone NVARCHAR(50) NOT NULL,
        email NVARCHAR(255) NULL,
        address NVARCHAR(500) NULL,
        tax_id NVARCHAR(100) NULL, -- PAN/VAT
        credit_limit_in_cents BIGINT NOT NULL DEFAULT 50000, -- Default credit limit (e.g. 500.00)
        current_due_in_cents BIGINT NOT NULL DEFAULT 0,
        status NVARCHAR(50) NOT NULL DEFAULT N'ACTIVE' CHECK (status IN (N'ACTIVE', N'BLOCKED', N'ARCHIVED')),
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT UQ_customers_org_phone UNIQUE (org_id, phone)
    );
END
GO

-- -----------------------------------------------------------------------------
-- 14. Customer Credit Terms (Khata Rules)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.customer_credit_terms', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.customer_credit_terms (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_credit_terms_org REFERENCES dbo.organizations(id),
        customer_id NVARCHAR(64) NOT NULL CONSTRAINT FK_credit_terms_customer REFERENCES dbo.customers(id),
        credit_limit_minor BIGINT NOT NULL DEFAULT 0,
        payment_terms_days INT NOT NULL DEFAULT 30,
        grace_period_days INT NOT NULL DEFAULT 0,
        interest_rate_bps INT NOT NULL DEFAULT 0,
        hold_status NVARCHAR(50) NOT NULL DEFAULT N'ACTIVE' CHECK (hold_status IN (N'ACTIVE', N'OVERDUE_HOLD', N'SUSPENDED')),
        approved_by NVARCHAR(128) NULL,
        approved_at DATETIMEOFFSET NULL,
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT UQ_credit_terms_org_customer UNIQUE (org_id, customer_id)
    );
END
GO

-- -----------------------------------------------------------------------------
-- 15. Customer Ledger (Khata Transaction History)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.customer_ledger', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.customer_ledger (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_customer_ledger_org REFERENCES dbo.organizations(id),
        customer_id NVARCHAR(64) NOT NULL CONSTRAINT FK_customer_ledger_customer REFERENCES dbo.customers(id),
        sale_id NVARCHAR(64) NULL,
        entry_type NVARCHAR(50) NOT NULL, -- CHARGE, PAYMENT, SALE_CREDIT, DEBT_REPAYMENT, REFUND_CREDIT, ADJUSTMENT
        amount_in_cents BIGINT NOT NULL,
        balance_after_in_cents BIGINT NOT NULL,
        reference_type NVARCHAR(50) NULL,
        reference_id NVARCHAR(100) NULL,
        idempotency_key NVARCHAR(128) NULL,
        notes NVARCHAR(1000) NOT NULL DEFAULT N'',
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
    CREATE INDEX IX_customer_ledger_timeline ON dbo.customer_ledger(org_id, customer_id, created_at);
END
GO

-- -----------------------------------------------------------------------------
-- 16. Fiscal Years (Audit & Statutory Compliance)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.fiscal_years', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.fiscal_years (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_fiscal_years_org REFERENCES dbo.organizations(id),
        name NVARCHAR(50) NOT NULL, -- e.g. "2081/82"
        code NVARCHAR(20) NOT NULL, -- e.g. "81/82"
        start_date DATETIMEOFFSET NOT NULL,
        end_date DATETIMEOFFSET NOT NULL,
        invoice_prefix NVARCHAR(50) NOT NULL DEFAULT N'INV',
        current_invoice_sequence BIGINT NOT NULL DEFAULT 0,
        is_current BIT NOT NULL DEFAULT 0,
        is_closed BIT NOT NULL DEFAULT 0,
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT UQ_fiscal_years_org_name UNIQUE (org_id, name)
    );
END
GO

-- -----------------------------------------------------------------------------
-- 17. Sales (Invoices / Orders)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.sales', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sales (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_sales_org REFERENCES dbo.organizations(id),
        branch_id NVARCHAR(64) NOT NULL CONSTRAINT FK_sales_branch REFERENCES dbo.branches(id),
        register_id NVARCHAR(64) NOT NULL CONSTRAINT FK_sales_register REFERENCES dbo.registers(id),
        invoice_no NVARCHAR(100) NOT NULL,
        fiscal_year NVARCHAR(50) NOT NULL DEFAULT N'',
        customer_id NVARCHAR(64) NULL CONSTRAINT FK_sales_customer REFERENCES dbo.customers(id),
        subtotal_in_cents BIGINT NOT NULL,
        discount_in_cents BIGINT NOT NULL DEFAULT 0,
        tax_in_cents BIGINT NOT NULL DEFAULT 0,
        grand_total_in_cents BIGINT NOT NULL,
        payment_status NVARCHAR(50) NOT NULL DEFAULT N'COMPLETED' CHECK (payment_status IN (N'COMPLETED', N'VOIDED', N'PARTIALLY_REFUNDED', N'REFUNDED')),
        currency_code NVARCHAR(10) NOT NULL DEFAULT N'NPR',
        tax_mode NVARCHAR(20) NOT NULL DEFAULT N'EXCLUSIVE',
        idempotency_key NVARCHAR(128) NULL,
        created_by NVARCHAR(128) NOT NULL DEFAULT N'',
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        CONSTRAINT UQ_sales_org_invoice UNIQUE (org_id, invoice_no)
    );
    CREATE INDEX IX_sales_lookup ON dbo.sales(org_id, branch_id, register_id, created_at);
END
GO

-- -----------------------------------------------------------------------------
-- 18. Sale Items (Line Items)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.sale_items', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sale_items (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_sale_items_org REFERENCES dbo.organizations(id),
        sale_id NVARCHAR(64) NOT NULL CONSTRAINT FK_sale_items_sale REFERENCES dbo.sales(id),
        product_id NVARCHAR(64) NOT NULL CONSTRAINT FK_sale_items_product REFERENCES dbo.products(id),
        product_name NVARCHAR(255) NOT NULL,
        sku NVARCHAR(100) NULL,
        unit_price_in_cents BIGINT NOT NULL,
        quantity INT NOT NULL,
        discount_in_cents BIGINT NOT NULL DEFAULT 0,
        tax_in_cents BIGINT NOT NULL DEFAULT 0,
        line_total_in_cents BIGINT NOT NULL
    );
    CREATE INDEX IX_sale_items_sale ON dbo.sale_items(sale_id);
END
GO

-- -----------------------------------------------------------------------------
-- 19. Sale Payments (Multi-Tender Payments)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.sale_payments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sale_payments (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_sale_payments_org REFERENCES dbo.organizations(id),
        sale_id NVARCHAR(64) NOT NULL CONSTRAINT FK_sale_payments_sale REFERENCES dbo.sales(id),
        payment_type NVARCHAR(50) NOT NULL CHECK (payment_type IN (N'CASH', N'CARD', N'QR', N'WALLET', N'CREDIT')),
        amount_in_cents BIGINT NOT NULL,
        tendered_in_cents BIGINT NULL,
        change_given_in_cents BIGINT NOT NULL DEFAULT 0,
        reference_no NVARCHAR(255) NOT NULL DEFAULT N'',
        provider NVARCHAR(100) NULL,
        provider_status NVARCHAR(50) NOT NULL DEFAULT N'CONFIRMED'
    );
    CREATE INDEX IX_sale_payments_sale ON dbo.sale_payments(sale_id);
END
GO

-- -----------------------------------------------------------------------------
-- 20. Held Orders (Cart Parking)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.held_orders', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.held_orders (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_held_orders_org REFERENCES dbo.organizations(id),
        register_id NVARCHAR(64) NOT NULL CONSTRAINT FK_held_orders_register REFERENCES dbo.registers(id),
        customer_name NVARCHAR(255) NOT NULL,
        cart_json NVARCHAR(MAX) NOT NULL,
        total_in_cents BIGINT NOT NULL,
        held_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 21. Shifts (Cashier Shift Sessions)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.shifts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shifts (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_shifts_org REFERENCES dbo.organizations(id),
        branch_id NVARCHAR(64) NOT NULL CONSTRAINT FK_shifts_branch REFERENCES dbo.branches(id),
        register_id NVARCHAR(64) NOT NULL CONSTRAINT FK_shifts_register REFERENCES dbo.registers(id),
        cashier_uid NVARCHAR(128) NOT NULL,
        cashier_name NVARCHAR(255) NOT NULL,
        business_date NVARCHAR(20) NOT NULL DEFAULT N'',
        opened_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        opening_float_in_cents BIGINT NOT NULL DEFAULT 0,
        opening_expected_in_cents BIGINT NOT NULL DEFAULT 0,
        opening_gap_in_cents BIGINT NOT NULL DEFAULT 0,
        opening_gap_reason NVARCHAR(500) NOT NULL DEFAULT N'',
        closed_at DATETIMEOFFSET NULL,
        counted_cash_in_cents BIGINT NULL,
        expected_cash_in_cents BIGINT NULL,
        closing_float_in_cents BIGINT NOT NULL DEFAULT 0,
        deposit_amount_in_cents BIGINT NOT NULL DEFAULT 0,
        variance_in_cents BIGINT NULL,
        variance_reason NVARCHAR(500) NOT NULL DEFAULT N'',
        status NVARCHAR(50) NOT NULL DEFAULT N'OPEN' CHECK (status IN (N'OPEN', N'CLOSING', N'PENDING_REVIEW', N'FLAGGED', N'APPROVED', N'FORCE_CLOSED')),
        closed_by NVARCHAR(128) NOT NULL DEFAULT N'',
        review_status NVARCHAR(50) NOT NULL DEFAULT N'PENDING',
        reviewed_by NVARCHAR(128) NULL,
        reviewed_at DATETIMEOFFSET NULL,
        review_note NVARCHAR(1000) NULL,
        version INT NOT NULL DEFAULT 1,
        last_device_id NVARCHAR(100) NOT NULL DEFAULT N'',
        notes NVARCHAR(1000) NOT NULL DEFAULT N''
    );
    CREATE INDEX IX_shifts_search ON dbo.shifts(org_id, register_id, status);
END
GO

-- -----------------------------------------------------------------------------
-- 22. Shift Denominations (Structured Cash Breakdown)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.shift_denominations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shift_denominations (
        shift_id NVARCHAR(64) NOT NULL PRIMARY KEY CONSTRAINT FK_shift_denominations_shift REFERENCES dbo.shifts(id) ON DELETE CASCADE,
        cashier_id NVARCHAR(128) NOT NULL,
        device_id NVARCHAR(100) NOT NULL DEFAULT N'',
        status NVARCHAR(50) NOT NULL DEFAULT N'OPEN',
        counts_json NVARCHAR(MAX) NULL,
        total_in_cents BIGINT NOT NULL DEFAULT 0,
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 23. Shift Cash Counts (Physical Drawer Counting)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.shift_cash_counts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shift_cash_counts (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        shift_id NVARCHAR(64) NOT NULL CONSTRAINT FK_cash_counts_shift REFERENCES dbo.shifts(id) ON DELETE CASCADE,
        phase NVARCHAR(50) NOT NULL, -- OPENING, CLOSING, ADJUSTMENT
        denomination INT NOT NULL,
        count INT NOT NULL,
        amount_in_cents BIGINT NOT NULL DEFAULT 0,
        subtotal_in_cents BIGINT NOT NULL DEFAULT 0
    );
END
GO

-- -----------------------------------------------------------------------------
-- 24. Shift Declarations (Digital Tenders e.g. eSewa, Khalti, Cards)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.shift_declarations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shift_declarations (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        shift_id NVARCHAR(64) NOT NULL CONSTRAINT FK_declarations_shift REFERENCES dbo.shifts(id) ON DELETE CASCADE,
        type NVARCHAR(50) NOT NULL DEFAULT N'ONLINE',
        method NVARCHAR(50) NOT NULL DEFAULT N'ONLINE',
        provider NVARCHAR(50) NOT NULL,
        pos_amount_in_cents BIGINT NOT NULL DEFAULT 0,
        provider_amount_in_cents BIGINT NOT NULL DEFAULT 0,
        declared_amount_in_cents BIGINT NOT NULL DEFAULT 0,
        variance_in_cents BIGINT NOT NULL DEFAULT 0,
        verification NVARCHAR(50) NOT NULL DEFAULT N'DECLARED_UNVERIFIED',
        reference NVARCHAR(255) NOT NULL DEFAULT N'',
        slip_count INT NOT NULL DEFAULT 0,
        reason_code NVARCHAR(50) NOT NULL DEFAULT N'',
        note NVARCHAR(1000) NOT NULL DEFAULT N''
    );
END
GO

-- -----------------------------------------------------------------------------
-- 25. Shift Credit Notes (Refunds & Redemptions during Shift)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.shift_credit_notes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shift_credit_notes (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        shift_id NVARCHAR(64) NOT NULL CONSTRAINT FK_credit_notes_shift REFERENCES dbo.shifts(id) ON DELETE CASCADE,
        credit_note_id NVARCHAR(100) NOT NULL DEFAULT N'',
        total_in_cents BIGINT NOT NULL DEFAULT 0,
        cash_refund_in_cents BIGINT NOT NULL DEFAULT 0,
        customer_balance_in_cents BIGINT NOT NULL DEFAULT 0,
        bank_refund_in_cents BIGINT NOT NULL DEFAULT 0,
        redeemed_as_tender_in_cents BIGINT NOT NULL DEFAULT 0,
        reason NVARCHAR(500) NOT NULL DEFAULT N''
    );
END
GO

-- -----------------------------------------------------------------------------
-- 26. Shift Adjustments (Manager Corrections)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.shift_adjustments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shift_adjustments (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        shift_id NVARCHAR(64) NOT NULL CONSTRAINT FK_adjustments_shift REFERENCES dbo.shifts(id) ON DELETE CASCADE,
        target NVARCHAR(50) NOT NULL DEFAULT N'CLOSING',
        supervisor_uid NVARCHAR(128) NOT NULL DEFAULT N'',
        amount_in_cents BIGINT NOT NULL DEFAULT 0,
        before_json NVARCHAR(MAX) NOT NULL DEFAULT N'{}',
        after_json NVARCHAR(MAX) NOT NULL DEFAULT N'{}',
        reason NVARCHAR(500) NOT NULL DEFAULT N'',
        approved_by NVARCHAR(128) NOT NULL DEFAULT N'',
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 27. Shift Versions (Snapshot Versioning for Audits)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.shift_versions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shift_versions (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        shift_id NVARCHAR(64) NOT NULL CONSTRAINT FK_versions_shift REFERENCES dbo.shifts(id) ON DELETE CASCADE,
        version INT NOT NULL,
        snapshot_json NVARCHAR(MAX) NOT NULL,
        reason NVARCHAR(500) NOT NULL,
        changed_by NVARCHAR(128) NOT NULL DEFAULT N'',
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 28. Shift Events (Lifecycle Log)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.shift_events', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.shift_events (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        shift_id NVARCHAR(64) NOT NULL CONSTRAINT FK_events_shift REFERENCES dbo.shifts(id) ON DELETE CASCADE,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_events_org REFERENCES dbo.organizations(id),
        action NVARCHAR(50) NOT NULL,
        event_type NVARCHAR(50) NOT NULL,
        actor_uid NVARCHAR(128) NOT NULL DEFAULT N'',
        actor_name NVARCHAR(255) NOT NULL DEFAULT N'',
        device_id NVARCHAR(100) NOT NULL DEFAULT N'',
        payload_json NVARCHAR(MAX) NOT NULL DEFAULT N'{}',
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 29. Cash Movements (Paid In, Paid Out, Safe Drop)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.cash_movements', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.cash_movements (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_cash_movements_org REFERENCES dbo.organizations(id),
        shift_id NVARCHAR(64) NOT NULL CONSTRAINT FK_cash_movements_shift REFERENCES dbo.shifts(id),
        movement_type NVARCHAR(50) NOT NULL CHECK (movement_type IN (N'PAID_IN', N'PAID_OUT', N'SAFE_DROP', N'CREDIT_COLLECTION')),
        tender NVARCHAR(50) NOT NULL DEFAULT N'CASH',
        amount_in_cents BIGINT NOT NULL,
        reason NVARCHAR(500) NOT NULL,
        created_by NVARCHAR(128) NOT NULL DEFAULT N'',
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 30. Store Settings (Operational & Hardware Preferences)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.store_settings', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.store_settings (
        org_id NVARCHAR(64) NOT NULL PRIMARY KEY CONSTRAINT FK_settings_org REFERENCES dbo.organizations(id),
        allow_cash BIT NOT NULL DEFAULT 1,
        allow_card BIT NOT NULL DEFAULT 1,
        allow_qr BIT NOT NULL DEFAULT 1,
        allow_credit BIT NOT NULL DEFAULT 1,
        max_discount_percent INT NOT NULL DEFAULT 15,
        receipt_header_note NVARCHAR(500) NOT NULL DEFAULT N'Thank you for shopping!',
        receipt_footer_note NVARCHAR(500) NOT NULL DEFAULT N'Please come again!',
        auto_print_receipt BIT NOT NULL DEFAULT 1,
        low_stock_threshold INT NOT NULL DEFAULT 5,
        negative_stock_behavior NVARCHAR(50) NOT NULL DEFAULT N'BLOCK',
        auto_lock_interval NVARCHAR(50) NOT NULL DEFAULT N'5 minutes',
        auto_logout_interval NVARCHAR(50) NOT NULL DEFAULT N'Never',
        biometric_enabled BIT NOT NULL DEFAULT 0,
        login_biometric_enabled BIT NOT NULL DEFAULT 0,
        enforce_strong_pin BIT NOT NULL DEFAULT 1,
        printer_name NVARCHAR(255) NOT NULL DEFAULT N'',
        printer_connection_type NVARCHAR(50) NOT NULL DEFAULT N'BLUETOOTH',
        paper_width_mm INT NOT NULL DEFAULT 80,
        scanner_mode NVARCHAR(100) NOT NULL DEFAULT N'USB HID Keyboard Emulation Mode',
        cash_drawer_trigger BIT NOT NULL DEFAULT 1,
        backup_email NVARCHAR(255) NOT NULL DEFAULT N'',
        last_backup_at DATETIMEOFFSET NULL,
        language NVARCHAR(10) NOT NULL DEFAULT N'en',
        updated_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 31. Audit Logs (Forensic Action Records)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.audit_logs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.audit_logs (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_audit_logs_org REFERENCES dbo.organizations(id),
        actor_uid NVARCHAR(128) NOT NULL,
        actor_name NVARCHAR(255) NOT NULL,
        actor_role NVARCHAR(50) NOT NULL,
        action NVARCHAR(100) NOT NULL,
        details NVARCHAR(MAX) NOT NULL,
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
    CREATE INDEX IX_audit_logs_lookup ON dbo.audit_logs(org_id, created_at);
END
GO

-- -----------------------------------------------------------------------------
-- 32. Price History (Auditable Cost and Selling Price Evolution)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.price_history', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.price_history (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_price_history_org REFERENCES dbo.organizations(id),
        product_id NVARCHAR(64) NOT NULL CONSTRAINT FK_price_history_product REFERENCES dbo.products(id),
        old_price_minor BIGINT NOT NULL,
        new_price_minor BIGINT NOT NULL,
        old_cost_minor BIGINT NULL,
        new_cost_minor BIGINT NULL,
        change_reason NVARCHAR(500) NULL,
        effective_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
        changed_by NVARCHAR(128) NULL
    );
END
GO

-- -----------------------------------------------------------------------------
-- 33. Sync Outbox (Offline-First Delta Sync)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.sync_outbox', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sync_outbox (
        id NVARCHAR(64) NOT NULL PRIMARY KEY,
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_sync_outbox_org REFERENCES dbo.organizations(id),
        table_name NVARCHAR(100) NOT NULL,
        record_id NVARCHAR(100) NOT NULL,
        payload_json NVARCHAR(MAX) NOT NULL,
        operation NVARCHAR(50) NOT NULL, -- INSERT, UPDATE, DELETE, UPSERT
        idempotency_key NVARCHAR(128) NULL,
        retry_count INT NOT NULL DEFAULT 0,
        last_error NVARCHAR(MAX) NULL,
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- -----------------------------------------------------------------------------
-- 34. Sync State (Watermarks per Entity)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.sync_state', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sync_state (
        org_id NVARCHAR(64) NOT NULL CONSTRAINT FK_sync_state_org REFERENCES dbo.organizations(id),
        entity_type NVARCHAR(100) NOT NULL,
        last_pulled_at DATETIMEOFFSET NULL,
        last_pushed_at DATETIMEOFFSET NULL,
        CONSTRAINT PK_sync_state PRIMARY KEY (org_id, entity_type)
    );
END
GO

-- -----------------------------------------------------------------------------
-- 35. Onboarding Idempotency Keys (Prevent Double Creation RPCs)
-- -----------------------------------------------------------------------------
IF OBJECT_ID(N'dbo.onboarding_idempotency_keys', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.onboarding_idempotency_keys (
        [key] NVARCHAR(128) NOT NULL PRIMARY KEY,
        user_id NVARCHAR(128) NOT NULL,
        organization_id NVARCHAR(64) NULL CONSTRAINT FK_idempotency_org REFERENCES dbo.organizations(id),
        response_payload NVARCHAR(MAX) NULL,
        created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
    );
END
GO

-- =============================================================================
-- SEED DATA (Bootstrap Default Organization, Branch, Register & Catalogs)
-- =============================================================================

-- Default Organization
IF NOT EXISTS (SELECT 1 FROM dbo.organizations WHERE id = N'org_omnipos_default')
BEGIN
    INSERT INTO dbo.organizations (
        id, display_name, legal_name, business_type, country_code, currency_code,
        currency_symbol, currency_minor_digits, timezone_id, tax_id, tax_mode,
        rounding_rule, invoice_prefix, owner_uid, status
    ) VALUES (
        N'org_omnipos_default', N'OmniPOS Flagship Store', N'OmniPOS Technologies Pvt. Ltd.',
        N'RETAIL', N'NP', N'NPR', N'Rs.', 2, N'Asia/Kathmandu', N'VAT-987654321',
        N'EXCLUSIVE', N'HALF_UP', N'INV', N'user_admin_01', N'ACTIVE'
    );
END
GO

-- Default Branch
IF NOT EXISTS (SELECT 1 FROM dbo.branches WHERE id = N'br_main_01')
BEGIN
    INSERT INTO dbo.branches (
        id, org_id, name, code, address, phone, receipt_header, receipt_footer, status
    ) VALUES (
        N'br_main_01', N'org_omnipos_default', N'Main Store Branch', N'BR1',
        N'New Road, Kathmandu, Nepal', N'+977-1-4234567',
        N'OmniPOS Flagship Store - Main Branch', N'Thank you for visiting! Please visit again.', N'ACTIVE'
    );
END
GO

-- Default Register
IF NOT EXISTS (SELECT 1 FROM dbo.registers WHERE id = N'reg_main_01')
BEGIN
    INSERT INTO dbo.registers (
        id, org_id, branch_id, name, code, receipt_prefix, receipt_header, receipt_footer, is_active
    ) VALUES (
        N'reg_main_01', N'org_omnipos_default', N'br_main_01', N'Terminal 1', N'REG1', N'BR1-REG1',
        N'OmniPOS Terminal 01', N'GST/PAN: 987654321', 1
    );
END
GO

-- Default Admin Profile
IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE id = N'user_admin_01')
BEGIN
    INSERT INTO dbo.users (
        id, email, full_name, phone, language, terms_accepted_at, active_org_id
    ) VALUES (
        N'user_admin_01', N'admin@omnipos.store', N'OmniPOS Administrator',
        N'+977-9800000001', N'en', SYSDATETIMEOFFSET(), N'org_omnipos_default'
    );
END
GO

-- Default Owner Membership
IF NOT EXISTS (SELECT 1 FROM dbo.staff_members WHERE id = N'mem_admin_01')
BEGIN
    INSERT INTO dbo.staff_members (
        id, org_id, user_id, branch_id, name, email, role, pin_hash, status, is_active
    ) VALUES (
        N'mem_admin_01', N'org_omnipos_default', N'user_admin_01', N'br_main_01',
        N'OmniPOS Administrator', N'admin@omnipos.store', N'OWNER', N'1234', N'ACTIVE', 1
    );
END
GO

-- Default Store Settings
IF NOT EXISTS (SELECT 1 FROM dbo.store_settings WHERE org_id = N'org_omnipos_default')
BEGIN
    INSERT INTO dbo.store_settings (
        org_id, allow_cash, allow_card, allow_qr, allow_credit, max_discount_percent,
        receipt_header_note, receipt_footer_note, auto_print_receipt, low_stock_threshold,
        negative_stock_behavior, auto_lock_interval, auto_logout_interval, biometric_enabled,
        login_biometric_enabled, enforce_strong_pin, printer_name, printer_connection_type,
        paper_width_mm, scanner_mode, cash_drawer_trigger, language
    ) VALUES (
        N'org_omnipos_default', 1, 1, 1, 1, 20,
        N'Welcome to OmniPOS Store', N'Goods once sold can be exchanged within 7 days.',
        1, 5, N'BLOCK', N'5 minutes', N'Never', 1, 1, 1,
        N'Thermal Receipt Printer', N'BLUETOOTH', 80, N'USB HID Keyboard Emulation Mode', 1, N'en'
    );
END
GO

-- Default Fiscal Year
IF NOT EXISTS (SELECT 1 FROM dbo.fiscal_years WHERE id = N'fy_2081_82')
BEGIN
    INSERT INTO dbo.fiscal_years (
        id, org_id, name, code, start_date, end_date, invoice_prefix, current_invoice_sequence, is_current, is_closed
    ) VALUES (
        N'fy_2081_82', N'org_omnipos_default', N'2081/82', N'81/82',
        '2024-07-16T00:00:00+05:45', '2025-07-15T23:59:59+05:45', N'INV', 100, 1, 0
    );
END
GO

-- Standard Tax Rates
IF NOT EXISTS (SELECT 1 FROM dbo.tax_rates WHERE id = N'tax_vat_13')
BEGIN
    INSERT INTO dbo.tax_rates (id, org_id, name, rate_bps, rate_percent, is_default)
    VALUES (N'tax_vat_13', N'org_omnipos_default', N'VAT 13%', 1300, 13.00, 1);
END
IF NOT EXISTS (SELECT 1 FROM dbo.tax_rates WHERE id = N'tax_zero_0')
BEGIN
    INSERT INTO dbo.tax_rates (id, org_id, name, rate_bps, rate_percent, is_default)
    VALUES (N'tax_zero_0', N'org_omnipos_default', N'Zero Rated (0%)', 0, 0.00, 0);
END
GO

-- Core Product Categories
IF NOT EXISTS (SELECT 1 FROM dbo.categories WHERE id = N'cat_bev')
BEGIN
    INSERT INTO dbo.categories (id, org_id, name, color_hex, icon_name, sort_order)
    VALUES 
        (N'cat_bev', N'org_omnipos_default', N'Beverages', N'#3B82F6', N'coffee', 1),
        (N'cat_grc', N'org_omnipos_default', N'Groceries', N'#10B981', N'shopping_cart', 2),
        (N'cat_snk', N'org_omnipos_default', N'Snacks & Confectionery', N'#F59E0B', N'cake', 3),
        (N'cat_per', N'org_omnipos_default', N'Personal Care', N'#8B5CF6', N'spa', 4),
        (N'cat_ele', N'org_omnipos_default', N'Electronics', N'#EC4899', N'devices', 5);
END
GO

-- Sample Products & Barcodes
IF NOT EXISTS (SELECT 1 FROM dbo.products WHERE id = N'prod_tea_01')
BEGIN
    INSERT INTO dbo.products (
        id, org_id, category_id, tax_rate_id, name, sku, barcode, brand,
        unit, cost_price_in_cents, selling_price_in_cents, tax_rate_percent, min_stock_alert, track_stock, status
    ) VALUES
        (N'prod_tea_01', N'org_omnipos_default', N'cat_bev', N'tax_vat_13', N'Himalayan Organic CTC Tea 500g', N'BEV-TEA-500', N'8901234567890', N'Himalayan Gold', N'pcs', 25000, 35000, 13.00, 10, 1, N'ACTIVE'),
        (N'prod_coffee_01', N'org_omnipos_default', N'cat_bev', N'tax_vat_13', N'Arabica Roasted Beans 250g', N'BEV-COF-250', N'8901234567891', N'Summit Roast', N'pcs', 45000, 65000, 13.00, 5, 1, N'ACTIVE'),
        (N'prod_rice_01', N'org_omnipos_default', N'cat_grc', N'tax_zero_0', N'Premium Basmati Rice 5kg', N'GRC-RIC-5KG', N'8901234567892', N'Annapurna', N'bag', 65000, 85000, 0.00, 8, 1, N'ACTIVE'),
        (N'prod_choc_01', N'org_omnipos_default', N'cat_snk', N'tax_vat_13', N'Dark Artisan Chocolate Bar 80g', N'SNK-CHO-080', N'8901234567893', N'Cocoa Haven', N'pcs', 12000, 18000, 13.00, 15, 1, N'ACTIVE');

    INSERT INTO dbo.item_barcodes (id, org_id, item_id, barcode, pack_qty)
    VALUES
        (N'bc_tea_01', N'org_omnipos_default', N'prod_tea_01', N'8901234567890', 1),
        (N'bc_coffee_01', N'org_omnipos_default', N'prod_coffee_01', N'8901234567891', 1),
        (N'bc_rice_01', N'org_omnipos_default', N'prod_rice_01', N'8901234567892', 1),
        (N'bc_choc_01', N'org_omnipos_default', N'prod_choc_01', N'8901234567893', 1);

    -- Opening inventory stock movements
    INSERT INTO dbo.stock_movements (id, org_id, branch_id, product_id, quantity_delta, type, reference_id, reason, created_by)
    VALUES
        (N'sm_init_01', N'org_omnipos_default', N'br_main_01', N'prod_tea_01', 50, N'OPENING', N'INIT-BATCH-01', N'Initial stock load', N'user_admin_01'),
        (N'sm_init_02', N'org_omnipos_default', N'br_main_01', N'prod_coffee_01', 30, N'OPENING', N'INIT-BATCH-01', N'Initial stock load', N'user_admin_01'),
        (N'sm_init_03', N'org_omnipos_default', N'br_main_01', N'prod_rice_01', 25, N'OPENING', N'INIT-BATCH-01', N'Initial stock load', N'user_admin_01'),
        (N'sm_init_04', N'org_omnipos_default', N'br_main_01', N'prod_choc_01', 60, N'OPENING', N'INIT-BATCH-01', N'Initial stock load', N'user_admin_01');
END
GO

-- Sample Customers (One Walk-in, One Khata Account)
IF NOT EXISTS (SELECT 1 FROM dbo.customers WHERE id = N'cust_walkin_01')
BEGIN
    INSERT INTO dbo.customers (id, org_id, name, phone, email, address, credit_limit_in_cents, current_due_in_cents, status)
    VALUES
        (N'cust_walkin_01', N'org_omnipos_default', N'Standard Walk-in Customer', N'+977-9800000000', N'walkin@omnipos.store', N'Cash Counter', 0, 0, N'ACTIVE'),
        (N'cust_regular_01', N'org_omnipos_default', N'Pradeep Sharma (VIP Khata)', N'+977-9841234567', N'pradeep.sharma@gmail.com', N'Lazimpat, Kathmandu', 5000000, 125000, N'ACTIVE');

    INSERT INTO dbo.customer_credit_terms (id, org_id, customer_id, credit_limit_minor, payment_terms_days, grace_period_days, hold_status)
    VALUES
        (N'cct_regular_01', N'org_omnipos_default', N'cust_regular_01', 5000000, 30, 5, N'ACTIVE');
END
GO
