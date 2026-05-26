-- =============================================================================
-- V4 — Create email_verification_tokens table
-- =============================================================================
-- Stores time-limited tokens sent to users after registration.
-- One token per user (UNIQUE on user_id). The old token is deleted before
-- a new one is issued (e.g. on resend-verification requests).
-- =============================================================================

CREATE TABLE IF NOT EXISTS email_verification_tokens
(
    id          BIGSERIAL       NOT NULL,
    token       VARCHAR(64)     NOT NULL,
    user_id     BIGINT          NOT NULL,
    expires_at  TIMESTAMPTZ     NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id),
    CONSTRAINT uq_evt_token                 UNIQUE      (token),
    CONSTRAINT uq_evt_user_id               UNIQUE      (user_id),
    CONSTRAINT fk_evt_user                  FOREIGN KEY (user_id)
                                                REFERENCES users (id)
                                                ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_evt_token    ON email_verification_tokens (token);
CREATE INDEX IF NOT EXISTS idx_evt_expires  ON email_verification_tokens (expires_at);
