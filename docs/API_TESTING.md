# WASAC Utility Billing — API Testing Guide

Manual test plan for all REST endpoints, input validation, role-based access, and database trigger/routine verification.

**Base URL:** `http://localhost:8080`  
**Swagger UI:** `http://localhost:8080/swagger-ui.html`  
**API prefix:** `/api/v1`

---

## Table of contents

1. [Prerequisites](#1-prerequisites)
2. [Seeded test data](#2-seeded-test-data)
3. [Standard response format](#3-standard-response-format)
4. [Authentication setup](#4-authentication-setup)
5. [End-to-end happy path](#5-end-to-end-happy-path)
6. [Endpoint test matrix](#6-endpoint-test-matrix)
7. [Input validation reference](#7-input-validation-reference)
8. [Authorization matrix](#8-authorization-matrix)
9. [Database verification](#9-database-verification)
10. [Full test checklist](#10-full-test-checklist)

---

## 1. Prerequisites

| Item | Details |
|------|---------|
| App running | `mvn spring-boot:run` — confirm log shows `Started JavaTApplication` |
| PostgreSQL | Database `javat` on `127.0.0.1:5432` |
| Migrations | Flyway at **v19** (`flyway_schema_history` latest version = 19) |
| HTTP client | Postman, Insomnia, curl, or Swagger UI |
| DB client | `psql`, pgAdmin, or DBeaver (for trigger/routine checks) |

### Using JWT in requests

After login, copy `data.accessToken` from the response and send:

```
Authorization: Bearer <accessToken>
```

In Swagger UI: click **Authorize** → paste the token (without the `Bearer` prefix if the dialog adds it automatically).

### Common HTTP status codes

| Code | Meaning in this API |
|------|---------------------|
| `200` | Success (GET, PATCH) |
| `201` | Created (POST) |
| `400` | Validation error or business rule violation |
| `401` | Missing or invalid JWT |
| `403` | Authenticated but wrong role, or customer accessing another customer's data |
| `404` | Resource not found |
| `409` | Duplicate resource (email, meter number, bill period, payment reference) |
| `500` | Unexpected server error |

Validation errors return `success: false` with an `errors` array listing field-level messages.

---

## 2. Seeded test data

After migrations **V17** (seed) and **V19** (email rebrand), use these accounts.  
**Password for all:** `Admin@1234`

| Role | Email | Username |
|------|-------|----------|
| ADMIN | `admin@wasac.gov.rw` | `admin` |
| FINANCE | `finance@wasac.gov.rw` | `finance` |
| OPERATOR | `operator@wasac.gov.rw` | `operator` |
| CUSTOMER | `customer@wasac.gov.rw` | `jp.uwimana` |

### Fixed UUIDs (from V17 seed)

Use these in request bodies and DB queries:

| Entity | UUID |
|--------|------|
| Finance user | `f1111111-1111-1111-1111-111111111101` |
| Operator user | `f1111111-1111-1111-1111-111111111102` |
| Customer user | `f1111111-1111-1111-1111-111111111103` |
| Customer record | `a1111111-1111-1111-1111-111111111101` |
| Meter `WTR-KGL-001234` | `b1111111-1111-1111-1111-111111111101` |
| Tariff (WASAC Water 2024) | `d1111111-1111-1111-1111-111111111101` |
| Reading (May 2026) | `e1111111-1111-1111-1111-111111111101` |

### Pre-seeded reading (May 2026)

| Field | Value |
|-------|-------|
| Previous reading | `1250.50` |
| Current reading | `1285.75` |
| Consumption | `35.25` |
| Reading date | `2026-05-28` |

### Expected bill amounts (flat tariff from seed)

When generating a bill for meter `b1111111-...` for **5/2026**:

| Component | Calculation | Amount (FRW) |
|-----------|-------------|--------------|
| Consumption charge | 35.25 × 250.00 | 8,812.50 |
| Fixed service charge | — | 1,500.00 |
| Subtotal | — | 10,312.50 |
| Tax (18%) | — | 1,856.25 |
| Penalty | No prior overdue bill | 0.00 |
| **Total** | — | **12,168.75** |
| Due date | 15th of month after billing period | **2026-06-15** |
| Initial status | — | `PENDING` |

---

## 4. Authentication setup

### 4.1 Login (all roles)

**`POST /api/v1/auth/login`** — Public

```json
{
  "email": "admin@wasac.gov.rw",
  "password": "Admin@1234"
}
```

| Test | Body change | Expected |
|------|-------------|----------|
| Valid login | Correct credentials | `200`, `data.accessToken` and `data.refreshToken` present |
| Wrong password | Bad password | `401` |
| Unknown email | Non-existent email | `401` |
| Missing email | `{}` or omit email | `400`, validation error |
| Invalid email format | `"email": "not-an-email"` | `400` |
| Missing password | Omit password | `400` |

---

## 5. End-to-end happy path

Run this sequence first to validate the core billing workflow and DB triggers.

| Step | Actor | Action | Expected result |
|------|-------|--------|-----------------|
| 1 | ADMIN | `POST /api/v1/auth/login` | `200`, receive `accessToken` |
| 2 | ADMIN | `POST /api/v1/bills/generate` (meter `b111...`, month `5`, year `2026`) | `201`, `billStatus: PENDING`, `totalAmount: 12168.75` |
| 3 | — | **DB check** | New row in `notifications` for bill-generated message (see [§9.1](#91-bill-generation-notification-trigger)) |
| 4 | FINANCE | `PATCH /api/v1/bills/{billId}/approve` | `200`, `billStatus: APPROVED` |
| 5 | FINANCE | `POST /api/v1/payments` (partial, e.g. `5000.00`) | `201`, bill `balance` reduced |
| 6 | FINANCE | `POST /api/v1/payments` (remaining balance) | `201`, bill `balance: 0`, `billStatus: PAID` |
| 7 | — | **DB check** | Payment confirmation notification inserted (see [§9.2](#92-payment-trigger--sp_mark_bill_paid)) |
| 8 | CUSTOMER | `GET /api/v1/bills` | `200`, only own bills returned |
| 9 | CUSTOMER | `GET /api/v1/payments` | `200`, payments for own bills only |
| 10 | CUSTOMER | `GET /api/v1/notifications` | `200`, bill + payment notifications visible |
| 11 | CUSTOMER | `PATCH /api/v1/notifications/{id}/read` | `200`, `read: true` |

---

## 6. Endpoint test matrix

### 6.1 Authentication — `/api/v1/auth`

| # | Method | Path | Auth | Role | Test steps | Expected |
|---|--------|------|------|------|------------|----------|
| A1 | POST | `/register` | None | — | Register new customer with valid body | `201`, tokens returned, user role `CUSTOMER` |
| A2 | POST | `/register` | None | — | Repeat with same email | `409` duplicate email |
| A3 | POST | `/register` | None | — | Repeat with same username | `409` duplicate username |
| A4 | POST | `/register` | None | — | Weak password `password` | `400` password validation |
| A5 | POST | `/forgot-password` | None | — | Valid email format | `200` (always, anti-enumeration) |
| A6 | POST | `/forgot-password` | None | — | Invalid email | `400` |
| A7 | POST | `/reset-password` | None | — | Valid token + new password | `200` |
| A8 | POST | `/reset-password` | None | — | Expired/invalid token | `400` |
| A9 | GET | `/verify-email?token=...` | None | — | Valid verification token | `200` |
| A10 | POST | `/resend-verification` | None | — | Valid email | `200` (always) |

**Register valid body example:**

```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "test.user@example.com",
  "phone": "+250788999888",
  "username": "test.user",
  "password": "Secret@123"
}
```

---

### 6.2 Users — `/api/v1/users`

| # | Method | Path | Role | Test steps | Expected |
|---|--------|------|------|------------|----------|
| U1 | GET | `/me` | Any authenticated | Login, call with token | `200`, own profile |
| U2 | GET | `/me` | None | No Authorization header | `401` |
| U3 | PATCH | `/me` | Any | Update `firstName` only | `200`, field updated |
| U4 | PATCH | `/me` | Any | Invalid phone `123` | `400` |
| U5 | PATCH | `/me/password` | Any | Correct current + valid new password | `200` |
| U6 | PATCH | `/me/password` | Any | Wrong current password | `400` or `401` |
| U7 | POST | `/` | ADMIN | Create OPERATOR user | `201` |
| U8 | POST | `/` | FINANCE | Same request | `403` |
| U9 | GET | `/` | ADMIN | List with `?role=FINANCE&page=0&size=10` | `200`, filtered page |
| U10 | GET | `/` | ADMIN | Invalid `sortBy=password` | `400` (disallowed sort field) |
| U11 | GET | `/{id}` | ADMIN | Valid user UUID | `200` |
| U12 | GET | `/{id}` | ADMIN | Random UUID | `404` |
| U13 | PATCH | `/{id}/role` | ADMIN | Change role to `OPERATOR` | `200` |
| U14 | PATCH | `/{id}/role` | ADMIN | Invalid role `SUPERUSER` | `400` |
| U15 | PATCH | `/{id}/deactivate` | ADMIN | Deactivate active user | `200`, status inactive |
| U16 | PATCH | `/{id}/activate` | ADMIN | Reactivate user | `200` |

**Create user body (ADMIN):**

```json
{
  "firstName": "New",
  "lastName": "Operator",
  "email": "new.operator@wasac.gov.rw",
  "phone": "+250788000199",
  "username": "new.operator",
  "password": "Secret@123",
  "role": "OPERATOR"
}
```

---

### 6.3 Customers — `/api/v1/customers`

| # | Method | Path | Role | Test steps | Expected |
|---|--------|------|------|------------|----------|
| C1 | POST | `/` | ADMIN | Valid new customer | `201` |
| C2 | POST | `/` | FINANCE | Same | `403` |
| C3 | POST | `/` | ADMIN | Duplicate `nationalId` | `409` |
| C4 | POST | `/` | ADMIN | Invalid national ID `123` (not 16 digits) | `400` |
| C5 | GET | `/` | ADMIN or FINANCE | List with `?search=Jean` | `200`, includes Jean Pierre |
| C6 | GET | `/` | OPERATOR | List customers | `403` |
| C7 | GET | `/{id}` | ADMIN | `a1111111-1111-1111-1111-111111111101` | `200` |
| C8 | PATCH | `/{id}` | ADMIN | Update `address` | `200` |
| C9 | PATCH | `/{id}/deactivate` | ADMIN | Deactivate customer | `200` |
| C10 | PATCH | `/{id}/activate` | ADMIN | Reactivate | `200` |
| C11 | POST | `/{id}/link-user` | ADMIN | Link existing CUSTOMER user email | `200` |
| C12 | POST | `/{id}/link-user` | ADMIN | Link user with wrong role (ADMIN) | `400` business error |
| C13 | POST | `/{id}/meters` | ADMIN | Attach new meter | `201` |
| C14 | POST | `/{id}/meters` | ADMIN | Duplicate meter number `WTR-KGL-001234` | `409` |
| C15 | POST | `/{id}/meters` | ADMIN | Attach to inactive customer | `400` `CUSTOMER_INACTIVE` |

**Create customer body:**

```json
{
  "fullNames": "Marie Claire Uwase",
  "nationalId": "1199887766554433",
  "email": "marie.uwase@example.com",
  "phone": "+250788111222",
  "address": "KG 7 Ave, Kigali, Rwanda",
  "createUserAccount": false
}
```

**Attach meter body:**

```json
{
  "meterNumber": "WTR-KGL-009999",
  "meterType": "WATER",
  "installationDate": "2025-01-15"
}
```

---

### 6.4 Meters — `/api/v1/meters`

| # | Method | Path | Role | Test steps | Expected |
|---|--------|------|------|------------|----------|
| M1 | GET | `/` | ADMIN or OPERATOR | List all meters | `200` |
| M2 | GET | `/` | ADMIN | Filter `?customerId=a1111111-...` | `200`, seeded meter |
| M3 | GET | `/` | FINANCE | List meters | `403` |
| M4 | GET | `/{id}` | OPERATOR | `b1111111-...` | `200` |
| M5 | PATCH | `/{id}/deactivate` | ADMIN | Deactivate meter | `200` |
| M6 | PATCH | `/{id}/activate` | ADMIN | Reactivate | `200` |
| M7 | — | — | — | After deactivate, try bill generate for that meter | `400` `METER_INACTIVE` |

---

### 6.5 Meter readings — `/api/v1/readings`

| # | Method | Path | Role | Test steps | Expected |
|---|--------|------|------|------------|----------|
| R1 | POST | `/` | OPERATOR | Valid reading for new month | `201` |
| R2 | POST | `/` | ADMIN | Same (not OPERATOR) | `403` |
| R3 | POST | `/` | OPERATOR | Duplicate month/year for same meter | `409` `DUPLICATE_READING` |
| R4 | POST | `/` | OPERATOR | `currentReading` ≤ `previousReading` | `400` `INVALID_READING_ORDER` |
| R5 | POST | `/` | OPERATOR | Reading date before installation date | `400` `INVALID_READING_DATE` |
| R6 | POST | `/` | OPERATOR | Future reading date | `400` validation |
| R7 | GET | `/` | OPERATOR | `?meterId=b111...&month=5&year=2026` | `200`, seeded reading |
| R8 | GET | `/{id}` | OPERATOR | `e1111111-...` | `200` |

**Capture reading body (June 2026 example):**

```json
{
  "meterId": "b1111111-1111-1111-1111-111111111101",
  "previousReading": 1285.75,
  "currentReading": 1310.00,
  "readingDate": "2026-06-28"
}
```

---

### 6.6 Tariffs — `/api/v1/tariffs`

| # | Method | Path | Role | Test steps | Expected |
|---|--------|------|------|------------|----------|
| T1 | POST | `/` | ADMIN | Create FLAT tariff | `201` |
| T2 | POST | `/` | ADMIN | FLAT without `flatRate` | `400` business/validation |
| T3 | POST | `/` | ADMIN | Create TIERED tariff with valid tiers | `201` |
| T4 | POST | `/` | ADMIN | TIERED without tiers | `400` |
| T5 | GET | `/` | FINANCE | List tariffs | `200` |
| T6 | GET | `/active?meterType=WATER` | FINANCE | Get active water tariff | `200`, seed tariff |
| T7 | GET | `/{id}` | FINANCE | `d1111111-...` | `200` |
| T8 | GET | `/` | OPERATOR | List tariffs | `403` |

**FLAT tariff body:**

```json
{
  "name": "Water Test Tariff 2026",
  "meterType": "WATER",
  "tariffType": "FLAT",
  "flatRate": 300.00,
  "fixedServiceCharge": 2000.00,
  "taxRate": 18.00,
  "penaltyRate": 5.00,
  "penaltyGraceDays": 15,
  "effectiveFrom": "2026-07-01"
}
```

**TIERED tariff body (minimal):**

```json
{
  "name": "Water Tiered 2026",
  "meterType": "WATER",
  "tariffType": "TIERED",
  "fixedServiceCharge": 1500.00,
  "taxRate": 18.00,
  "penaltyRate": 5.00,
  "penaltyGraceDays": 15,
  "effectiveFrom": "2026-08-01",
  "tiers": [
    { "minUnits": 0, "maxUnits": 10, "ratePerUnit": 100.00 },
    { "minUnits": 11, "maxUnits": null, "ratePerUnit": 200.00 }
  ]
}
```

---

### 6.7 Billing — `/api/v1/bills`

| # | Method | Path | Role | Test steps | Expected |
|---|--------|------|------|------------|----------|
| B1 | POST | `/generate` | ADMIN | Valid meter + period with reading | `201`, `PENDING` |
| B2 | POST | `/generate` | FINANCE | Same | `403` |
| B3 | POST | `/generate` | ADMIN | Same meter/period again | `409` duplicate bill |
| B4 | POST | `/generate` | ADMIN | Period with no reading | `400` `READING_NOT_FOUND` |
| B5 | POST | `/generate` | ADMIN | `billingMonth: 13` | `400` validation |
| B6 | POST | `/generate` | ADMIN | `billingYear: 2019` | `400` validation |
| B7 | PATCH | `/{id}/approve` | FINANCE | Approve PENDING bill | `200`, `APPROVED` |
| B8 | PATCH | `/{id}/approve` | FINANCE | Approve already APPROVED bill | `400` `BILL_NOT_PENDING` |
| B9 | PATCH | `/{id}/cancel` | ADMIN | Cancel PENDING bill | `200`, `CANCELLED` |
| B10 | PATCH | `/{id}/cancel` | ADMIN | Cancel APPROVED bill | `400` `BILL_NOT_PENDING` |
| B11 | POST | `/generate` | ADMIN | Regenerate same period after cancel | `201` (V18 partial unique index) |
| B12 | GET | `/` | CUSTOMER | List bills | `200`, own bills only |
| B13 | GET | `/` | FINANCE | `?status=PENDING` | `200` |
| B14 | GET | `/{id}` | CUSTOMER | Own bill | `200` |
| B15 | GET | `/{id}` | CUSTOMER | Another customer's bill UUID | `403` or `404` |

**Generate bill body:**

```json
{
  "meterId": "b1111111-1111-1111-1111-111111111101",
  "billingMonth": 5,
  "billingYear": 2026
}
```

---

### 6.8 Payments — `/api/v1/payments`

| # | Method | Path | Role | Test steps | Expected |
|---|--------|------|------|------------|----------|
| P1 | POST | `/` | FINANCE | Pay approved bill (partial) | `201` |
| P2 | POST | `/` | FINANCE | Pay PENDING bill (not approved) | `400` `BILL_NOT_APPROVED` |
| P3 | POST | `/` | FINANCE | Amount > balance | `400` `PAYMENT_EXCEEDS_BALANCE` |
| P4 | POST | `/` | FINANCE | Duplicate `referenceNumber` | `409` |
| P5 | POST | `/` | FINANCE | Future `paymentDate` | `400` |
| P6 | POST | `/` | FINANCE | Full payment (balance → 0) | `201`, bill becomes `PAID` |
| P7 | GET | `/` | CUSTOMER | List payments | `200`, scoped to own bills |
| P8 | GET | `/` | FINANCE | `?billId={billId}` | `200` |
| P9 | GET | `/{id}` | CUSTOMER | Own payment | `200` |
| P10 | GET | `/{id}` | OPERATOR | Any payment | `403` |

**Record payment body:**

```json
{
  "billId": "<approved-bill-uuid>",
  "amountPaid": 5000.00,
  "paymentMethod": "MOBILE_MONEY",
  "paymentDate": "2026-06-10",
  "referenceNumber": "MM-20260610-TEST001"
}
```

Valid `paymentMethod` values: `CASH`, `MOBILE_MONEY`, `BANK_TRANSFER`, `CARD`.

---

### 6.9 Notifications — `/api/v1/notifications`

| # | Method | Path | Role | Test steps | Expected |
|---|--------|------|------|------------|----------|
| N1 | GET | `/` | CUSTOMER | List own notifications | `200` |
| N2 | GET | `/` | ADMIN | List all notifications | `200` |
| N3 | GET | `/` | FINANCE | List notifications | `403` |
| N4 | GET | `/` | CUSTOMER | `?read=false` | `200`, unread only |
| N5 | PATCH | `/{id}/read` | CUSTOMER | Mark own notification read | `200`, `read: true` |
| N6 | PATCH | `/{id}/read` | ADMIN | Mark notification | `403` (CUSTOMER only) |

---

## 7. Input validation reference

Use these invalid payloads to confirm `400` responses and field errors.

### 7.1 Password rules (`@ValidPassword`)

- Minimum 8 characters, maximum 72
- At least one uppercase, lowercase, digit, and special character

| Value | Result |
|-------|--------|
| `short1!` | Fail — too short |
| `alllowercase1!` | Fail — no uppercase |
| `ALLUPPERCASE1!` | Fail — no lowercase |
| `NoDigitsHere!` | Fail — no digit |
| `NoSpecial123` | Fail — no special char |
| `Admin@1234` | Pass |

### 7.2 Phone (`@ValidPhone`)

| Value | Result |
|-------|--------|
| `+250788123456` | Pass (Rwanda international) |
| `0788123456` | Pass (Rwanda local) |
| `12345` | Fail |
| `+250123` | Fail |

### 7.3 National ID (`@ValidNationalId`)

| Value | Result |
|-------|--------|
| `1199880012345678` | Pass (16 digits) |
| `12345` | Fail |
| `11998800123456789` | Fail (17 digits) |

### 7.4 Meter number (`@ValidMeterNumber`)

Pattern: `^[A-Z0-9-]{6,20}$` (uppercase letters, digits, hyphens)

| Value | Result |
|-------|--------|
| `WTR-KGL-001234` | Pass |
| `abc` | Fail (too short / lowercase — API uppercases on save but validation runs on raw input) |
| `WTR@INVALID` | Fail (invalid character) |

### 7.5 Person name (`@ValidPersonName`)

- Letters, spaces, hyphens, apostrophes only
- Must contain at least one letter
- Cannot be all digits

| Value | Result |
|-------|--------|
| `Jean Pierre` | Pass |
| `123456` | Fail |
| `Jean123` | Fail (digits in name) |

### 7.6 Dates

| Annotation | Rule | Invalid example |
|------------|------|-----------------|
| `@PastOrPresentDate` | Not in the future | `installationDate: "2030-01-01"` |
| `@FutureOrPresentDate` | Not in the past | `effectiveFrom: "2020-01-01"` (when today is 2026) |

### 7.7 Bill generation

| Field | Rule | Invalid example |
|-------|------|-----------------|
| `billingMonth` | 1–12 | `0`, `13` |
| `billingYear` | ≥ 2020 | `2019` |
| `meterId` | Required UUID | `null` |

### 7.8 Payment

| Field | Rule | Invalid example |
|-------|------|-----------------|
| `amountPaid` | ≥ 0.01, max 2 decimal places | `0`, `-10`, `10.999` |
| `referenceNumber` | 6–64 characters | `MM-01` |
| `paymentMethod` | Valid enum | `BITCOIN` |

### 7.9 Meter reading

| Field | Rule | Invalid example |
|-------|------|-----------------|
| `currentReading` | Required, ≥ 0 | `null`, `-1` |
| `currentReading` vs `previousReading` | Current must be greater | `prev: 100`, `curr: 100` |
| `readingDate` | Past or present | Future date |

---

## 8. Authorization matrix

| Endpoint group | ADMIN | FINANCE | OPERATOR | CUSTOMER | Public |
|----------------|:-----:|:-------:|:--------:|:--------:|:------:|
| Auth (`/auth/**`) | ✓ | ✓ | ✓ | ✓ | ✓ |
| Users `/me` | ✓ | ✓ | ✓ | ✓ | — |
| Users admin CRUD | ✓ | — | — | — | — |
| Customers write | ✓ | — | — | — | — |
| Customers read | ✓ | ✓ | — | — | — |
| Meters read | ✓ | — | ✓ | — | — |
| Meters activate/deactivate | ✓ | — | — | — | — |
| Readings write | — | — | ✓ | — | — |
| Readings read | ✓ | — | ✓ | — | — |
| Tariffs | ✓ write / ✓ read | ✓ read | — | — | — |
| Bills generate/cancel | ✓ | — | — | — | — |
| Bills approve | ✓ | ✓ | — | — | — |
| Bills read | ✓ all | ✓ all | — | ✓ own | — |
| Payments write | ✓ | ✓ | — | — | — |
| Payments read | ✓ all | ✓ all | — | ✓ own | — |
| Notifications read | ✓ all | — | — | ✓ own | — |
| Notifications mark read | — | — | — | ✓ | — |

**Spot-check:** For each ✓ cell, send one authenticated request and confirm `200`/`201`. For each — cell, confirm `403`.

---

## 9. Database verification

Connect to PostgreSQL:

```bash
psql -h 127.0.0.1 -U <username> -d javat
```

### 9.0 Migration health

```sql
SELECT version, description, success, checksum, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
```

**Expected:** Latest `version = 19`, all `success = true`.

### 9.1 Bill generation notification trigger

**Routine:** `fn_notify_bill_generated()`  
**Trigger:** `trg_bill_generated_notify` — fires `AFTER INSERT ON bills`

After `POST /bills/generate`, run:

```sql
SELECT n.id, n.customer_id, n.bill_id, n.message, n.channel, n.read, n.created_at
FROM notifications n
WHERE n.bill_id = '<generated-bill-uuid>'
ORDER BY n.created_at DESC;
```

**Expected:**
- One row with `channel = 'IN_APP'`
- `read = false`
- Message contains customer name, billing month/year, and total amount
- `customer_id = 'a1111111-1111-1111-1111-111111111101'`

Verify trigger exists:

```sql
SELECT tgname, tgrelid::regclass AS table_name
FROM pg_trigger
WHERE tgname = 'trg_bill_generated_notify';
```

### 9.2 Payment trigger + `sp_mark_bill_paid`

**Procedure:** `sp_mark_bill_paid(p_bill_id)`  
**Function:** `fn_on_payment_insert()`  
**Trigger:** `trg_payment_insert` — fires `AFTER INSERT ON payments`

When a payment reduces `bills.balance` to `0`, the trigger calls the procedure to:
1. Set `bill_status = 'PAID'`
2. Insert a payment-confirmation notification

**After partial payment** (balance > 0):

```sql
SELECT id, bill_status, total_amount, amount_paid, balance
FROM bills
WHERE id = '<bill-uuid>';
```

**Expected:** `bill_status` still `APPROVED`, `balance > 0`, `amount_paid` increased.

**After full payment** (balance = 0):

```sql
-- Bill should be PAID
SELECT id, bill_status, balance, amount_paid, total_amount
FROM bills
WHERE id = '<bill-uuid>';

-- Payment confirmation notification
SELECT id, message, read, created_at
FROM notifications
WHERE bill_id = '<bill-uuid>'
ORDER BY created_at DESC;
```

**Expected:**
- `bill_status = 'PAID'`
- `balance = 0`
- Second notification with "fully paid" message

Verify objects exist:

```sql
SELECT proname, prokind
FROM pg_proc
WHERE proname IN ('fn_on_payment_insert', 'sp_mark_bill_paid');

SELECT tgname FROM pg_trigger WHERE tgname = 'trg_payment_insert';
```

### 9.3 Payment reference uniqueness

```sql
SELECT reference_number, COUNT(*)
FROM payments
GROUP BY reference_number
HAVING COUNT(*) > 1;
```

**Expected:** No rows (unique constraint enforced).

### 9.4 Cancelled bill — period re-generation (V18)

After cancelling a PENDING bill:

```sql
SELECT id, meter_id, billing_month, billing_year, bill_status
FROM bills
WHERE meter_id = 'b1111111-1111-1111-1111-111111111101'
  AND billing_month = 5
  AND billing_year = 2026
ORDER BY created_at;
```

**Expected:** One row with `bill_status = 'CANCELLED'`.

After regenerating the same period:

**Expected:** New row with `bill_status = 'PENDING'` (partial unique index `uq_bill_meter_period_active` excludes `CANCELLED`).

Verify index:

```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'bills'
  AND indexname = 'uq_bill_meter_period_active';
```

### 9.5 Email rebrand (V19)

```sql
SELECT email FROM users WHERE email LIKE '%@wasac.gov.rw';
SELECT email FROM customers WHERE email LIKE '%@wasac.gov.rw';
```

**Expected:** No remaining `@javat.com` in seeded accounts (admin, finance, operator, customer).

### 9.6 Seed data integrity

```sql
-- Customer linked to portal user
SELECT c.id, c.full_names, c.email, u.email AS user_email, u.role
FROM customers c
LEFT JOIN users u ON c.user_id = u.id
WHERE c.id = 'a1111111-1111-1111-1111-111111111101';

-- Meter and May 2026 reading
SELECT m.meter_number, r.billing_month, r.billing_year,
       r.previous_reading, r.current_reading
FROM meters m
JOIN meter_readings r ON r.meter_id = m.id
WHERE m.id = 'b1111111-1111-1111-1111-111111111101';

-- Active water tariff
SELECT id, name, flat_rate, tax_rate, active, effective_from, effective_to
FROM tariff_versions
WHERE id = 'd1111111-1111-1111-1111-111111111101';
```

---

## 10. Full test checklist

Use this as a sign-off list before demo or submission.

### Authentication
- [ ] Login works for all 4 seeded roles
- [ ] Register validation (email, password, phone)
- [ ] Duplicate email/username rejected
- [ ] Unauthenticated access to protected endpoints returns `401`

### Users
- [ ] Profile read/update (`/me`)
- [ ] Password change
- [ ] Admin user CRUD and role change
- [ ] Non-admin blocked from admin endpoints

### Customers & meters
- [ ] Customer registration validation (national ID, address)
- [ ] Meter attach validation (meter number format, installation date)
- [ ] Duplicate meter number rejected
- [ ] Meter list filters work

### Readings
- [ ] OPERATOR can capture reading
- [ ] Duplicate period rejected
- [ ] Invalid reading order rejected
- [ ] Reading date before installation rejected

### Tariffs
- [ ] FLAT and TIERED tariff creation
- [ ] Active tariff retrieval for WATER
- [ ] Missing flatRate on FLAT tariff rejected

### Billing (core workflow)
- [ ] Bill generated with correct amounts (12,168.75 FRW for seed data)
- [ ] DB notification created on bill insert
- [ ] Approve changes status to APPROVED
- [ ] Cancel PENDING bill works
- [ ] Regenerate after cancel works
- [ ] Duplicate active bill for same period rejected
- [ ] Customer sees only own bills

### Payments
- [ ] Payment against PENDING bill rejected
- [ ] Partial payment reduces balance
- [ ] Overpayment rejected
- [ ] Duplicate reference rejected
- [ ] Full payment sets bill to PAID
- [ ] DB payment-confirmation notification created
- [ ] Customer sees only own payments

### Notifications
- [ ] Customer sees bill and payment notifications
- [ ] Mark as read works
- [ ] ADMIN can list all notifications

### Database routines
- [ ] `trg_bill_generated_notify` verified in DB
- [ ] `trg_payment_insert` + `sp_mark_bill_paid` verified in DB
- [ ] `uq_bill_meter_period_active` allows re-bill after cancel
- [ ] Flyway at v19, no checksum errors

---

## Quick curl examples

**Login:**

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@wasac.gov.rw","password":"Admin@1234"}'
```

**Generate bill (replace TOKEN):**

```bash
curl -s -X POST http://localhost:8080/api/v1/bills/generate \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"meterId":"b1111111-1111-1111-1111-111111111101","billingMonth":5,"billingYear":2026}'
```

**Approve bill:**

```bash
curl -s -X PATCH http://localhost:8080/api/v1/bills/<BILL_ID>/approve \
  -H "Authorization: Bearer FINANCE_TOKEN"
```

---

*Document version: aligned with Flyway migrations V1–V19 and Spring Boot 3.4.5.*
