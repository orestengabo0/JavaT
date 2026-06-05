package com.spring.JavaT.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validates application usernames.
 */
public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;

    private static final Pattern ALLOWED    = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern HAS_LETTER = Pattern.compile("[a-zA-Z]");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            return false;
        }

        return ALLOWED.matcher(value).matches() && HAS_LETTER.matcher(value).find();
    }
}
