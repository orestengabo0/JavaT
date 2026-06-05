package com.spring.JavaT.reading;

import com.spring.JavaT.customer.CustomerService;
import com.spring.JavaT.exception.BusinessException;
import com.spring.JavaT.meter.Meter;
import com.spring.JavaT.meter.MeterService;
import com.spring.JavaT.meter.MeterType;
import com.spring.JavaT.reading.dto.CreateMeterReadingRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingServiceTest {

    @Mock private MeterReadingRepository meterReadingRepository;
    @Mock private MeterService           meterService;
    @Mock private CustomerService   customerService;
    @Mock private MeterReadingMapper meterReadingMapper;

    @InjectMocks
    private MeterReadingService meterReadingService;

    @Test
    void captureReading_rejectsReadingDateBeforeInstallationDate() {
        UUID meterId = UUID.randomUUID();
        Meter meter = activeMeter(meterId, LocalDate.of(2026, 6, 5));

        CreateMeterReadingRequest request = new CreateMeterReadingRequest();
        request.setMeterId(meterId);
        request.setCurrentReading(new BigDecimal("100.00"));
        request.setReadingDate(LocalDate.of(2026, 5, 28));

        when(meterService.requireActiveMeter(meterId)).thenReturn(meter);
        when(meterService.findByIdOrThrow(meterId)).thenReturn(meter);

        assertThatThrownBy(() -> meterReadingService.captureReading(request, "operator@wasac.gov.rw"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2026-05-28")
                .hasMessageContaining("2026-06-05");

        verify(meterReadingRepository, never()).save(any());
    }

    private static Meter activeMeter(UUID meterId, LocalDate installationDate) {
        Meter meter = Meter.builder()
                .meterNumber("WTR-TEST-001")
                .meterType(MeterType.WATER)
                .installationDate(installationDate)
                .build();
        meter.setId(meterId);
        return meter;
    }
}
