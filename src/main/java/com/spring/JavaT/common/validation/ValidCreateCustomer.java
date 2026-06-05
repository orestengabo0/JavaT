package com.spring.JavaT.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ensures {@link com.spring.JavaT.customer.dto.CreateCustomerRequest} includes
 * {@code userId}, {@code nationalId}, and {@code address}.
 */
@Documented
@Constraint(validatedBy = ValidCreateCustomerValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCreateCustomer {

    String message() default ValidationMessages.INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
