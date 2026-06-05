-- =============================================================================
-- V22 — Mention late payment penalty in bill notification when penalty_amount > 0
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_notify_bill_generated()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_customer_name VARCHAR(100);
    v_message       TEXT;
BEGIN
    SELECT full_names INTO v_customer_name
    FROM customers
    WHERE id = NEW.customer_id;

    IF NEW.penalty_amount > 0 THEN
        v_message := format(
            'Dear %s, Your %s/%s utility bill of %s FRW has been successfully processed. This includes a late payment penalty of %s FRW.',
            v_customer_name,
            NEW.billing_month,
            NEW.billing_year,
            NEW.total_amount,
            NEW.penalty_amount
        );
    ELSE
        v_message := format(
            'Dear %s, Your %s/%s utility bill of %s FRW has been successfully processed.',
            v_customer_name,
            NEW.billing_month,
            NEW.billing_year,
            NEW.total_amount
        );
    END IF;

    INSERT INTO notifications (customer_id, bill_id, message, channel, email_sent, read, created_at)
    VALUES (NEW.customer_id, NEW.id, v_message, 'IN_APP', FALSE, FALSE, NOW());

    RETURN NEW;
END;
$$;
