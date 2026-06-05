package com.spring.JavaT.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validates utility meter numbers: 6–20 uppercase letters, digits, or hyphens. */
@Documented
@Constraint(validatedBy = ValidMeterNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMeterNumber {

    String message() default ValidationMessages.METER_NUMBER_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
