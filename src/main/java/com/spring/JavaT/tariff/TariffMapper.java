package com.spring.JavaT.tariff;

import com.spring.JavaT.tariff.dto.TariffTierDto;
import com.spring.JavaT.tariff.dto.TariffVersionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TariffMapper {

    @Mapping(source = "tiers", target = "tiers")
    TariffVersionDto toDto(TariffVersion tariff);

    TariffTierDto toTierDto(TariffTier tier);

    List<TariffTierDto> toTierDtoList(List<TariffTier> tiers);
}
