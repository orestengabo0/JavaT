-- =============================================================================
-- V13 — Notifications table + bill generation trigger (Task 6 partial)
-- =============================================================================

CREATE TABLE IF NOT EXISTS notifications
(
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    customer_id     UUID            NOT NULL,
    bill_id         UUID,
    message         TEXT            NOT NULL,
    channel         VARCHAR(20)     NOT NULL    DEFAULT 'IN_APP',
    email_sent      BOOLEAN         NOT NULL    DEFAULT FALSE,
    read            BOOLEAN         NOT NULL    DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT pk_notifications           PRIMARY KEY (id),
    CONSTRAINT fk_notifications_customer  FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_notifications_bill      FOREIGN KEY (bill_id)     REFERENCES bills (id),
    CONSTRAINT chk_notifications_channel  CHECK (channel IN ('IN_APP', 'EMAIL'))
);

CREATE INDEX IF NOT EXISTS idx_notifications_customer_id ON notifications (customer_id);
CREATE INDEX IF NOT EXISTS idx_notifications_bill_id     ON notifications (bill_id);

-- -----------------------------------------------------------------------------
-- Trigger: insert notification when a bill is generated
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

CREATE TRIGGER trg_bill_generated_notify
    AFTER INSERT ON bills
    FOR EACH ROW
    EXECUTE FUNCTION fn_notify_bill_generated();
