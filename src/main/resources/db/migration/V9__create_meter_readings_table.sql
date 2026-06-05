-- =============================================================================
-- V9 — Create meter_readings table
-- =============================================================================

CREATE TABLE IF NOT EXISTS meter_readings
(
    id                  BIGSERIAL       NOT NULL,
    meter_id            BIGINT          NOT NULL,
    previous_reading    DECIMAL(12, 2)  NOT NULL,
    current_reading     DECIMAL(12, 2)  NOT NULL,
    reading_date        DATE            NOT NULL,
    billing_month       INT             NOT NULL,
    billing_year        INT             NOT NULL,
    captured_by         BIGINT          NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT pk_meter_readings             PRIMARY KEY (id),
    CONSTRAINT fk_meter_readings_meter       FOREIGN KEY (meter_id)      REFERENCES meters (id),
    CONSTRAINT fk_meter_readings_captured_by FOREIGN KEY (captured_by)   REFERENCES users (id),
    CONSTRAINT uq_reading_meter_period       UNIQUE (meter_id, billing_month, billing_year),
    CONSTRAINT chk_reading_order             CHECK (current_reading > previous_reading),
    CONSTRAINT chk_billing_month             CHECK (billing_month BETWEEN 1 AND 12),
    CONSTRAINT chk_billing_year              CHECK (billing_year >= 2020)
);

CREATE INDEX IF NOT EXISTS idx_meter_readings_meter_id    ON meter_readings (meter_id);
CREATE INDEX IF NOT EXISTS idx_meter_readings_period      ON meter_readings (billing_year, billing_month);
CREATE INDEX IF NOT EXISTS idx_meter_readings_reading_date ON meter_readings (reading_date);
