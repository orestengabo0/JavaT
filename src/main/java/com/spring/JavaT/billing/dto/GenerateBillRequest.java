package com.spring.JavaT.billing.dto;

import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "Request body for generating a monthly utility bill")
public class GenerateBillRequest {

    @Schema(description = "Meter to bill", example = "10")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    private UUID meterId;

    @Schema(description = "Billing month (1–12)", example = "5")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @Min(value = 1, message = ValidationMessages.INVALID, groups = ValidationGroups.OnCreate.class)
    @Max(value = 12, message = ValidationMessages.INVALID, groups = ValidationGroups.OnCreate.class)
    private Integer billingMonth;

    @Schema(description = "Billing year", example = "2026")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @Min(value = 2020, message = ValidationMessages.INVALID, groups = ValidationGroups.OnCreate.class)
    private Integer billingYear;
}
