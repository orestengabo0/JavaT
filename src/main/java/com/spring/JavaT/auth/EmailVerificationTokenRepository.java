package com.spring.JavaT.auth;

import com.spring.JavaT.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    /** Deletes any existing token for this user before issuing a new one. */
    void deleteByUser(User user);

    /** Housekeeping: removes all expired tokens. Called by a scheduled task. */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now")
    void deleteAllExpired(Instant now);
}
