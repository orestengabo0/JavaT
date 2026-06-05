package com.spring.JavaT.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validates person names — rejects numeric-only values like {@code 123456789}.
 */
public class ValidPersonNameValidator implements ConstraintValidator<ValidPersonName, String> {

    private static final Pattern HAS_LETTER  = Pattern.compile("\\p{L}");
    private static final Pattern ALL_DIGITS  = Pattern.compile("^\\d+$");
    private static final Pattern ALLOWED     = Pattern.compile("^[\\p{L}\\s'-]+$");

    private int min;
    private int max;

    @Override
    public void initialize(ValidPersonName annotation) {
        min = annotation.min();
        max = annotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String trimmed = value.strip();
        if (trimmed.length() < min || trimmed.length() > max) {
            return false;
        }

        if (!ALLOWED.matcher(trimmed).matches()) {
            return false;
        }

        if (!HAS_LETTER.matcher(trimmed).find()) {
            return false;
        }

        String lettersAndDigitsOnly = trimmed.replaceAll("[\\s'-]", "");
        return !ALL_DIGITS.matcher(lettersAndDigitsOnly).matches();
    }
}
