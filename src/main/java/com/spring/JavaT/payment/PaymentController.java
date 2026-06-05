package com.spring.JavaT.payment;

import com.spring.JavaT.common.ApiResponse;
import com.spring.JavaT.common.ResponseBuilder;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.common.pagination.PageResponse;
import com.spring.JavaT.common.pagination.PaginationUtil;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.payment.dto.CreatePaymentRequest;
import com.spring.JavaT.payment.dto.PaymentDto;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Bill payment recording and retrieval")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
public class PaymentController {

    private final PaymentService paymentService;

    private static final Set<String> PAYMENT_SORT_FIELDS = Set.of(
            "id", "amountPaid", "paymentDate", "paymentMethod", "createdAt"
    );

    @PostMapping
    @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE)
    @Operation(summary = "Record a payment against a bill — ADMIN or FINANCE")
    public ResponseEntity<ApiResponse<PaymentDto>> recordPayment(
            @Validated(ValidationGroups.OnCreate.class) @RequestBody CreatePaymentRequest body,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        PaymentDto dto = paymentService.recordPayment(body, principal.getUsername());
        return ResponseBuilder.created(dto, "Payment recorded successfully", request);
    }

    @GetMapping
    @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE_OR_CUSTOMER)
    @Operation(summary = "List payments — staff see all; customers see own bills only")
    public ResponseEntity<ApiResponse<PageResponse<PaymentDto>>> getPayments(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @Parameter(description = "Filter by bill ID")
            @RequestParam(required = false) UUID billId,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        Pageable pageable = PaginationUtil.toPageable(page, size, sortBy, sortDir, PAYMENT_SORT_FIELDS);
        List<SearchCriteria> criteria = Collections.emptyList();

        Page<PaymentDto> paymentPage = isCustomer(principal)
                ? paymentService.getPaymentsForCustomer(principal.getUsername(), billId, criteria, pageable)
                : paymentService.getPaymentsForStaff(billId, criteria, pageable);

        return ResponseBuilder.ok(PageResponse.of(paymentPage), "Payments retrieved successfully", request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityRoles.ADMIN_OR_FINANCE_OR_CUSTOMER)
    @Operation(summary = "Get a payment by ID — staff see any; customers see own bills only")
    public ResponseEntity<ApiResponse<PaymentDto>> getPaymentById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        PaymentDto dto = isCustomer(principal)
                ? paymentService.getPaymentByIdForCustomer(id, principal.getUsername())
                : paymentService.getPaymentByIdForStaff(id);

        return ResponseBuilder.ok(dto, "Payment retrieved successfully", request);
    }

    private boolean isCustomer(UserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> Role.CUSTOMER.name().equals(a.getAuthority().replace("ROLE_", "")));
    }
}
