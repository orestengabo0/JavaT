-- =============================================================================
-- V20 — Fix seeded demo account passwords to Admin@1234
-- =============================================================================
-- Earlier migrations used an incorrect BCrypt hash for "Admin@1234".
-- Correct hash (BCrypt strength 10):
--   $2a$10$9pN0DQBWCDfR69zUrOs/He5TGRa.ukHlga6Xwwbpb6xvtUVNfje3G
-- =============================================================================

UPDATE users
SET password   = '$2a$10$9pN0DQBWCDfR69zUrOs/He5TGRa.ukHlga6Xwwbpb6xvtUVNfje3G',
    updated_at = NOW(),
    updated_by = 'system'
WHERE email IN (
    'admin@wasac.gov.rw',
    'finance@wasac.gov.rw',
    'operator@wasac.gov.rw',
    'customer@wasac.gov.rw',
    'admin@javat.com',
    'finance@javat.com',
    'operator@javat.com',
    'customer@javat.com'
);
