package com.spring.JavaT.billing;

import com.spring.JavaT.common.EntityStatus;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.customer.Customer;
import com.spring.JavaT.customer.CustomerRepository;
import com.spring.JavaT.customer.CustomerService;
import com.spring.JavaT.exception.BusinessException;
import com.spring.JavaT.exception.DuplicateResourceException;
import com.spring.JavaT.exception.ResourceNotFoundException;
import com.spring.JavaT.meter.Meter;
import com.spring.JavaT.meter.MeterService;
import com.spring.JavaT.reading.MeterReading;
import com.spring.JavaT.reading.MeterReadingService;
import com.spring.JavaT.billing.dto.BillDto;
import com.spring.JavaT.billing.dto.GenerateBillRequest;
import com.spring.JavaT.notification.NotificationDispatchTrigger;
import com.spring.JavaT.tariff.TariffService;
import com.spring.JavaT.tariff.TariffVersion;
import com.spring.JavaT.user.Role;
import com.spring.JavaT.user.User;
import com.spring.JavaT.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Bill generation, approval, and customer-scoped retrieval.
 */
@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillRepository         billRepository;
    private final CustomerService        customerService;
    private final CustomerRepository     customerRepository;
    private final MeterService           meterService;
    private final MeterReadingService    meterReadingService;
    private final TariffService          tariffService;
    private final UserRepository         userRepository;
    private final BillMapper                   billMapper;
    private final NotificationDispatchTrigger  notificationDispatchTrigger;

    // -------------------------------------------------------------------------
    // Generate / approve
    // -------------------------------------------------------------------------

    @Transactional
    public BillDto generateBill(GenerateBillRequest request) {
        Meter meter = meterService.requireActiveMeter(request.getMeterId());
        Customer customer = customerService.requireActiveCustomer(meter.getCustomer().getId());

        int month = request.getBillingMonth();
        int year  = request.getBillingYear();

        assertNoDuplicateBill(meter.getId(), month, year);

        MeterReading reading = meterReadingService.requireReadingForPeriod(meter.getId(), month, year);

        LocalDate billDate = lastDayOfMonth(year, month);
        TariffVersion tariff = tariffService.requireEffectiveTariff(meter.getMeterType(), billDate);

        BigDecimal consumption = reading.getConsumption();
        BigDecimal subtotal    = tariffService.calculateSubtotal(tariff, consumption);
        BigDecimal taxAmount   = tariffService.calculateTaxAmount(subtotal, tariff);
        BigDecimal penaltyAmount = resolvePenaltyAmount(meter.getId(), month, year, tariff);

        BigDecimal totalAmount = subtotal.add(taxAmount).add(penaltyAmount);
        LocalDate dueDate = dueDateForPeriod(year, month);

        Bill bill = Bill.builder()
                .customer(customer)
                .meter(meter)
                .tariffVersion(tariff)
                .billingMonth(month)
                .billingYear(year)
                .consumption(consumption)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .penaltyAmount(penaltyAmount)
                .totalAmount(totalAmount)
                .amountPaid(BigDecimal.ZERO)
                .balance(totalAmount)
                .billStatus(BillStatus.PENDING)
                .dueDate(dueDate)
                .build();
        bill.setStatus(EntityStatus.ACTIVE);

        BillDto dto = billMapper.toDto(billRepository.save(bill));
        notificationDispatchTrigger.dispatchAfterCommit();
        return dto;
    }

    @Transactional
    public BillDto approveBill(UUID billId, String approverEmail) {
        User approver = requireStaffApprover(approverEmail);
        Bill bill = findByIdOrThrow(billId);

        if (bill.getBillStatus() != BillStatus.PENDING) {
            throw new BusinessException(
                    "Only pending bills can be approved",
                    HttpStatus.BAD_REQUEST,
                    "BILL_NOT_PENDING"
            );
        }

        bill.setBillStatus(BillStatus.APPROVED);
        bill.setApprovedBy(approver);
        bill.setApprovedAt(Instant.now());

        return billMapper.toDto(billRepository.save(bill));
    }

    /**
     * Voids a bill that has not yet been approved, freeing the billing period for re-generation.
     */
    @Transactional
    public BillDto cancelBill(UUID billId) {
        Bill bill = findByIdOrThrow(billId);

        if (bill.getBillStatus() != BillStatus.PENDING) {
            throw new BusinessException(
                    "Only pending bills can be cancelled",
                    HttpStatus.BAD_REQUEST,
                    "BILL_NOT_PENDING"
            );
        }

        bill.setBillStatus(BillStatus.CANCELLED);
        bill.setBalance(BigDecimal.ZERO);

        return billMapper.toDto(billRepository.save(bill));
    }

    // -------------------------------------------------------------------------
    // Read — staff sees all; customers see own bills only
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<BillDto> getAllBillsForStaff(List<SearchCriteria> criteria, Pageable pageable) {
        Specification<Bill> spec = new BillSpecification(criteria);
        return billRepository.findAll(spec, pageable).map(billMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<BillDto> getAllBillsForCustomer(String customerEmail, List<SearchCriteria> criteria, Pageable pageable) {
        Customer customer = requireCustomerForPortalUser(customerEmail);
        Specification<Bill> spec = new BillSpecification(criteria, customer.getId());
        return billRepository.findAll(spec, pageable).map(billMapper::toDto);
    }

    @Transactional(readOnly = true)
    public BillDto getBillByIdForStaff(UUID id) {
        return billMapper.toDto(findByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public BillDto getBillByIdForCustomer(UUID id, String customerEmail) {
        Bill bill = findByIdOrThrow(id);
        Customer customer = requireCustomerForPortalUser(customerEmail);

        if (!bill.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException(
                    "You do not have access to this bill",
                    HttpStatus.FORBIDDEN,
                    "BILL_ACCESS_DENIED"
            );
        }

        return billMapper.toDto(bill);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private BigDecimal resolvePenaltyAmount(UUID meterId, int month, int year, TariffVersion tariff) {
        return billRepository.findLatestBeforePeriod(meterId, month, year)
                .map(prior -> calculatePenaltyForPriorBill(prior, tariff))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * If the prior bill is overdue and unpaid (past due date + grace days),
     * marks it {@link BillStatus#OVERDUE} and returns penalty on its balance.
     */
    private BigDecimal calculatePenaltyForPriorBill(Bill prior, TariffVersion tariff) {
        if (prior.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        LocalDate penaltyStart = prior.getDueDate().plusDays(tariff.getPenaltyGraceDays());
        if (!LocalDate.now().isAfter(penaltyStart)) {
            return BigDecimal.ZERO;
        }

        if (prior.getBillStatus() == BillStatus.APPROVED) {
            prior.setBillStatus(BillStatus.OVERDUE);
            billRepository.save(prior);
        } else if (prior.getBillStatus() != BillStatus.OVERDUE) {
            return BigDecimal.ZERO;
        }

        return tariffService.calculatePenaltyAmount(prior.getBalance(), tariff);
    }

    private void assertNoDuplicateBill(UUID meterId, int month, int year) {
        if (billRepository.existsByMeterIdAndBillingMonthAndBillingYearAndDeletedFalseAndBillStatusNot(
                meterId, month, year, BillStatus.CANCELLED)) {
            throw new DuplicateResourceException(
                    "Bill",
                    "meterId/period",
                    "%s/%d/%d".formatted(meterId, month, year)
            );
        }
    }

    private User requireStaffApprover(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.FINANCE) {
            throw new BusinessException(
                    "Only ADMIN or FINANCE users can approve bills",
                    HttpStatus.FORBIDDEN,
                    "BILL_APPROVAL_DENIED"
            );
        }
        return user;
    }

    private Customer requireCustomerForPortalUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return customerRepository.findByUser_Id(user.getId())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        "No customer record linked to this portal account",
                        HttpStatus.FORBIDDEN,
                        "CUSTOMER_NOT_LINKED"
                ));
    }

    private Bill findByIdOrThrow(UUID id) {
        return billRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Bill", "id", id));
    }

    private LocalDate lastDayOfMonth(int year, int month) {
        return LocalDate.of(year, month, 1).withDayOfMonth(
                LocalDate.of(year, month, 1).lengthOfMonth()
        );
    }

    /** Payment due on the 15th of the month following the billing period. */
    private LocalDate dueDateForPeriod(int year, int month) {
        return LocalDate.of(year, month, 1).plusMonths(1).withDayOfMonth(15);
    }
}
