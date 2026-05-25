package com.spring.JavaT.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a password meets the application's strength requirements:
 * <ul>
 *   <li>8 to 72 characters (72 is bcrypt's effective max)</li>
 *   <li>At least one uppercase letter (A–Z)</li>
 *   <li>At least one lowercase letter (a–z)</li>
 *   <li>At least one digit (0–9)</li>
 *   <li>At least one special character ({@code !@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?})</li>
 * </ul>
 *
 * <p>{@code null} values are considered valid — combine with {@code @NotBlank} if
 * the field is required.
 *
 * <p>Usage:
 * <pre>
 * {@literal @}NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
 * {@literal @}ValidPassword
 * private String password;
 * </pre>
 */
@Documented
@Constraint(validatedBy = ValidPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default ValidationMessages.PASSWORD_INVALID;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
