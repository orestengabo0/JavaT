package com.spring.JavaT.auth;

import com.spring.JavaT.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores a time-limited token used to verify a user's email address.
 *
 * <p>Created during registration. The user clicks the link in the verification
 * email, which calls {@code GET /api/v1/auth/verify-email?token=...}.
 * On success the user's status is promoted from {@code PENDING} to {@code ACTIVE}.
 *
 * <p>One-to-one with {@link User} — each user has at most one pending
 * verification token at a time.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The opaque token sent in the verification email link. */
    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    /** The user this token belongs to. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** UTC timestamp after which this token is no longer valid. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** UTC timestamp of when this token was created. */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /** Returns {@code true} if the token's expiry timestamp is in the past. */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
