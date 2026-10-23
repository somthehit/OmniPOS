-- =============================================================================
-- ShopFlow POS: Multi-Tenant Auth, Onboarding & Device Security Migration
-- =============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -----------------------------------------------------------------------------
-- 1. Profiles Table (Derived from Google Sign-In & Supabase Auth)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    avatar_url TEXT,
    phone TEXT,
    language TEXT NOT NULL DEFAULT 'en',
    terms_accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- -----------------------------------------------------------------------------
-- 2. Organizations Table (Strict Multi-Tenant Boundary)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name TEXT NOT NULL,
    legal_name TEXT,
    business_type TEXT NOT NULL,
    country_code TEXT NOT NULL DEFAULT 'NP',
    currency_code TEXT NOT NULL DEFAULT 'NPR',
    timezone_id TEXT NOT NULL DEFAULT 'Asia/Kathmandu',
    tax_id TEXT,
    tax_mode TEXT NOT NULL DEFAULT 'EXCLUSIVE' CHECK (tax_mode IN ('INCLUSIVE', 'EXCLUSIVE', 'NONE')),
    owner_id UUID NOT NULL REFERENCES public.profiles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.organizations ENABLE ROW LEVEL SECURITY;

-- -----------------------------------------------------------------------------
-- 3. Branches Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    code TEXT NOT NULL,
    address TEXT,
    phone TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, code)
);

ALTER TABLE public.branches ENABLE ROW LEVEL SECURITY;

-- -----------------------------------------------------------------------------
-- 4. Registers (Tills) Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.registers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES public.branches(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    receipt_prefix TEXT NOT NULL,
    receipt_header TEXT,
    receipt_footer TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(branch_id, receipt_prefix)
);

ALTER TABLE public.registers ENABLE ROW LEVEL SECURITY;

-- -----------------------------------------------------------------------------
-- 5. Memberships Table (Access Gate: Google Auth != Organization Access)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MANAGER', 'STOREKEEPER', 'CASHIER')),
    branch_id UUID REFERENCES public.branches(id), -- NULL means all branches
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    pin_hash TEXT, -- SHA-256 / bcrypt hash for offline till unlocking
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, user_id)
);

ALTER TABLE public.memberships ENABLE ROW LEVEL SECURITY;

-- -----------------------------------------------------------------------------
-- 6. Registered Devices Table (Per-Organization Device Registry)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    device_fingerprint TEXT NOT NULL,
    device_name TEXT NOT NULL,
    registered_by UUID NOT NULL REFERENCES public.profiles(id),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'REVOKED')),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(organization_id, device_fingerprint)
);

ALTER TABLE public.devices ENABLE ROW LEVEL SECURITY;

-- -----------------------------------------------------------------------------
-- 7. Invitations Table (Single-Use, Expirable, Strict Email Match)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES public.branches(id),
    email TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'STOREKEEPER', 'CASHIER')),
    code TEXT NOT NULL UNIQUE,
    invited_by UUID NOT NULL REFERENCES public.profiles(id),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED')),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '7 days'),
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.invitations ENABLE ROW LEVEL SECURITY;

-- -----------------------------------------------------------------------------
-- 8. Idempotency Log Table (Guarantees double-taps never create 2 organizations)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.onboarding_idempotency_keys (
    key TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    organization_id UUID REFERENCES public.organizations(id) ON DELETE CASCADE,
    response_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- 9. Row Level Security Policies for Multi-Tenancy
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.current_user_has_active_membership(p_org_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.memberships
        WHERE organization_id = p_org_id
          AND user_id = auth.uid()
          AND status = 'ACTIVE'
    );
$$;

CREATE POLICY "Org members can view their organization"
    ON public.organizations FOR SELECT
    USING (public.current_user_has_active_membership(id));

CREATE POLICY "Org members can view branches"
    ON public.branches FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

CREATE POLICY "Org members can view registers"
    ON public.registers FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

CREATE POLICY "Org members can view memberships"
    ON public.memberships FOR SELECT
    USING (public.current_user_has_active_membership(organization_id));

-- -----------------------------------------------------------------------------
-- 10. Last Owner Protection Trigger
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.trg_prevent_last_owner_removal()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    owner_count INT;
BEGIN
    IF (OLD.role = 'OWNER' AND (NEW.role <> 'OWNER' OR NEW.status <> 'ACTIVE')) THEN
        SELECT COUNT(*) INTO owner_count
        FROM public.memberships
        WHERE organization_id = OLD.organization_id
          AND role = 'OWNER'
          AND status = 'ACTIVE'
          AND id <> OLD.id;

        IF owner_count = 0 THEN
            RAISE EXCEPTION 'Cannot remove, revoke or downgrade the only active owner of the organization. Transfer ownership first.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS check_last_owner_on_update ON public.memberships;
CREATE TRIGGER check_last_owner_on_update
    BEFORE UPDATE OR DELETE ON public.memberships
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_prevent_last_owner_removal();

-- -----------------------------------------------------------------------------
-- 11. Atomic Server Function: create_organization
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.create_organization(
    p_idempotency_key TEXT,
    p_display_name TEXT,
    p_legal_name TEXT DEFAULT NULL,
    p_business_type TEXT DEFAULT 'RETAIL',
    p_country_code TEXT DEFAULT 'NP',
    p_currency_code TEXT DEFAULT 'NPR',
    p_timezone_id TEXT DEFAULT 'Asia/Kathmandu',
    p_tax_id TEXT DEFAULT NULL,
    p_branch_name TEXT DEFAULT 'Main Branch',
    p_branch_address TEXT DEFAULT NULL,
    p_till_name TEXT DEFAULT 'Register 1',
    p_receipt_header TEXT DEFAULT NULL,
    p_receipt_footer TEXT DEFAULT 'Thank you for your business!',
    p_pin_hash TEXT DEFAULT NULL,
    p_device_fingerprint TEXT DEFAULT NULL,
    p_device_name TEXT DEFAULT 'Primary Register'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_user_id UUID;
    v_existing_org_id UUID;
    v_existing_payload JSONB;
    v_org_id UUID;
    v_branch_id UUID;
    v_register_id UUID;
    v_membership_id UUID;
    v_device_id UUID;
    v_result JSONB;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthenticated request. Google sign-in required.';
    END IF;

    -- 1. Check idempotency key to prevent double creations
    IF p_idempotency_key IS NOT NULL THEN
        SELECT response_payload INTO v_existing_payload
        FROM public.onboarding_idempotency_keys
        WHERE key = p_idempotency_key AND user_id = v_user_id;

        IF v_existing_payload IS NOT NULL THEN
            RETURN v_existing_payload;
        END IF;
    END IF;

    -- 2. Create organization
    INSERT INTO public.organizations (
        display_name,
        legal_name,
        business_type,
        country_code,
        currency_code,
        timezone_id,
        tax_id,
        owner_id
    ) VALUES (
        p_display_name,
        COALESCE(p_legal_name, p_display_name),
        p_business_type,
        COALESCE(p_country_code, 'NP'),
        COALESCE(p_currency_code, 'NPR'),
        COALESCE(p_timezone_id, 'Asia/Kathmandu'),
        p_tax_id,
        v_user_id
    ) RETURNING id INTO v_org_id;

    -- 3. Create initial Branch
    INSERT INTO public.branches (
        organization_id,
        name,
        code,
        address
    ) VALUES (
        v_org_id,
        COALESCE(p_branch_name, 'Main Branch'),
        'BR1',
        p_branch_address
    ) RETURNING id INTO v_branch_id;

    -- 4. Create initial Register (Till)
    INSERT INTO public.registers (
        organization_id,
        branch_id,
        name,
        receipt_prefix,
        receipt_header,
        receipt_footer
    ) VALUES (
        v_org_id,
        v_branch_id,
        COALESCE(p_till_name, 'Register 1'),
        'BR1-REG1',
        COALESCE(p_receipt_header, p_display_name),
        COALESCE(p_receipt_footer, 'Thank you for your business!')
    ) RETURNING id INTO v_register_id;

    -- 5. Create Owner Membership
    INSERT INTO public.memberships (
        organization_id,
        user_id,
        role,
        branch_id,
        status,
        pin_hash
    ) VALUES (
        v_org_id,
        v_user_id,
        'OWNER',
        NULL, -- NULL allows access to all branches
        'ACTIVE',
        p_pin_hash
    ) RETURNING id INTO v_membership_id;

    -- 6. Register Device (if device fingerprint provided)
    IF p_device_fingerprint IS NOT NULL THEN
        INSERT INTO public.devices (
            organization_id,
            device_fingerprint,
            device_name,
            registered_by,
            status
        ) VALUES (
            v_org_id,
            p_device_fingerprint,
            COALESCE(p_device_name, 'Primary POS Register'),
            v_user_id,
            'ACTIVE'
        ) RETURNING id INTO v_device_id;
    END IF;

    -- Construct output payload
    v_result := jsonb_build_object(
        'organization_id', v_org_id,
        'organization_name', p_display_name,
        'currency_code', COALESCE(p_currency_code, 'NPR'),
        'timezone_id', COALESCE(p_timezone_id, 'Asia/Kathmandu'),
        'branch_id', v_branch_id,
        'branch_code', 'BR1',
        'register_id', v_register_id,
        'register_prefix', 'BR1-REG1',
        'role', 'OWNER',
        'created_at', now()
    );

    -- Store idempotency record
    IF p_idempotency_key IS NOT NULL THEN
        INSERT INTO public.onboarding_idempotency_keys (
            key, user_id, organization_id, response_payload
        ) VALUES (
            p_idempotency_key, v_user_id, v_org_id, v_result
        );
    END IF;

    RETURN v_result;
END;
$$;

-- -----------------------------------------------------------------------------
-- 12. Server Function: accept_invitation
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.accept_invitation(
    p_code TEXT,
    p_pin_hash TEXT DEFAULT NULL,
    p_device_fingerprint TEXT DEFAULT NULL,
    p_device_name TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_user_id UUID;
    v_user_email TEXT;
    v_invitation RECORD;
    v_membership_id UUID;
    v_device_id UUID;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthenticated request. Sign in before accepting an invitation.';
    END IF;

    -- Get signed-in user email
    SELECT email INTO v_user_email FROM public.profiles WHERE id = v_user_id;
    IF v_user_email IS NULL THEN
        SELECT email INTO v_user_email FROM auth.users WHERE id = v_user_id;
    END IF;

    -- Find matching pending invitation
    SELECT * INTO v_invitation
    FROM public.invitations
    WHERE code = UPPER(TRIM(p_code))
      AND status = 'PENDING';

    IF v_invitation IS NULL THEN
        RAISE EXCEPTION 'Invalid or expired invitation code.';
    END IF;

    -- Strict email verification: Refuse if signed-in email doesn't match invited email
    IF LOWER(TRIM(v_invitation.email)) <> LOWER(TRIM(v_user_email)) THEN
        RAISE EXCEPTION 'Invitation was issued to %, but you are signed in as %. Sign in with the matching account to accept.',
            v_invitation.email, v_user_email;
    END IF;

    IF v_invitation.expires_at < now() THEN
        UPDATE public.invitations SET status = 'EXPIRED' WHERE id = v_invitation.id;
        RAISE EXCEPTION 'This invitation code expired on %.', v_invitation.expires_at;
    END IF;

    -- Create or update membership with invited role & branch (locked)
    INSERT INTO public.memberships (
        organization_id,
        user_id,
        role,
        branch_id,
        status,
        pin_hash
    ) VALUES (
        v_invitation.organization_id,
        v_user_id,
        v_invitation.role,
        v_invitation.branch_id,
        'ACTIVE',
        p_pin_hash
    )
    ON CONFLICT (organization_id, user_id) DO UPDATE SET
        role = EXCLUDED.role,
        branch_id = EXCLUDED.branch_id,
        status = 'ACTIVE',
        pin_hash = COALESCE(EXCLUDED.pin_hash, memberships.pin_hash),
        updated_at = now()
    RETURNING id INTO v_membership_id;

    -- Mark invitation as accepted (single-use)
    UPDATE public.invitations
    SET status = 'ACCEPTED',
        used_at = now()
    WHERE id = v_invitation.id;

    -- Register device if fingerprint provided
    IF p_device_fingerprint IS NOT NULL THEN
        INSERT INTO public.devices (
            organization_id,
            device_fingerprint,
            device_name,
            registered_by,
            status
        ) VALUES (
            v_invitation.organization_id,
            p_device_fingerprint,
            COALESCE(p_device_name, 'Staff Mobile POS'),
            v_user_id,
            'ACTIVE'
        )
        ON CONFLICT (organization_id, device_fingerprint) DO UPDATE SET
            last_seen_at = now(),
            status = 'ACTIVE';
    END IF;

    RETURN jsonb_build_object(
        'status', 'SUCCESS',
        'organization_id', v_invitation.organization_id,
        'role', v_invitation.role,
        'branch_id', v_invitation.branch_id
    );
END;
$$;

-- -----------------------------------------------------------------------------
-- 13. Server Function: get_user_memberships
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_user_memberships(
    p_device_fingerprint TEXT DEFAULT NULL
)
RETURNS TABLE (
    organization_id UUID,
    organization_name TEXT,
    currency_code TEXT,
    timezone_id TEXT,
    role TEXT,
    status TEXT,
    branch_id UUID,
    device_status TEXT,
    pin_configured BOOLEAN
)
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
    SELECT 
        o.id AS organization_id,
        o.display_name AS organization_name,
        o.currency_code,
        o.timezone_id,
        m.role,
        m.status,
        m.branch_id,
        COALESCE(d.status, 'UNREGISTERED') AS device_status,
        (m.pin_hash IS NOT NULL) AS pin_configured
    FROM public.memberships m
    JOIN public.organizations o ON o.id = m.organization_id
    LEFT JOIN public.devices d ON d.organization_id = o.id 
                               AND d.device_fingerprint = p_device_fingerprint
    WHERE m.user_id = auth.uid()
    ORDER BY m.created_at DESC;
$$;
