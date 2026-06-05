package com.spring.JavaT.meter;

import com.spring.JavaT.meter.dto.MeterDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MeterMapper {

    @Mapping(source = "customer.id", target = "customerId")
    MeterDto toDto(Meter meter);
}
