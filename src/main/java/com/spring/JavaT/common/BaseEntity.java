package com.spring.JavaT.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Abstract base class for all JPA entities in this application.
 *
 * <p>Every entity that extends this class automatically gets:
 * <ul>
 *   <li><b>id</b>          — auto-generated primary key (IDENTITY strategy)</li>
 *   <li><b>createdAt</b>   — timestamp set once on INSERT, never updated</li>
 *   <li><b>updatedAt</b>   — timestamp updated on every UPDATE</li>
 *   <li><b>createdBy</b>   — username/ID of the principal who created the record</li>
 *   <li><b>updatedBy</b>   — username/ID of the principal who last modified the record</li>
 *   <li><b>deleted</b>     — soft-delete flag; {@code true} means logically deleted</li>
 *   <li><b>status</b>      — lifecycle status ({@link EntityStatus})</li>
 * </ul>
 *
 * <p>{@link AuditingEntityListener} is registered here so subclasses don't need to
 * declare it themselves. JPA auditing must be enabled via {@code @EnableJpaAuditing}
 * in a configuration class (see {@code JpaConfig}).
 *
 * <p>Usage:
 * <pre>
 * {@literal @}Entity
 * {@literal @}Table(name = "users")
 * public class User extends BaseEntity {
 *     // domain-specific fields only
 * }
 * </pre>
 *
 * <p><b>Soft delete:</b> use {@link #softDelete()} to mark a record as deleted
 * instead of issuing a {@code DELETE} statement. Repositories should filter on
 * {@code deleted = false} by default — see the note on {@code @Where} below.
 *
 * <p><b>Note on {@code @SQLRestriction}:</b> you can add
 * {@code @SQLRestriction("deleted = false")} to each entity subclass (or a shared
 * repository base) to automatically exclude soft-deleted rows from all queries.
 * It is intentionally omitted here so subclasses can opt in explicitly, giving
 * admin repositories the ability to query deleted records when needed.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    // -------------------------------------------------------------------------
    // Primary key
    // -------------------------------------------------------------------------

    /**
     * Auto-generated surrogate primary key.
     *
     * <p>{@code IDENTITY} delegates key generation to the database column
     * (e.g. PostgreSQL {@code BIGSERIAL} / {@code GENERATED ALWAYS AS IDENTITY}).
     * Switch to {@code SEQUENCE} with a named sequence if you need batch inserts
     * without per-row round-trips.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    // -------------------------------------------------------------------------
    // Audit timestamps
    // -------------------------------------------------------------------------

    /**
     * UTC timestamp of when this record was first persisted.
     * Set automatically by {@link AuditingEntityListener}; never updated after INSERT.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * UTC timestamp of the most recent update to this record.
     * Set automatically by {@link AuditingEntityListener} on every UPDATE.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // -------------------------------------------------------------------------
    // Audit principals
    // -------------------------------------------------------------------------

    /**
     * Username or user ID of the principal who created this record.
     * Populated automatically by {@link AuditingEntityListener} via
     * {@code AuditAwareImpl}. Stored as a string to remain decoupled from the
     * User entity (avoids a circular FK dependency).
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    /**
     * Username or user ID of the principal who last modified this record.
     * Populated automatically by {@link AuditingEntityListener} via
     * {@code AuditAwareImpl}.
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // -------------------------------------------------------------------------
    // Soft delete
    // -------------------------------------------------------------------------

    /**
     * Soft-delete flag. When {@code true} the record is considered logically
     * deleted and should be excluded from normal queries.
     *
     * <p>Never set this field directly — use {@link #softDelete()} and
     * {@link #restore()} so that {@code status} is kept in sync.
     */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    /**
     * UTC timestamp of when this record was soft-deleted.
     * {@code null} when the record has not been deleted.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Username or user ID of the principal who soft-deleted this record.
     * {@code null} when the record has not been deleted.
     */
    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    /**
     * Lifecycle status of this record.
     * Stored as a string ({@code EnumType.STRING}) so that adding new enum
     * constants never corrupts existing rows.
     * Defaults to {@link EntityStatus#ACTIVE} on creation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntityStatus status = EntityStatus.ACTIVE;

    // -------------------------------------------------------------------------
    // Soft-delete helpers
    // -------------------------------------------------------------------------

    /**
     * Marks this record as soft-deleted and sets its status to {@link EntityStatus#INACTIVE}.
     *
     * <p>Call this from a service method instead of issuing a repository {@code delete()}.
     * The caller is responsible for persisting the change (e.g. the entity is already
     * managed inside a transaction, or the service calls {@code save(entity)}).
     *
     * @param deletedByUser the username or ID of the principal performing the deletion
     */
    public void softDelete(String deletedByUser) {
        this.deleted   = true;
        this.deletedAt = Instant.now();
        this.deletedBy = deletedByUser;
        this.status    = EntityStatus.INACTIVE;
    }

    /**
     * Restores a soft-deleted record and sets its status back to {@link EntityStatus#ACTIVE}.
     */
    public void restore() {
        this.deleted   = false;
        this.deletedAt = null;
        this.deletedBy = null;
        this.status    = EntityStatus.ACTIVE;
    }

    /**
     * Returns {@code true} if this record has been soft-deleted.
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Returns {@code true} if this record is currently active.
     */
    public boolean isActive() {
        return EntityStatus.ACTIVE.equals(status) && !deleted;
    }
}
