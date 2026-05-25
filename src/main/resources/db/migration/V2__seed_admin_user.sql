-- =============================================================================
-- V2 — Seed default admin user
-- =============================================================================
-- Creates a single ADMIN account for initial access.
-- This user can then promote other users via PATCH /api/v1/users/{id}/role.
--
-- Credentials:
--   Email    : admin@javat.com
--   Password : Admin@1234
--   Role     : ADMIN
--
-- The password hash below is a BCrypt (strength 10) hash of "Admin@1234".
-- CHANGE THIS PASSWORD immediately after first login in any real environment.
-- =============================================================================

INSERT INTO users (
    first_name,
    last_name,
    username,
    email,
    password,
    role,
    created_at,
    updated_at,
    created_by,
    updated_by,
    deleted,
    status
)
VALUES (
    'Admin',
    'User',
    'admin',
    'admin@javat.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- Admin@1234
    'ADMIN',
    NOW(),
    NOW(),
    'system',
    'system',
    FALSE,
    'ACTIVE'
)
ON CONFLICT (email) DO NOTHING;
-- ON CONFLICT ensures re-running this migration (e.g. after a restore) is safe
