package com.spring.JavaT.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/** Ensures current meter reading exceeds previous reading. */
public class ValidMeterReadingValidator implements ConstraintValidator<ValidMeterReading, MeterReadingValues> {

    @Override
    public boolean isValid(MeterReadingValues value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BigDecimal previous = value.getPreviousReading();
        BigDecimal current  = value.getCurrentReading();

        if (previous == null || current == null) {
            return true;
        }

        return current.compareTo(previous) > 0;
    }
}
