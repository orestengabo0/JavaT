-- =============================================================================
-- V14 — Create payments table
-- =============================================================================

CREATE TABLE IF NOT EXISTS payments
(
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    bill_id             UUID            NOT NULL,
    amount_paid         DECIMAL(12, 2)  NOT NULL,
    payment_method      VARCHAR(30)     NOT NULL,
    payment_date        DATE            NOT NULL,
    reference_number    VARCHAR(64)     NOT NULL,
    recorded_by         UUID            NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_payments                    PRIMARY KEY (id),
    CONSTRAINT uq_payments_reference_number   UNIQUE (reference_number),
    CONSTRAINT fk_payments_bill               FOREIGN KEY (bill_id)       REFERENCES bills (id),
    CONSTRAINT fk_payments_recorded_by        FOREIGN KEY (recorded_by)   REFERENCES users (id),
    CONSTRAINT chk_payments_amount            CHECK (amount_paid > 0),
    CONSTRAINT chk_payments_method              CHECK (payment_method IN ('CASH', 'MOBILE_MONEY', 'BANK_TRANSFER', 'CARD'))
);

CREATE INDEX IF NOT EXISTS idx_payments_bill_id ON payments (bill_id);
CREATE INDEX IF NOT EXISTS idx_payments_date    ON payments (payment_date);
