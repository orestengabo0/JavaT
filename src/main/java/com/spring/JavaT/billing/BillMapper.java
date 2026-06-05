package com.spring.JavaT.billing;

import com.spring.JavaT.billing.dto.BillDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BillMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.fullNames", target = "customerName")
    @Mapping(source = "meter.id", target = "meterId")
    @Mapping(source = "meter.meterNumber", target = "meterNumber")
    @Mapping(source = "tariffVersion.id", target = "tariffVersionId")
    @Mapping(source = "tariffVersion.name", target = "tariffName")
    @Mapping(source = "billStatus", target = "status")
    @Mapping(source = "approvedBy.email", target = "approvedBy")
    BillDto toDto(Bill bill);
}
