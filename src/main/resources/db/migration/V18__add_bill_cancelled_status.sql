-- =============================================================================
-- V18 — Add CANCELLED bill status and allow re-generation after cancel
-- =============================================================================

ALTER TABLE bills DROP CONSTRAINT IF EXISTS chk_bills_bill_status;

ALTER TABLE bills
    ADD CONSTRAINT chk_bills_bill_status
        CHECK (bill_status IN ('DRAFT', 'PENDING', 'APPROVED', 'PAID', 'OVERDUE', 'CANCELLED'));

-- Replace table-wide unique with partial index so a cancelled bill frees the period
ALTER TABLE bills DROP CONSTRAINT IF EXISTS uq_bill_meter_period;

CREATE UNIQUE INDEX IF NOT EXISTS uq_bill_meter_period_active
    ON bills (meter_id, billing_month, billing_year)
    WHERE deleted = FALSE AND bill_status <> 'CANCELLED';
