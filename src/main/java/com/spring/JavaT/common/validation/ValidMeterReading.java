package com.spring.JavaT.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint: current reading must be greater than previous reading.
 * Apply to types implementing {@link MeterReadingValues}.
 */
@Documented
@Constraint(validatedBy = ValidMeterReadingValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMeterReading {

    String message() default ValidationMessages.READING_ORDER_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
