package com.spring.JavaT.billing;

import com.spring.JavaT.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock private BillRepository billRepository;
    @Mock private BillMapper billMapper;

    @InjectMocks
    private BillingService billingService;

    @Test
    void cancelBill_rejectsNonPendingBill() {
        UUID billId = UUID.randomUUID();
        Bill bill = Bill.builder()
                .billStatus(BillStatus.APPROVED)
                .balance(new BigDecimal("1000.00"))
                .dueDate(LocalDate.now())
                .build();
        bill.setId(billId);

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));

        assertThatThrownBy(() -> billingService.cancelBill(billId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending");
    }

    @Test
    void cancelBill_setsCancelledAndZeroBalance() {
        UUID billId = UUID.randomUUID();
        Bill bill = Bill.builder()
                .billStatus(BillStatus.PENDING)
                .balance(new BigDecimal("5000.00"))
                .dueDate(LocalDate.now())
                .build();
        bill.setId(billId);

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(billRepository.save(bill)).thenReturn(bill);

        billingService.cancelBill(billId);

        verify(billRepository).save(bill);
        org.assertj.core.api.Assertions.assertThat(bill.getBillStatus()).isEqualTo(BillStatus.CANCELLED);
        org.assertj.core.api.Assertions.assertThat(bill.getBalance()).isEqualByComparingTo("0");
    }
}
