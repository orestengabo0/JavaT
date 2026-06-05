package com.spring.JavaT.payment.dto;

import com.spring.JavaT.common.validation.PastOrPresentDate;
import com.spring.JavaT.common.validation.ValidEnum;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import com.spring.JavaT.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Request body for recording a bill payment")
public class CreatePaymentRequest {

    @Schema(description = "Bill being paid", example = "10")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    private UUID billId;

    @Schema(description = "Amount paid in this transaction", example = "10000.00")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @DecimalMin(value = "0.01", message = ValidationMessages.POSITIVE, groups = ValidationGroups.OnCreate.class)
    @Digits(integer = 10, fraction = 2, message = ValidationMessages.INVALID, groups = ValidationGroups.OnCreate.class)
    private BigDecimal amountPaid;

    @Schema(description = "Payment channel", example = "MOBILE_MONEY")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @ValidEnum(enumClass = PaymentMethod.class, groups = ValidationGroups.OnCreate.class)
    private String paymentMethod;

    @Schema(description = "Date the payment was received", example = "2026-06-10")
    @NotNull(message = ValidationMessages.DATE_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @PastOrPresentDate(groups = ValidationGroups.OnCreate.class)
    private LocalDate paymentDate;

    @Schema(description = "Unique external transaction reference (e.g. mobile-money txn ID)",
            example = "MM-20260610-8F3A2B1C")
    @NotBlank(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @Size(min = 6, max = 64, message = ValidationMessages.INVALID, groups = ValidationGroups.OnCreate.class)
    private String referenceNumber;
}
