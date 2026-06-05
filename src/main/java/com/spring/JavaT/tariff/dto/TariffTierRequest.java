package com.spring.JavaT.tariff.dto;

import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Consumption band for a tiered tariff")
public class TariffTierRequest {

    @Schema(example = "0")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @DecimalMin(value = "0", message = ValidationMessages.POSITIVE_OR_ZERO, groups = ValidationGroups.OnCreate.class)
    @Digits(integer = 10, fraction = 2, groups = ValidationGroups.OnCreate.class)
    private BigDecimal minUnits;

    @Schema(description = "Inclusive upper bound; omit for unlimited top tier", example = "50")
    @DecimalMin(value = "0", message = ValidationMessages.POSITIVE_OR_ZERO, groups = ValidationGroups.OnCreate.class)
    @Digits(integer = 10, fraction = 2, groups = ValidationGroups.OnCreate.class)
    private BigDecimal maxUnits;

    @Schema(example = "120.00")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @DecimalMin(value = "0.01", message = ValidationMessages.POSITIVE, groups = ValidationGroups.OnCreate.class)
    @Digits(integer = 10, fraction = 4, groups = ValidationGroups.OnCreate.class)
    private BigDecimal ratePerUnit;
}
