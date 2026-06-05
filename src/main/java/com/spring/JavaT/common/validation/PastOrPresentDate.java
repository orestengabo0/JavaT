package com.spring.JavaT.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validates that a {@link java.time.LocalDate} is today or in the past. */
@Documented
@Constraint(validatedBy = PastOrPresentDateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PastOrPresentDate {

    String message() default ValidationMessages.DATE_PAST_OR_NOW;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
