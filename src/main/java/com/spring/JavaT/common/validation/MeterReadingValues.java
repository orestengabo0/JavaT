package com.spring.JavaT.common.validation;

/**
 * Marker interface for DTOs validated by {@link ValidMeterReadingValidator}.
 *
 * <p>Implement on meter-reading request types so the class-level constraint
 * can compare previous and current readings.
 */
public interface MeterReadingValues {

    /** Previous meter reading value. */
    java.math.BigDecimal getPreviousReading();

    /** Current meter reading value. */
    java.math.BigDecimal getCurrentReading();
}
