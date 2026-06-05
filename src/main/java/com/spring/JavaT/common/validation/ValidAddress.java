package com.spring.JavaT.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a postal-style address: 5–255 characters, must contain at least
 * one letter and at least one digit or comma (rejects pure numbers).
 */
@Documented
@Constraint(validatedBy = ValidAddressValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAddress {

    String message() default ValidationMessages.ADDRESS_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
