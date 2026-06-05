package com.spring.JavaT.reading.dto;

import com.spring.JavaT.common.validation.MeterReadingValues;
import com.spring.JavaT.common.validation.PastOrPresentDate;
import com.spring.JavaT.common.validation.ValidMeterReading;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@ValidMeterReading(groups = ValidationGroups.OnCreate.class)
@Schema(description = "Request body for capturing a meter reading")
public class CreateMeterReadingRequest implements MeterReadingValues {

    @Schema(description = "ID of the meter being read", example = "10")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    private UUID meterId;

    @Schema(description = "Previous meter reading (auto-filled from last capture when omitted)",
            example = "1250.50")
    @DecimalMin(value = "0", message = ValidationMessages.POSITIVE_OR_ZERO, groups = ValidationGroups.OnCreate.class)
    @Digits(integer = 10, fraction = 2, message = ValidationMessages.INVALID, groups = ValidationGroups.OnCreate.class)
    private BigDecimal previousReading;

    @Schema(description = "Current meter reading", example = "1285.75")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @DecimalMin(value = "0", message = ValidationMessages.POSITIVE_OR_ZERO, groups = ValidationGroups.OnCreate.class)
    @Digits(integer = 10, fraction = 2, message = ValidationMessages.INVALID, groups = ValidationGroups.OnCreate.class)
    private BigDecimal currentReading;

    @Schema(description = "Date the reading was taken", example = "2026-05-28")
    @NotNull(message = ValidationMessages.DATE_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @PastOrPresentDate(groups = ValidationGroups.OnCreate.class)
    private LocalDate readingDate;
}
