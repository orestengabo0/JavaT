package com.spring.JavaT.auth.dto;

import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for the forgot-password endpoint.
 */
@Getter
@Setter
@Schema(description = "Request body for initiating a password reset")
public class ForgotPasswordRequest {

    @Schema(description = "Email address of the account to reset", example = "john.doe@example.com")
    @NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @Email(message = ValidationMessages.EMAIL_INVALID)
    private String email;
}
