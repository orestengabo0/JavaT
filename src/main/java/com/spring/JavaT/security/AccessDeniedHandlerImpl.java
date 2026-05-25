package com.spring.JavaT.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.JavaT.common.ApiError;
import com.spring.JavaT.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Handles authenticated requests that lack the required authority.
 *
 * <p>Spring Security invokes this when a user is authenticated but tries to
 * access a resource they are not permitted to use (e.g. a USER hitting an
 * ADMIN-only endpoint). Returns a standard {@link ApiResponse} with HTTP 403.
 */
@Component
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest   request,
            HttpServletResponse  response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .message("Access denied")
                .errors(List.of(ApiError.ofGlobal(
                        "You do not have permission to perform this action", "FORBIDDEN")))
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
