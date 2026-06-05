package com.spring.JavaT.tariff;

import com.spring.JavaT.common.BaseEntity;
import com.spring.JavaT.meter.MeterType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A versioned tariff configuration including tax and penalty settings.
 *
 * <p>New versions close the previous active version for the same {@link MeterType}
 * by setting {@link #effectiveTo} to {@code new.effectiveFrom - 1 day}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tariff_versions")
public class TariffVersion extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false, length = 20)
    private MeterType meterType;

    @Enumerated(EnumType.STRING)
    @Column(name = "tariff_type", nullable = false, length = 10)
    private TariffType tariffType;

    /** Required when {@link TariffType#FLAT}. */
    @Column(name = "flat_rate", precision = 12, scale = 4)
    private BigDecimal flatRate;

    @Column(name = "fixed_service_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedServiceCharge;

    /** VAT or other tax percentage applied to the subtotal. */
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    /** Late payment penalty percentage. */
    @Column(name = "penalty_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal penaltyRate;

    @Column(name = "penalty_grace_days", nullable = false)
    private int penaltyGraceDays;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** {@code null} means this is the current open-ended version. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "tariffVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("minUnits ASC")
    @Builder.Default
    private List<TariffTier> tiers = new ArrayList<>();

    /** Returns {@code true} if this version applies on the given date. */
    public boolean isEffectiveOn(LocalDate date) {
        if (date.isBefore(effectiveFrom)) {
            return false;
        }
        return effectiveTo == null || !date.isAfter(effectiveTo);
    }
}
