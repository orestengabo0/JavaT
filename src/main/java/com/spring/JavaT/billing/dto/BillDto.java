package com.spring.JavaT.billing.dto;

import com.spring.JavaT.billing.BillStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Monthly utility bill")
public class BillDto {

    private final UUID id;
    private final UUID customerId;
    private final String customerName;
    private final UUID meterId;
    private final String meterNumber;
    private final UUID tariffVersionId;
    private final String tariffName;
    private final int billingMonth;
    private final int billingYear;
    private final BigDecimal consumption;
    private final BigDecimal subtotal;
    private final BigDecimal taxAmount;
    private final BigDecimal penaltyAmount;
    private final BigDecimal totalAmount;
    private final BigDecimal amountPaid;
    private final BigDecimal balance;
    private final BillStatus status;
    private final String approvedBy;
    private final Instant approvedAt;
    private final LocalDate dueDate;
    private final Instant createdAt;
}
