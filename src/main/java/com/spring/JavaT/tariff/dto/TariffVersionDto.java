package com.spring.JavaT.tariff.dto;

import com.spring.JavaT.meter.MeterType;
import com.spring.JavaT.tariff.TariffType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Versioned tariff configuration including tax and penalty settings")
public class TariffVersionDto {

    private final UUID id;

    @Schema(example = "Water Standard 2026")
    private final String name;

    @Schema(example = "WATER")
    private final MeterType meterType;

    @Schema(example = "FLAT")
    private final TariffType tariffType;

    @Schema(example = "350.0000")
    private final BigDecimal flatRate;

    @Schema(example = "1500.00")
    private final BigDecimal fixedServiceCharge;

    @Schema(description = "VAT percentage", example = "18.00")
    private final BigDecimal taxRate;

    @Schema(description = "Late payment penalty percentage", example = "5.00")
    private final BigDecimal penaltyRate;

    @Schema(example = "15")
    private final int penaltyGraceDays;

    @Schema(example = "2026-07-01")
    private final LocalDate effectiveFrom;

    @Schema(example = "2026-12-31")
    private final LocalDate effectiveTo;

    @Schema(example = "true")
    private final boolean active;

    private final List<TariffTierDto> tiers;

    private final Instant createdAt;
}
