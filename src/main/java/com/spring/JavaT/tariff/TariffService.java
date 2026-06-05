package com.spring.JavaT.tariff;

import com.spring.JavaT.common.filter.BaseSpecification;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.exception.BusinessException;
import com.spring.JavaT.exception.ResourceNotFoundException;
import com.spring.JavaT.meter.MeterType;
import com.spring.JavaT.tariff.dto.CreateTariffRequest;
import com.spring.JavaT.tariff.dto.TariffTierRequest;
import com.spring.JavaT.tariff.dto.TariffVersionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Manages versioned tariff, tax, and penalty configuration.
 */
@Service
@RequiredArgsConstructor
public class TariffService {

    private static final int MONEY_SCALE = 2;

    private final TariffVersionRepository tariffVersionRepository;
    private final TariffMapper            tariffMapper;

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @Transactional
    public TariffVersionDto createTariff(CreateTariffRequest request) {
        MeterType meterType = MeterType.valueOf(request.getMeterType());
        TariffType tariffType = TariffType.valueOf(request.getTariffType());

        validateTariffTypeFields(tariffType, request);
        if (tariffType == TariffType.TIERED) {
            validateTiers(request.getTiers());
        }

        closePreviousActiveVersions(meterType, request.getEffectiveFrom());

        TariffVersion tariff = TariffVersion.builder()
                .name(request.getName().strip())
                .meterType(meterType)
                .tariffType(tariffType)
                .flatRate(request.getFlatRate())
                .fixedServiceCharge(request.getFixedServiceCharge())
                .taxRate(request.getTaxRate())
                .penaltyRate(request.getPenaltyRate())
                .penaltyGraceDays(request.getPenaltyGraceDays())
                .effectiveFrom(request.getEffectiveFrom())
                .active(true)
                .build();

        if (tariffType == TariffType.TIERED) {
            request.getTiers().stream()
                    .sorted(Comparator.comparing(TariffTierRequest::getMinUnits))
                    .forEach(tierReq -> {
                        TariffTier tier = TariffTier.builder()
                                .tariffVersion(tariff)
                                .minUnits(tierReq.getMinUnits())
                                .maxUnits(tierReq.getMaxUnits())
                                .ratePerUnit(tierReq.getRatePerUnit())
                                .build();
                        tariff.getTiers().add(tier);
                    });
        }

        return tariffMapper.toDto(tariffVersionRepository.save(tariff));
    }

    @Transactional(readOnly = true)
    public Page<TariffVersionDto> getAllTariffs(List<SearchCriteria> criteria, Pageable pageable) {
        Specification<TariffVersion> spec = new TariffSpecification(criteria);
        return tariffVersionRepository.findAll(spec, pageable).map(tariffMapper::toDto);
    }

    @Transactional(readOnly = true)
    public TariffVersionDto getActiveTariff(MeterType meterType) {
        return tariffMapper.toDto(requireEffectiveTariff(meterType, LocalDate.now()));
    }

    @Transactional(readOnly = true)
    public TariffVersionDto getTariffById(UUID id) {
        return tariffMapper.toDto(findByIdOrThrow(id));
    }

    // -------------------------------------------------------------------------
    // Billing helpers — used by BillingService in Phase 5
    // -------------------------------------------------------------------------

    /**
     * Resolves the tariff version effective on a billing date.
     *
     * <p>Query rule: {@code effectiveFrom <= billDate AND (effectiveTo IS NULL OR effectiveTo >= billDate)}.
     */
    @Transactional(readOnly = true)
    public TariffVersion requireEffectiveTariff(MeterType meterType, LocalDate billDate) {
        return tariffVersionRepository.findEffectiveForDate(meterType, billDate)
                .orElseThrow(() -> new BusinessException(
                        "No active tariff found for meter type %s on %s".formatted(meterType, billDate),
                        HttpStatus.BAD_REQUEST,
                        "TARIFF_NOT_FOUND"
                ));
    }

    /**
     * Calculates consumption cost before tax and service charge.
     *
     * <p>FLAT: {@code consumption × flatRate}<br>
     * TIERED: cumulative band pricing using tier min/max unit ranges.
     */
    public BigDecimal calculateConsumptionCharge(TariffVersion tariff, BigDecimal consumption) {
        if (consumption == null || consumption.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        return switch (tariff.getTariffType()) {
            case FLAT -> consumption.multiply(tariff.getFlatRate()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            case TIERED -> calculateTieredCharge(consumption, tariff.getTiers());
        };
    }

    /** Subtotal = consumption charge + fixed service charge. */
    public BigDecimal calculateSubtotal(TariffVersion tariff, BigDecimal consumption) {
        return calculateConsumptionCharge(tariff, consumption)
                .add(tariff.getFixedServiceCharge())
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Tax amount from subtotal and tariff tax rate. */
    public BigDecimal calculateTaxAmount(BigDecimal subtotal, TariffVersion tariff) {
        return subtotal.multiply(tariff.getTaxRate())
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Penalty amount from outstanding balance and tariff penalty rate. */
    public BigDecimal calculatePenaltyAmount(BigDecimal outstandingBalance, TariffVersion tariff) {
        if (outstandingBalance == null || outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return outstandingBalance.multiply(tariff.getPenaltyRate())
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public TariffVersion findByIdOrThrow(UUID id) {
        return tariffVersionRepository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("TariffVersion", "id", id));
    }

    // -------------------------------------------------------------------------
    // Versioning
    // -------------------------------------------------------------------------

    /**
     * Closes any open-ended active version for the meter type so the new version
     * applies only to future billing cycles.
     */
    private void closePreviousActiveVersions(MeterType meterType, LocalDate newEffectiveFrom) {
        tariffVersionRepository
                .findFirstByMeterTypeAndActiveTrueAndEffectiveToIsNullAndDeletedFalseOrderByEffectiveFromDesc(meterType)
                .ifPresent(previous -> {
                    if (!newEffectiveFrom.isAfter(previous.getEffectiveFrom())) {
                        throw new BusinessException(
                                "New tariff effective date must be after the current version's effective date (%s)"
                                        .formatted(previous.getEffectiveFrom()),
                                HttpStatus.BAD_REQUEST,
                                "TARIFF_EFFECTIVE_DATE_INVALID"
                        );
                    }
                    previous.setEffectiveTo(newEffectiveFrom.minusDays(1));
                    previous.setActive(false);
                    tariffVersionRepository.save(previous);
                });
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private void validateTariffTypeFields(TariffType tariffType, CreateTariffRequest request) {
        if (tariffType == TariffType.FLAT) {
            if (request.getFlatRate() == null || request.getFlatRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        "Flat tariff must include a positive flat rate",
                        HttpStatus.BAD_REQUEST,
                        "FLAT_RATE_REQUIRED"
                );
            }
        } else {
            if (request.getTiers() == null || request.getTiers().isEmpty()) {
                throw new BusinessException(
                        "Tiered tariff must include at least one tier",
                        HttpStatus.BAD_REQUEST,
                        "TARIFF_TIERS_REQUIRED"
                );
            }
        }
    }

    private void validateTiers(List<TariffTierRequest> tiers) {
        List<TariffTierRequest> sorted = tiers.stream()
                .sorted(Comparator.comparing(TariffTierRequest::getMinUnits))
                .toList();

        if (sorted.getFirst().getMinUnits().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(
                    "First tier must start at 0 units",
                    HttpStatus.BAD_REQUEST,
                    "TARIFF_TIER_INVALID"
            );
        }

        for (int i = 0; i < sorted.size(); i++) {
            TariffTierRequest tier = sorted.get(i);
            if (tier.getMaxUnits() != null && tier.getMaxUnits().compareTo(tier.getMinUnits()) < 0) {
                throw new BusinessException(
                        "Tier max units must be greater than or equal to min units",
                        HttpStatus.BAD_REQUEST,
                        "TARIFF_TIER_INVALID"
                );
            }
            if (i < sorted.size() - 1) {
                TariffTierRequest next = sorted.get(i + 1);
                if (tier.getMaxUnits() == null) {
                    throw new BusinessException(
                            "Only the last tier may have an unlimited max units",
                            HttpStatus.BAD_REQUEST,
                            "TARIFF_TIER_INVALID"
                    );
                }
                BigDecimal expectedNextMin = tier.getMaxUnits().add(BigDecimal.ONE);
                if (next.getMinUnits().compareTo(expectedNextMin) != 0) {
                    throw new BusinessException(
                            "Tiers must be contiguous: expected next minUnits=%s but got %s"
                                    .formatted(expectedNextMin, next.getMinUnits()),
                            HttpStatus.BAD_REQUEST,
                            "TARIFF_TIER_INVALID"
                    );
                }
            }
        }
    }

    /**
     * Cumulative cap tier pricing: each tier covers units above the previous tier's cap.
     */
    private BigDecimal calculateTieredCharge(BigDecimal consumption, List<TariffTier> tiers) {
        List<TariffTier> sorted = tiers.stream()
                .sorted(Comparator.comparing(TariffTier::getMinUnits))
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal previousCap = BigDecimal.ZERO;

        for (TariffTier tier : sorted) {
            BigDecimal cap = tier.getMaxUnits() != null ? tier.getMaxUnits() : consumption;
            if (consumption.compareTo(tier.getMinUnits()) <= 0) {
                break;
            }
            BigDecimal upper = consumption.min(cap);
            BigDecimal unitsInBand = upper.subtract(previousCap);
            if (unitsInBand.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(unitsInBand.multiply(tier.getRatePerUnit()));
            }
            previousCap = cap;
            if (consumption.compareTo(cap) <= 0) {
                break;
            }
        }

        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
