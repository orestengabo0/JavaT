package com.spring.JavaT.user;

/**
 * Application roles for the Utility Billing System.
 *
 * <p>Stored as strings in the database via {@code @Enumerated(EnumType.STRING)}.
 * Spring Security expects role names prefixed with {@code ROLE_} when using
 * {@code hasRole()} expressions — that prefix is added in {@link User#getAuthorities()}.
 */
public enum Role {

    /** Configure tariffs, approve bills, manage users and customers. */
    ADMIN,

    /** Capture meter readings. */
    OPERATOR,

    /** Approve bills and record payments. */
    FINANCE,

    /** View own bills, payments, and notifications. */
    CUSTOMER
}
