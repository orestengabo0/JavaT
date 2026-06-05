package com.spring.JavaT.user.dto;

import com.spring.JavaT.common.validation.NoWhitespace;
import com.spring.JavaT.common.validation.ValidPersonName;
import com.spring.JavaT.common.validation.ValidPhone;
import com.spring.JavaT.common.validation.ValidUsername;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for updating a user's own profile (PATCH /users/me).
 *
 * <p>All fields are optional — only non-null fields are applied.
 */
@Getter
@Setter
@Schema(description = "Fields available for the user to update on their own profile")
public class UpdateProfileRequest {

    @Schema(description = "New first name", example = "Jane")
    @ValidPersonName(min = 2, max = 50, groups = ValidationGroups.OnPatch.class)
    private String firstName;

    @Schema(description = "New last name", example = "Smith")
    @ValidPersonName(min = 2, max = 50, groups = ValidationGroups.OnPatch.class)
    private String lastName;

    @Schema(description = "New phone number", example = "+250788123456")
    @ValidPhone(groups = ValidationGroups.OnPatch.class)
    private String phone;

    @Schema(description = "New unique username", example = "janesmith")
    @Size(
            min     = 3,
            max     = 50,
            message = ValidationMessages.USERNAME_TOO_SHORT,
            groups  = ValidationGroups.OnPatch.class
    )
    @ValidUsername(groups = ValidationGroups.OnPatch.class)
    @NoWhitespace(groups = ValidationGroups.OnPatch.class)
    private String username;
}
