package com.spring.JavaT.auth;

import com.spring.JavaT.auth.dto.AuthResponse;
import com.spring.JavaT.auth.dto.ForgotPasswordRequest;
import com.spring.JavaT.auth.dto.LoginRequest;
import com.spring.JavaT.auth.dto.RegisterRequest;
import com.spring.JavaT.auth.dto.ResetPasswordRequest;
import com.spring.JavaT.exception.BusinessException;
import com.spring.JavaT.exception.DuplicateResourceException;
import com.spring.JavaT.exception.ResourceNotFoundException;
import com.spring.JavaT.notification.EmailService;
import com.spring.JavaT.security.JwtProperties;
import com.spring.JavaT.security.JwtService;
import com.spring.JavaT.user.Role;
import com.spring.JavaT.user.User;
import com.spring.JavaT.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
/**
 * Handles user registration and login.
 *
 * <p>Both operations return an {@link AuthResponse} containing access and refresh tokens
 * so the client is immediately authenticated after registering.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository               userRepository;
    private final PasswordEncoder              passwordEncoder;
    private final JwtService                   jwtService;
    private final JwtProperties                jwtProperties;
    private final AuthenticationManager        authenticationManager;
    private final AuthMapper                   authMapper;
    private final PasswordResetTokenRepository       passwordResetTokenRepository;
    private final EmailVerificationTokenRepository   emailVerificationTokenRepository;
    private final EmailService                 emailService;

    @Value("${app.auth.password-reset-token-expiry-minutes:15}")
    private int passwordResetTokenExpiryMinutes;

    @Value("${app.auth.verification-token-expiry-hours:24}")
    private int verificationTokenExpiryHours;

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Creates a new user account and returns authentication tokens.
     *
     * @param request the registration payload
     * @return access and refresh tokens for the newly created user
     * @throws DuplicateResourceException if the email or username is already taken
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        // Map all simple fields; password and role are set explicitly below
        User user = authMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        // New accounts start as PENDING until email is verified
        user.setStatus(com.spring.JavaT.common.EntityStatus.PENDING);

        userRepository.save(user);

        // Issue a verification token and send the email asynchronously
        issueAndSendVerificationToken(user);

        return buildAuthResponse(user);
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    /**
     * Authenticates a user by email and password and returns tokens.
     *
     * <p>Delegates credential verification to Spring Security's
     * {@link AuthenticationManager}, which calls {@code UserDetailsServiceImpl}
     * and the {@code PasswordEncoder}. If authentication fails, Spring throws
     * {@code BadCredentialsException} which the global handler maps to 401.
     *
     * @param request the login payload
     * @return access and refresh tokens
     */
    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException if credentials are wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found after successful authentication"));

        // Block unverified accounts with a clear, actionable message
        if (com.spring.JavaT.common.EntityStatus.PENDING.equals(user.getStatus())) {
            throw new BusinessException(
                    "Email address not verified. Please check your inbox and click the verification link.",
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "EMAIL_NOT_VERIFIED"
            );
        }

        return buildAuthResponse(user);
    }

    // -------------------------------------------------------------------------
    // Token builder
    // -------------------------------------------------------------------------

    private AuthResponse buildAuthResponse(User user) {
        // Embed the role as a custom claim so it's available without a DB lookup
        Map<String, Object> extraClaims = Map.of("role", user.getRole().name());

        String accessToken  = jwtService.generateAccessToken(extraClaims, user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getExpirationMs() / 1000)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    // -------------------------------------------------------------------------
    // Password reset
    // -------------------------------------------------------------------------

    /**
     * Initiates a password reset by generating a token and sending a reset email.
     *
     * <p>Always returns successfully even if the email is not found — this prevents
     * user enumeration attacks (an attacker cannot tell whether an account exists).
     *
     * @param request contains the email address to reset
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            // Invalidate any existing tokens for this user
            passwordResetTokenRepository.deleteAllByUser(user);

            String rawToken = generateSecureToken();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(rawToken)
                    .user(user)
                    .expiresAt(Instant.now().plus(passwordResetTokenExpiryMinutes, ChronoUnit.MINUTES))
                    .build();

            passwordResetTokenRepository.save(resetToken);

            // Fire-and-forget — runs on the email thread pool
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), rawToken);
        });
    }

    /**
     * Completes a password reset by validating the token and updating the password.
     *
     * @param request contains the token and the new password
     * @throws ResourceNotFoundException if the token does not exist
     * @throws BusinessException         if the token has expired or was already used
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Password reset token not found or already used"));

        if (resetToken.isExpiredOrUsed()) {
            throw new BusinessException(
                    "Password reset token has expired or has already been used. Please request a new one.",
                    HttpStatus.BAD_REQUEST
            );
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used — prevents replay attacks
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // -------------------------------------------------------------------------
    // Email verification
    // -------------------------------------------------------------------------

    /**
     * Verifies a user's email address using the token from the verification email.
     *
     * <p>On success the user's status is promoted from {@code PENDING} to {@code ACTIVE}
     * and the token is deleted so it cannot be reused.
     *
     * @param token the raw token from the email link query parameter
     * @throws ResourceNotFoundException if the token does not exist
     * @throws BusinessException         if the token has expired
     */
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Verification link is invalid or has already been used"));

        if (verificationToken.isExpired()) {
            throw new BusinessException(
                    "Verification link has expired. Please request a new one.",
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "VERIFICATION_TOKEN_EXPIRED"
            );
        }

        User user = verificationToken.getUser();
        user.setStatus(com.spring.JavaT.common.EntityStatus.ACTIVE);
        userRepository.save(user);

        // Delete the token — it's single-use
        emailVerificationTokenRepository.delete(verificationToken);
    }

    /**
     * Resends the verification email for an unverified account.
     *
     * <p>Always returns successfully even if the email is not found or the account
     * is already verified — prevents user enumeration.
     *
     * @param email the email address to resend to
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (com.spring.JavaT.common.EntityStatus.PENDING.equals(user.getStatus())) {
                // Delete any existing token before issuing a fresh one
                emailVerificationTokenRepository.deleteByUser(user);
                issueAndSendVerificationToken(user);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a verification token for the given user and fires the email asynchronously.
     */
    private void issueAndSendVerificationToken(User user) {
        String rawToken = generateSecureToken();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(rawToken)
                .user(user)
                .expiresAt(Instant.now().plus(verificationTokenExpiryHours, ChronoUnit.HOURS))
                .build();

        emailVerificationTokenRepository.save(verificationToken);
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), rawToken);
    }

    /**
     * Generates a cryptographically secure URL-safe token (48 random bytes → 64 Base64 chars).
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
