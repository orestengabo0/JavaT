-- =============================================================================
-- V21 — Enforce reading_date >= meter installation_date at database level
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_validate_meter_reading_date()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_installation_date DATE;
BEGIN
    SELECT installation_date
    INTO v_installation_date
    FROM meters
    WHERE id = NEW.meter_id;

    IF v_installation_date IS NULL THEN
        RAISE EXCEPTION 'Meter installation date is not configured for meter %', NEW.meter_id;
    END IF;

    IF NEW.reading_date < v_installation_date THEN
        RAISE EXCEPTION
            'Reading date % cannot be before meter installation date %',
            NEW.reading_date,
            v_installation_date;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_meter_reading_validate_date
    BEFORE INSERT OR UPDATE ON meter_readings
    FOR EACH ROW
    EXECUTE FUNCTION fn_validate_meter_reading_date();
