# Phase 0 — Design Artifacts

**Project:** WASAC / REG Utility Billing System  
**Stack:** Spring Boot 3.4 · Java 22 · PostgreSQL · JWT · Flyway  
**Status:** Design-only (pre-implementation)

---

## 1. Diagram Source Files

All diagram **source code** lives in `docs/design/`. Render to PNG/PDF for exam submission.

| File | Format | Description |
|------|--------|-------------|
| [`erd.mmd`](design/erd.mmd) | Mermaid | Entity Relationship Diagram |
| [`erd.puml`](design/erd.puml) | PlantUML | ERD (alternative export) |
| [`system-flow.mmd`](design/system-flow.mmd) | Mermaid | Spring Boot application flow |
| [`system-flow.puml`](design/system-flow.puml) | PlantUML | Application flow (alternative) |
| [`bill-lifecycle.mmd`](design/bill-lifecycle.mmd) | Mermaid | Bill status state machine |
| [`reading-capture-flow.mmd`](design/reading-capture-flow.mmd) | Mermaid | Operator reading capture sequence |

### Export to PNG / PDF

**Option A — Mermaid CLI (recommended)**

```powershell
cd docs/design
.\generate-diagrams.ps1
```

Or manually:

```bash
npm install -g @mermaid-js/mermaid-cli
mmdc -i erd.mmd -o ../exports/erd.png -b transparent
mmdc -i system-flow.mmd -o ../exports/system-flow.png -b transparent
mmdc -i bill-lifecycle.mmd -o ../exports/bill-lifecycle.png -b transparent
mmdc -i reading-capture-flow.mmd -o ../exports/reading-capture-flow.png -b transparent
```

**Option B — PlantUML (requires Java)**

```bash
# Download plantuml.jar from https://plantuml.com/download
java -jar plantuml.jar -tpng docs/design/erd.puml docs/design/system-flow.puml
java -jar plantuml.jar -tpdf docs/design/erd.puml
```

**Option C — Online (no install)**

- Mermaid Live Editor: https://mermaid.live — paste `.mmd` contents → Export PNG/SVG
- PlantUML Online: https://www.plantuml.com/plantuml — paste `.puml` contents

Exported images are written to [`docs/exports/`](exports/).

---

## 2. Entity Relationship Overview

### Core relationships

```
users (1) ──optional── (0..1) customers
customers (1) ── (N) meters
meters (1) ── (N) meter_readings
meters (1) ── (N) bills
customers (1) ── (N) bills
tariff_versions (1) ── (N) tariff_tiers
tariff_versions (1) ── (N) bills
bills (1) ── (N) payments
customers (1) ── (N) notifications
bills (1) ── (N) notifications
```

### Key constraints (database level)

| Table | Constraint | Purpose |
|-------|-----------|---------|
| `customers` | `UNIQUE (national_id)` | No duplicate customer registration |
| `customers` | `UNIQUE (email)` | No duplicate email |
| `meters` | `UNIQUE (meter_number)` | Globally unique meter |
| `meter_readings` | `UNIQUE (meter_id, billing_month, billing_year)` | One reading per meter per month |
| `meter_readings` | `CHECK (current_reading > previous_reading)` | Valid consumption |
| `bills` | `UNIQUE (meter_id, billing_month, billing_year)` | One bill per meter per cycle |
| `bills` | Trigger on INSERT | Insert notification message |
| `payments` | Trigger on INSERT when balance = 0 | Mark PAID + notify customer |

### Role model

| Role | Responsibilities |
|------|-----------------|
| `ROLE_ADMIN` | Tariffs, users, customers, meters, bill generation |
| `ROLE_OPERATOR` | Capture meter readings |
| `ROLE_FINANCE` | Approve bills, record payments |
| `ROLE_CUSTOMER` | View own bills, payments, notifications |

---

## 3. API Contract

All authenticated endpoints require header:

```
Authorization: Bearer <access_token>
```

All responses use the standard envelope:

```json
{
  "success": true,
  "message": "Human-readable summary",
  "data": { },
  "errors": null,
  "path": "/api/v1/...",
  "timestamp": "2026-06-05T10:00:00Z"
}
```

Pagination responses wrap `data` in:

```json
{
  "content": [ ],
  "page": 0,
  "size": 10,
  "totalElements": 100,
  "totalPages": 10,
  "first": true,
  "last": false
}
```

---

### 3.1 Authentication (Public)

| Method | Endpoint | Role | Request Body | Success Response |
|--------|----------|------|--------------|------------------|
| POST | `/api/v1/auth/register` | Public | `RegisterRequest` | `201` → `AuthResponse` |
| POST | `/api/v1/auth/login` | Public | `LoginRequest` | `200` → `AuthResponse` |
| POST | `/api/v1/auth/forgot-password` | Public | `ForgotPasswordRequest` | `200` → void |
| POST | `/api/v1/auth/reset-password` | Public | `ResetPasswordRequest` | `200` → void |
| GET | `/api/v1/auth/verify-email?token=` | Public | — | `200` → void |

**RegisterRequest**

```json
{
  "firstName": "Jean",
  "lastName": "Uwimana",
  "email": "jean.uwimana@example.com",
  "phone": "+250788123456",
  "username": "jean.uwimana",
  "password": "Secret@123"
}
```

**AuthResponse**

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn": 86400,
  "email": "jean.uwimana@example.com",
  "role": "CUSTOMER"
}
```

---

### 3.2 User Management

| Method | Endpoint | Role | Request | Response |
|--------|----------|------|---------|----------|
| GET | `/api/v1/users/me` | Any authenticated | — | `UserDto` |
| PATCH | `/api/v1/users/me` | Any authenticated | `UpdateProfileRequest` | `UserDto` |
| PATCH | `/api/v1/users/me/password` | Any authenticated | `UpdatePasswordRequest` | void |
| GET | `/api/v1/users` | ADMIN | query: page, size, role, status, search | `PageResponse<UserDto>` |
| GET | `/api/v1/users/{id}` | ADMIN | — | `UserDto` |
| POST | `/api/v1/users` | ADMIN | `CreateUserRequest` | `201 UserDto` |
| PATCH | `/api/v1/users/{id}/role` | ADMIN | `UpdateRoleRequest` | `UserDto` |
| PATCH | `/api/v1/users/{id}/deactivate` | ADMIN | — | `UserDto` |
| PATCH | `/api/v1/users/{id}/activate` | ADMIN | — | `UserDto` |

**CreateUserRequest** (admin creates staff accounts)

```json
{
  "firstName": "Alice",
  "lastName": "Mukamana",
  "email": "alice@wasac.gov.rw",
  "phone": "+250788000001",
  "username": "alice.mukamana",
  "password": "Secret@123",
  "role": "OPERATOR"
}
```

---

### 3.3 Customer Management

| Method | Endpoint | Role | Request | Response |
|--------|----------|------|---------|----------|
| POST | `/api/v1/customers` | ADMIN | `CreateCustomerRequest` | `201 CustomerDto` |
| GET | `/api/v1/customers` | ADMIN, FINANCE | query: page, size, status, search | `PageResponse<CustomerDto>` |
| GET | `/api/v1/customers/{id}` | ADMIN, FINANCE | — | `CustomerDto` |
| GET | `/api/v1/customers/me` | CUSTOMER | — | `CustomerDto` |
| PATCH | `/api/v1/customers/{id}` | ADMIN | `UpdateCustomerRequest` | `CustomerDto` |
| PATCH | `/api/v1/customers/{id}/deactivate` | ADMIN | — | `CustomerDto` |
| PATCH | `/api/v1/customers/{id}/activate` | ADMIN | — | `CustomerDto` |
| POST | `/api/v1/customers/{id}/link-user` | ADMIN | `{ "email": "..." }` | `CustomerDto` |

**CreateCustomerRequest**

```json
{
  "fullNames": "Jean Pierre Uwimana",
  "nationalId": "1199887766554433",
  "email": "jean.uwimana@example.com",
  "phone": "+250788123456",
  "address": "KG 123 St, Kigali, Rwanda",
  "createUserAccount": true
}
```

**CustomerDto**

```json
{
  "id": 1,
  "fullNames": "Jean Pierre Uwimana",
  "nationalId": "1199887766554433",
  "email": "jean.uwimana@example.com",
  "phone": "+250788123456",
  "address": "KG 123 St, Kigali, Rwanda",
  "status": "ACTIVE",
  "userId": 5,
  "createdAt": "2026-06-05T10:00:00Z"
}
```

---

### 3.4 Meter Management

| Method | Endpoint | Role | Request | Response |
|--------|----------|------|---------|----------|
| POST | `/api/v1/customers/{customerId}/meters` | ADMIN | `CreateMeterRequest` | `201 MeterDto` |
| GET | `/api/v1/meters` | ADMIN, OPERATOR | query: customerId, meterType, status | `PageResponse<MeterDto>` |
| GET | `/api/v1/meters/{id}` | ADMIN, OPERATOR | — | `MeterDto` |
| PATCH | `/api/v1/meters/{id}` | ADMIN | `UpdateMeterRequest` | `MeterDto` |
| PATCH | `/api/v1/meters/{id}/deactivate` | ADMIN | — | `MeterDto` |
| PATCH | `/api/v1/meters/{id}/activate` | ADMIN | — | `MeterDto` |

**CreateMeterRequest**

```json
{
  "meterNumber": "WTR-KGL-001234",
  "meterType": "WATER",
  "installationDate": "2024-03-15"
}
```

**MeterDto**

```json
{
  "id": 10,
  "customerId": 1,
  "meterNumber": "WTR-KGL-001234",
  "meterType": "WATER",
  "installationDate": "2024-03-15",
  "status": "ACTIVE"
}
```

---

### 3.5 Meter Reading Management

| Method | Endpoint | Role | Request | Response |
|--------|----------|------|---------|----------|
| POST | `/api/v1/readings` | OPERATOR | `CreateMeterReadingRequest` | `201 MeterReadingDto` |
| GET | `/api/v1/readings` | OPERATOR, ADMIN | query: meterId, month, year | `PageResponse<MeterReadingDto>` |
| GET | `/api/v1/readings/{id}` | OPERATOR, ADMIN | — | `MeterReadingDto` |

**CreateMeterReadingRequest**

```json
{
  "meterId": 10,
  "previousReading": 1250.50,
  "currentReading": 1285.75,
  "readingDate": "2026-05-28"
}
```

**MeterReadingDto**

```json
{
  "id": 100,
  "meterId": 10,
  "previousReading": 1250.50,
  "currentReading": 1285.75,
  "consumption": 35.25,
  "readingDate": "2026-05-28",
  "billingMonth": 5,
  "billingYear": 2026,
  "capturedBy": "operator@wasac.gov.rw"
}
```

---

### 3.6 Tariff Configuration

| Method | Endpoint | Role | Request | Response |
|--------|----------|------|---------|----------|
| POST | `/api/v1/tariffs` | ADMIN | `CreateTariffRequest` | `201 TariffVersionDto` |
| GET | `/api/v1/tariffs` | ADMIN, FINANCE | query: meterType, active | `PageResponse<TariffVersionDto>` |
| GET | `/api/v1/tariffs/active?meterType=WATER` | ADMIN, FINANCE | — | `TariffVersionDto` |
| GET | `/api/v1/tariffs/{id}` | ADMIN, FINANCE | — | `TariffVersionDto` |

**CreateTariffRequest (FLAT)**

```json
{
  "name": "Water Standard 2026",
  "meterType": "WATER",
  "tariffType": "FLAT",
  "flatRate": 350.00,
  "fixedServiceCharge": 1500.00,
  "taxRate": 18.00,
  "penaltyRate": 5.00,
  "penaltyGraceDays": 15,
  "effectiveFrom": "2026-07-01"
}
```

**CreateTariffRequest (TIERED)**

```json
{
  "name": "Electricity Tiered 2026",
  "meterType": "ELECTRICITY",
  "tariffType": "TIERED",
  "fixedServiceCharge": 2000.00,
  "taxRate": 18.00,
  "penaltyRate": 5.00,
  "penaltyGraceDays": 15,
  "effectiveFrom": "2026-07-01",
  "tiers": [
    { "minUnits": 0, "maxUnits": 50, "ratePerUnit": 120.00 },
    { "minUnits": 51, "maxUnits": 200, "ratePerUnit": 180.00 },
    { "minUnits": 201, "maxUnits": null, "ratePerUnit": 250.00 }
  ]
}
```

---

### 3.7 Billing

| Method | Endpoint | Role | Request | Response |
|--------|----------|------|---------|----------|
| POST | `/api/v1/bills/generate` | ADMIN | `GenerateBillRequest` | `201 BillDto` |
| PATCH | `/api/v1/bills/{id}/approve` | ADMIN, FINANCE | — | `BillDto` |
| GET | `/api/v1/bills` | ADMIN, FINANCE (all); CUSTOMER (own) | query: status, month, year | `PageResponse<BillDto>` |
| GET | `/api/v1/bills/{id}` | ADMIN, FINANCE; CUSTOMER (own) | — | `BillDto` |

**GenerateBillRequest**

```json
{
  "meterId": 10,
  "billingMonth": 5,
  "billingYear": 2026
}
```

**BillDto**

```json
{
  "id": 500,
  "customerId": 1,
  "customerName": "Jean Pierre Uwimana",
  "meterId": 10,
  "meterNumber": "WTR-KGL-001234",
  "billingMonth": 5,
  "billingYear": 2026,
  "consumption": 35.25,
  "subtotal": 13837.50,
  "taxAmount": 2490.75,
  "penaltyAmount": 0.00,
  "totalAmount": 16328.25,
  "amountPaid": 0.00,
  "balance": 16328.25,
  "status": "PENDING",
  "dueDate": "2026-06-15"
}
```

**Notification message format (DB trigger)**

```
Dear Jean Pierre Uwimana,
Your 5/2026 utility bill of 16328.25 FRW has been successfully processed.
```

---

### 3.8 Payments

| Method | Endpoint | Role | Request | Response |
|--------|----------|------|---------|----------|
| POST | `/api/v1/payments` | ADMIN, FINANCE | `CreatePaymentRequest` | `201 PaymentDto` |
| GET | `/api/v1/payments` | ADMIN, FINANCE; CUSTOMER (own) | query: billId | `PageResponse<PaymentDto>` |
| GET | `/api/v1/payments/{id}` | ADMIN, FINANCE; CUSTOMER (own) | — | `PaymentDto` |

**CreatePaymentRequest**

```json
{
  "billId": 500,
  "amountPaid": 10000.00,
  "paymentMethod": "MOBILE_MONEY",
  "paymentDate": "2026-06-10"
}
```

**PaymentDto**

```json
{
  "id": 900,
  "billId": 500,
  "amountPaid": 10000.00,
  "paymentMethod": "MOBILE_MONEY",
  "paymentDate": "2026-06-10",
  "recordedBy": "finance@wasac.gov.rw",
  "remainingBalance": 6328.25
}
```

---

### 3.9 Notifications

| Method | Endpoint | Role | Request | Response |
|--------|----------|------|---------|----------|
| GET | `/api/v1/notifications` | CUSTOMER (own); ADMIN | query: read | `PageResponse<NotificationDto>` |
| PATCH | `/api/v1/notifications/{id}/read` | CUSTOMER | — | `NotificationDto` |

**NotificationDto**

```json
{
  "id": 1,
  "billId": 500,
  "message": "Dear Jean Pierre Uwimana, Your 5/2026 utility bill of 16328.25 FRW has been successfully processed.",
  "channel": "IN_APP",
  "read": false,
  "createdAt": "2026-06-05T10:00:00Z"
}
```

---

### 3.10 Standard HTTP Error Codes

| Code | When |
|------|------|
| `400` | Validation failure, business rule violation |
| `401` | Missing or invalid JWT |
| `403` | Authenticated but wrong role / not owner |
| `404` | Resource not found |
| `409` | Duplicate (national ID, email, reading, bill) |
| `500` | Unexpected server error |

---

## 4. Validation Rules Matrix

Custom validators are defined in `com.spring.JavaT.common.validation` (Phase 1 implementation).

### 4.1 User & Authentication

| Field | DTO(s) | Rules | Error Message |
|-------|--------|-------|---------------|
| `firstName` | RegisterRequest, CreateUserRequest, UpdateProfileRequest | `@NotBlank`, `@Size(2,50)`, `@ValidPersonName` | First name is required / must be 2–50 characters / must contain letters and cannot be numbers only |
| `lastName` | RegisterRequest, CreateUserRequest, UpdateProfileRequest | `@NotBlank`, `@Size(2,50)`, `@ValidPersonName` | Last name is required / must be 2–50 characters / must contain letters and cannot be numbers only |
| `email` | RegisterRequest, LoginRequest, CreateUserRequest | `@NotBlank`, `@Email`, `@Size(max=254)` | Email address is required / Must be a valid email address |
| `phone` | RegisterRequest, CreateUserRequest | `@NotBlank`, `@ValidPhone` | Phone number is required / Must be a valid Rwanda phone number (e.g. +250788123456) |
| `username` | RegisterRequest, CreateUserRequest | `@NotBlank`, `@Size(3,50)`, `@ValidUsername`, `@NoWhitespace` | Username is required / must be 3–50 characters / must contain at least one letter and use only letters, digits, dots, hyphens, underscores |
| `password` | RegisterRequest, CreateUserRequest | `@NotBlank`, `@ValidPassword` | Password must be 8–72 characters with upper, lower, digit, and special character |
| `role` | CreateUserRequest, UpdateRoleRequest | `@NotBlank`, `@ValidEnum(Role)` | Invalid value. Accepted values are: ADMIN, OPERATOR, FINANCE, CUSTOMER |

**`@ValidPersonName` rejects:** `123456789`, `@John`, `John123`, empty spaces, names without any letter.

**`@ValidUsername` rejects:** `123456789`, `___`, leading/trailing spaces.

---

### 4.2 Customer

| Field | DTO(s) | Rules | Error Message |
|-------|--------|-------|---------------|
| `fullNames` | CreateCustomerRequest, UpdateCustomerRequest | `@NotBlank`, `@Size(2,100)`, `@ValidPersonName` | Full names are required / must contain letters and cannot be numbers only |
| `nationalId` | CreateCustomerRequest | `@NotBlank`, `@ValidNationalId` | National ID is required / must be exactly 16 digits |
| `email` | CreateCustomerRequest | `@NotBlank`, `@Email`, `@Size(max=254)` | Email address is required / Must be a valid email address |
| `phone` | CreateCustomerRequest | `@NotBlank`, `@ValidPhone` | Phone number is required / Must be a valid phone number |
| `address` | CreateCustomerRequest | `@NotBlank`, `@Size(5,255)`, `@ValidAddress` | Address is required / must be 5–255 characters / must look like a real address (not numbers only) |

**Service rules:**

| Rule | Error Code | Message |
|------|-----------|---------|
| Duplicate national ID | `DUPLICATE_NATIONAL_ID` | Customer with national ID '…' already exists |
| Duplicate email | `DUPLICATE_EMAIL` | Customer with email '…' already exists |
| Inactive customer billed | `CUSTOMER_INACTIVE` | Inactive customers cannot receive bills |

---

### 4.3 Meter

| Field | DTO(s) | Rules | Error Message |
|-------|--------|-------|---------------|
| `meterNumber` | CreateMeterRequest | `@NotBlank`, `@ValidMeterNumber` | Meter number is required / must be 6–20 uppercase letters, digits, or hyphens |
| `meterType` | CreateMeterRequest | `@NotNull`, `@ValidEnum(MeterType)` | Meter type is required / must be WATER or ELECTRICITY |
| `installationDate` | CreateMeterRequest | `@NotNull`, `@PastOrPresent` | Installation date is required / must not be in the future |

**Service rules:**

| Rule | Error Code | Message |
|------|-----------|---------|
| Duplicate meter number | `DUPLICATE_METER` | Meter number '…' is already registered |
| Customer inactive | `CUSTOMER_INACTIVE` | Cannot attach meter to inactive customer |
| Inactive meter reading | `METER_INACTIVE` | Cannot capture reading for inactive meter |

---

### 4.4 Meter Reading

| Field | DTO(s) | Rules | Error Message |
|-------|--------|-------|---------------|
| `meterId` | CreateMeterReadingRequest | `@NotNull` | Meter ID is required |
| `previousReading` | CreateMeterReadingRequest | `@NotNull`, `@DecimalMin("0")`, `@Digits(12,2)` | Previous reading is required / must be zero or positive |
| `currentReading` | CreateMeterReadingRequest | `@NotNull`, `@DecimalMin("0")`, `@Digits(12,2)` | Current reading is required / must be zero or positive |
| `readingDate` | CreateMeterReadingRequest | `@NotNull`, `@PastOrPresent` | Reading date is required / must not be in the future |
| *(class-level)* | CreateMeterReadingRequest | `@ValidMeterReading` | Current reading must be greater than previous reading |

**Service rules:**

| Rule | Error Code | Message |
|------|-----------|---------|
| One reading per meter/month | `DUPLICATE_READING` | A reading for this meter already exists for {month}/{year} |
| Meter not active | `METER_INACTIVE` | Meter must be active to capture a reading |

---

### 4.5 Tariff

| Field | DTO(s) | Rules | Error Message |
|-------|--------|-------|---------------|
| `name` | CreateTariffRequest | `@NotBlank`, `@Size(2,100)` | Tariff name is required |
| `meterType` | CreateTariffRequest | `@NotNull`, `@ValidEnum(MeterType)` | Meter type is required |
| `tariffType` | CreateTariffRequest | `@NotNull`, `@ValidEnum(TariffType)` | Tariff type must be FLAT or TIERED |
| `flatRate` | CreateTariffRequest | `@DecimalMin("0.01")` when FLAT | Flat rate must be positive |
| `fixedServiceCharge` | CreateTariffRequest | `@NotNull`, `@DecimalMin("0")` | Fixed service charge is required |
| `taxRate` | CreateTariffRequest | `@NotNull`, `@DecimalMin("0")`, `@DecimalMax("100")` | Tax rate must be between 0 and 100 |
| `penaltyRate` | CreateTariffRequest | `@NotNull`, `@DecimalMin("0")`, `@DecimalMax("100")` | Penalty rate must be between 0 and 100 |
| `penaltyGraceDays` | CreateTariffRequest | `@NotNull`, `@Min(0)`, `@Max(90)` | Grace days must be 0–90 |
| `effectiveFrom` | CreateTariffRequest | `@NotNull`, `@FutureOrPresent` | Effective date must be today or in the future |
| `tiers[].minUnits` | CreateTariffRequest | `@DecimalMin("0")` | Tier min units must be zero or positive |
| `tiers[].ratePerUnit` | CreateTariffRequest | `@DecimalMin("0.01")` | Tier rate must be positive |

**Service rules:**

| Rule | Error Code | Message |
|------|-----------|---------|
| TIERED without tiers | `TARIFF_TIERS_REQUIRED` | Tiered tariff must include at least one tier |
| FLAT without flatRate | `FLAT_RATE_REQUIRED` | Flat tariff must include a flat rate |
| New tariff closes old | — | Previous active version gets `effective_to = new.effective_from - 1 day` |

---

### 4.6 Billing

| Field | DTO(s) | Rules | Error Message |
|-------|--------|-------|---------------|
| `meterId` | GenerateBillRequest | `@NotNull` | Meter ID is required |
| `billingMonth` | GenerateBillRequest | `@NotNull`, `@Min(1)`, `@Max(12)` | Billing month must be 1–12 |
| `billingYear` | GenerateBillRequest | `@NotNull`, `@Min(2020)`, `@Max(2100)` | Billing year is invalid |

**Service rules:**

| Rule | Error Code | Message |
|------|-----------|---------|
| No reading for period | `READING_NOT_FOUND` | No meter reading found for {month}/{year} |
| Duplicate bill | `DUPLICATE_BILL` | Bill already exists for this meter and period |
| Customer inactive | `CUSTOMER_INACTIVE` | Inactive customers cannot receive bills |
| No active tariff | `TARIFF_NOT_FOUND` | No active tariff found for meter type on billing date |
| Approve non-pending | `INVALID_BILL_STATUS` | Only PENDING bills can be approved |

---

### 4.7 Payment

| Field | DTO(s) | Rules | Error Message |
|-------|--------|-------|---------------|
| `billId` | CreatePaymentRequest | `@NotNull` | Bill reference is required |
| `amountPaid` | CreatePaymentRequest | `@NotNull`, `@DecimalMin("0.01")`, `@Digits(12,2)` | Amount paid must be greater than zero |
| `paymentMethod` | CreatePaymentRequest | `@NotNull`, `@ValidEnum(PaymentMethod)` | Payment method is required |
| `paymentDate` | CreatePaymentRequest | `@NotNull`, `@PastOrPresent` | Payment date must not be in the future |

**Service rules:**

| Rule | Error Code | Message |
|------|-----------|---------|
| Bill not approved | `BILL_NOT_APPROVED` | Payments can only be recorded against APPROVED bills |
| Amount exceeds balance | `PAYMENT_EXCEEDS_BALANCE` | Payment amount exceeds outstanding balance |
| Partial payment | — | Allowed; balance updated, status stays APPROVED/OVERDUE |
| Full payment | — | balance = 0 → status PAID (DB trigger + notification) |

---

## 5. Database Routines (Design Reference)

See [`db-routines.sql`](design/db-routines.sql) for trigger and stored procedure SQL used in Phase 7.

| Routine | Event | Action |
|---------|-------|--------|
| `fn_notify_bill_generated()` | AFTER INSERT on `bills` | Insert IN_APP notification with exam message format |
| `fn_on_payment_insert()` | AFTER INSERT on `payments` | Recalculate balance; if 0, call `sp_mark_bill_paid` |
| `sp_mark_bill_paid(bill_id)` | Called from payment trigger | SET status = PAID; insert payment confirmation notification |

---

## 6. Exam Submission Checklist (Phase 0)

- [x] ERD diagram source (Mermaid + PlantUML)
- [x] Spring Boot flow diagram source
- [x] Bill lifecycle state diagram
- [x] Meter reading sequence diagram
- [x] API contract table
- [x] Validation rules matrix
- [ ] Export PNG/PDF to `docs/exports/` (run `generate-diagrams.ps1`)
- [ ] Include exported diagrams in exam report

---

*Next step: Phase 1 — implement custom validators, update roles, add phone to User entity.*
