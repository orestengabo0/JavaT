-- =============================================================================
-- V10 — Migrate all primary keys and foreign keys from BIGINT to UUID
-- =============================================================================
-- Preserves existing rows by mapping old BIGINT ids to new UUID values.
-- Safe to run on databases created with V1–V9 (BIGSERIAL/BIGINT).
-- =============================================================================

-- -------------------------------------------------------------------------
-- 1. Add UUID columns and populate
-- -------------------------------------------------------------------------

ALTER TABLE users ADD COLUMN id_uuid UUID;
UPDATE users SET id_uuid = gen_random_uuid();
ALTER TABLE users ALTER COLUMN id_uuid SET NOT NULL;

ALTER TABLE password_reset_tokens ADD COLUMN id_uuid UUID;
UPDATE password_reset_tokens SET id_uuid = gen_random_uuid();
ALTER TABLE password_reset_tokens ALTER COLUMN id_uuid SET NOT NULL;

ALTER TABLE password_reset_tokens ADD COLUMN user_id_uuid UUID;
UPDATE password_reset_tokens prt
SET user_id_uuid = u.id_uuid
FROM users u
WHERE prt.user_id = u.id;
ALTER TABLE password_reset_tokens ALTER COLUMN user_id_uuid SET NOT NULL;

ALTER TABLE email_verification_tokens ADD COLUMN id_uuid UUID;
UPDATE email_verification_tokens SET id_uuid = gen_random_uuid();
ALTER TABLE email_verification_tokens ALTER COLUMN id_uuid SET NOT NULL;

ALTER TABLE email_verification_tokens ADD COLUMN user_id_uuid UUID;
UPDATE email_verification_tokens evt
SET user_id_uuid = u.id_uuid
FROM users u
WHERE evt.user_id = u.id;
ALTER TABLE email_verification_tokens ALTER COLUMN user_id_uuid SET NOT NULL;

ALTER TABLE customers ADD COLUMN id_uuid UUID;
UPDATE customers SET id_uuid = gen_random_uuid();
ALTER TABLE customers ALTER COLUMN id_uuid SET NOT NULL;

ALTER TABLE customers ADD COLUMN user_id_uuid UUID;
UPDATE customers c
SET user_id_uuid = u.id_uuid
FROM users u
WHERE c.user_id = u.id;

ALTER TABLE meters ADD COLUMN id_uuid UUID;
UPDATE meters SET id_uuid = gen_random_uuid();
ALTER TABLE meters ALTER COLUMN id_uuid SET NOT NULL;

ALTER TABLE meters ADD COLUMN customer_id_uuid UUID;
UPDATE meters m
SET customer_id_uuid = c.id_uuid
FROM customers c
WHERE m.customer_id = c.id;
ALTER TABLE meters ALTER COLUMN customer_id_uuid SET NOT NULL;

ALTER TABLE meter_readings ADD COLUMN id_uuid UUID;
UPDATE meter_readings SET id_uuid = gen_random_uuid();
ALTER TABLE meter_readings ALTER COLUMN id_uuid SET NOT NULL;

ALTER TABLE meter_readings ADD COLUMN meter_id_uuid UUID;
UPDATE meter_readings mr
SET meter_id_uuid = m.id_uuid
FROM meters m
WHERE mr.meter_id = m.id;
ALTER TABLE meter_readings ALTER COLUMN meter_id_uuid SET NOT NULL;

ALTER TABLE meter_readings ADD COLUMN captured_by_uuid UUID;
UPDATE meter_readings mr
SET captured_by_uuid = u.id_uuid
FROM users u
WHERE mr.captured_by = u.id;
ALTER TABLE meter_readings ALTER COLUMN captured_by_uuid SET NOT NULL;

-- -------------------------------------------------------------------------
-- 2. Drop foreign keys and old primary keys
-- -------------------------------------------------------------------------

ALTER TABLE meter_readings DROP CONSTRAINT IF EXISTS fk_meter_readings_meter;
ALTER TABLE meter_readings DROP CONSTRAINT IF EXISTS fk_meter_readings_captured_by;
ALTER TABLE meters DROP CONSTRAINT IF EXISTS fk_meters_customer;
ALTER TABLE customers DROP CONSTRAINT IF EXISTS fk_customers_user;
ALTER TABLE password_reset_tokens DROP CONSTRAINT IF EXISTS fk_prt_user;
ALTER TABLE email_verification_tokens DROP CONSTRAINT IF EXISTS fk_evt_user;

ALTER TABLE meter_readings DROP CONSTRAINT IF EXISTS pk_meter_readings;
ALTER TABLE meters DROP CONSTRAINT IF EXISTS pk_meters;
ALTER TABLE customers DROP CONSTRAINT IF EXISTS pk_customers;
ALTER TABLE email_verification_tokens DROP CONSTRAINT IF EXISTS pk_email_verification_tokens;
ALTER TABLE password_reset_tokens DROP CONSTRAINT IF EXISTS pk_password_reset_tokens;
ALTER TABLE users DROP CONSTRAINT IF EXISTS pk_users;

-- -------------------------------------------------------------------------
-- 3. Drop old BIGINT columns and rename UUID columns
-- -------------------------------------------------------------------------

ALTER TABLE meter_readings DROP COLUMN id;
ALTER TABLE meter_readings DROP COLUMN meter_id;
ALTER TABLE meter_readings DROP COLUMN captured_by;
ALTER TABLE meter_readings RENAME COLUMN id_uuid TO id;
ALTER TABLE meter_readings RENAME COLUMN meter_id_uuid TO meter_id;
ALTER TABLE meter_readings RENAME COLUMN captured_by_uuid TO captured_by;

ALTER TABLE meters DROP COLUMN id;
ALTER TABLE meters DROP COLUMN customer_id;
ALTER TABLE meters RENAME COLUMN id_uuid TO id;
ALTER TABLE meters RENAME COLUMN customer_id_uuid TO customer_id;

ALTER TABLE customers DROP COLUMN id;
ALTER TABLE customers DROP COLUMN user_id;
ALTER TABLE customers RENAME COLUMN id_uuid TO id;
ALTER TABLE customers RENAME COLUMN user_id_uuid TO user_id;

ALTER TABLE email_verification_tokens DROP COLUMN id;
ALTER TABLE email_verification_tokens DROP COLUMN user_id;
ALTER TABLE email_verification_tokens RENAME COLUMN id_uuid TO id;
ALTER TABLE email_verification_tokens RENAME COLUMN user_id_uuid TO user_id;

ALTER TABLE password_reset_tokens DROP COLUMN id;
ALTER TABLE password_reset_tokens DROP COLUMN user_id;
ALTER TABLE password_reset_tokens RENAME COLUMN id_uuid TO id;
ALTER TABLE password_reset_tokens RENAME COLUMN user_id_uuid TO user_id;

ALTER TABLE users DROP COLUMN id;
ALTER TABLE users RENAME COLUMN id_uuid TO id;

-- Drop orphaned BIGSERIAL sequences
DROP SEQUENCE IF EXISTS users_id_seq;
DROP SEQUENCE IF EXISTS password_reset_tokens_id_seq;
DROP SEQUENCE IF EXISTS email_verification_tokens_id_seq;
DROP SEQUENCE IF EXISTS customers_id_seq;
DROP SEQUENCE IF EXISTS meters_id_seq;
DROP SEQUENCE IF EXISTS meter_readings_id_seq;

-- -------------------------------------------------------------------------
-- 4. Re-create primary keys and foreign keys
-- -------------------------------------------------------------------------

ALTER TABLE users ADD CONSTRAINT pk_users PRIMARY KEY (id);

ALTER TABLE password_reset_tokens ADD CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id);
ALTER TABLE password_reset_tokens
    ADD CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE email_verification_tokens ADD CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id);
ALTER TABLE email_verification_tokens
    ADD CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE customers ADD CONSTRAINT pk_customers PRIMARY KEY (id);
ALTER TABLE customers
    ADD CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE meters ADD CONSTRAINT pk_meters PRIMARY KEY (id);
ALTER TABLE meters
    ADD CONSTRAINT fk_meters_customer FOREIGN KEY (customer_id) REFERENCES customers (id);

ALTER TABLE meter_readings ADD CONSTRAINT pk_meter_readings PRIMARY KEY (id);
ALTER TABLE meter_readings
    ADD CONSTRAINT fk_meter_readings_meter FOREIGN KEY (meter_id) REFERENCES meters (id);
ALTER TABLE meter_readings
    ADD CONSTRAINT fk_meter_readings_captured_by FOREIGN KEY (captured_by) REFERENCES users (id);
