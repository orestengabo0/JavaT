-- =============================================================================
-- V19 — Rebrand seeded @javat.com addresses to @wasac.gov.rw
-- =============================================================================
-- Safe for DBs seeded before this rebrand. No-op if addresses already updated.
-- Login after migrate: admin@wasac.gov.rw (password unchanged: Admin@1234)
-- =============================================================================

UPDATE users
SET email = REPLACE(email, '@javat.com', '@wasac.gov.rw')
WHERE email LIKE '%@javat.com';

UPDATE customers
SET email = REPLACE(email, '@javat.com', '@wasac.gov.rw')
WHERE email LIKE '%@javat.com';
