package com.spring.JavaT.customer.dto;

import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Link an existing CUSTOMER user account to a customer record")
public class LinkUserRequest {

    @Schema(description = "Email of the existing user to link", example = "jean.uwimana@example.com")
    @NotBlank(message = ValidationMessages.EMAIL_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @Email(message = ValidationMessages.EMAIL_INVALID, groups = ValidationGroups.OnCreate.class)
    private String email;
}
