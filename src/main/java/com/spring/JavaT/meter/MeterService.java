package com.spring.JavaT.meter;

import com.spring.JavaT.common.EntityStatus;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.customer.Customer;
import com.spring.JavaT.customer.CustomerService;
import com.spring.JavaT.exception.BusinessException;
import com.spring.JavaT.exception.DuplicateResourceException;
import com.spring.JavaT.exception.ResourceNotFoundException;
import com.spring.JavaT.meter.dto.CreateMeterRequest;
import com.spring.JavaT.meter.dto.MeterDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for utility meter management.
 */
@Service
@RequiredArgsConstructor
public class MeterService {

    private final MeterRepository   meterRepository;
    private final CustomerService   customerService;
    private final MeterMapper       meterMapper;

    @Transactional
    public MeterDto attachMeter(UUID customerId, CreateMeterRequest request) {
        Customer customer = customerService.findByIdOrThrow(customerId);

        if (!customer.isActive()) {
            throw new BusinessException(
                    "Cannot attach a meter to an inactive customer",
                    HttpStatus.BAD_REQUEST,
                    "CUSTOMER_INACTIVE"
            );
        }

        String meterNumber = request.getMeterNumber().strip().toUpperCase();
        if (meterRepository.existsByMeterNumber(meterNumber)) {
            throw new DuplicateResourceException("Meter", "meterNumber", meterNumber);
        }

        Meter meter = Meter.builder()
                .customer(customer)
                .meterNumber(meterNumber)
                .meterType(MeterType.valueOf(request.getMeterType()))
                .installationDate(request.getInstallationDate())
                .build();
        meter.setStatus(EntityStatus.ACTIVE);

        return meterMapper.toDto(meterRepository.save(meter));
    }

    @Transactional(readOnly = true)
    public Page<MeterDto> getAllMeters(List<SearchCriteria> criteria, Pageable pageable) {
        Specification<Meter> spec = new MeterSpecification(criteria);
        return meterRepository.findAll(spec, pageable).map(meterMapper::toDto);
    }

    @Transactional(readOnly = true)
    public MeterDto getMeterById(UUID id) {
        return meterMapper.toDto(findByIdOrThrow(id));
    }

    @Transactional
    public MeterDto deactivateMeter(UUID id, String adminEmail) {
        Meter meter = findByIdOrThrow(id);
        meter.softDelete(adminEmail);
        return meterMapper.toDto(meterRepository.save(meter));
    }

    @Transactional
    public MeterDto activateMeter(UUID id) {
        Meter meter = findByIdIncludingDeletedOrThrow(id);
        meter.restore();
        return meterMapper.toDto(meterRepository.save(meter));
    }

    /**
     * Ensures the meter exists, is not soft-deleted, and is {@link EntityStatus#ACTIVE}.
     *
     * @throws BusinessException with {@code METER_INACTIVE} when readings or billing are not allowed
     */
    @Transactional(readOnly = true)
    public Meter requireActiveMeter(UUID meterId) {
        Meter meter = findByIdOrThrow(meterId);
        if (!meter.isActive()) {
            throw new BusinessException(
                    "Meter must be active",
                    HttpStatus.BAD_REQUEST,
                    "METER_INACTIVE"
            );
        }
        return meter;
    }

    @Transactional(readOnly = true)
    public Meter findByIdOrThrow(UUID id) {
        return meterRepository.findById(id)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Meter", "id", id));
    }

    private Meter findByIdIncludingDeletedOrThrow(UUID id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter", "id", id));
    }
}
