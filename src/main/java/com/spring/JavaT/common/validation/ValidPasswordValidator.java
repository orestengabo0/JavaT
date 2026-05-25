package com.spring.JavaT.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Implementation of the {@link ValidPassword} constraint.
 *
 * <p>Uses individual regex checks rather than one complex pattern so that
 * the failure reason is easy to debug and the rules are easy to adjust.
 */
public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // bcrypt silently truncates at 72 bytes; enforce that as the hard max
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 72;

    private static final Pattern HAS_UPPERCASE    = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE    = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT        = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL_CHAR = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // null is handled by @NotBlank — don't double-report
        if (password == null) {
            return true;
        }

        return password.length() >= MIN_LENGTH
                && password.length() <= MAX_LENGTH
                && HAS_UPPERCASE.matcher(password).find()
                && HAS_LOWERCASE.matcher(password).find()
                && HAS_DIGIT.matcher(password).find()
                && HAS_SPECIAL_CHAR.matcher(password).find();
    }
}
