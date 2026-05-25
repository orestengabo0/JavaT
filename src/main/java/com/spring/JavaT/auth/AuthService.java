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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService                 emailService;

    @Value("${app.auth.password-reset-token-expiry-minutes:15}")
    private int passwordResetTokenExpiryMinutes;

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

        userRepository.save(user);

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
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Generates a cryptographically secure URL-safe token (48 random bytes → 64 Base64 chars).
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
