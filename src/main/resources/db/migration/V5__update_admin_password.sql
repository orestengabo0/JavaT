-- =============================================================================
-- V5 — Update admin user password
-- =============================================================================
-- Updates the password hash for the seeded admin account (admin@javat.com).
-- The new hash was generated with BCrypt strength 12.
-- =============================================================================

UPDATE users
SET    password   = '$2a$12$LFzYDzFjW5/sobDavnnG5ON314MRp5dvkjOfbwVXGROl/Y.S4HMji',
       updated_at = NOW(),
       updated_by = 'system'
WHERE  email = 'admin@javat.com';
