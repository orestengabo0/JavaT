-- =============================================================================
-- V15 — Payment trigger: mark bill PAID + payment confirmation notification
-- Balance updates are applied in PaymentService; this trigger finalises PAID
-- status and notifies the customer when the outstanding balance reaches zero.
-- =============================================================================

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

CREATE OR REPLACE FUNCTION fn_on_payment_insert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_balance DECIMAL(12, 2);
BEGIN
    SELECT balance INTO v_balance FROM bills WHERE id = NEW.bill_id;

    IF v_balance <= 0 THEN
        CALL sp_mark_bill_paid(NEW.bill_id);
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_payment_insert
    AFTER INSERT ON payments
    FOR EACH ROW
    EXECUTE FUNCTION fn_on_payment_insert();
