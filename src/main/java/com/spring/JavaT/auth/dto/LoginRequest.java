package com.spring.JavaT.auth.dto;

import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for the login endpoint.
 */
@Getter
@Setter
@Schema(description = "Request body for user login")
public class LoginRequest {

    @Schema(description = "Registered email address", example = "john.doe@example.com")
    @NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @Email(message = ValidationMessages.EMAIL_INVALID)
    private String email;

    @Schema(description = "Account password", example = "Secret@123")
    @NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    private String password;
}
