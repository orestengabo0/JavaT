-- =============================================================================
-- V1 — Create users table
-- =============================================================================
-- Derived from:
--   BaseEntity  : id, created_at, updated_at, created_by, updated_by,
--                 deleted, deleted_at, deleted_by, status
--   User entity : first_name, last_name, username, email, password, role
-- =============================================================================

CREATE TABLE IF NOT EXISTS users
(
    -- -------------------------------------------------------------------------
    -- Primary key
    -- BIGSERIAL = BIGINT + auto-increment sequence (maps to GenerationType.IDENTITY)
    -- -------------------------------------------------------------------------
    id          BIGSERIAL       NOT NULL,

    -- -------------------------------------------------------------------------
    -- User-specific columns
    -- -------------------------------------------------------------------------
    first_name  VARCHAR(50)     NOT NULL,
    last_name   VARCHAR(50)     NOT NULL,
    username    VARCHAR(50)     NOT NULL,
    email       VARCHAR(254)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,   -- BCrypt hash is always 60 chars; 255 gives headroom
    role        VARCHAR(20)     NOT NULL,   -- USER | MODERATOR | ADMIN

    -- -------------------------------------------------------------------------
    -- Audit timestamps  (BaseEntity — @CreatedDate / @LastModifiedDate)
    -- TIMESTAMPTZ stores timezone-aware instants; maps to java.time.Instant
    -- -------------------------------------------------------------------------
    created_at  TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL    DEFAULT NOW(),

    -- -------------------------------------------------------------------------
    -- Audit principals  (BaseEntity — @CreatedBy / @LastModifiedBy)
    -- -------------------------------------------------------------------------
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),

    -- -------------------------------------------------------------------------
    -- Soft delete  (BaseEntity)
    -- -------------------------------------------------------------------------
    deleted     BOOLEAN         NOT NULL    DEFAULT FALSE,
    deleted_at  TIMESTAMPTZ,
    deleted_by  VARCHAR(100),

    -- -------------------------------------------------------------------------
    -- Lifecycle status  (BaseEntity — EntityStatus enum stored as string)
    -- -------------------------------------------------------------------------
    status      VARCHAR(20)     NOT NULL    DEFAULT 'ACTIVE',

    -- -------------------------------------------------------------------------
    -- Constraints
    -- -------------------------------------------------------------------------
    CONSTRAINT pk_users             PRIMARY KEY (id),
    CONSTRAINT uq_users_email       UNIQUE      (email),
    CONSTRAINT uq_users_username    UNIQUE      (username),
    CONSTRAINT chk_users_role       CHECK       (role   IN ('USER', 'MODERATOR', 'ADMIN')),
    CONSTRAINT chk_users_status     CHECK       (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING'))
);

-- -------------------------------------------------------------------------
-- Indexes
-- -------------------------------------------------------------------------

-- Email is the login identifier — looked up on every authentication
CREATE INDEX IF NOT EXISTS idx_users_email      ON users (email);

-- Username is looked up on profile updates and duplicate checks
CREATE INDEX IF NOT EXISTS idx_users_username   ON users (username);

-- Most queries filter out soft-deleted rows
CREATE INDEX IF NOT EXISTS idx_users_deleted    ON users (deleted);

-- Status-based filtering (e.g. list only ACTIVE users)
CREATE INDEX IF NOT EXISTS idx_users_status     ON users (status);
