-- =============================================================================
-- V7 — Create customers table
-- =============================================================================

CREATE TABLE IF NOT EXISTS customers
(
    id              BIGSERIAL       NOT NULL,

    user_id         BIGINT,
    full_names      VARCHAR(100)    NOT NULL,
    national_id     VARCHAR(16)     NOT NULL,
    email           VARCHAR(254)    NOT NULL,
    phone           VARCHAR(20)     NOT NULL,
    address         VARCHAR(255)    NOT NULL,

    created_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    deleted         BOOLEAN         NOT NULL    DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      VARCHAR(100),
    status          VARCHAR(20)     NOT NULL    DEFAULT 'ACTIVE',

    CONSTRAINT pk_customers              PRIMARY KEY (id),
    CONSTRAINT uq_customers_national_id  UNIQUE (national_id),
    CONSTRAINT uq_customers_email        UNIQUE (email),
    CONSTRAINT uq_customers_user_id      UNIQUE (user_id),
    CONSTRAINT fk_customers_user         FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_customers_status      CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_customers_email       ON customers (email);
CREATE INDEX IF NOT EXISTS idx_customers_national_id ON customers (national_id);
CREATE INDEX IF NOT EXISTS idx_customers_status      ON customers (status);
CREATE INDEX IF NOT EXISTS idx_customers_deleted     ON customers (deleted);
CREATE INDEX IF NOT EXISTS idx_customers_user_id     ON customers (user_id);
