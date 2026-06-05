package com.spring.JavaT.reading.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Captured meter reading data")
public class MeterReadingDto {

    @Schema(example = "100")
    private final UUID id;

    @Schema(example = "10")
    private final UUID meterId;

    @Schema(example = "WTR-KGL-001234")
    private final String meterNumber;

    @Schema(example = "1250.50")
    private final BigDecimal previousReading;

    @Schema(example = "1285.75")
    private final BigDecimal currentReading;

    @Schema(example = "35.25")
    private final BigDecimal consumption;

    @Schema(example = "2026-05-28")
    private final LocalDate readingDate;

    @Schema(example = "5")
    private final int billingMonth;

    @Schema(example = "2026")
    private final int billingYear;

    @Schema(description = "Email of the operator who captured the reading", example = "operator@wasac.gov.rw")
    private final String capturedBy;

    private final Instant createdAt;
}
