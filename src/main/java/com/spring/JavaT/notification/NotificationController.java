package com.spring.JavaT.notification;

import com.spring.JavaT.common.ApiResponse;
import com.spring.JavaT.common.ResponseBuilder;
import com.spring.JavaT.common.pagination.PageResponse;
import com.spring.JavaT.common.pagination.PaginationUtil;
import com.spring.JavaT.notification.dto.NotificationDto;
import com.spring.JavaT.common.swagger.StandardApiResponses;
import com.spring.JavaT.security.SecurityRoles;
import com.spring.JavaT.user.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app customer notifications")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
public class NotificationController {

    private final NotificationService notificationService;

    private static final Set<String> NOTIFICATION_SORT_FIELDS = Set.of("id", "read", "createdAt");

    @GetMapping
    @PreAuthorize(SecurityRoles.ADMIN_OR_CUSTOMER)
    @Operation(summary = "List notifications — ADMIN sees all; CUSTOMER sees own")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDto>>> getNotifications(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @Parameter(description = "Filter by read flag")
            @RequestParam(required = false) Boolean read,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        Pageable pageable = PaginationUtil.toPageable(page, size, sortBy, sortDir, NOTIFICATION_SORT_FIELDS);

        Page<NotificationDto> notificationPage = isAdmin(principal)
                ? notificationService.getNotificationsForAdmin(read, pageable)
                : notificationService.getNotificationsForCustomer(principal.getUsername(), read, pageable);

        return ResponseBuilder.ok(PageResponse.of(notificationPage), "Notifications retrieved successfully", request);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize(SecurityRoles.CUSTOMER)
    @Operation(summary = "Mark a notification as read — CUSTOMER only")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        NotificationDto dto = notificationService.markAsRead(id, principal.getUsername());
        return ResponseBuilder.ok(dto, "Notification marked as read", request);
    }

    private boolean isAdmin(UserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> Role.ADMIN.name().equals(a.getAuthority().replace("ROLE_", "")));
    }
}
