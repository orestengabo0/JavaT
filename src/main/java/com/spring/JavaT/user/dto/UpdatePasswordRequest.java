package com.spring.JavaT.user.dto;

import com.spring.JavaT.common.validation.ValidPassword;
import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for changing the authenticated user's password (PATCH /users/me/password).
 *
 * <p>Requires the current password to prevent account takeover if a session
 * is left open on a shared device.
 */
@Getter
@Setter
@Schema(description = "Request body for changing the user's password")
public class UpdatePasswordRequest {

    @Schema(description = "Current password for verification", example = "OldSecret@123")
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @Schema(description = "New password", example = "NewSecret@456")
    @NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    @ValidPassword
    private String newPassword;
}
