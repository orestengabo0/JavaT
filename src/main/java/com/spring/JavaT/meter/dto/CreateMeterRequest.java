package com.spring.JavaT.meter.dto;

import com.spring.JavaT.common.validation.PastOrPresentDate;
import com.spring.JavaT.common.validation.ValidEnum;
import com.spring.JavaT.common.validation.ValidMeterNumber;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import com.spring.JavaT.meter.MeterType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Request body for attaching a meter to a customer")
public class CreateMeterRequest {

    @Schema(example = "WTR-KGL-001234")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @ValidMeterNumber(groups = ValidationGroups.OnCreate.class)
    private String meterNumber;

    @Schema(example = "WATER", allowableValues = {"WATER", "ELECTRICITY"})
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @ValidEnum(enumClass = MeterType.class, groups = ValidationGroups.OnCreate.class)
    private String meterType;

    @Schema(example = "2024-03-15")
    @NotNull(message = ValidationMessages.DATE_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @PastOrPresentDate(groups = ValidationGroups.OnCreate.class)
    private LocalDate installationDate;
}
