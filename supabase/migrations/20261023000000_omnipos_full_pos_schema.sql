-- =============================================================================
-- OmniPOS / ShopFlow: Complete PostgreSQL / Supabase Schema
-- Includes POS Catalog, Orders, Sales, Shifts, Inventory, Khata & Audit
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Categories
CREATE TABLE IF NOT EXISTS public.categories (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    color_hex TEXT NOT NULL DEFAULT '#3B82F6',
    icon_name TEXT NOT NULL DEFAULT 'category',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, name)
);

ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Categories viewable by org members" ON public.categories FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 2. Tax Rates
CREATE TABLE IF NOT EXISTS public.tax_rates (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    rate_bps INT NOT NULL, -- 1300 = 13%
    rate_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.tax_rates ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Tax rates viewable by org members" ON public.tax_rates FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 3. Products
CREATE TABLE IF NOT EXISTS public.products (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    category_id TEXT REFERENCES public.categories(id),
    tax_rate_id TEXT REFERENCES public.tax_rates(id),
    name TEXT NOT NULL,
    sku TEXT NOT NULL,
    barcode TEXT,
    brand TEXT,
    description TEXT,
    unit TEXT NOT NULL DEFAULT 'pcs',
    cost_price_in_cents BIGINT NOT NULL DEFAULT 0,
    selling_price_in_cents BIGINT NOT NULL DEFAULT 0,
    tax_rate_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    min_stock_alert INT NOT NULL DEFAULT 5,
    track_stock BOOLEAN NOT NULL DEFAULT true,
    image_path TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, sku)
);

CREATE INDEX IF NOT EXISTS idx_products_barcode ON public.products(organization_id, barcode);
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Products viewable by org members" ON public.products FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 4. Item Barcodes
CREATE TABLE IF NOT EXISTS public.item_barcodes (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    item_id TEXT NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    barcode TEXT NOT NULL,
    pack_qty INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, barcode)
);

ALTER TABLE public.item_barcodes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Barcodes viewable by org members" ON public.item_barcodes FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 5. Customers
CREATE TABLE IF NOT EXISTS public.customers (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT,
    address TEXT,
    tax_id TEXT,
    credit_limit_in_cents BIGINT NOT NULL DEFAULT 50000,
    current_due_in_cents BIGINT NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BLOCKED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, phone)
);

ALTER TABLE public.customers ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Customers viewable by org members" ON public.customers FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 6. Customer Credit Terms
CREATE TABLE IF NOT EXISTS public.customer_credit_terms (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    customer_id TEXT NOT NULL REFERENCES public.customers(id) ON DELETE CASCADE,
    credit_limit_minor BIGINT NOT NULL DEFAULT 0,
    payment_terms_days INT NOT NULL DEFAULT 30,
    grace_period_days INT NOT NULL DEFAULT 0,
    interest_rate_bps INT NOT NULL DEFAULT 0,
    hold_status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (hold_status IN ('ACTIVE', 'OVERDUE_HOLD', 'SUSPENDED')),
    approved_by TEXT,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, customer_id)
);

ALTER TABLE public.customer_credit_terms ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Credit terms viewable by org members" ON public.customer_credit_terms FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 7. Customer Ledger (Khata)
CREATE TABLE IF NOT EXISTS public.customer_ledger (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    customer_id TEXT NOT NULL REFERENCES public.customers(id),
    sale_id TEXT,
    entry_type TEXT NOT NULL,
    amount_in_cents BIGINT NOT NULL,
    balance_after_in_cents BIGINT NOT NULL,
    reference_type TEXT,
    reference_id TEXT,
    idempotency_key TEXT,
    notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.customer_ledger ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Customer ledger viewable by org members" ON public.customer_ledger FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 8. Stock Movements
CREATE TABLE IF NOT EXISTS public.stock_movements (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES public.branches(id),
    product_id TEXT NOT NULL REFERENCES public.products(id),
    quantity_delta INT NOT NULL,
    type TEXT NOT NULL,
    reference_id TEXT NOT NULL DEFAULT '',
    reason TEXT,
    idempotency_key TEXT,
    created_by TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_stock_movements_lookup ON public.stock_movements(organization_id, branch_id, product_id, created_at);
ALTER TABLE public.stock_movements ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Stock movements viewable by org members" ON public.stock_movements FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 9. Sales
CREATE TABLE IF NOT EXISTS public.sales (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES public.branches(id),
    register_id UUID NOT NULL REFERENCES public.registers(id),
    invoice_no TEXT NOT NULL,
    fiscal_year TEXT NOT NULL DEFAULT '',
    customer_id TEXT REFERENCES public.customers(id),
    subtotal_in_cents BIGINT NOT NULL,
    discount_in_cents BIGINT NOT NULL DEFAULT 0,
    tax_in_cents BIGINT NOT NULL DEFAULT 0,
    grand_total_in_cents BIGINT NOT NULL,
    payment_status TEXT NOT NULL DEFAULT 'COMPLETED' CHECK (payment_status IN ('COMPLETED', 'VOIDED', 'PARTIALLY_REFUNDED', 'REFUNDED')),
    currency_code TEXT NOT NULL DEFAULT 'NPR',
    tax_mode TEXT NOT NULL DEFAULT 'EXCLUSIVE',
    idempotency_key TEXT,
    created_by TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, invoice_no)
);

ALTER TABLE public.sales ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Sales viewable by org members" ON public.sales FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 10. Sale Items
CREATE TABLE IF NOT EXISTS public.sale_items (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    sale_id TEXT NOT NULL REFERENCES public.sales(id) ON DELETE CASCADE,
    product_id TEXT NOT NULL REFERENCES public.products(id),
    product_name TEXT NOT NULL,
    sku TEXT,
    unit_price_in_cents BIGINT NOT NULL,
    quantity INT NOT NULL,
    discount_in_cents BIGINT NOT NULL DEFAULT 0,
    tax_in_cents BIGINT NOT NULL DEFAULT 0,
    line_total_in_cents BIGINT NOT NULL
);

ALTER TABLE public.sale_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Sale items viewable by org members" ON public.sale_items FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 11. Sale Payments
CREATE TABLE IF NOT EXISTS public.sale_payments (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    sale_id TEXT NOT NULL REFERENCES public.sales(id) ON DELETE CASCADE,
    payment_type TEXT NOT NULL CHECK (payment_type IN ('CASH', 'CARD', 'QR', 'WALLET', 'CREDIT')),
    amount_in_cents BIGINT NOT NULL,
    tendered_in_cents BIGINT,
    change_given_in_cents BIGINT NOT NULL DEFAULT 0,
    reference_no TEXT NOT NULL DEFAULT '',
    provider TEXT,
    provider_status TEXT NOT NULL DEFAULT 'CONFIRMED'
);

ALTER TABLE public.sale_payments ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Sale payments viewable by org members" ON public.sale_payments FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 12. Shifts
CREATE TABLE IF NOT EXISTS public.shifts (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES public.branches(id),
    register_id UUID NOT NULL REFERENCES public.registers(id),
    cashier_uid TEXT NOT NULL,
    cashier_name TEXT NOT NULL,
    business_date TEXT NOT NULL DEFAULT '',
    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    opening_float_in_cents BIGINT NOT NULL DEFAULT 0,
    opening_expected_in_cents BIGINT NOT NULL DEFAULT 0,
    opening_gap_in_cents BIGINT NOT NULL DEFAULT 0,
    opening_gap_reason TEXT NOT NULL DEFAULT '',
    closed_at TIMESTAMPTZ,
    counted_cash_in_cents BIGINT,
    expected_cash_in_cents BIGINT,
    closing_float_in_cents BIGINT NOT NULL DEFAULT 0,
    deposit_amount_in_cents BIGINT NOT NULL DEFAULT 0,
    variance_in_cents BIGINT,
    variance_reason TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSING', 'PENDING_REVIEW', 'FLAGGED', 'APPROVED', 'FORCE_CLOSED')),
    closed_by TEXT NOT NULL DEFAULT '',
    review_status TEXT NOT NULL DEFAULT 'PENDING',
    reviewed_by TEXT,
    reviewed_at TIMESTAMPTZ,
    review_note TEXT,
    version INT NOT NULL DEFAULT 1,
    last_device_id TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT ''
);

ALTER TABLE public.shifts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Shifts viewable by org members" ON public.shifts FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 13. Cash Movements
CREATE TABLE IF NOT EXISTS public.cash_movements (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    shift_id TEXT NOT NULL REFERENCES public.shifts(id),
    movement_type TEXT NOT NULL CHECK (movement_type IN ('PAID_IN', 'PAID_OUT', 'SAFE_DROP', 'CREDIT_COLLECTION')),
    tender TEXT NOT NULL DEFAULT 'CASH',
    amount_in_cents BIGINT NOT NULL,
    reason TEXT NOT NULL,
    created_by TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.cash_movements ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Cash movements viewable by org members" ON public.cash_movements FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 14. Store Settings
CREATE TABLE IF NOT EXISTS public.store_settings (
    organization_id UUID NOT NULL PRIMARY KEY REFERENCES public.organizations(id) ON DELETE CASCADE,
    allow_cash BOOLEAN NOT NULL DEFAULT true,
    allow_card BOOLEAN NOT NULL DEFAULT true,
    allow_qr BOOLEAN NOT NULL DEFAULT true,
    allow_credit BOOLEAN NOT NULL DEFAULT true,
    max_discount_percent INT NOT NULL DEFAULT 15,
    receipt_header_note TEXT NOT NULL DEFAULT 'Thank you for shopping!',
    receipt_footer_note TEXT NOT NULL DEFAULT 'Please come again!',
    auto_print_receipt BOOLEAN NOT NULL DEFAULT true,
    low_stock_threshold INT NOT NULL DEFAULT 5,
    negative_stock_behavior TEXT NOT NULL DEFAULT 'BLOCK',
    auto_lock_interval TEXT NOT NULL DEFAULT '5 minutes',
    auto_logout_interval TEXT NOT NULL DEFAULT 'Never',
    biometric_enabled BOOLEAN NOT NULL DEFAULT false,
    login_biometric_enabled BOOLEAN NOT NULL DEFAULT false,
    enforce_strong_pin BOOLEAN NOT NULL DEFAULT true,
    printer_name TEXT NOT NULL DEFAULT '',
    printer_connection_type TEXT NOT NULL DEFAULT 'BLUETOOTH',
    paper_width_mm INT NOT NULL DEFAULT 80,
    scanner_mode TEXT NOT NULL DEFAULT 'USB HID Keyboard Emulation Mode',
    cash_drawer_trigger BOOLEAN NOT NULL DEFAULT true,
    backup_email TEXT NOT NULL DEFAULT '',
    last_backup_at TIMESTAMPTZ,
    language TEXT NOT NULL DEFAULT 'en',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.store_settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Settings viewable by org members" ON public.store_settings FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- 15. Audit Logs
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    actor_uid TEXT NOT NULL,
    actor_name TEXT NOT NULL,
    actor_role TEXT NOT NULL,
    action TEXT NOT NULL,
    details TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Audit logs viewable by org members" ON public.audit_logs FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- -----------------------------------------------------------------------------
-- 16. Server Function: fn_get_product_stock
-- Calculates real-time physical stock as SUM of stock movements
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.fn_get_product_stock(
    p_org_id UUID,
    p_product_id TEXT
)
RETURNS BIGINT
LANGUAGE sql
STABLE
SECURITY DEFINER
AS $$
    SELECT COALESCE(SUM(quantity_delta), 0)::BIGINT
    FROM public.stock_movements
    WHERE organization_id = p_org_id
      AND product_id = p_product_id;
$$;

-- -----------------------------------------------------------------------------
-- 17. Server Function: record_customer_ledger_entry
-- Records khata entry and atomically updates customer current due amount
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.record_customer_ledger_entry(
    p_org_id UUID,
    p_customer_id TEXT,
    p_entry_type TEXT,
    p_amount_in_cents BIGINT,
    p_sale_id TEXT DEFAULT NULL,
    p_notes TEXT DEFAULT ''
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_current_due BIGINT;
    v_new_due BIGINT;
    v_ledger_id TEXT;
BEGIN
    SELECT current_due_in_cents INTO v_current_due
    FROM public.customers
    WHERE id = p_customer_id AND organization_id = p_org_id
    FOR UPDATE;

    IF v_current_due IS NULL THEN
        RAISE EXCEPTION 'Customer not found.';
    END IF;

    IF p_entry_type IN ('CHARGE', 'SALE_CREDIT') THEN
        v_new_due := v_current_due + p_amount_in_cents;
    ELSE
        v_new_due := GREATEST(0, v_current_due - p_amount_in_cents);
    END IF;

    v_ledger_id := gen_random_uuid()::text;

    INSERT INTO public.customer_ledger (
        id, organization_id, customer_id, sale_id, entry_type,
        amount_in_cents, balance_after_in_cents, notes
    ) VALUES (
        v_ledger_id, p_org_id, p_customer_id, p_sale_id, p_entry_type,
        p_amount_in_cents, v_new_due, p_notes
    );

    UPDATE public.customers
    SET current_due_in_cents = v_new_due,
        updated_at = now()
    WHERE id = p_customer_id AND organization_id = p_org_id;

    RETURN jsonb_build_object(
        'status', 'SUCCESS',
        'ledger_id', v_ledger_id,
        'new_due_in_cents', v_new_due
    );
END;
$$;

