-- =============================================================================
-- Utility Billing System — Database Routines (Design Reference)
-- =============================================================================
-- This file documents the PostgreSQL triggers and stored procedures required
-- by Task 6. Actual migration will be applied in Flyway V13 during Phase 7.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Notification on bill generation (INSERT on bills)
-- -----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_notify_bill_generated()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_customer_name VARCHAR(100);
BEGIN
    SELECT full_names INTO v_customer_name
    FROM customers
    WHERE id = NEW.customer_id;

    INSERT INTO notifications (
        customer_id,
        bill_id,
        message,
        channel,
        email_sent,
        read,
        created_at
    ) VALUES (
        NEW.customer_id,
        NEW.id,
        format(
            'Dear %s, Your %s/%s utility bill of %s FRW has been successfully processed.',
            v_customer_name,
            NEW.billing_month,
            NEW.billing_year,
            NEW.total_amount
        ),
        'IN_APP',
        FALSE,
        FALSE,
        NOW()
    );

    RETURN NEW;
END;
$$;

-- Applied in migration:
-- CREATE TRIGGER trg_bill_generated_notify
--     AFTER INSERT ON bills
--     FOR EACH ROW
--     EXECUTE FUNCTION fn_notify_bill_generated();


-- -----------------------------------------------------------------------------
-- 2. Stored procedure — mark bill as PAID and notify customer
-- -----------------------------------------------------------------------------

CREATE OR REPLACE PROCEDURE sp_mark_bill_paid(p_bill_id BIGINT)
LANGUAGE plpgsql
AS $$
DECLARE
    v_bill        bills%ROWTYPE;
    v_customer    customers%ROWTYPE;
BEGIN
    SELECT * INTO v_bill FROM bills WHERE id = p_bill_id FOR UPDATE;

    IF v_bill.balance > 0 THEN
        RETURN; -- not fully paid yet
    END IF;

    UPDATE bills
    SET status = 'PAID'
    WHERE id = p_bill_id;

    SELECT * INTO v_customer FROM customers WHERE id = v_bill.customer_id;

    INSERT INTO notifications (
        customer_id,
        bill_id,
        message,
        channel,
        email_sent,
        read,
        created_at
    ) VALUES (
        v_bill.customer_id,
        p_bill_id,
        format(
            'Dear %s, Your %s/%s utility bill of %s FRW has been fully paid. Thank you.',
            v_customer.full_names,
            v_bill.billing_month,
            v_bill.billing_year,
            v_bill.total_amount
        ),
        'IN_APP',
        FALSE,
        FALSE,
        NOW()
    );
END;
$$;


-- -----------------------------------------------------------------------------
-- 3. Trigger on payment INSERT — update balance and call sp_mark_bill_paid
-- -----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_on_payment_insert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_new_balance DECIMAL(12, 2);
BEGIN
    UPDATE bills
    SET
        amount_paid = amount_paid + NEW.amount_paid,
        balance     = balance - NEW.amount_paid
    WHERE id = NEW.bill_id
    RETURNING balance INTO v_new_balance;

    IF v_new_balance <= 0 THEN
        CALL sp_mark_bill_paid(NEW.bill_id);
    END IF;

    RETURN NEW;
END;
$$;

-- Applied in migration:
-- CREATE TRIGGER trg_payment_insert
--     AFTER INSERT ON payments
--     FOR EACH ROW
--     EXECUTE FUNCTION fn_on_payment_insert();


-- -----------------------------------------------------------------------------
-- 4. Relationship integrity notes (for ERD cross-reference)
-- -----------------------------------------------------------------------------

-- users (1) ── (0..1) customers       : customers.user_id → users.id
-- customers (1) ── (N) meters         : meters.customer_id → customers.id
-- meters (1) ── (N) meter_readings    : meter_readings.meter_id → meters.id
-- meters (1) ── (N) bills             : bills.meter_id → meters.id
-- customers (1) ── (N) bills          : bills.customer_id → customers.id
-- tariff_versions (1) ── (N) tiers    : tariff_tiers.tariff_version_id → tariff_versions.id
-- tariff_versions (1) ── (N) bills    : bills.tariff_version_id → tariff_versions.id
-- bills (1) ── (N) payments           : payments.bill_id → bills.id
-- customers (1) ── (N) notifications  : notifications.customer_id → customers.id
