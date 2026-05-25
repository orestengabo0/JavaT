package com.spring.JavaT.auth;

import com.spring.JavaT.auth.dto.AuthResponse;
import com.spring.JavaT.auth.dto.LoginRequest;
import com.spring.JavaT.auth.dto.RegisterRequest;
import com.spring.JavaT.common.ApiResponse;
import com.spring.JavaT.common.ResponseBuilder;
import com.spring.JavaT.common.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
