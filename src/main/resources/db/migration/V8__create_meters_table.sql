-- =============================================================================
-- V8 — Create meters table
-- =============================================================================

CREATE TABLE IF NOT EXISTS meters
(
    id                  BIGSERIAL       NOT NULL,
    customer_id         BIGINT          NOT NULL,
    meter_number        VARCHAR(20)     NOT NULL,
    meter_type          VARCHAR(20)     NOT NULL,
    installation_date   DATE            NOT NULL,

    created_at          TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    deleted             BOOLEAN         NOT NULL    DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          VARCHAR(100),
    status              VARCHAR(20)     NOT NULL    DEFAULT 'ACTIVE',

    CONSTRAINT pk_meters               PRIMARY KEY (id),
    CONSTRAINT uq_meters_meter_number  UNIQUE (meter_number),
    CONSTRAINT fk_meters_customer      FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT chk_meters_type         CHECK (meter_type IN ('WATER', 'ELECTRICITY')),
    CONSTRAINT chk_meters_status       CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_meters_customer_id  ON meters (customer_id);
CREATE INDEX IF NOT EXISTS idx_meters_meter_number ON meters (meter_number);
CREATE INDEX IF NOT EXISTS idx_meters_meter_type   ON meters (meter_type);
CREATE INDEX IF NOT EXISTS idx_meters_status       ON meters (status);
CREATE INDEX IF NOT EXISTS idx_meters_deleted      ON meters (deleted);
