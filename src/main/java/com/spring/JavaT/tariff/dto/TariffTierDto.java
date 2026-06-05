package com.spring.JavaT.tariff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "A consumption band within a tiered tariff")
public class TariffTierDto {

    private final UUID id;

    @Schema(example = "0")
    private final BigDecimal minUnits;

    @Schema(example = "50")
    private final BigDecimal maxUnits;

    @Schema(example = "120.00")
    private final BigDecimal ratePerUnit;
}
