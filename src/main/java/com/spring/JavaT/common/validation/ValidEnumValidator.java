package com.spring.JavaT.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link ValidEnum} constraint.
 *
 * <p>Builds a set of accepted names from the enum constants at initialisation time
 * (once per validator instance) so the check itself is just a set lookup.
 */
public class ValidEnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Set<String> acceptedValues;
    private boolean ignoreCase;

    @Override
    public void initialize(ValidEnum annotation) {
        ignoreCase = annotation.ignoreCase();
        acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .map(name -> ignoreCase ? name.toUpperCase() : name)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // let @NotNull handle the null case
        }

        String candidate = ignoreCase ? value.toUpperCase() : value;
        boolean valid = acceptedValues.contains(candidate);

        if (!valid) {
            // Replace the default message to include the accepted values list
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid value '%s'. Accepted values are: %s".formatted(
                            value,
                            String.join(", ", acceptedValues)
                    )
            ).addConstraintViolation();
        }

        return valid;
    }
}
