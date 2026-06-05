package com.spring.JavaT.common.validation;

import com.spring.JavaT.customer.dto.CreateCustomerRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates create-customer payloads for manual entry vs. existing portal user linking. */
public class ValidCreateCustomerValidator implements ConstraintValidator<ValidCreateCustomer, CreateCustomerRequest> {

    @Override
    public boolean isValid(CreateCustomerRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        if (request.getUserId() != null) {
            return requireField(context, "nationalId", request.getNationalId())
                    && requireField(context, "address", request.getAddress());
        }

        return requireField(context, "fullNames", request.getFullNames())
                && requireField(context, "nationalId", request.getNationalId())
                && requireField(context, "email", request.getEmail())
                && requireField(context, "phone", request.getPhone())
                && requireField(context, "address", request.getAddress());
    }

    private boolean requireField(ConstraintValidatorContext context, String field, String value) {
        if (value != null && !value.isBlank()) {
            return true;
        }
        context.buildConstraintViolationWithTemplate(ValidationMessages.REQUIRED)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
