-- =============================================================================
-- V16 — Refresh DB routines to exam-aligned implementations
-- (UUID keys + bill_status column; idempotent CREATE OR REPLACE)
-- Original routines live in V13 (bill notify) and V15 (payment / PAID).
-- =============================================================================

-- 7.1 Bill generation notification trigger function
CREATE OR REPLACE FUNCTION fn_notify_bill_generated()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO notifications (customer_id, bill_id, message, channel, email_sent, read, created_at)
    SELECT
        NEW.customer_id,
        NEW.id,
        format(
            'Dear %s, Your %s/%s utility bill of %s FRW has been successfully processed.',
            c.full_names,
            NEW.billing_month,
            NEW.billing_year,
            NEW.total_amount
        ),
        'IN_APP',
        FALSE,
        FALSE,
        NOW()
    FROM customers c
    WHERE c.id = NEW.customer_id;

    RETURN NEW;
END;
$$;

-- 7.2 Stored procedure — mark bill PAID and notify on full payment
CREATE OR REPLACE PROCEDURE sp_mark_bill_paid(p_bill_id UUID)
LANGUAGE plpgsql
AS $$
DECLARE
    v_bill     bills%ROWTYPE;
    v_customer customers%ROWTYPE;
BEGIN
    SELECT * INTO v_bill FROM bills WHERE id = p_bill_id FOR UPDATE;

    IF v_bill.balance > 0 THEN
        RETURN;
    END IF;

    IF v_bill.bill_status = 'PAID' THEN
        RETURN;
    END IF;

    UPDATE bills
    SET bill_status = 'PAID',
        updated_at  = NOW()
    WHERE id = p_bill_id;

    SELECT * INTO v_customer FROM customers WHERE id = v_bill.customer_id;

    INSERT INTO notifications (customer_id, bill_id, message, channel, email_sent, read, created_at)
    VALUES (
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
