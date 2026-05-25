package com.spring.JavaT.auth.dto;

import com.spring.JavaT.common.validation.ValidPassword;
import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for the reset-password endpoint.
 */
@Getter
@Setter
@Schema(description = "Request body for completing a password reset")
public class ResetPasswordRequest {

    @Schema(description = "The reset token received by email")
    @NotBlank(message = "Reset token is required")
    private String token;

    @Schema(description = "The new password", example = "NewSecret@456")
    @NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    @ValidPassword
    private String newPassword;
}
