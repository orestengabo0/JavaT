package com.spring.JavaT.user.dto;

import com.spring.JavaT.common.validation.NoWhitespace;
import com.spring.JavaT.common.validation.ValidEnum;
import com.spring.JavaT.common.validation.ValidPassword;
import com.spring.JavaT.common.validation.ValidPersonName;
import com.spring.JavaT.common.validation.ValidPhone;
import com.spring.JavaT.common.validation.ValidUsername;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import com.spring.JavaT.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for admin-created user accounts (POST /api/v1/users).
 *
 * <p>Staff accounts (ADMIN, OPERATOR, FINANCE) are created as {@code ACTIVE}
 * immediately. CUSTOMER accounts may optionally require email verification
 * depending on service configuration.
 */
@Getter
@Setter
@Schema(description = "Request body for admin user creation")
public class CreateUserRequest {

    @Schema(description = "First name", example = "Alice")
    @NotBlank(message = ValidationMessages.FIRST_NAME_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @ValidPersonName(min = 2, max = 50, groups = ValidationGroups.OnCreate.class)
    private String firstName;

    @Schema(description = "Last name", example = "Mukamana")
    @NotBlank(message = ValidationMessages.LAST_NAME_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @ValidPersonName(min = 2, max = 50, groups = ValidationGroups.OnCreate.class)
    private String lastName;

    @Schema(description = "Email address (login identifier)", example = "alice@wasac.gov.rw")
    @NotBlank(message = ValidationMessages.EMAIL_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @Email(message = ValidationMessages.EMAIL_INVALID, groups = ValidationGroups.OnCreate.class)
    @Size(max = 254, message = ValidationMessages.EMAIL_TOO_LONG, groups = ValidationGroups.OnCreate.class)
    private String email;

    @Schema(description = "Phone number", example = "+250788000001")
    @NotBlank(message = ValidationMessages.PHONE_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @ValidPhone(groups = ValidationGroups.OnCreate.class)
    private String phone;

    @Schema(description = "Unique username", example = "alice.mukamana")
    @NotBlank(message = ValidationMessages.USERNAME_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @Size(min = 3, max = 50, message = ValidationMessages.USERNAME_TOO_SHORT, groups = ValidationGroups.OnCreate.class)
    @ValidUsername(groups = ValidationGroups.OnCreate.class)
    @NoWhitespace(groups = ValidationGroups.OnCreate.class)
    private String username;

    @Schema(description = "Initial password", example = "Secret@123")
    @NotBlank(message = ValidationMessages.PASSWORD_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @ValidPassword(groups = ValidationGroups.OnCreate.class)
    private String password;

    @Schema(description = "Role to assign", example = "OPERATOR",
            allowableValues = {"ADMIN", "OPERATOR", "FINANCE", "CUSTOMER"})
    @NotBlank(message = ValidationMessages.ROLE_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @ValidEnum(enumClass = Role.class, message = ValidationMessages.ROLE_INVALID, groups = ValidationGroups.OnCreate.class)
    private String role;
}
