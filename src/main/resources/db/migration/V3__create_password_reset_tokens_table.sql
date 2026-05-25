-- =============================================================================
-- V3 — Create password_reset_tokens table
-- =============================================================================
-- Stores time-limited tokens for the forgot-password / reset-password flow.
-- Each user can have at most one active token at a time (old ones are deleted
-- before a new one is issued in AuthService.forgotPassword).
-- =============================================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens
(
    id          BIGSERIAL       NOT NULL,
    token       VARCHAR(64)     NOT NULL,
    user_id     BIGINT          NOT NULL,
    expires_at  TIMESTAMPTZ     NOT NULL,
    used        BOOLEAN         NOT NULL    DEFAULT FALSE,
    created_at  TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT pk_password_reset_tokens     PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_token      UNIQUE      (token),
    CONSTRAINT fk_prt_user                  FOREIGN KEY (user_id)
                                                REFERENCES users (id)
                                                ON DELETE CASCADE
);

-- Index for token lookup (called on every reset-password request)
CREATE INDEX IF NOT EXISTS idx_prt_token    ON password_reset_tokens (token);

-- Index for user lookup (called when deleting old tokens before issuing a new one)
CREATE INDEX IF NOT EXISTS idx_prt_user_id  ON password_reset_tokens (user_id);

-- Index for housekeeping queries that delete expired tokens
CREATE INDEX IF NOT EXISTS idx_prt_expires  ON password_reset_tokens (expires_at);
