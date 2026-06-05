package com.spring.JavaT.customer.dto;

import com.spring.JavaT.common.validation.ValidAddress;
import com.spring.JavaT.common.validation.ValidCreateCustomer;
import com.spring.JavaT.common.validation.ValidNationalId;
import com.spring.JavaT.common.validation.ValidPassword;
import com.spring.JavaT.common.validation.ValidPersonName;
import com.spring.JavaT.common.validation.ValidPhone;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@ValidCreateCustomer(groups = ValidationGroups.OnCreate.class)
@Schema(description = "Request body for registering a new customer")
public class CreateCustomerRequest {

    @Schema(description = "Link an existing CUSTOMER portal user — name, email, and phone are copied from the user",
            example = "f1111111-1111-1111-1111-111111111103")
    private UUID userId;

    @Schema(example = "Jean Pierre Uwimana", description = "Required when userId is omitted")
    @ValidPersonName(min = 2, max = 100, groups = ValidationGroups.OnCreate.class)
    private String fullNames;

    @Schema(example = "1199887766554433")
    @ValidNationalId(groups = ValidationGroups.OnCreate.class)
    private String nationalId;

    @Schema(example = "jean.uwimana@example.com", description = "Required when userId is omitted")
    @Email(message = ValidationMessages.EMAIL_INVALID, groups = ValidationGroups.OnCreate.class)
    @Size(max = 254, message = ValidationMessages.EMAIL_TOO_LONG, groups = ValidationGroups.OnCreate.class)
    private String email;

    @Schema(example = "+250788123456", description = "Required when userId is omitted")
    @ValidPhone(groups = ValidationGroups.OnCreate.class)
    private String phone;

    @Schema(example = "KG 123 St, Kigali, Rwanda")
    @ValidAddress(groups = ValidationGroups.OnCreate.class)
    private String address;

    @Schema(description = "When true (and userId is omitted), link or create a CUSTOMER portal user for email",
            example = "true", defaultValue = "false")
    private boolean createUserAccount;

    @Schema(description = "Password for a new portal user (required when createUserAccount is true and no user exists)",
            example = "Secret@123")
    @ValidPassword(groups = ValidationGroups.OnCreate.class)
    private String password;
}
