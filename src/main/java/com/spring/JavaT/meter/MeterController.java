package com.spring.JavaT.meter;

import com.spring.JavaT.common.ApiResponse;
import com.spring.JavaT.common.ResponseBuilder;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.common.pagination.PageResponse;
import com.spring.JavaT.common.pagination.PaginationUtil;
import com.spring.JavaT.meter.dto.MeterDto;
import com.spring.JavaT.common.swagger.StandardApiResponses;
import com.spring.JavaT.security.SecurityRoles;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meters")
@RequiredArgsConstructor
@Tag(name = "Meter Management", description = "Manage utility meters")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
public class MeterController {

    private final MeterService meterService;

    private static final Set<String> METER_SORT_FIELDS = Set.of(
            "id", "meterNumber", "meterType", "installationDate", "status", "createdAt"
    );

    @GetMapping
    @PreAuthorize(SecurityRoles.ADMIN_OR_OPERATOR)
    @Operation(summary = "List meters with optional filtering — ADMIN or OPERATOR")
    public ResponseEntity<ApiResponse<PageResponse<MeterDto>>> getAllMeters(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) UUID customerId,
            @Parameter(description = "Filter by meter type: WATER, ELECTRICITY")
            @RequestParam(required = false) String meterType,
            @Parameter(description = "Filter by status: ACTIVE, INACTIVE")
            @RequestParam(required = false) String status,
            HttpServletRequest request) {

        Pageable pageable = PaginationUtil.toPageable(page, size, sortBy, sortDir, METER_SORT_FIELDS);

        List<SearchCriteria> criteria = new ArrayList<>();
        if (customerId != null) {
            criteria.add(new SearchCriteria("customer.id", SearchCriteria.Op.EQ, customerId));
        }
        if (meterType != null && !meterType.isBlank()) {
            criteria.add(new SearchCriteria("meterType", SearchCriteria.Op.EQ, meterType.toUpperCase()));
        }
        if (status != null && !status.isBlank()) {
            criteria.add(new SearchCriteria("status", SearchCriteria.Op.EQ, status.toUpperCase()));
        }

        Page<MeterDto> meterPage = meterService.getAllMeters(criteria, pageable);
        return ResponseBuilder.ok(PageResponse.of(meterPage), "Meters retrieved successfully", request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_OR_OPERATOR)
    @Operation(summary = "Get meter by ID — ADMIN or OPERATOR")
    public ResponseEntity<ApiResponse<MeterDto>> getMeterById(
            @PathVariable UUID id,
            HttpServletRequest request) {

        return ResponseBuilder.ok(meterService.getMeterById(id), "Meter retrieved successfully", request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Deactivate a meter — ADMIN only")
    public ResponseEntity<ApiResponse<MeterDto>> deactivateMeter(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        return ResponseBuilder.ok(
                meterService.deactivateMeter(id, principal.getUsername()),
                "Meter deactivated successfully",
                request
        );
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Reactivate a meter — ADMIN only")
    public ResponseEntity<ApiResponse<MeterDto>> activateMeter(
            @PathVariable UUID id,
            HttpServletRequest request) {

        return ResponseBuilder.ok(meterService.activateMeter(id), "Meter activated successfully", request);
    }
}
