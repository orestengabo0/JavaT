package com.spring.JavaT.customer.dto;

import com.spring.JavaT.common.validation.ValidAddress;
import com.spring.JavaT.common.validation.ValidPersonName;
import com.spring.JavaT.common.validation.ValidPhone;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Partial update for a customer (PATCH /customers/{id})")
public class UpdateCustomerRequest {

    @ValidPersonName(min = 2, max = 100, groups = ValidationGroups.OnPatch.class)
    private String fullNames;

    @ValidPhone(groups = ValidationGroups.OnPatch.class)
    private String phone;

    @Email(message = ValidationMessages.EMAIL_INVALID, groups = ValidationGroups.OnPatch.class)
    @Size(max = 254, message = ValidationMessages.EMAIL_TOO_LONG, groups = ValidationGroups.OnPatch.class)
    private String email;

    @ValidAddress(groups = ValidationGroups.OnPatch.class)
    private String address;
}
