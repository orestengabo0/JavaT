-- =============================================================================
-- V12 — Create bills table
-- =============================================================================

CREATE TABLE IF NOT EXISTS bills
(
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    customer_id         UUID            NOT NULL,
    meter_id            UUID            NOT NULL,
    tariff_version_id   UUID            NOT NULL,
    billing_month       INT             NOT NULL,
    billing_year        INT             NOT NULL,
    consumption         DECIMAL(12, 2)  NOT NULL,
    subtotal            DECIMAL(12, 2)  NOT NULL,
    tax_amount          DECIMAL(12, 2)  NOT NULL,
    penalty_amount      DECIMAL(12, 2)  NOT NULL    DEFAULT 0,
    total_amount        DECIMAL(12, 2)  NOT NULL,
    amount_paid         DECIMAL(12, 2)  NOT NULL    DEFAULT 0,
    balance             DECIMAL(12, 2)  NOT NULL,
    bill_status         VARCHAR(20)     NOT NULL    DEFAULT 'PENDING',
    approved_by         UUID,
    approved_at         TIMESTAMPTZ,
    due_date            DATE            NOT NULL,

    created_at          TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted             BOOLEAN         NOT NULL    DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          VARCHAR(100),
    status              VARCHAR(20)     NOT NULL    DEFAULT 'ACTIVE',

    CONSTRAINT pk_bills                   PRIMARY KEY (id),
    CONSTRAINT uq_bill_meter_period         UNIQUE (meter_id, billing_month, billing_year),
    CONSTRAINT fk_bills_customer          FOREIGN KEY (customer_id)       REFERENCES customers (id),
    CONSTRAINT fk_bills_meter               FOREIGN KEY (meter_id)          REFERENCES meters (id),
    CONSTRAINT fk_bills_tariff              FOREIGN KEY (tariff_version_id) REFERENCES tariff_versions (id),
    CONSTRAINT fk_bills_approved_by         FOREIGN KEY (approved_by)       REFERENCES users (id),
    CONSTRAINT chk_bills_month              CHECK (billing_month BETWEEN 1 AND 12),
    CONSTRAINT chk_bills_year               CHECK (billing_year >= 2020),
    CONSTRAINT chk_bills_bill_status        CHECK (bill_status IN ('DRAFT', 'PENDING', 'APPROVED', 'PAID', 'OVERDUE')),
    CONSTRAINT chk_bills_amounts            CHECK (balance >= 0 AND amount_paid >= 0 AND total_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bills_customer_id  ON bills (customer_id);
CREATE INDEX IF NOT EXISTS idx_bills_meter_id     ON bills (meter_id);
CREATE INDEX IF NOT EXISTS idx_bills_period       ON bills (billing_year, billing_month);
CREATE INDEX IF NOT EXISTS idx_bills_bill_status  ON bills (bill_status);
CREATE INDEX IF NOT EXISTS idx_bills_due_date     ON bills (due_date);
