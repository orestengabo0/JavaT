-- =============================================================================
-- V17 — Seed sample data for demo and API testing
-- =============================================================================
-- All seeded staff/customer portal passwords: Admin@1234
-- BCrypt hash (strength 10) of "Admin@1234":
--   $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
--
-- Fixed UUIDs (use in Postman / Swagger after migrate):
--   Finance user  : f1111111-1111-1111-1111-111111111101
--   Operator user : f1111111-1111-1111-1111-111111111102
--   Customer user : f1111111-1111-1111-1111-111111111103
--   Customer      : a1111111-1111-1111-1111-111111111101
--   Meter         : b1111111-1111-1111-1111-111111111101
--   Tariff        : d1111111-1111-1111-1111-111111111101
--   Reading       : e1111111-1111-1111-1111-111111111101
--
-- Demo flow after seed:
--   1. Login admin@javat.com → POST /api/v1/bills/generate (meter above, 5/2026)
--   2. Login finance@javat.com → PATCH /api/v1/bills/{id}/approve
--   3. POST /api/v1/payments → full or partial payment
--   4. Login customer@javat.com → GET bills, payments, notifications
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Staff + portal users
-- -----------------------------------------------------------------------------

INSERT INTO users (
    id, first_name, last_name, username, email, phone, password, role,
    created_at, updated_at, created_by, updated_by, deleted, status
)
SELECT
    'f1111111-1111-1111-1111-111111111101'::uuid,
    'Finance', 'Officer', 'finance', 'finance@javat.com', '+250788000101',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'FINANCE', NOW(), NOW(), 'system', 'system', FALSE, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'finance@javat.com');

INSERT INTO users (
    id, first_name, last_name, username, email, phone, password, role,
    created_at, updated_at, created_by, updated_by, deleted, status
)
SELECT
    'f1111111-1111-1111-1111-111111111102'::uuid,
    'Meter', 'Operator', 'operator', 'operator@javat.com', '+250788000102',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'OPERATOR', NOW(), NOW(), 'system', 'system', FALSE, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'operator@javat.com');

INSERT INTO users (
    id, first_name, last_name, username, email, phone, password, role,
    created_at, updated_at, created_by, updated_by, deleted, status
)
SELECT
    'f1111111-1111-1111-1111-111111111103'::uuid,
    'Jean Pierre', 'Uwimana', 'jp.uwimana', 'customer@javat.com', '+250788000103',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'CUSTOMER', NOW(), NOW(), 'system', 'system', FALSE, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'customer@javat.com');

-- -----------------------------------------------------------------------------
-- Customer (linked to portal user)
-- -----------------------------------------------------------------------------

INSERT INTO customers (
    id, user_id, full_names, national_id, email, phone, address,
    created_at, updated_at, created_by, updated_by, deleted, status
)
SELECT
    'a1111111-1111-1111-1111-111111111101'::uuid,
    u.id,
    'Jean Pierre Uwimana',
    '1199880012345678',
    'customer@javat.com',
    '+250788123456',
    'KG 15 Ave, Kigali, Rwanda',
    NOW(), NOW(), 'system', 'system', FALSE, 'ACTIVE'
FROM users u
WHERE u.email = 'customer@javat.com'
  AND NOT EXISTS (SELECT 1 FROM customers WHERE email = 'customer@javat.com');

-- -----------------------------------------------------------------------------
-- Water meter
-- -----------------------------------------------------------------------------

INSERT INTO meters (
    id, customer_id, meter_number, meter_type, installation_date,
    created_at, updated_at, created_by, updated_by, deleted, status
)
SELECT
    'b1111111-1111-1111-1111-111111111101'::uuid,
    c.id,
    'WTR-KGL-001234',
    'WATER',
    '2024-03-15'::date,
    NOW(), NOW(), 'system', 'system', FALSE, 'ACTIVE'
FROM customers c
WHERE c.email = 'customer@javat.com'
  AND NOT EXISTS (SELECT 1 FROM meters WHERE meter_number = 'WTR-KGL-001234');

-- -----------------------------------------------------------------------------
-- Active flat water tariff
-- -----------------------------------------------------------------------------

INSERT INTO tariff_versions (
    id, name, meter_type, tariff_type, flat_rate, fixed_service_charge,
    tax_rate, penalty_rate, penalty_grace_days, effective_from, effective_to, active,
    created_at, updated_at, created_by, updated_by, deleted, status
)
SELECT
    'd1111111-1111-1111-1111-111111111101'::uuid,
    'WASAC Water Tariff 2024',
    'WATER',
    'FLAT',
    250.0000,
    1500.00,
    18.00,
    5.00,
    15,
    '2024-01-01'::date,
    NULL,
    TRUE,
    NOW(), NOW(), 'system', 'system', FALSE, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM tariff_versions
    WHERE name = 'WASAC Water Tariff 2024' AND meter_type = 'WATER' AND deleted = FALSE
);

-- -----------------------------------------------------------------------------
-- Meter reading — May 2026 (ready for bill generation)
-- -----------------------------------------------------------------------------

INSERT INTO meter_readings (
    id, meter_id, previous_reading, current_reading, reading_date,
    billing_month, billing_year, captured_by, created_at
)
SELECT
    'e1111111-1111-1111-1111-111111111101'::uuid,
    m.id,
    1250.50,
    1285.75,
    '2026-05-28'::date,
    5,
    2026,
    u.id,
    NOW()
FROM meters m
CROSS JOIN users u
WHERE m.meter_number = 'WTR-KGL-001234'
  AND u.email = 'operator@javat.com'
  AND NOT EXISTS (
      SELECT 1 FROM meter_readings
      WHERE meter_id = m.id AND billing_month = 5 AND billing_year = 2026
  );
