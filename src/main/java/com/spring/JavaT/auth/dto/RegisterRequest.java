package com.spring.JavaT.auth.dto;

import com.spring.JavaT.common.validation.NoWhitespace;
import com.spring.JavaT.common.validation.ValidPassword;
import com.spring.JavaT.common.validation.ValidPersonName;
import com.spring.JavaT.common.validation.ValidPhone;
import com.spring.JavaT.common.validation.ValidUsername;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for customer self-registration.
 *
 * <p>Self-registered accounts receive {@link com.spring.JavaT.user.Role#CUSTOMER}
 * and start in {@code PENDING} status until email verification completes.
 */
@Getter
@Setter
@Schema(description = "Request body for user registration")
public class RegisterRequest {

    @Schema(description = "User's first name", example = "Jean")
    @NotBlank(
            message = ValidationMessages.FIRST_NAME_REQUIRED,
            groups  = ValidationGroups.OnCreate.class
    )
    @ValidPersonName(min = 2, max = 50, groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    private String firstName;

    @Schema(description = "User's last name", example = "Uwimana")
    @NotBlank(
            message = ValidationMessages.LAST_NAME_REQUIRED,
            groups  = ValidationGroups.OnCreate.class
    )
    @ValidPersonName(min = 2, max = 50, groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    private String lastName;

    @Schema(description = "User's email address (used for login)", example = "jean.uwimana@example.com")
    @NotBlank(
            message = ValidationMessages.EMAIL_REQUIRED,
            groups  = ValidationGroups.OnCreate.class
    )
    @Email(
            message = ValidationMessages.EMAIL_INVALID,
            groups  = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class}
    )
    @Size(
            max     = 254,
            message = ValidationMessages.EMAIL_TOO_LONG,
            groups  = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class}
    )
    private String email;

    @Schema(description = "Phone number", example = "+250788123456")
    @NotBlank(
            message = ValidationMessages.PHONE_REQUIRED,
            groups  = ValidationGroups.OnCreate.class
    )
    @ValidPhone(groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    private String phone;

    @Schema(description = "Unique username (no spaces)", example = "jean.uwimana")
    @NotBlank(
            message = ValidationMessages.USERNAME_REQUIRED,
            groups  = ValidationGroups.OnCreate.class
    )
    @Size(
            min     = 3,
            max     = 50,
            message = ValidationMessages.USERNAME_TOO_SHORT,
            groups  = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class}
    )
    @ValidUsername(groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    @NoWhitespace(groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    private String username;

    @Schema(description = "Password (min 8 chars, must include upper, lower, digit, special char)",
            example = "Secret@123")
    @NotBlank(
            message = ValidationMessages.PASSWORD_REQUIRED,
            groups  = ValidationGroups.OnCreate.class
    )
    @ValidPassword(groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    private String password;
}
