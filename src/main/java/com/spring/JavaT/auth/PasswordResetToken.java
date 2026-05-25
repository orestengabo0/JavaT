package com.spring.JavaT.auth;

import com.spring.JavaT.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores a time-limited token used for password reset.
 *
 * <p>A new token is created on every "forgot password" request.
 * Old tokens for the same user are deleted before the new one is saved,
 * so a user can only have one active reset token at a time.
 *
 * <p>Does NOT extend {@link com.spring.JavaT.common.BaseEntity} — this is a
 * short-lived record that doesn't need audit fields, soft-delete, or status.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The opaque token sent to the user's email. */
    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    /** The user this token belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** UTC timestamp after which this token is no longer valid. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Whether this token has already been used. */
    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

    /** UTC timestamp of when this token was created. */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns {@code true} if the token has expired or has already been used. */
    public boolean isExpiredOrUsed() {
        return used || Instant.now().isAfter(expiresAt);
    }
}
