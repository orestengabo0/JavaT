package com.spring.JavaT.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/** Validates customer address strings. */
public class ValidAddressValidator implements ConstraintValidator<ValidAddress, String> {

    private static final int MIN_LENGTH = 5;
    private static final int MAX_LENGTH = 255;

    private static final Pattern HAS_LETTER      = Pattern.compile("\\p{L}");
    private static final Pattern HAS_DIGIT       = Pattern.compile("\\d");
    private static final Pattern HAS_COMMA       = Pattern.compile(",");
    private static final Pattern ALL_DIGITS      = Pattern.compile("^\\d+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String trimmed = value.strip();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            return false;
        }

        if (ALL_DIGITS.matcher(trimmed).matches()) {
            return false;
        }

        return HAS_LETTER.matcher(trimmed).find()
                && (HAS_DIGIT.matcher(trimmed).find() || HAS_COMMA.matcher(trimmed).find());
    }
}
