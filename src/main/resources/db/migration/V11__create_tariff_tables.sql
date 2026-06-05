-- =============================================================================
-- V11 — Create tariff_versions and tariff_tiers tables
-- =============================================================================

CREATE TABLE IF NOT EXISTS tariff_versions
(
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    name                    VARCHAR(100)    NOT NULL,
    meter_type              VARCHAR(20)     NOT NULL,
    tariff_type             VARCHAR(10)     NOT NULL,
    flat_rate               DECIMAL(12, 4),
    fixed_service_charge    DECIMAL(12, 2)  NOT NULL,
    tax_rate                DECIMAL(5, 2)   NOT NULL,
    penalty_rate            DECIMAL(5, 2)   NOT NULL,
    penalty_grace_days      INT             NOT NULL,
    effective_from          DATE            NOT NULL,
    effective_to            DATE,
    active                  BOOLEAN         NOT NULL    DEFAULT TRUE,

    created_at              TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),
    deleted                 BOOLEAN         NOT NULL    DEFAULT FALSE,
    deleted_at              TIMESTAMPTZ,
    deleted_by              VARCHAR(100),
    status                  VARCHAR(20)     NOT NULL    DEFAULT 'ACTIVE',

    CONSTRAINT pk_tariff_versions           PRIMARY KEY (id),
    CONSTRAINT chk_tariff_meter_type        CHECK (meter_type IN ('WATER', 'ELECTRICITY')),
    CONSTRAINT chk_tariff_type              CHECK (tariff_type IN ('FLAT', 'TIERED')),
    CONSTRAINT chk_tariff_tax_rate          CHECK (tax_rate >= 0 AND tax_rate <= 100),
    CONSTRAINT chk_tariff_penalty_rate      CHECK (penalty_rate >= 0 AND penalty_rate <= 100),
    CONSTRAINT chk_tariff_grace_days        CHECK (penalty_grace_days >= 0 AND penalty_grace_days <= 90),
    CONSTRAINT chk_tariff_effective_range   CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE TABLE IF NOT EXISTS tariff_tiers
(
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    tariff_version_id   UUID            NOT NULL,
    min_units           DECIMAL(12, 2)  NOT NULL,
    max_units           DECIMAL(12, 2),
    rate_per_unit       DECIMAL(12, 4)  NOT NULL,

    CONSTRAINT pk_tariff_tiers              PRIMARY KEY (id),
    CONSTRAINT fk_tariff_tiers_version      FOREIGN KEY (tariff_version_id)
                                                REFERENCES tariff_versions (id)
                                                ON DELETE CASCADE,
    CONSTRAINT chk_tier_min_units           CHECK (min_units >= 0),
    CONSTRAINT chk_tier_rate                  CHECK (rate_per_unit > 0),
    CONSTRAINT chk_tier_range                 CHECK (max_units IS NULL OR max_units >= min_units)
);

CREATE INDEX IF NOT EXISTS idx_tariff_versions_meter_type   ON tariff_versions (meter_type);
CREATE INDEX IF NOT EXISTS idx_tariff_versions_effective    ON tariff_versions (effective_from, effective_to);
CREATE INDEX IF NOT EXISTS idx_tariff_versions_active       ON tariff_versions (active);
CREATE INDEX IF NOT EXISTS idx_tariff_tiers_version_id      ON tariff_tiers (tariff_version_id);
