package com.spring.JavaT.payment;

import com.spring.JavaT.billing.Bill;
import com.spring.JavaT.billing.BillRepository;
import com.spring.JavaT.billing.BillStatus;
import com.spring.JavaT.customer.Customer;
import com.spring.JavaT.customer.CustomerRepository;
import com.spring.JavaT.exception.BusinessException;
import com.spring.JavaT.notification.NotificationDispatchTrigger;
import com.spring.JavaT.payment.dto.CreatePaymentRequest;
import com.spring.JavaT.user.User;
import com.spring.JavaT.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BillRepository billRepository;
    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private NotificationDispatchTrigger notificationDispatchTrigger;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void recordPayment_rejectsPendingBill() {
        UUID billId = UUID.randomUUID();
        Bill bill = approvedBill(billId);
        bill.setBillStatus(BillStatus.PENDING);

        when(paymentRepository.existsByReferenceNumber("MM-TEST-001")).thenReturn(false);
        when(billRepository.findByIdForUpdate(billId)).thenReturn(Optional.of(bill));

        CreatePaymentRequest request = paymentRequest(billId, "5000.00", "MM-TEST-001");

        assertThatThrownBy(() -> paymentService.recordPayment(request, "finance@wasac.gov.rw"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("approved");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void recordPayment_rejectsAmountExceedingBalance() {
        UUID billId = UUID.randomUUID();
        Bill bill = approvedBill(billId);
        bill.setBalance(new BigDecimal("1000.00"));

        when(paymentRepository.existsByReferenceNumber("MM-TEST-002")).thenReturn(false);
        when(billRepository.findByIdForUpdate(billId)).thenReturn(Optional.of(bill));

        CreatePaymentRequest request = paymentRequest(billId, "1500.00", "MM-TEST-002");

        assertThatThrownBy(() -> paymentService.recordPayment(request, "finance@wasac.gov.rw"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds outstanding balance");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void recordPayment_rejectsDuplicateReferenceNumber() {
        when(paymentRepository.existsByReferenceNumber("MM-DUP-001")).thenReturn(true);

        CreatePaymentRequest request = paymentRequest(UUID.randomUUID(), "100.00", "MM-DUP-001");

        assertThatThrownBy(() -> paymentService.recordPayment(request, "finance@wasac.gov.rw"))
                .isInstanceOf(com.spring.JavaT.exception.DuplicateResourceException.class);

        verify(billRepository, never()).findByIdForUpdate(any());
    }

    private static Bill approvedBill(UUID billId) {
        Customer customer = Customer.builder().fullNames("Test Customer").build();
        customer.setId(UUID.randomUUID());

        Bill bill = Bill.builder()
                .customer(customer)
                .balance(new BigDecimal("10000.00"))
                .amountPaid(BigDecimal.ZERO)
                .billStatus(BillStatus.APPROVED)
                .dueDate(LocalDate.now().plusDays(30))
                .build();
        bill.setId(billId);
        bill.setApprovedAt(Instant.now().minusSeconds(86_400));
        return bill;
    }

    private static CreatePaymentRequest paymentRequest(UUID billId, String amount, String reference) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBillId(billId);
        request.setAmountPaid(new BigDecimal(amount));
        request.setPaymentMethod("MOBILE_MONEY");
        request.setPaymentDate(LocalDate.now());
        request.setReferenceNumber(reference);
        return request;
    }
}
