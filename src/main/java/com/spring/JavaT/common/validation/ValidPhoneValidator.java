package com.spring.JavaT.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validates Rwanda and E.164 phone numbers.
 */
public class ValidPhoneValidator implements ConstraintValidator<ValidPhone, String> {

    /** Rwanda international mobile: +2507XXXXXXXX */
    private static final Pattern RWANDA_INTL = Pattern.compile("^\\+2507\\d{8}$");

    /** Rwanda local mobile: 07XXXXXXXX */
    private static final Pattern RWANDA_LOCAL = Pattern.compile("^07\\d{8}$");

    /** E.164 international format */
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String trimmed = value.strip();
        return RWANDA_INTL.matcher(trimmed).matches()
                || RWANDA_LOCAL.matcher(trimmed).matches()
                || E164.matcher(trimmed).matches();
    }
}
