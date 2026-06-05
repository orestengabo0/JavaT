package com.spring.JavaT.payment.dto;

import com.spring.JavaT.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Recorded bill payment")
public class PaymentDto {

    private final UUID id;
    private final UUID billId;
    private final BigDecimal amountPaid;
    private final PaymentMethod paymentMethod;
    private final LocalDate paymentDate;
    private final String referenceNumber;
    private final String recordedBy;
    private final BigDecimal remainingBalance;
    private final Instant createdAt;
}
