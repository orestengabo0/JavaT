-- =============================================================================
-- V23 — Bill notification: include billing month/year of the unpaid bill
--         that caused the late payment penalty
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_notify_bill_generated()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_customer_name VARCHAR(100);
    v_message       TEXT;
    v_prior_month   INT;
    v_prior_year    INT;
BEGIN
    SELECT full_names INTO v_customer_name
    FROM customers
    WHERE id = NEW.customer_id;

    IF NEW.penalty_amount > 0 THEN
        SELECT billing_month, billing_year
        INTO v_prior_month, v_prior_year
        FROM bills
        WHERE meter_id = NEW.meter_id
          AND deleted = FALSE
          AND bill_status <> 'CANCELLED'
          AND (billing_year < NEW.billing_year
               OR (billing_year = NEW.billing_year AND billing_month < NEW.billing_month))
        ORDER BY billing_year DESC, billing_month DESC
        LIMIT 1;

        IF v_prior_month IS NOT NULL THEN
            v_message := format(
                'Dear %s, Your %s/%s utility bill of %s FRW has been successfully processed. This includes a late payment penalty of %s FRW for your unpaid %s/%s bill.',
                v_customer_name,
                NEW.billing_month,
                NEW.billing_year,
                NEW.total_amount,
                NEW.penalty_amount,
                v_prior_month,
                v_prior_year
            );
        ELSE
            v_message := format(
                'Dear %s, Your %s/%s utility bill of %s FRW has been successfully processed. This includes a late payment penalty of %s FRW.',
                v_customer_name,
                NEW.billing_month,
                NEW.billing_year,
                NEW.total_amount,
                NEW.penalty_amount
            );
        END IF;
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
