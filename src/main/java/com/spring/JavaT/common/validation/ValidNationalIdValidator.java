package com.spring.JavaT.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/** Validates Rwanda National ID format (16 digits). */
public class ValidNationalIdValidator implements ConstraintValidator<ValidNationalId, String> {

    private static final Pattern NATIONAL_ID = Pattern.compile("^\\d{16}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return NATIONAL_ID.matcher(value.strip()).matches();
    }
}
