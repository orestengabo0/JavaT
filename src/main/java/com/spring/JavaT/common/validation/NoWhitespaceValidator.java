package com.spring.JavaT.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementation of the {@link NoWhitespace} constraint.
 */
public class NoWhitespaceValidator implements ConstraintValidator<NoWhitespace, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // let @NotBlank handle the null/empty case
        }
        return value.equals(value.strip());
    }
}
