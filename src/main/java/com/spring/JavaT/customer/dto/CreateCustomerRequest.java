package com.spring.JavaT.customer.dto;

import com.spring.JavaT.common.validation.ValidAddress;
import com.spring.JavaT.common.validation.ValidCreateCustomer;
import com.spring.JavaT.common.validation.ValidNationalId;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@ValidCreateCustomer(groups = ValidationGroups.OnCreate.class)
@Schema(description = "Request body for linking a self-registered portal user to a customer record")
public class CreateCustomerRequest {

    @Schema(description = "Existing CUSTOMER portal user — name, email, and phone are copied from this account",
            example = "f1111111-1111-1111-1111-111111111103")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    private UUID userId;

    @Schema(example = "1199887766554433")
    @ValidNationalId(groups = ValidationGroups.OnCreate.class)
    private String nationalId;

    @Schema(example = "KG 123 St, Kigali, Rwanda")
    @ValidAddress(groups = ValidationGroups.OnCreate.class)
    private String address;
}
