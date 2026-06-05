package com.spring.JavaT.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validates Rwanda National ID — exactly 16 digits. */
@Documented
@Constraint(validatedBy = ValidNationalIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNationalId {

    String message() default ValidationMessages.NATIONAL_ID_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
