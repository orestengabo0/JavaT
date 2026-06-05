package com.spring.JavaT.meter.dto;

import com.spring.JavaT.common.EntityStatus;
import com.spring.JavaT.meter.MeterType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Utility meter data")
public class MeterDto {

    @Schema(example = "10")
    private final UUID id;

    @Schema(example = "1")
    private final UUID customerId;

    @Schema(example = "WTR-KGL-001234")
    private final String meterNumber;

    @Schema(example = "WATER")
    private final MeterType meterType;

    @Schema(example = "2024-03-15")
    private final LocalDate installationDate;

    @Schema(example = "ACTIVE")
    private final EntityStatus status;

    private final Instant createdAt;

    private final Instant updatedAt;
}
