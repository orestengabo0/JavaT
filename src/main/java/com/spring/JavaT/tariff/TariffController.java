package com.spring.JavaT.tariff;

import com.spring.JavaT.common.ApiResponse;
import com.spring.JavaT.common.ResponseBuilder;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.common.pagination.PageResponse;
import com.spring.JavaT.common.pagination.PaginationUtil;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.meter.MeterType;
import com.spring.JavaT.common.swagger.StandardApiResponses;
import com.spring.JavaT.security.SecurityRoles;
import com.spring.JavaT.tariff.dto.CreateTariffRequest;
import com.spring.JavaT.tariff.dto.TariffVersionDto;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tariffs")
@RequiredArgsConstructor
@Tag(name = "Tariff Configuration", description = "Versioned consumption tariffs, tax, and penalty settings")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
public class TariffController {

    private final TariffService tariffService;

    private static final Set<String> TARIFF_SORT_FIELDS = Set.of(
            "id", "name", "meterType", "tariffType", "effectiveFrom", "effectiveTo", "active", "createdAt"
    );

    @PostMapping
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Create a new versioned tariff — ADMIN only")
    public ResponseEntity<ApiResponse<TariffVersionDto>> createTariff(
            @Validated(ValidationGroups.OnCreate.class) @RequestBody CreateTariffRequest body,
            HttpServletRequest request) {

        TariffVersionDto dto = tariffService.createTariff(body);
        return ResponseBuilder.created(dto, "Tariff version created successfully", request);
    }

    @GetMapping
    @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE)
    @Operation(summary = "List all tariff versions — ADMIN or FINANCE")
    public ResponseEntity<ApiResponse<PageResponse<TariffVersionDto>>> getAllTariffs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @Parameter(description = "Filter by meter type: WATER, ELECTRICITY")
            @RequestParam(required = false) String meterType,
            @Parameter(description = "Filter by active flag")
            @RequestParam(required = false) Boolean active,
            HttpServletRequest request) {

        Pageable pageable = PaginationUtil.toPageable(page, size, sortBy, sortDir, TARIFF_SORT_FIELDS);

        List<SearchCriteria> criteria = new ArrayList<>();
        if (meterType != null && !meterType.isBlank()) {
            criteria.add(new SearchCriteria("meterType", SearchCriteria.Op.EQ, meterType.toUpperCase()));
        }
        if (active != null) {
            criteria.add(new SearchCriteria("active", SearchCriteria.Op.EQ, active));
        }

        Page<TariffVersionDto> tariffPage = tariffService.getAllTariffs(criteria, pageable);
        return ResponseBuilder.ok(PageResponse.of(tariffPage), "Tariffs retrieved successfully", request);
    }

    @GetMapping("/active")
    @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE)
    @Operation(summary = "Get the tariff currently effective for a meter type — ADMIN or FINANCE")
    public ResponseEntity<ApiResponse<TariffVersionDto>> getActiveTariff(
            @Parameter(description = "Meter type: WATER or ELECTRICITY", required = true)
            @RequestParam MeterType meterType,
            HttpServletRequest request) {

        return ResponseBuilder.ok(
                tariffService.getActiveTariff(meterType),
                "Active tariff retrieved successfully",
                request
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE)
    @Operation(summary = "Get a tariff version by ID — ADMIN or FINANCE")
    public ResponseEntity<ApiResponse<TariffVersionDto>> getTariffById(
            @PathVariable UUID id,
            HttpServletRequest request) {

        return ResponseBuilder.ok(tariffService.getTariffById(id), "Tariff retrieved successfully", request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Soft-delete a tariff version not linked to any bill — ADMIN only")
    public ResponseEntity<ApiResponse<TariffVersionDto>> deleteTariff(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        TariffVersionDto dto = tariffService.deleteTariff(id, principal.getUsername());
        return ResponseBuilder.ok(dto, "Tariff deleted successfully", request);
    }
}
