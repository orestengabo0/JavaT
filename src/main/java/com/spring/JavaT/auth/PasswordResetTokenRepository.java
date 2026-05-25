package com.spring.JavaT.auth;

import com.spring.JavaT.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /** Deletes all tokens belonging to a user — called before issuing a new one. */
    void deleteAllByUser(User user);

    /** Housekeeping: removes all expired tokens. Can be called by a scheduled task. */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteAllExpired(Instant now);
}
