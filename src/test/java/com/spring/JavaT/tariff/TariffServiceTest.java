package com.spring.JavaT.tariff;

import com.spring.JavaT.meter.MeterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

    @Mock
    private TariffVersionRepository tariffVersionRepository;

    @Mock
    private TariffMapper tariffMapper;

    @InjectMocks
    private TariffService tariffService;

    private TariffVersion flatTariff;

    @BeforeEach
    void setUp() {
        flatTariff = TariffVersion.builder()
                .name("Test Water Flat")
                .meterType(MeterType.WATER)
                .tariffType(TariffType.FLAT)
                .flatRate(new BigDecimal("250.0000"))
                .fixedServiceCharge(new BigDecimal("1500.00"))
                .taxRate(new BigDecimal("18.00"))
                .penaltyRate(new BigDecimal("5.00"))
                .penaltyGraceDays(15)
                .active(true)
                .build();
    }

    @Test
    void calculateConsumptionCharge_flatTariff_multipliesConsumptionByRate() {
        BigDecimal consumption = new BigDecimal("35.25");

        BigDecimal charge = tariffService.calculateConsumptionCharge(flatTariff, consumption);

        assertThat(charge).isEqualByComparingTo("8812.50");
    }

    @Test
    void calculateSubtotal_includesFixedServiceCharge() {
        BigDecimal consumption = new BigDecimal("35.25");

        BigDecimal subtotal = tariffService.calculateSubtotal(flatTariff, consumption);

        assertThat(subtotal).isEqualByComparingTo("10312.50");
    }

    @Test
    void calculateTaxAmount_appliesPercentageToSubtotal() {
        BigDecimal subtotal = new BigDecimal("10312.50");

        BigDecimal tax = tariffService.calculateTaxAmount(subtotal, flatTariff);

        assertThat(tax).isEqualByComparingTo("1856.25");
    }

    @Test
    void calculatePenaltyAmount_appliesRateToOutstandingBalance() {
        BigDecimal balance = new BigDecimal("5000.00");

        BigDecimal penalty = tariffService.calculatePenaltyAmount(balance, flatTariff);

        assertThat(penalty).isEqualByComparingTo("250.00");
    }

    @Test
    void calculateTieredCharge_usesCumulativeBands() {
        TariffVersion tiered = TariffVersion.builder()
                .tariffType(TariffType.TIERED)
                .fixedServiceCharge(BigDecimal.ZERO)
                .taxRate(BigDecimal.ZERO)
                .penaltyRate(BigDecimal.ZERO)
                .penaltyGraceDays(0)
                .tiers(List.of(
                        TariffTier.builder()
                                .minUnits(BigDecimal.ZERO)
                                .maxUnits(new BigDecimal("10"))
                                .ratePerUnit(new BigDecimal("100"))
                                .build(),
                        TariffTier.builder()
                                .minUnits(new BigDecimal("11"))
                                .maxUnits(null)
                                .ratePerUnit(new BigDecimal("200"))
                                .build()
                ))
                .build();

        BigDecimal charge = tariffService.calculateConsumptionCharge(tiered, new BigDecimal("15"));

        // 10 units @ 100 + 5 units @ 200 = 2000
        assertThat(charge).isEqualByComparingTo("2000.00");
    }
}
