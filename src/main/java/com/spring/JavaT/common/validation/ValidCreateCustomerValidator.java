package com.spring.JavaT.common.validation;

import com.spring.JavaT.customer.dto.CreateCustomerRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates that create-customer payloads link a portal user with national ID and address. */
public class ValidCreateCustomerValidator implements ConstraintValidator<ValidCreateCustomer, CreateCustomerRequest> {

    @Override
    public boolean isValid(CreateCustomerRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        return requirePresent(context, "userId", request.getUserId() != null)
                && requireField(context, "nationalId", request.getNationalId())
                && requireField(context, "address", request.getAddress());
    }

    private boolean requirePresent(ConstraintValidatorContext context, String field, boolean present) {
        if (present) {
            return true;
        }
        context.buildConstraintViolationWithTemplate(ValidationMessages.REQUIRED)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
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
