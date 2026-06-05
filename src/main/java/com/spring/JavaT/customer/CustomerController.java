package com.spring.JavaT.customer;

import com.spring.JavaT.common.ApiResponse;
import com.spring.JavaT.common.ResponseBuilder;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.common.pagination.PageResponse;
import com.spring.JavaT.common.pagination.PaginationUtil;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.customer.dto.CreateCustomerRequest;
import com.spring.JavaT.customer.dto.CustomerDto;
import com.spring.JavaT.customer.dto.LinkUserRequest;
import com.spring.JavaT.customer.dto.UpdateCustomerRequest;
import com.spring.JavaT.meter.dto.CreateMeterRequest;
import com.spring.JavaT.meter.dto.MeterDto;
import com.spring.JavaT.meter.MeterService;
import com.spring.JavaT.reading.MeterReadingService;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Register and manage utility customers")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
public class CustomerController {

    private final CustomerService     customerService;
    private final MeterService        meterService;
    private final MeterReadingService meterReadingService;

    private static final Set<String> CUSTOMER_SORT_FIELDS = Set.of(
            "id", "fullNames", "nationalId", "email", "phone", "status", "createdAt"
    );

    private static final Set<String> METER_SORT_FIELDS = Set.of(
            "id", "meterNumber", "meterType", "installationDate", "status", "createdAt"
    );

    private static final Set<String> READING_SORT_FIELDS = Set.of(
            "id", "readingDate", "billingMonth", "billingYear", "currentReading", "createdAt"
    );

    @PostMapping
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Register a new customer — ADMIN only. Pass userId to link a self-registered portal user with only nationalId + address.")
    public ResponseEntity<ApiResponse<CustomerDto>> createCustomer(
            @Validated(ValidationGroups.OnCreate.class) @RequestBody CreateCustomerRequest body,
            HttpServletRequest request) {

        CustomerDto dto = customerService.createCustomer(body);
        return ResponseBuilder.created(dto, "Customer registered successfully", request);
    }

    @GetMapping
    @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE)
    @Operation(summary = "List customers with optional filtering — ADMIN or FINANCE")
    public ResponseEntity<ApiResponse<PageResponse<CustomerDto>>> getAllCustomers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @Parameter(description = "Filter by status: ACTIVE, INACTIVE")
            @RequestParam(required = false) String status,
            @Parameter(description = "Search by full name or email (partial match)")
            @RequestParam(required = false) String search,
            HttpServletRequest request) {

        Pageable pageable = PaginationUtil.toPageable(page, size, sortBy, sortDir, CUSTOMER_SORT_FIELDS);

        List<SearchCriteria> criteria = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            criteria.add(new SearchCriteria("status", SearchCriteria.Op.EQ, status.toUpperCase()));
        }
        if (search != null && !search.isBlank()) {
            criteria.add(new SearchCriteria("fullNames", SearchCriteria.Op.LIKE, search));
        }

        Page<CustomerDto> customerPage = customerService.getAllCustomers(criteria, pageable);
        return ResponseBuilder.ok(PageResponse.of(customerPage), "Customers retrieved successfully", request);
    }

    @GetMapping("/me/meters")
    @PreAuthorize(SecurityRoles.CUSTOMER)
    @Operation(summary = "List meters assigned to the authenticated customer — CUSTOMER only")
    public ResponseEntity<ApiResponse<PageResponse<MeterDto>>> getMyMeters(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @Parameter(description = "Filter by meter type: WATER, ELECTRICITY")
            @RequestParam(required = false) String meterType,
            @Parameter(description = "Filter by status: ACTIVE, INACTIVE")
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        Pageable pageable = PaginationUtil.toPageable(page, size, sortBy, sortDir, METER_SORT_FIELDS);

        List<SearchCriteria> criteria = new ArrayList<>();
        if (meterType != null && !meterType.isBlank()) {
            criteria.add(new SearchCriteria("meterType", SearchCriteria.Op.EQ, meterType.toUpperCase()));
        }
        if (status != null && !status.isBlank()) {
            criteria.add(new SearchCriteria("status", SearchCriteria.Op.EQ, status.toUpperCase()));
        }

        Page<MeterDto> meterPage = meterService.getMetersForPortalUser(
                principal.getUsername(),
                criteria,
                pageable
        );

        return ResponseBuilder.ok(PageResponse.of(meterPage), "Meters retrieved successfully", request);
    }

    @GetMapping("/me/readings")
    @PreAuthorize(SecurityRoles.CUSTOMER)
    @Operation(summary = "List meter readings for the authenticated customer — CUSTOMER only")
    public ResponseEntity<ApiResponse<PageResponse<MeterReadingDto>>> getMyReadings(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @Parameter(description = "Filter by meter ID (must belong to the customer)")
            @RequestParam(required = false) UUID meterId,
            @Parameter(description = "Filter by billing month (1–12)")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Filter by billing year")
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        Pageable pageable = PaginationUtil.toPageable(page, size, sortBy, sortDir, READING_SORT_FIELDS);

        List<SearchCriteria> criteria = new ArrayList<>();
        if (month != null) {
            criteria.add(new SearchCriteria("billingMonth", SearchCriteria.Op.EQ, month));
        }
        if (year != null) {
            criteria.add(new SearchCriteria("billingYear", SearchCriteria.Op.EQ, year));
        }

        Page<MeterReadingDto> readingPage = meterReadingService.getReadingsForPortalUser(
                principal.getUsername(),
                meterId,
                criteria,
                pageable
        );

        return ResponseBuilder.ok(PageResponse.of(readingPage), "Readings retrieved successfully", request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE)
    @Operation(summary = "Get customer by ID — ADMIN or FINANCE")
    public ResponseEntity<ApiResponse<CustomerDto>> getCustomerById(
            @PathVariable UUID id,
            HttpServletRequest request) {

        return ResponseBuilder.ok(customerService.getCustomerById(id), "Customer retrieved successfully", request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Update customer details — ADMIN only")
    public ResponseEntity<ApiResponse<CustomerDto>> updateCustomer(
            @PathVariable UUID id,
            @Validated(ValidationGroups.OnPatch.class) @RequestBody UpdateCustomerRequest body,
            HttpServletRequest request) {

        return ResponseBuilder.ok(customerService.updateCustomer(id, body), "Customer updated successfully", request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Deactivate a customer (soft-delete) — ADMIN only")
    public ResponseEntity<ApiResponse<CustomerDto>> deactivateCustomer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        return ResponseBuilder.ok(
                customerService.deactivateCustomer(id, principal.getUsername()),
                "Customer deactivated successfully",
                request
        );
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Reactivate a customer — ADMIN only")
    public ResponseEntity<ApiResponse<CustomerDto>> activateCustomer(
            @PathVariable UUID id,
            HttpServletRequest request) {

        return ResponseBuilder.ok(customerService.activateCustomer(id), "Customer activated successfully", request);
    }

    @PostMapping("/{id}/link-user")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Link an existing CUSTOMER user to this customer record — ADMIN only")
    public ResponseEntity<ApiResponse<CustomerDto>> linkUser(
            @PathVariable UUID id,
            @Validated(ValidationGroups.OnCreate.class) @RequestBody LinkUserRequest body,
            HttpServletRequest request) {

        return ResponseBuilder.ok(customerService.linkUser(id, body), "User linked successfully", request);
    }

    @PostMapping("/{id}/meters")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Attach a meter to a customer — ADMIN only")
    public ResponseEntity<ApiResponse<MeterDto>> attachMeter(
            @PathVariable UUID id,
            @Validated(ValidationGroups.OnCreate.class) @RequestBody CreateMeterRequest body,
            HttpServletRequest request) {

        MeterDto dto = meterService.attachMeter(id, body);
        return ResponseBuilder.created(dto, "Meter attached successfully", request);
    }
}
