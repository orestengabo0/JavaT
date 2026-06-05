package com.spring.JavaT.reading;

import com.spring.JavaT.common.filter.BaseSpecification;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.customer.Customer;
import com.spring.JavaT.customer.CustomerService;
import com.spring.JavaT.exception.BusinessException;
import com.spring.JavaT.exception.ResourceNotFoundException;
import com.spring.JavaT.meter.Meter;
import com.spring.JavaT.meter.MeterService;
import com.spring.JavaT.reading.dto.CreateMeterReadingRequest;
import com.spring.JavaT.reading.dto.MeterReadingDto;
import com.spring.JavaT.user.User;
import com.spring.JavaT.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for operator meter reading capture.
 */
@Service
@RequiredArgsConstructor
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterService           meterService;
    private final CustomerService        customerService;
    private final UserRepository           userRepository;
    private final MeterReadingMapper       meterReadingMapper;

    @Transactional
    public MeterReadingDto captureReading(CreateMeterReadingRequest request, String operatorEmail) {
        Meter meter = meterService.requireActiveMeter(request.getMeterId());

        int billingMonth = request.getReadingDate().getMonthValue();
        int billingYear  = request.getReadingDate().getYear();

        validateReadingDate(meter.getId(), request.getReadingDate());
        validateNoDuplicateReading(meter.getId(), billingMonth, billingYear);

        BigDecimal previousReading = resolvePreviousReading(meter.getId());
        BigDecimal currentReading  = request.getCurrentReading();

        validateReadingOrder(previousReading, currentReading);

        User operator = userRepository.findByEmail(operatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", operatorEmail));

        MeterReading reading = MeterReading.builder()
                .meter(meter)
                .previousReading(previousReading)
                .currentReading(currentReading)
                .readingDate(request.getReadingDate())
                .billingMonth(billingMonth)
                .billingYear(billingYear)
                .capturedBy(operator)
                .build();

        return meterReadingMapper.toDto(meterReadingRepository.save(reading));
    }

    @Transactional(readOnly = true)
    public Page<MeterReadingDto> getAllReadings(List<SearchCriteria> criteria, Pageable pageable) {
        Specification<MeterReading> spec = new BaseSpecification<>(criteria);
        return meterReadingRepository.findAll(spec, pageable).map(meterReadingMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<MeterReadingDto> getReadingsForPortalUser(
            String email,
            UUID meterId,
            List<SearchCriteria> extraCriteria,
            Pageable pageable) {

        Customer customer = customerService.requireLinkedCustomerForPortalUser(email);

        if (meterId != null) {
            assertMeterOwnedByCustomer(meterId, customer.getId());
        }

        List<SearchCriteria> criteria = new ArrayList<>();
        criteria.add(new SearchCriteria("meter.customer.id", SearchCriteria.Op.EQ, customer.getId()));
        if (meterId != null) {
            criteria.add(new SearchCriteria("meter.id", SearchCriteria.Op.EQ, meterId));
        }
        if (extraCriteria != null) {
            criteria.addAll(extraCriteria);
        }

        return getAllReadings(criteria, pageable);
    }

    @Transactional(readOnly = true)
    public MeterReadingDto getReadingById(UUID id) {
        return meterReadingMapper.toDto(findByIdOrThrow(id));
    }

    /**
     * Returns the reading for a meter in a given billing period, if one exists.
     * Used by {@code BillingService} in Phase 5.
     */
    @Transactional(readOnly = true)
    public MeterReading requireReadingForPeriod(UUID meterId, int billingMonth, int billingYear) {
        return meterReadingRepository
                .findByMeterIdAndBillingMonthAndBillingYear(meterId, billingMonth, billingYear)
                .orElseThrow(() -> new BusinessException(
                        "No meter reading found for %d/%d".formatted(billingMonth, billingYear),
                        HttpStatus.BAD_REQUEST,
                        "READING_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public MeterReading findByIdOrThrow(UUID id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MeterReading", "id", id));
    }

    // -------------------------------------------------------------------------
    // Private validation helpers
    // -------------------------------------------------------------------------

    private void assertMeterOwnedByCustomer(UUID meterId, UUID customerId) {
        Meter meter = meterService.findByIdOrThrow(meterId);

        if (!meter.getCustomer().getId().equals(customerId)) {
            throw new BusinessException(
                    "You do not have access to readings for this meter",
                    HttpStatus.FORBIDDEN,
                    "METER_ACCESS_DENIED"
            );
        }
    }

    private void validateNoDuplicateReading(UUID meterId, int billingMonth, int billingYear) {
        if (meterReadingRepository.existsByMeterIdAndBillingMonthAndBillingYear(
                meterId, billingMonth, billingYear)) {
            throw new BusinessException(
                    "A reading for this meter already exists for %d/%d".formatted(billingMonth, billingYear),
                    HttpStatus.CONFLICT,
                    "DUPLICATE_READING"
            );
        }
    }

    private void validateReadingOrder(BigDecimal previous, BigDecimal current) {
        if (current.compareTo(previous) <= 0) {
            throw new BusinessException(
                    "Current reading must be greater than previous reading",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_READING_ORDER"
            );
        }
    }

    private void validateReadingDate(UUID meterId, java.time.LocalDate readingDate) {
        Meter meter = meterService.findByIdOrThrow(meterId);
        java.time.LocalDate installationDate = meter.getInstallationDate();

        if (installationDate == null) {
            throw new BusinessException(
                    "Meter installation date is not configured",
                    HttpStatus.BAD_REQUEST,
                    "METER_INSTALLATION_DATE_MISSING"
            );
        }

        if (readingDate.isBefore(installationDate)) {
            throw new BusinessException(
                    "Reading date %s cannot be before meter installation date %s"
                            .formatted(readingDate, installationDate),
                    HttpStatus.BAD_REQUEST,
                    "INVALID_READING_DATE"
            );
        }
    }

    /**
     * Resolves the previous reading from the most recent capture for this meter,
     * or {@code 0} when no prior reading exists.
     */
    private BigDecimal resolvePreviousReading(UUID meterId) {
        return meterReadingRepository.findTopByMeterIdOrderByReadingDateDescIdDesc(meterId)
                .map(MeterReading::getCurrentReading)
                .orElse(BigDecimal.ZERO);
    }
}
