package com.spring.JavaT.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string is a plausible person name: letters (Unicode), spaces,
 * hyphens, and apostrophes only; must contain at least one letter; rejects
 * all-digit strings such as {@code 123456789}.
 */
@Documented
@Constraint(validatedBy = ValidPersonNameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPersonName {

    int min() default 2;

    int max() default 100;

    String message() default ValidationMessages.PERSON_NAME_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
