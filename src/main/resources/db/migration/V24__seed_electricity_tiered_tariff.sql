-- =============================================================================
-- V24 — Seed tiered electricity tariff (REG)
-- =============================================================================
-- Fixed UUIDs:
--   Tariff : d1111111-1111-1111-1111-111111111102
--   Tier 1 : d2222222-2222-2222-2222-222222222201  (0–50 kWh)
--   Tier 2 : d2222222-2222-2222-2222-222222222202  (51–200 kWh)
--   Tier 3 : d2222222-2222-2222-2222-222222222203  (201+ kWh)
--
-- Tier bands (contiguous, first tier starts at 0):
--   0–50 kWh    @ 120.00 FRW / kWh
--   51–200 kWh  @ 180.00 FRW / kWh
--   201+ kWh    @ 250.00 FRW / kWh (unlimited top band)
-- =============================================================================

INSERT INTO tariff_versions (
    id, name, meter_type, tariff_type, flat_rate, fixed_service_charge,
    tax_rate, penalty_rate, penalty_grace_days, effective_from, effective_to, active,
    created_at, updated_at, created_by, updated_by, deleted, status
)
SELECT
    'd1111111-1111-1111-1111-111111111102'::uuid,
    'REG Electricity Tiered 2024',
    'ELECTRICITY',
    'TIERED',
    NULL,
    2000.00,
    18.00,
    5.00,
    15,
    '2024-01-01'::date,
    NULL,
    TRUE,
    NOW(), NOW(), 'system', 'system', FALSE, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM tariff_versions
    WHERE name = 'REG Electricity Tiered 2024'
      AND meter_type = 'ELECTRICITY'
      AND deleted = FALSE
);

INSERT INTO tariff_tiers (id, tariff_version_id, min_units, max_units, rate_per_unit)
SELECT
    'd2222222-2222-2222-2222-222222222201'::uuid,
    t.id,
    0.00,
    50.00,
    120.0000
FROM tariff_versions t
WHERE t.id = 'd1111111-1111-1111-1111-111111111102'::uuid
  AND NOT EXISTS (
      SELECT 1 FROM tariff_tiers
      WHERE tariff_version_id = t.id AND min_units = 0.00
  );

INSERT INTO tariff_tiers (id, tariff_version_id, min_units, max_units, rate_per_unit)
SELECT
    'd2222222-2222-2222-2222-222222222202'::uuid,
    t.id,
    51.00,
    200.00,
    180.0000
FROM tariff_versions t
WHERE t.id = 'd1111111-1111-1111-1111-111111111102'::uuid
  AND NOT EXISTS (
      SELECT 1 FROM tariff_tiers
      WHERE tariff_version_id = t.id AND min_units = 51.00
  );

INSERT INTO tariff_tiers (id, tariff_version_id, min_units, max_units, rate_per_unit)
SELECT
    'd2222222-2222-2222-2222-222222222203'::uuid,
    t.id,
    201.00,
    NULL,
    250.0000
FROM tariff_versions t
WHERE t.id = 'd1111111-1111-1111-1111-111111111102'::uuid
  AND NOT EXISTS (
      SELECT 1 FROM tariff_tiers
      WHERE tariff_version_id = t.id AND min_units = 201.00
  );
