package com.spring.JavaT.tariff;

import com.spring.JavaT.meter.MeterType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TariffVersionRepository extends JpaRepository<TariffVersion, UUID>, JpaSpecificationExecutor<TariffVersion> {

    Optional<TariffVersion> findFirstByMeterTypeAndActiveTrueAndEffectiveToIsNullAndDeletedFalseOrderByEffectiveFromDesc(
            MeterType meterType);

    @Query("""
            SELECT t FROM TariffVersion t
            WHERE t.meterType = :meterType
              AND t.deleted = false
              AND t.effectiveFrom <= :billDate
              AND (t.effectiveTo IS NULL OR t.effectiveTo >= :billDate)
            ORDER BY t.effectiveFrom DESC
            """)
    List<TariffVersion> findEffectiveForDate(@Param("meterType") MeterType meterType,
                                             @Param("billDate") LocalDate billDate,
                                             Pageable pageable);

    default Optional<TariffVersion> findEffectiveForDate(MeterType meterType, LocalDate billDate) {
        List<TariffVersion> results = findEffectiveForDate(
                meterType, billDate, org.springframework.data.domain.PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    List<TariffVersion> findAllByMeterTypeAndActiveTrueAndDeletedFalse(MeterType meterType);
}
