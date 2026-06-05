package com.spring.JavaT.reading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID>, JpaSpecificationExecutor<MeterReading> {

    boolean existsByMeterIdAndBillingMonthAndBillingYear(UUID meterId, int billingMonth, int billingYear);

    Optional<MeterReading> findByMeterIdAndBillingMonthAndBillingYear(
            UUID meterId, int billingMonth, int billingYear);

    Optional<MeterReading> findTopByMeterIdOrderByReadingDateDescIdDesc(UUID meterId);
}
