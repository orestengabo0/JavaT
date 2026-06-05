package com.spring.JavaT.security;

/**
 * Spring Security role expressions reused across controllers.
 *
 * <p>Usage: {@code @PreAuthorize(SecurityRoles.ADMIN)} or
 * {@code @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE)}.
 */
public final class SecurityRoles {

    private SecurityRoles() {}

    public static final String ADMIN            = "hasRole('ADMIN')";
    public static final String OPERATOR         = "hasRole('OPERATOR')";
    public static final String FINANCE          = "hasRole('FINANCE')";
    public static final String CUSTOMER         = "hasRole('CUSTOMER')";

    public static final String ADMIN_OR_FINANCE = "hasAnyRole('ADMIN', 'FINANCE')";
    public static final String ADMIN_OR_OPERATOR = "hasAnyRole('ADMIN', 'OPERATOR')";
    public static final String ADMIN_OR_FINANCE_OR_CUSTOMER = "hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')";
    public static final String ADMIN_OR_CUSTOMER          = "hasAnyRole('ADMIN', 'CUSTOMER')";
    public static final String STAFF            = "hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')";
}
