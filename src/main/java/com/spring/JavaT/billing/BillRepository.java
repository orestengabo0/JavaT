package com.spring.JavaT.billing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID>, JpaSpecificationExecutor<Bill> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bill b WHERE b.id = :id AND b.deleted = false")
    Optional<Bill> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByMeterIdAndBillingMonthAndBillingYearAndDeletedFalseAndBillStatusNot(
            UUID meterId, int billingMonth, int billingYear, BillStatus billStatus);

    @Query(value = """
            SELECT * FROM bills
            WHERE meter_id = :meterId
              AND deleted = FALSE
              AND bill_status <> 'CANCELLED'
              AND (billing_year < :year OR (billing_year = :year AND billing_month < :month))
            ORDER BY billing_year DESC, billing_month DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Bill> findLatestBeforePeriod(@Param("meterId") UUID meterId,
                                          @Param("month") int month,
                                          @Param("year") int year);

    boolean existsByTariffVersion_Id(UUID tariffVersionId);
}
