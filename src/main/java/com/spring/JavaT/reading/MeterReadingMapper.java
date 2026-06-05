package com.spring.JavaT.reading;

import com.spring.JavaT.reading.dto.MeterReadingDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MeterReadingMapper {

    @Mapping(source = "meter.id", target = "meterId")
    @Mapping(source = "meter.meterNumber", target = "meterNumber")
    @Mapping(source = "capturedBy.email", target = "capturedBy")
    @Mapping(expression = "java(reading.getConsumption())", target = "consumption")
    MeterReadingDto toDto(MeterReading reading);
}
