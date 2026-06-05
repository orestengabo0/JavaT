package com.spring.JavaT.payment;

import com.spring.JavaT.billing.Bill;
import com.spring.JavaT.billing.BillRepository;
import com.spring.JavaT.billing.BillStatus;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.customer.Customer;
import com.spring.JavaT.customer.CustomerRepository;
import com.spring.JavaT.notification.NotificationDispatchTrigger;
import com.spring.JavaT.exception.BusinessException;
import com.spring.JavaT.exception.DuplicateResourceException;
import com.spring.JavaT.exception.ResourceNotFoundException;
import com.spring.JavaT.payment.dto.CreatePaymentRequest;
import com.spring.JavaT.payment.dto.PaymentDto;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Records and retrieves bill payments with concurrency-safe balance updates.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository  paymentRepository;
    private final BillRepository     billRepository;
    private final UserRepository     userRepository;
    private final CustomerRepository customerRepository;
    private final PaymentMapper                 paymentMapper;
    private final NotificationDispatchTrigger   notificationDispatchTrigger;

    // -------------------------------------------------------------------------
    // Record payment
    // -------------------------------------------------------------------------

    @Transactional
    public PaymentDto recordPayment(CreatePaymentRequest request, String recorderEmail) {
        assertUniqueReference(request.getReferenceNumber().strip());

        Bill bill = billRepository.findByIdForUpdate(request.getBillId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill", "id", request.getBillId()));

        validateBillPayable(bill);
        validateAmount(request.getAmountPaid(), bill.getBalance());
        validatePaymentDate(request.getPaymentDate(), bill);

        User recorder = userRepository.findByEmail(recorderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", recorderEmail));

        BigDecimal newBalance = bill.getBalance().subtract(request.getAmountPaid());
        bill.setBalance(newBalance);
        bill.setAmountPaid(bill.getAmountPaid().add(request.getAmountPaid()));

        Payment payment = Payment.builder()
                .bill(bill)
                .amountPaid(request.getAmountPaid())
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
                .paymentDate(request.getPaymentDate())
                .referenceNumber(request.getReferenceNumber().strip())
                .recordedBy(recorder)
                .build();

        billRepository.saveAndFlush(bill);
        Payment saved = paymentRepository.save(payment);

        Bill refreshedBill = billRepository.findById(bill.getId()).orElseThrow();
        PaymentDto dto = paymentMapper.toDto(saved, refreshedBill.getBalance());
        notificationDispatchTrigger.dispatchAfterCommit();
        return dto;
    }

    // -------------------------------------------------------------------------
    // Read — staff sees all; customers see payments on their own bills
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<PaymentDto> getPaymentsForStaff(UUID billId, List<SearchCriteria> criteria, Pageable pageable) {
        Specification<Payment> spec = new PaymentSpecification(criteria, billId);
        return paymentRepository.findAll(spec, pageable)
                .map(p -> paymentMapper.toDto(p, p.getBill().getBalance()));
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getPaymentsForCustomer(String customerEmail, UUID billId,
                                                   List<SearchCriteria> criteria, Pageable pageable) {
        Customer customer = requireCustomerForPortalUser(customerEmail);

        if (billId != null) {
            assertBillOwnedByCustomer(billId, customer.getId());
        }

        Specification<Payment> spec = new PaymentSpecification(criteria, billId, customer.getId());
        return paymentRepository.findAll(spec, pageable)
                .map(p -> paymentMapper.toDto(p, p.getBill().getBalance()));
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentByIdForStaff(UUID id) {
        Payment payment = findByIdOrThrow(id);
        return paymentMapper.toDto(payment, payment.getBill().getBalance());
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentByIdForCustomer(UUID id, String customerEmail) {
        Payment payment = findByIdOrThrow(id);
        Customer customer = requireCustomerForPortalUser(customerEmail);
        assertBillOwnedByCustomer(payment.getBill().getId(), customer.getId());
        return paymentMapper.toDto(payment, payment.getBill().getBalance());
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private void validateBillPayable(Bill bill) {
        BillStatus status = bill.getBillStatus();

        if (status == BillStatus.PENDING || status == BillStatus.DRAFT) {
            throw new BusinessException(
                    "Payments can only be recorded against approved bills",
                    HttpStatus.BAD_REQUEST,
                    "BILL_NOT_APPROVED"
            );
        }

        if (bill.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "This bill has no outstanding balance",
                    HttpStatus.CONFLICT,
                    "BILL_ALREADY_PAID"
            );
        }

        if (status != BillStatus.APPROVED && status != BillStatus.OVERDUE) {
            throw new BusinessException(
                    "Bill status %s does not allow payment".formatted(status),
                    HttpStatus.BAD_REQUEST,
                    "BILL_NOT_PAYABLE"
            );
        }
    }

    private void validateAmount(BigDecimal amount, BigDecimal balance) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "Payment amount must be greater than zero",
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_AMOUNT_INVALID"
            );
        }
        if (amount.compareTo(balance) > 0) {
            throw new BusinessException(
                    "Payment amount exceeds outstanding balance of %s".formatted(balance),
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_EXCEEDS_BALANCE"
            );
        }
    }

    private void validatePaymentDate(LocalDate paymentDate, Bill bill) {
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new BusinessException(
                    "Payment date cannot be in the future",
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_DATE_INVALID"
            );
        }
        if (bill.getApprovedAt() != null
                && paymentDate.isBefore(bill.getApprovedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate())) {
            throw new BusinessException(
                    "Payment date cannot be before the bill approval date",
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_DATE_INVALID"
            );
        }
    }

    private void assertUniqueReference(String referenceNumber) {
        if (paymentRepository.existsByReferenceNumber(referenceNumber)) {
            throw new DuplicateResourceException("Payment", "referenceNumber", referenceNumber);
        }
    }

    private void assertBillOwnedByCustomer(UUID billId, UUID customerId) {
        Bill bill = billRepository.findById(billId)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Bill", "id", billId));

        if (!bill.getCustomer().getId().equals(customerId)) {
            throw new BusinessException(
                    "You do not have access to payments for this bill",
                    HttpStatus.FORBIDDEN,
                    "PAYMENT_ACCESS_DENIED"
            );
        }
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

    private Payment findByIdOrThrow(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
    }
}
