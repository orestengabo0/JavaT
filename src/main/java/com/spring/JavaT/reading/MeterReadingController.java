package com.spring.JavaT.reading;

import com.spring.JavaT.common.ApiResponse;
import com.spring.JavaT.common.ResponseBuilder;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.common.pagination.PageResponse;
import com.spring.JavaT.common.pagination.PaginationUtil;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.reading.dto.CreateMeterReadingRequest;
import com.spring.JavaT.reading.dto.MeterReadingDto;
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
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/v1/readings")
@RequiredArgsConstructor
@Tag(name = "Meter Readings", description = "Capture and view meter readings")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    private static final Set<String> READING_SORT_FIELDS = Set.of(
            "id", "readingDate", "billingMonth", "billingYear", "currentReading", "createdAt"
    );

    @PostMapping
    @PreAuthorize(SecurityRoles.OPERATOR)
    @Operation(summary = "Capture a meter reading — OPERATOR only. Previous reading is resolved automatically from the last capture.")
    public ResponseEntity<ApiResponse<MeterReadingDto>> captureReading(
            @Validated(ValidationGroups.OnCreate.class) @RequestBody CreateMeterReadingRequest body,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        MeterReadingDto dto = meterReadingService.captureReading(body, principal.getUsername());
        return ResponseBuilder.created(dto, "Meter reading captured successfully", request);
    }

    @GetMapping
    @PreAuthorize(SecurityRoles.ADMIN_OR_OPERATOR)
    @Operation(summary = "List meter readings with optional filters — ADMIN or OPERATOR")
    public ResponseEntity<ApiResponse<PageResponse<MeterReadingDto>>> getAllReadings(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @Parameter(description = "Filter by meter ID")
            @RequestParam(required = false) UUID meterId,
            @Parameter(description = "Filter by billing month (1–12)")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Filter by billing year")
            @RequestParam(required = false) Integer year,
            HttpServletRequest request) {

        Pageable pageable = PaginationUtil.toPageable(page, size, sortBy, sortDir, READING_SORT_FIELDS);

        List<SearchCriteria> criteria = new ArrayList<>();
        if (meterId != null) {
            criteria.add(new SearchCriteria("meter.id", SearchCriteria.Op.EQ, meterId));
        }
        if (month != null) {
            criteria.add(new SearchCriteria("billingMonth", SearchCriteria.Op.EQ, month));
        }
        if (year != null) {
            criteria.add(new SearchCriteria("billingYear", SearchCriteria.Op.EQ, year));
        }

        Page<MeterReadingDto> readingPage = meterReadingService.getAllReadings(criteria, pageable);
        return ResponseBuilder.ok(PageResponse.of(readingPage), "Readings retrieved successfully", request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_OR_OPERATOR)
    @Operation(summary = "Get a meter reading by ID — ADMIN or OPERATOR")
    public ResponseEntity<ApiResponse<MeterReadingDto>> getReadingById(
            @PathVariable UUID id,
            HttpServletRequest request) {

        return ResponseBuilder.ok(
                meterReadingService.getReadingById(id),
                "Reading retrieved successfully",
                request
        );
    }
}
