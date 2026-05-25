package com.spring.JavaT.common.validation;

/**
 * Central registry of all validation message strings used across DTOs.
 *
 * <p>Using constants here instead of inline strings in annotations means:
 * <ul>
 *   <li>Messages are consistent — the same rule always produces the same wording.</li>
 *   <li>Easy to update — change one constant, every DTO using it picks it up.</li>
 *   <li>IDE navigation — find all usages of a message across the codebase.</li>
 * </ul>
 *
 * <p>Usage in a DTO:
 * <pre>
 * {@literal @}NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
 * {@literal @}Email(message = ValidationMessages.EMAIL_INVALID)
 * private String email;
 * </pre>
 */
public final class ValidationMessages {

    private ValidationMessages() {}

    // -------------------------------------------------------------------------
    // Generic
    // -------------------------------------------------------------------------
    public static final String REQUIRED          = "This field is required";
    public static final String INVALID           = "This field contains an invalid value";

    // -------------------------------------------------------------------------
    // String / text
    // -------------------------------------------------------------------------
    public static final String NOT_BLANK         = "This field must not be blank";
    public static final String NO_WHITESPACE     = "This field must not contain leading or trailing whitespace";
    public static final String TOO_SHORT         = "This field is too short";
    public static final String TOO_LONG          = "This field is too long";

    // -------------------------------------------------------------------------
    // Email
    // -------------------------------------------------------------------------
    public static final String EMAIL_REQUIRED    = "Email address is required";
    public static final String EMAIL_INVALID     = "Must be a valid email address";
    public static final String EMAIL_TOO_LONG    = "Email address must not exceed 254 characters";

    // -------------------------------------------------------------------------
    // Password
    // -------------------------------------------------------------------------
    public static final String PASSWORD_REQUIRED = "Password is required";
    public static final String PASSWORD_INVALID  =
            "Password must be 8–72 characters and contain at least one uppercase letter, "
            + "one lowercase letter, one digit, and one special character";
    public static final String PASSWORD_MISMATCH = "Passwords do not match";

    // -------------------------------------------------------------------------
    // Name fields
    // -------------------------------------------------------------------------
    public static final String FIRST_NAME_REQUIRED  = "First name is required";
    public static final String FIRST_NAME_TOO_SHORT = "First name must be at least 2 characters";
    public static final String FIRST_NAME_TOO_LONG  = "First name must not exceed 50 characters";

    public static final String LAST_NAME_REQUIRED   = "Last name is required";
    public static final String LAST_NAME_TOO_SHORT  = "Last name must be at least 2 characters";
    public static final String LAST_NAME_TOO_LONG   = "Last name must not exceed 50 characters";

    public static final String USERNAME_REQUIRED    = "Username is required";
    public static final String USERNAME_TOO_SHORT   = "Username must be at least 3 characters";
    public static final String USERNAME_TOO_LONG    = "Username must not exceed 50 characters";

    // -------------------------------------------------------------------------
    // Numeric / range
    // -------------------------------------------------------------------------
    public static final String POSITIVE            = "Value must be positive";
    public static final String POSITIVE_OR_ZERO    = "Value must be zero or positive";
    public static final String MIN_VALUE           = "Value is below the allowed minimum";
    public static final String MAX_VALUE           = "Value exceeds the allowed maximum";

    // -------------------------------------------------------------------------
    // Date / time
    // -------------------------------------------------------------------------
    public static final String DATE_REQUIRED       = "Date is required";
    public static final String DATE_PAST           = "Date must be in the past";
    public static final String DATE_FUTURE         = "Date must be in the future";
    public static final String DATE_PAST_OR_NOW    = "Date must be in the past or present";
    public static final String DATE_FUTURE_OR_NOW  = "Date must be in the present or future";

    // -------------------------------------------------------------------------
    // Collections
    // -------------------------------------------------------------------------
    public static final String NOT_EMPTY           = "List must not be empty";
    public static final String SIZE_MIN            = "List has too few elements";
    public static final String SIZE_MAX            = "List has too many elements";

    // -------------------------------------------------------------------------
    // Enum
    // -------------------------------------------------------------------------
    public static final String ENUM_INVALID        = "Invalid value. Accepted values are: {accepted}";

    // -------------------------------------------------------------------------
    // Phone
    // -------------------------------------------------------------------------
    public static final String PHONE_INVALID       = "Must be a valid phone number (e.g. +1234567890)";
}
