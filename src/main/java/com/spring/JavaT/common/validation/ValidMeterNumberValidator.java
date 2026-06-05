package com.spring.JavaT.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/** Validates meter number format. */
public class ValidMeterNumberValidator implements ConstraintValidator<ValidMeterNumber, String> {

    private static final Pattern METER_NUMBER = Pattern.compile("^[A-Z0-9-]{6,20}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return METER_NUMBER.matcher(value.strip()).matches();
    }
}
