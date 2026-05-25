package com.spring.JavaT.user;

import com.spring.JavaT.common.ApiResponse;
import com.spring.JavaT.common.PageableRequest;
import com.spring.JavaT.common.PagedResponse;
import com.spring.JavaT.common.ResponseBuilder;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.user.dto.UpdatePasswordRequest;
import com.spring.JavaT.user.dto.UpdateProfileRequest;
import com.spring.JavaT.user.dto.UpdateRoleRequest;
import com.spring.JavaT.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User management endpoints.
 *
 * <p>Two access tiers:
 * <ul>
 *   <li><b>Self-service</b> ({@code /me}) — any authenticated user can read and
 *       update their own profile.</li>
 *   <li><b>Admin</b> ({@code /{id}}) — only {@code ADMIN} role can list all users,
 *       view any user, change roles, and deactivate/activate accounts.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Profile, role, and account management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // =========================================================================
    // Self-service — /me
    // =========================================================================

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserDto>> getMyProfile(
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        UserDto dto = userService.getMyProfile(principal.getUsername());
        return ResponseBuilder.ok(dto, "Profile retrieved successfully", request);
    }

    @PatchMapping("/me")
    @Operation(summary = "Update the authenticated user's profile (name, username)")
    public ResponseEntity<ApiResponse<UserDto>> updateMyProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Validated(ValidationGroups.OnPatch.class) @RequestBody UpdateProfileRequest body,
            HttpServletRequest request) {

        UserDto dto = userService.updateMyProfile(principal.getUsername(), body);
        return ResponseBuilder.ok(dto, "Profile updated successfully", request);
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Change the authenticated user's password")
    public ResponseEntity<ApiResponse<Void>> updateMyPassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdatePasswordRequest body,
            HttpServletRequest request) {

        userService.updateMyPassword(principal.getUsername(), body);
        return ResponseBuilder.ok("Password changed successfully", request);
    }

    // =========================================================================
    // Admin — /{id}
    // =========================================================================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users — ADMIN only")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getAllUsers(
            @ParameterObject @Valid PageableRequest pageableRequest,
            HttpServletRequest request) {

        Page<UserDto> page = userService.getAllUsers(pageableRequest.toPageable());
        return ResponseBuilder.ok(page, "Users retrieved successfully", request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get any user by ID — ADMIN only")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(
            @PathVariable Long id,
            HttpServletRequest request) {

        UserDto dto = userService.getUserById(id);
        return ResponseBuilder.ok(dto, "User retrieved successfully", request);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change a user's role — ADMIN only")
    public ResponseEntity<ApiResponse<UserDto>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest body,
            HttpServletRequest request) {

        UserDto dto = userService.updateRole(id, body);
        return ResponseBuilder.ok(dto, "Role updated successfully", request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-deactivate a user account — ADMIN only")
    public ResponseEntity<ApiResponse<UserDto>> deactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        UserDto dto = userService.deactivateUser(id, principal.getUsername());
        return ResponseBuilder.ok(dto, "User deactivated successfully", request);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Restore a deactivated user account — ADMIN only")
    public ResponseEntity<ApiResponse<UserDto>> activateUser(
            @PathVariable Long id,
            HttpServletRequest request) {

        UserDto dto = userService.activateUser(id);
        return ResponseBuilder.ok(dto, "User activated successfully", request);
    }
}
