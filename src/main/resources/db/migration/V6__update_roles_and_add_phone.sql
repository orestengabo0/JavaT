-- =============================================================================
-- V6 — Utility billing roles + user phone number
-- =============================================================================
-- Migrates template roles (USER, MODERATOR) to exam roles (CUSTOMER, OPERATOR).
-- Adds a required, unique phone column for all users.
-- =============================================================================

-- -------------------------------------------------------------------------
-- Phone column (nullable first, backfill, then NOT NULL)
-- -------------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20);

UPDATE users
SET phone = '+2507000' || LPAD(id::text, 5, '0')
WHERE phone IS NULL;

ALTER TABLE users
    ALTER COLUMN phone SET NOT NULL;

-- -------------------------------------------------------------------------
-- Role migration
-- Drop the legacy CHECK first — otherwise USER→CUSTOMER / MODERATOR→OPERATOR
-- updates are rejected while the old constraint is still active.
-- -------------------------------------------------------------------------
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;

UPDATE users SET role = 'CUSTOMER'  WHERE role = 'USER';
UPDATE users SET role = 'OPERATOR'  WHERE role = 'MODERATOR';

ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('ADMIN', 'OPERATOR', 'FINANCE', 'CUSTOMER'));

-- -------------------------------------------------------------------------
-- Phone uniqueness + lookup index
-- -------------------------------------------------------------------------
ALTER TABLE users
    ADD CONSTRAINT uq_users_phone UNIQUE (phone);

CREATE INDEX IF NOT EXISTS idx_users_phone ON users (phone);

-- Ensure seeded admin has a realistic phone (after unique constraint is in place)
UPDATE users
SET phone = '+250788000001'
WHERE email = 'admin@javat.com';
