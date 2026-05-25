package com.spring.JavaT.user.dto;

import com.spring.JavaT.common.EntityStatus;
import com.spring.JavaT.user.Role;
import com.spring.JavaT.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Safe read-only projection of a {@link User}.
 *
 * <p>Never exposes the password hash or internal audit fields.
 * Used as the response body for all user-facing and admin endpoints.
 */
@Getter
@Builder
@Schema(description = "User profile data")
public class UserDto {

    @Schema(description = "User ID", example = "1")
    private final Long id;

    @Schema(description = "First name", example = "John")
    private final String firstName;

    @Schema(description = "Last name", example = "Doe")
    private final String lastName;

    @Schema(description = "Unique username", example = "johndoe")
    private final String username;

    @Schema(description = "Email address", example = "john.doe@example.com")
    private final String email;

    @Schema(description = "Assigned role", example = "USER")
    private final Role role;

    @Schema(description = "Account lifecycle status", example = "ACTIVE")
    private final EntityStatus status;

    @Schema(description = "UTC timestamp of account creation")
    private final Instant createdAt;

    @Schema(description = "UTC timestamp of last update")
    private final Instant updatedAt;

    // -------------------------------------------------------------------------
    // Mapper
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link User} entity to a {@link UserDto}.
     * Centralising the mapping here means no MapStruct dependency is needed
     * for a simple projection like this.
     */
    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
