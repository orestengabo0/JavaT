package com.spring.JavaT.user.dto;

import com.spring.JavaT.common.EntityStatus;
import com.spring.JavaT.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe read-only projection of a {@link com.spring.JavaT.user.User}.
 */
@Getter
@Builder
@Schema(description = "User profile data")
public class UserDto {

    @Schema(description = "User ID", example = "1")
    private final UUID id;

    @Schema(description = "First name", example = "Jean")
    private final String firstName;

    @Schema(description = "Last name", example = "Uwimana")
    private final String lastName;

    @Schema(description = "Unique username", example = "jean.uwimana")
    private final String username;

    @Schema(description = "Email address", example = "jean.uwimana@example.com")
    private final String email;

    @Schema(description = "Phone number", example = "+250788123456")
    private final String phone;

    @Schema(description = "Assigned role", example = "CUSTOMER")
    private final Role role;

    @Schema(description = "Account lifecycle status", example = "ACTIVE")
    private final EntityStatus status;

    @Schema(description = "UTC timestamp of account creation")
    private final Instant createdAt;

    @Schema(description = "UTC timestamp of last update")
    private final Instant updatedAt;
}
