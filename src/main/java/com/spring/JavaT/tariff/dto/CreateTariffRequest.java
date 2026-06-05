package com.spring.JavaT.tariff.dto;

import com.spring.JavaT.common.validation.FutureOrPresentDate;
import com.spring.JavaT.common.validation.ValidEnum;
import com.spring.JavaT.common.validation.ValidationGroups;
import com.spring.JavaT.common.validation.ValidationMessages;
import com.spring.JavaT.meter.MeterType;
import com.spring.JavaT.tariff.TariffType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Schema(description = "Request body for creating a new versioned tariff")
public class CreateTariffRequest {

    @Schema(example = "Water Standard 2026")
    @NotBlank(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @Size(min = 2, max = 100, groups = ValidationGroups.OnCreate.class)
    private String name;

    @Schema(example = "WATER", allowableValues = {"WATER", "ELECTRICITY"})
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @ValidEnum(enumClass = MeterType.class, groups = ValidationGroups.OnCreate.class)
    private String meterType;

    @Schema(example = "FLAT", allowableValues = {"FLAT", "TIERED"})
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @ValidEnum(enumClass = TariffType.class, groups = ValidationGroups.OnCreate.class)
    private String tariffType;

    @Schema(description = "Required when tariffType is FLAT", example = "350.00")
    @DecimalMin(value = "0.01", message = ValidationMessages.POSITIVE, groups = ValidationGroups.OnCreate.class)
    @Digits(integer = 10, fraction = 4, groups = ValidationGroups.OnCreate.class)
    private BigDecimal flatRate;

    @Schema(example = "1500.00")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @DecimalMin(value = "0", message = ValidationMessages.POSITIVE_OR_ZERO, groups = ValidationGroups.OnCreate.class)
    @Digits(integer = 10, fraction = 2, groups = ValidationGroups.OnCreate.class)
    private BigDecimal fixedServiceCharge;

    @Schema(description = "VAT percentage", example = "18.00")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @DecimalMin(value = "0", message = ValidationMessages.POSITIVE_OR_ZERO, groups = ValidationGroups.OnCreate.class)
    @DecimalMax(value = "100", message = ValidationMessages.MAX_VALUE, groups = ValidationGroups.OnCreate.class)
    @Digits(integer = 3, fraction = 2, groups = ValidationGroups.OnCreate.class)
    private BigDecimal taxRate;

    @Schema(description = "Late payment penalty percentage", example = "5.00")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @DecimalMin(value = "0", message = ValidationMessages.POSITIVE_OR_ZERO, groups = ValidationGroups.OnCreate.class)
    @DecimalMax(value = "100", message = ValidationMessages.MAX_VALUE, groups = ValidationGroups.OnCreate.class)
    @Digits(integer = 3, fraction = 2, groups = ValidationGroups.OnCreate.class)
    private BigDecimal penaltyRate;

    @Schema(example = "15")
    @NotNull(message = ValidationMessages.REQUIRED, groups = ValidationGroups.OnCreate.class)
    @Min(value = 0, groups = ValidationGroups.OnCreate.class)
    @Max(value = 90, groups = ValidationGroups.OnCreate.class)
    private Integer penaltyGraceDays;

    @Schema(description = "Date from which this version applies to future billing cycles", example = "2026-07-01")
    @NotNull(message = ValidationMessages.DATE_REQUIRED, groups = ValidationGroups.OnCreate.class)
    @FutureOrPresentDate(groups = ValidationGroups.OnCreate.class)
    private LocalDate effectiveFrom;

    @Schema(description = "Required when tariffType is TIERED")
    @Valid
    private List<TariffTierRequest> tiers;
}
