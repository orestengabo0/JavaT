package com.spring.JavaT.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.JavaT.common.ApiError;
import com.spring.JavaT.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Handles unauthenticated requests that reach a protected endpoint.
 *
 * <p>Spring Security invokes this when no valid authentication is present
 * (missing token, expired token, bad credentials). Returns a standard
 * {@link ApiResponse} with HTTP 401 instead of Spring's default HTML error page.
 */
@Component
@RequiredArgsConstructor
public class SecurityEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest       request,
            HttpServletResponse      response,
            AuthenticationException  authException
    ) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .message("Authentication required")
                .errors(List.of(ApiError.ofGlobal(
                        "No valid authentication token was provided", "UNAUTHORIZED")))
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
