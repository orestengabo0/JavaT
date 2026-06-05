package com.spring.JavaT.payment;

import com.spring.JavaT.payment.dto.PaymentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

    @Mapping(source = "payment.bill.id", target = "billId")
    @Mapping(source = "payment.recordedBy.email", target = "recordedBy")
    @Mapping(source = "remainingBalance", target = "remainingBalance")
    PaymentDto toDto(Payment payment, BigDecimal remainingBalance);
}
