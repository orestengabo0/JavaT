package com.spring.JavaT.auth;

import com.spring.JavaT.auth.dto.AuthResponse;
import com.spring.JavaT.auth.dto.ForgotPasswordRequest;
import com.spring.JavaT.auth.dto.LoginRequest;
import com.spring.JavaT.auth.dto.RegisterRequest;
import com.spring.JavaT.auth.dto.ResetPasswordRequest;
import com.spring.JavaT.common.ApiResponse;
import com.spring.JavaT.common.ResponseBuilder;
import com.spring.JavaT.common.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints — publicly accessible (no JWT required).
 *
 * <p>All paths under {@code /api/v1/auth/**} are whitelisted in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account.
     *
     * <p>Returns 201 Created with access and refresh tokens so the client is
     * immediately authenticated without a separate login call.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Validated(ValidationGroups.OnCreate.class) @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.register(request);
        return ResponseBuilder.created(response, "Account created successfully", httpRequest);
    }

    /**
     * Authenticates an existing user and returns tokens.
     */
    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Validated @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.login(request);
        return ResponseBuilder.ok(response, "Login successful", httpRequest);
    }

    /**
     * Initiates a password reset. Always returns 200 to prevent user enumeration.
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        authService.forgotPassword(request);
        return ResponseBuilder.ok(
                "If an account with that email exists, a password reset link has been sent.",
                httpRequest);
    }
    
    /**
     * Completes a password reset using the token from the email.
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the token from email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        authService.resetPassword(request);
        return ResponseBuilder.ok("Password has been reset successfully. Please log in.", httpRequest);
    }

    /**
     * Verifies a user's email address using the token from the verification email.
     * The token arrives as a query parameter from the link clicked in the email.
     */
    @GetMapping("/verify-email")
    @Operation(summary = "Verify email address using the token from the verification email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token,
            HttpServletRequest httpRequest) {

        authService.verifyEmail(token);
        return ResponseBuilder.ok("Email verified successfully. You can now log in.", httpRequest);
    }

    /**
     * Resends the verification email for an unverified account.
     * Always returns 200 to prevent user enumeration.
     */
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend the email verification link")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        authService.resendVerificationEmail(request.getEmail());
        return ResponseBuilder.ok(
                "If an unverified account with that email exists, a new verification link has been sent.",
                httpRequest);
    }
}
