package com.spring.JavaT.billing;

import com.spring.JavaT.common.ApiResponse;
import com.spring.JavaT.common.ResponseBuilder;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.common.pagination.PageResponse;
import com.spring.JavaT.common.pagination.PaginationUtil;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.billing.dto.BillDto;
import com.spring.JavaT.billing.dto.GenerateBillRequest;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Bill generation, approval, and retrieval")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
public class BillController {

    private final BillingService billingService;

    private static final Set<String> BILL_SORT_FIELDS = Set.of(
            "id", "billingMonth", "billingYear", "totalAmount", "balance",
            "billStatus", "dueDate", "createdAt"
    );

    @PostMapping("/generate")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Generate a monthly bill for a meter — ADMIN only")
    public ResponseEntity<ApiResponse<BillDto>> generateBill(
            @Validated(ValidationGroups.OnCreate.class) @RequestBody GenerateBillRequest body,
            HttpServletRequest request) {

        BillDto dto = billingService.generateBill(body);
        return ResponseBuilder.created(dto, "Bill generated successfully", request);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE)
    @Operation(summary = "Approve a pending bill — ADMIN or FINANCE")
    public ResponseEntity<ApiResponse<BillDto>> approveBill(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        BillDto dto = billingService.approveBill(id, principal.getUsername());
        return ResponseBuilder.ok(dto, "Bill approved successfully", request);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Cancel a pending bill — ADMIN only")
    public ResponseEntity<ApiResponse<BillDto>> cancelBill(
            @PathVariable UUID id,
            HttpServletRequest request) {

        BillDto dto = billingService.cancelBill(id);
        return ResponseBuilder.ok(dto, "Bill cancelled successfully", request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "Soft-delete a pending or cancelled bill — ADMIN only")
    public ResponseEntity<ApiResponse<BillDto>> deleteBill(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        BillDto dto = billingService.deleteBill(id, principal.getUsername());
        return ResponseBuilder.ok(dto, "Bill deleted successfully", request);
    }

    @GetMapping
    @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE_OR_CUSTOMER)
    @Operation(summary = "List bills — staff see all; customers see their own")
    public ResponseEntity<ApiResponse<PageResponse<BillDto>>> getAllBills(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @Parameter(description = "Filter by bill status: DRAFT, PENDING, APPROVED, PAID, OVERDUE, CANCELLED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by billing month (1–12)")
            @RequestParam(required = false) Integer billingMonth,
            @Parameter(description = "Filter by billing year")
            @RequestParam(required = false) Integer billingYear,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        Pageable pageable = PaginationUtil.toPageable(page, size, sortBy, sortDir, BILL_SORT_FIELDS);
        List<SearchCriteria> criteria = buildCriteria(status, billingMonth, billingYear);

        Page<BillDto> billPage = isCustomer(principal)
                ? billingService.getAllBillsForCustomer(principal.getUsername(), criteria, pageable)
                : billingService.getAllBillsForStaff(criteria, pageable);

        return ResponseBuilder.ok(PageResponse.of(billPage), "Bills retrieved successfully", request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE_OR_CUSTOMER)
    @Operation(summary = "Get a bill by ID — staff see any; customers see their own")
    public ResponseEntity<ApiResponse<BillDto>> getBillById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        BillDto dto = isCustomer(principal)
                ? billingService.getBillByIdForCustomer(id, principal.getUsername())
                : billingService.getBillByIdForStaff(id);

        return ResponseBuilder.ok(dto, "Bill retrieved successfully", request);
    }

    private List<SearchCriteria> buildCriteria(String status, Integer billingMonth, Integer billingYear) {
        List<SearchCriteria> criteria = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            criteria.add(new SearchCriteria("billStatus", SearchCriteria.Op.EQ, status.toUpperCase()));
        }
        if (billingMonth != null) {
            criteria.add(new SearchCriteria("billingMonth", SearchCriteria.Op.EQ, billingMonth));
        }
        if (billingYear != null) {
            criteria.add(new SearchCriteria("billingYear", SearchCriteria.Op.EQ, billingYear));
        }
        return criteria;
    }

    private boolean isCustomer(UserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> Role.CUSTOMER.name().equals(a.getAuthority().replace("ROLE_", "")));
    }
}
