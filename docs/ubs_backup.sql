--
-- PostgreSQL database dump
--

-- Dumped from database version 17.0
-- Dumped by pg_dump version 17.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: fn_notify_bill_generated(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_notify_bill_generated() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    INSERT INTO notifications (customer_id, bill_id, message, channel, email_sent, read, created_at)
    SELECT
        NEW.customer_id,
        NEW.id,
        format(
            'Dear %s, Your %s/%s utility bill of %s FRW has been successfully processed.',
            c.full_names,
            NEW.billing_month,
            NEW.billing_year,
            NEW.total_amount
        ),
        'IN_APP',
        FALSE,
        FALSE,
        NOW()
    FROM customers c
    WHERE c.id = NEW.customer_id;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.fn_notify_bill_generated() OWNER TO postgres;

--
-- Name: fn_on_payment_insert(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_on_payment_insert() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_balance DECIMAL(12, 2);
BEGIN
    SELECT balance INTO v_balance FROM bills WHERE id = NEW.bill_id;

    IF v_balance <= 0 THEN
        CALL sp_mark_bill_paid(NEW.bill_id);
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.fn_on_payment_insert() OWNER TO postgres;

--
-- Name: sp_mark_bill_paid(uuid); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.sp_mark_bill_paid(IN p_bill_id uuid)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_bill     bills%ROWTYPE;
    v_customer customers%ROWTYPE;
BEGIN
    SELECT * INTO v_bill FROM bills WHERE id = p_bill_id FOR UPDATE;

    IF v_bill.balance > 0 THEN
        RETURN;
    END IF;

    IF v_bill.bill_status = 'PAID' THEN
        RETURN;
    END IF;

    UPDATE bills
    SET bill_status = 'PAID',
        updated_at  = NOW()
    WHERE id = p_bill_id;

    SELECT * INTO v_customer FROM customers WHERE id = v_bill.customer_id;

    INSERT INTO notifications (customer_id, bill_id, message, channel, email_sent, read, created_at)
    VALUES (
        v_bill.customer_id,
        p_bill_id,
        format(
            'Dear %s, Your %s/%s utility bill of %s FRW has been fully paid. Thank you.',
            v_customer.full_names,
            v_bill.billing_month,
            v_bill.billing_year,
            v_bill.total_amount
        ),
        'IN_APP',
        FALSE,
        FALSE,
        NOW()
    );
END;
$$;


ALTER PROCEDURE public.sp_mark_bill_paid(IN p_bill_id uuid) OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: bills; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bills (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    customer_id uuid NOT NULL,
    meter_id uuid NOT NULL,
    tariff_version_id uuid NOT NULL,
    billing_month integer NOT NULL,
    billing_year integer NOT NULL,
    consumption numeric(12,2) NOT NULL,
    subtotal numeric(12,2) NOT NULL,
    tax_amount numeric(12,2) NOT NULL,
    penalty_amount numeric(12,2) DEFAULT 0 NOT NULL,
    total_amount numeric(12,2) NOT NULL,
    amount_paid numeric(12,2) DEFAULT 0 NOT NULL,
    balance numeric(12,2) NOT NULL,
    bill_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    approved_by uuid,
    approved_at timestamp with time zone,
    due_date date NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_by character varying(100),
    deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by character varying(100),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    CONSTRAINT chk_bills_amounts CHECK (((balance >= (0)::numeric) AND (amount_paid >= (0)::numeric) AND (total_amount >= (0)::numeric))),
    CONSTRAINT chk_bills_bill_status CHECK (((bill_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING'::character varying, 'APPROVED'::character varying, 'PAID'::character varying, 'OVERDUE'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT chk_bills_month CHECK (((billing_month >= 1) AND (billing_month <= 12))),
    CONSTRAINT chk_bills_year CHECK ((billing_year >= 2020))
);


ALTER TABLE public.bills OWNER TO postgres;

--
-- Name: customers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.customers (
    full_names character varying(100) NOT NULL,
    national_id character varying(16) NOT NULL,
    email character varying(254) NOT NULL,
    phone character varying(20) NOT NULL,
    address character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_by character varying(100),
    deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by character varying(100),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    id uuid NOT NULL,
    user_id uuid,
    CONSTRAINT chk_customers_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


ALTER TABLE public.customers OWNER TO postgres;

--
-- Name: email_verification_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.email_verification_tokens (
    token character varying(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.email_verification_tokens OWNER TO postgres;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: meter_readings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.meter_readings (
    previous_reading numeric(12,2) NOT NULL,
    current_reading numeric(12,2) NOT NULL,
    reading_date date NOT NULL,
    billing_month integer NOT NULL,
    billing_year integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL,
    meter_id uuid NOT NULL,
    captured_by uuid NOT NULL,
    CONSTRAINT chk_billing_month CHECK (((billing_month >= 1) AND (billing_month <= 12))),
    CONSTRAINT chk_billing_year CHECK ((billing_year >= 2020)),
    CONSTRAINT chk_reading_order CHECK ((current_reading > previous_reading))
);


ALTER TABLE public.meter_readings OWNER TO postgres;

--
-- Name: meters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.meters (
    meter_number character varying(20) NOT NULL,
    meter_type character varying(20) NOT NULL,
    installation_date date NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_by character varying(100),
    deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by character varying(100),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    id uuid NOT NULL,
    customer_id uuid NOT NULL,
    CONSTRAINT chk_meters_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[]))),
    CONSTRAINT chk_meters_type CHECK (((meter_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[])))
);


ALTER TABLE public.meters OWNER TO postgres;

--
-- Name: notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    customer_id uuid NOT NULL,
    bill_id uuid,
    message text NOT NULL,
    channel character varying(20) DEFAULT 'IN_APP'::character varying NOT NULL,
    email_sent boolean DEFAULT false NOT NULL,
    read boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_notifications_channel CHECK (((channel)::text = ANY ((ARRAY['IN_APP'::character varying, 'EMAIL'::character varying])::text[])))
);


ALTER TABLE public.notifications OWNER TO postgres;

--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.password_reset_tokens (
    token character varying(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.password_reset_tokens OWNER TO postgres;

--
-- Name: payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    bill_id uuid NOT NULL,
    amount_paid numeric(12,2) NOT NULL,
    payment_method character varying(30) NOT NULL,
    payment_date date NOT NULL,
    reference_number character varying(64) NOT NULL,
    recorded_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_payments_amount CHECK ((amount_paid > (0)::numeric)),
    CONSTRAINT chk_payments_method CHECK (((payment_method)::text = ANY ((ARRAY['CASH'::character varying, 'MOBILE_MONEY'::character varying, 'BANK_TRANSFER'::character varying, 'CARD'::character varying])::text[])))
);


ALTER TABLE public.payments OWNER TO postgres;

--
-- Name: tariff_tiers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tariff_tiers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tariff_version_id uuid NOT NULL,
    min_units numeric(12,2) NOT NULL,
    max_units numeric(12,2),
    rate_per_unit numeric(12,4) NOT NULL,
    CONSTRAINT chk_tier_min_units CHECK ((min_units >= (0)::numeric)),
    CONSTRAINT chk_tier_range CHECK (((max_units IS NULL) OR (max_units >= min_units))),
    CONSTRAINT chk_tier_rate CHECK ((rate_per_unit > (0)::numeric))
);


ALTER TABLE public.tariff_tiers OWNER TO postgres;

--
-- Name: tariff_versions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tariff_versions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    meter_type character varying(20) NOT NULL,
    tariff_type character varying(10) NOT NULL,
    flat_rate numeric(12,4),
    fixed_service_charge numeric(12,2) NOT NULL,
    tax_rate numeric(5,2) NOT NULL,
    penalty_rate numeric(5,2) NOT NULL,
    penalty_grace_days integer NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_by character varying(100),
    deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by character varying(100),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    CONSTRAINT chk_tariff_effective_range CHECK (((effective_to IS NULL) OR (effective_to >= effective_from))),
    CONSTRAINT chk_tariff_grace_days CHECK (((penalty_grace_days >= 0) AND (penalty_grace_days <= 90))),
    CONSTRAINT chk_tariff_meter_type CHECK (((meter_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[]))),
    CONSTRAINT chk_tariff_penalty_rate CHECK (((penalty_rate >= (0)::numeric) AND (penalty_rate <= (100)::numeric))),
    CONSTRAINT chk_tariff_tax_rate CHECK (((tax_rate >= (0)::numeric) AND (tax_rate <= (100)::numeric))),
    CONSTRAINT chk_tariff_type CHECK (((tariff_type)::text = ANY ((ARRAY['FLAT'::character varying, 'TIERED'::character varying])::text[])))
);


ALTER TABLE public.tariff_versions OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    first_name character varying(50) NOT NULL,
    last_name character varying(50) NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(254) NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(20) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_by character varying(100),
    deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by character varying(100),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    phone character varying(20) NOT NULL,
    id uuid NOT NULL,
    CONSTRAINT chk_users_role CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'OPERATOR'::character varying, 'FINANCE'::character varying, 'CUSTOMER'::character varying])::text[]))),
    CONSTRAINT chk_users_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'SUSPENDED'::character varying, 'PENDING'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Data for Name: bills; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bills (id, customer_id, meter_id, tariff_version_id, billing_month, billing_year, consumption, subtotal, tax_amount, penalty_amount, total_amount, amount_paid, balance, bill_status, approved_by, approved_at, due_date, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, status) FROM stdin;
7c208bfb-f262-4acb-93cb-3ace6f2bd4cd	f32b0ca2-9524-42ff-a5c0-3351bdb01583	2847db71-10ad-44d4-96d6-7ca7f8796f35	d1111111-1111-1111-1111-111111111101	5	2026	107.80	28450.00	5121.00	0.00	33571.00	33571.00	0.00	PAID	f1111111-1111-1111-1111-111111111101	2026-06-05 04:24:19.044088-07	2026-06-15	2026-06-05 04:03:08.486717-07	2026-06-05 04:28:27.002175-07	admin@wasac.gov.rw	finance@wasac.gov.rw	f	\N	\N	ACTIVE
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.customers (full_names, national_id, email, phone, address, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, status, id, user_id) FROM stdin;
Jean Pierre Uwimana	1199880012345678	customer@wasac.gov.rw	+250788123456	KG 15 Ave, Kigali, Rwanda	2026-06-05 02:12:06.633045-07	2026-06-05 02:12:06.633045-07	system	system	f	\N	\N	ACTIVE	a1111111-1111-1111-1111-111111111101	f1111111-1111-1111-1111-111111111103
Marie Claire Uwase	1199887766554433	marie.uwase@example.com	+250788111222	KG 7 Ave, Kigali, Rwanda	2026-06-05 02:57:50.366623-07	2026-06-05 02:57:50.366623-07	admin@wasac.gov.rw	admin@wasac.gov.rw	f	\N	\N	ACTIVE	dd99567c-3f91-41a5-91ac-68bd789dbe27	\N
NGABO Oreste	1199887766554423	ngaboreste5@gmail.com	+250788111223	KG 7 Ave, Kigali, Rwanda	2026-06-05 03:12:18.782816-07	2026-06-05 03:12:18.782816-07	admin@wasac.gov.rw	admin@wasac.gov.rw	f	\N	\N	ACTIVE	b6e94cae-af20-4b41-b2bd-c02294cfd791	515565ce-fa74-4054-bf76-74063927262a
Mugisha Prince	1199887766554435	ngaboreste4@gmail.com	+250788123457	KG 123 St, Kigali, Rwanda	2026-06-05 03:35:18.204852-07	2026-06-05 03:35:18.204852-07	admin@wasac.gov.rw	admin@wasac.gov.rw	f	\N	\N	ACTIVE	f32b0ca2-9524-42ff-a5c0-3351bdb01583	a1254adc-2d50-4150-863d-beeb68c7db12
\.


--
-- Data for Name: email_verification_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.email_verification_tokens (token, expires_at, created_at, id, user_id) FROM stdin;
QCTKw7Ujqc8bNSt0AOtczCbHYSaAZFRrhXHb-Qe0jfy6KA97O34nvlBAGwdHTZmd	2026-06-06 01:07:35.414428-07	2026-06-05 01:07:35.414428-07	9374ee8d-39da-467c-bd17-1a7c09213996	9d6c9bb2-2a44-4d82-b9e1-f8d918d051a3
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create users table	SQL	V1__create_users_table.sql	753537118	postgres	2026-05-29 21:18:56.617498	17	t
2	2	seed admin user	SQL	V2__seed_admin_user.sql	-1413552071	postgres	2026-05-29 21:18:56.659532	7	t
3	3	create password reset tokens table	SQL	V3__create_password_reset_tokens_table.sql	1003729252	postgres	2026-05-29 21:18:56.676406	27	t
4	4	create email verification tokens table	SQL	V4__create_email_verification_tokens_table.sql	-699825629	postgres	2026-05-29 21:18:56.713909	9	t
5	5	update admin password	SQL	V5__update_admin_password.sql	1770104125	postgres	2026-05-29 21:18:56.731258	3	t
6	6	update roles and add phone	SQL	V6__update_roles_and_add_phone.sql	822658526	postgres	2026-06-05 10:02:07.037894	25	t
7	7	create customers table	SQL	V7__create_customers_table.sql	-1519102432	postgres	2026-06-05 10:10:39.459179	26	t
8	8	create meters table	SQL	V8__create_meters_table.sql	1721507108	postgres	2026-06-05 10:10:39.503728	12	t
9	9	create meter readings table	SQL	V9__create_meter_readings_table.sql	1605779589	postgres	2026-06-05 10:15:54.565701	36	t
10	10	migrate ids to uuid	SQL	V10__migrate_ids_to_uuid.sql	-49253419	postgres	2026-06-05 10:27:45.325043	80	t
11	11	create tariff tables	SQL	V11__create_tariff_tables.sql	-120083383	postgres	2026-06-05 10:36:36.866936	21	t
12	12	create bills table	SQL	V12__create_bills_table.sql	-1467011095	postgres	2026-06-05 10:53:27.336105	21	t
13	13	create notifications and bill trigger	SQL	V13__create_notifications_and_bill_trigger.sql	-2014729248	postgres	2026-06-05 10:53:27.379236	23	t
14	14	create payments table	SQL	V14__create_payments_table.sql	-1478674840	postgres	2026-06-05 10:59:58.991125	17	t
15	15	payment trigger	SQL	V15__payment_trigger.sql	-136793344	postgres	2026-06-05 10:59:59.025602	6	t
16	16	align db routines to exam spec	SQL	V16__align_db_routines_to_exam_spec.sql	1403617630	postgres	2026-06-05 11:05:50.897708	11	t
17	17	seed sample data	SQL	V17__seed_sample_data.sql	1410183868	postgres	2026-06-05 11:12:06.618971	20	t
18	18	add bill cancelled status	SQL	V18__add_bill_cancelled_status.sql	-1892462027	postgres	2026-06-05 11:38:55.397007	12	t
19	19	rebrand javat emails to wasac	SQL	V19__rebrand_javat_emails_to_wasac.sql	-1261281518	postgres	2026-06-05 11:38:55.429827	6	t
20	20	fix seeded password hashes	SQL	V20__fix_seeded_password_hashes.sql	1305375305	postgres	2026-06-05 12:49:40.855474	6	t
\.


--
-- Data for Name: meter_readings; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.meter_readings (previous_reading, current_reading, reading_date, billing_month, billing_year, created_at, id, meter_id, captured_by) FROM stdin;
1250.50	1285.75	2026-05-28	5	2026	2026-06-05 02:12:06.633045-07	e1111111-1111-1111-1111-111111111101	b1111111-1111-1111-1111-111111111101	f1111111-1111-1111-1111-111111111102
0.00	107.80	2026-05-28	5	2026	2026-06-05 03:50:14.531436-07	14290636-4896-4e9a-b126-9e5a314083ca	2847db71-10ad-44d4-96d6-7ca7f8796f35	f1111111-1111-1111-1111-111111111102
\.


--
-- Data for Name: meters; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.meters (meter_number, meter_type, installation_date, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, status, id, customer_id) FROM stdin;
WTR-KGL-001234	WATER	2024-03-15	2026-06-05 02:12:06.633045-07	2026-06-05 02:12:06.633045-07	system	system	f	\N	\N	ACTIVE	b1111111-1111-1111-1111-111111111101	a1111111-1111-1111-1111-111111111101
WTR-KGL-009999	WATER	2025-01-15	2026-06-05 03:37:14.74933-07	2026-06-05 03:37:14.74933-07	admin@wasac.gov.rw	admin@wasac.gov.rw	f	\N	\N	ACTIVE	2847db71-10ad-44d4-96d6-7ca7f8796f35	f32b0ca2-9524-42ff-a5c0-3351bdb01583
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notifications (id, customer_id, bill_id, message, channel, email_sent, read, created_at) FROM stdin;
de9b2e88-1266-47e9-b593-ce5642eaa372	f32b0ca2-9524-42ff-a5c0-3351bdb01583	7c208bfb-f262-4acb-93cb-3ace6f2bd4cd	Dear Mugisha Prince, Your 5/2026 utility bill of 33571.00 FRW has been successfully processed.	IN_APP	t	f	2026-06-05 04:03:08.391284-07
a902b99a-8698-44cf-b0e4-80a2bf7f3c37	f32b0ca2-9524-42ff-a5c0-3351bdb01583	7c208bfb-f262-4acb-93cb-3ace6f2bd4cd	Dear Mugisha Prince, Your 5/2026 utility bill of 33571.00 FRW has been fully paid. Thank you.	IN_APP	t	f	2026-06-05 04:28:27.002175-07
\.


--
-- Data for Name: password_reset_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.password_reset_tokens (token, expires_at, used, created_at, id, user_id) FROM stdin;
1JXeGgpzGE3mlFaHeKYJTZGlNRrMuSx9FxQAxzMn-hB99tctC-Y4B98AAmkj6nCy	2026-06-05 04:27:23.028072-07	t	2026-06-05 04:12:23.028072-07	fcc29b94-4ca7-408b-ba9d-62f1887853ad	a1254adc-2d50-4150-863d-beeb68c7db12
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.payments (id, bill_id, amount_paid, payment_method, payment_date, reference_number, recorded_by, created_at) FROM stdin;
a2814fba-9c28-46b0-829f-8d2f60cdec7e	7c208bfb-f262-4acb-93cb-3ace6f2bd4cd	10000.00	MOBILE_MONEY	2026-06-05	MM-20260610-8F3A2B1C	f1111111-1111-1111-1111-111111111101	2026-06-05 04:24:31.91658-07
663271d9-16db-4149-afbb-f31fce3de1a9	7c208bfb-f262-4acb-93cb-3ace6f2bd4cd	23571.00	MOBILE_MONEY	2026-06-05	MM-20260610-8F3A2B1D	f1111111-1111-1111-1111-111111111101	2026-06-05 04:28:27.012721-07
\.


--
-- Data for Name: tariff_tiers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tariff_tiers (id, tariff_version_id, min_units, max_units, rate_per_unit) FROM stdin;
6cfca6fb-9669-4f1b-bdac-3e9921f8d330	517b51e9-fc6b-49f3-8ee9-a6e9cd2cddf4	0.00	50.00	120.0000
\.


--
-- Data for Name: tariff_versions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tariff_versions (id, name, meter_type, tariff_type, flat_rate, fixed_service_charge, tax_rate, penalty_rate, penalty_grace_days, effective_from, effective_to, active, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, status) FROM stdin;
d1111111-1111-1111-1111-111111111101	WASAC Water Tariff 2024	WATER	FLAT	250.0000	1500.00	18.00	5.00	15	2024-01-01	2026-06-30	f	2026-06-05 02:12:06.633045-07	2026-06-05 03:51:56.808994-07	system	admin@wasac.gov.rw	f	\N	\N	ACTIVE
517b51e9-fc6b-49f3-8ee9-a6e9cd2cddf4	Water Standard 2026	WATER	TIERED	\N	1500.00	18.00	5.00	15	2026-08-01	\N	t	2026-06-05 03:59:15.712443-07	2026-06-05 03:59:15.712443-07	admin@wasac.gov.rw	admin@wasac.gov.rw	f	\N	\N	ACTIVE
19de9ec3-785f-435c-8b6b-b6ce04b6f041	Water Standard 2026	WATER	FLAT	350.0000	1500.00	18.00	5.00	15	2026-07-01	2026-07-31	f	2026-06-05 03:51:56.804276-07	2026-06-05 03:59:15.714443-07	admin@wasac.gov.rw	admin@wasac.gov.rw	f	\N	\N	ACTIVE
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (first_name, last_name, username, email, password, role, created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, status, phone, id) FROM stdin;
Mugisha	Prince	prince.mugisha	ngaboreste4@gmail.com	$2a$10$35EAxeGMaLWX41qWTM4jW.Qpu84a/qbe5XF9KIvt0nM88NdPA/kf.	CUSTOMER	2026-06-05 03:15:08.066216-07	2026-06-05 04:14:54.904814-07	system	system	f	\N	\N	ACTIVE	+250788123457	a1254adc-2d50-4150-863d-beeb68c7db12
Hello	World	helloworld	ngaboreste5@gmail.com	$2a$10$VLg2hanIpTH6dZdYZuL8jeFixCwXPLTzSqU2DGF.6s7l5nvbJAvoy	CUSTOMER	2026-06-04 22:30:00.420112-07	2026-06-04 22:34:16.897228-07	system	system	f	\N	\N	ACTIVE	+250700000002	515565ce-fa74-4054-bf76-74063927262a
NGABO	Oreste	orestengabo0	orestengabo0@gmail.com	$2a$10$N0okoPyZEKxkPrOgCyu.qedIq19.6mzt817WH7XPKpWFZq5SthBZe	CUSTOMER	2026-06-05 01:07:35.343111-07	2026-06-05 01:07:35.343111-07	system	system	f	\N	\N	PENDING	+250788123456	9d6c9bb2-2a44-4d82-b9e1-f8d918d051a3
Finance	Officer	finance	finance@wasac.gov.rw	$2a$10$9pN0DQBWCDfR69zUrOs/He5TGRa.ukHlga6Xwwbpb6xvtUVNfje3G	FINANCE	2026-06-05 02:12:06.633045-07	2026-06-05 03:49:40.865744-07	system	system	f	\N	\N	ACTIVE	+250788000101	f1111111-1111-1111-1111-111111111101
Meter	Operator	operator	operator@wasac.gov.rw	$2a$10$9pN0DQBWCDfR69zUrOs/He5TGRa.ukHlga6Xwwbpb6xvtUVNfje3G	OPERATOR	2026-06-05 02:12:06.633045-07	2026-06-05 03:49:40.865744-07	system	system	f	\N	\N	ACTIVE	+250788000102	f1111111-1111-1111-1111-111111111102
Jean Pierre	Uwimana	jp.uwimana	customer@wasac.gov.rw	$2a$10$9pN0DQBWCDfR69zUrOs/He5TGRa.ukHlga6Xwwbpb6xvtUVNfje3G	CUSTOMER	2026-06-05 02:12:06.633045-07	2026-06-05 03:49:40.865744-07	system	system	f	\N	\N	ACTIVE	+250788000103	f1111111-1111-1111-1111-111111111103
Jane	Smith	janesmith	admin@wasac.gov.rw	$2a$10$9pN0DQBWCDfR69zUrOs/He5TGRa.ukHlga6Xwwbpb6xvtUVNfje3G	ADMIN	2026-05-29 12:18:56.665465-07	2026-06-05 03:49:40.865744-07	system	system	f	\N	\N	ACTIVE	+250788123455	ef53da3b-04d3-4e23-bbf1-626bbb0f17bf
\.


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: bills pk_bills; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT pk_bills PRIMARY KEY (id);


--
-- Name: customers pk_customers; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT pk_customers PRIMARY KEY (id);


--
-- Name: email_verification_tokens pk_email_verification_tokens; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id);


--
-- Name: meter_readings pk_meter_readings; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT pk_meter_readings PRIMARY KEY (id);


--
-- Name: meters pk_meters; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT pk_meters PRIMARY KEY (id);


--
-- Name: notifications pk_notifications; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT pk_notifications PRIMARY KEY (id);


--
-- Name: password_reset_tokens pk_password_reset_tokens; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id);


--
-- Name: payments pk_payments; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT pk_payments PRIMARY KEY (id);


--
-- Name: tariff_tiers pk_tariff_tiers; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tariff_tiers
    ADD CONSTRAINT pk_tariff_tiers PRIMARY KEY (id);


--
-- Name: tariff_versions pk_tariff_versions; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tariff_versions
    ADD CONSTRAINT pk_tariff_versions PRIMARY KEY (id);


--
-- Name: users pk_users; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT pk_users PRIMARY KEY (id);


--
-- Name: customers uq_customers_email; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT uq_customers_email UNIQUE (email);


--
-- Name: customers uq_customers_national_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT uq_customers_national_id UNIQUE (national_id);


--
-- Name: email_verification_tokens uq_evt_token; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT uq_evt_token UNIQUE (token);


--
-- Name: meters uq_meters_meter_number; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT uq_meters_meter_number UNIQUE (meter_number);


--
-- Name: password_reset_tokens uq_password_reset_token; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT uq_password_reset_token UNIQUE (token);


--
-- Name: payments uq_payments_reference_number; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT uq_payments_reference_number UNIQUE (reference_number);


--
-- Name: users uq_users_email; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_email UNIQUE (email);


--
-- Name: users uq_users_phone; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_phone UNIQUE (phone);


--
-- Name: users uq_users_username; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_username UNIQUE (username);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_bills_bill_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bills_bill_status ON public.bills USING btree (bill_status);


--
-- Name: idx_bills_customer_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bills_customer_id ON public.bills USING btree (customer_id);


--
-- Name: idx_bills_due_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bills_due_date ON public.bills USING btree (due_date);


--
-- Name: idx_bills_meter_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bills_meter_id ON public.bills USING btree (meter_id);


--
-- Name: idx_bills_period; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bills_period ON public.bills USING btree (billing_year, billing_month);


--
-- Name: idx_customers_deleted; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_customers_deleted ON public.customers USING btree (deleted);


--
-- Name: idx_customers_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_customers_email ON public.customers USING btree (email);


--
-- Name: idx_customers_national_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_customers_national_id ON public.customers USING btree (national_id);


--
-- Name: idx_customers_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_customers_status ON public.customers USING btree (status);


--
-- Name: idx_evt_expires; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_evt_expires ON public.email_verification_tokens USING btree (expires_at);


--
-- Name: idx_evt_token; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_evt_token ON public.email_verification_tokens USING btree (token);


--
-- Name: idx_meter_readings_period; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_meter_readings_period ON public.meter_readings USING btree (billing_year, billing_month);


--
-- Name: idx_meter_readings_reading_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_meter_readings_reading_date ON public.meter_readings USING btree (reading_date);


--
-- Name: idx_meters_deleted; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_meters_deleted ON public.meters USING btree (deleted);


--
-- Name: idx_meters_meter_number; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_meters_meter_number ON public.meters USING btree (meter_number);


--
-- Name: idx_meters_meter_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_meters_meter_type ON public.meters USING btree (meter_type);


--
-- Name: idx_meters_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_meters_status ON public.meters USING btree (status);


--
-- Name: idx_notifications_bill_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_bill_id ON public.notifications USING btree (bill_id);


--
-- Name: idx_notifications_customer_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_customer_id ON public.notifications USING btree (customer_id);


--
-- Name: idx_payments_bill_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payments_bill_id ON public.payments USING btree (bill_id);


--
-- Name: idx_payments_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payments_date ON public.payments USING btree (payment_date);


--
-- Name: idx_prt_expires; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prt_expires ON public.password_reset_tokens USING btree (expires_at);


--
-- Name: idx_prt_token; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prt_token ON public.password_reset_tokens USING btree (token);


--
-- Name: idx_tariff_tiers_version_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tariff_tiers_version_id ON public.tariff_tiers USING btree (tariff_version_id);


--
-- Name: idx_tariff_versions_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tariff_versions_active ON public.tariff_versions USING btree (active);


--
-- Name: idx_tariff_versions_effective; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tariff_versions_effective ON public.tariff_versions USING btree (effective_from, effective_to);


--
-- Name: idx_tariff_versions_meter_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tariff_versions_meter_type ON public.tariff_versions USING btree (meter_type);


--
-- Name: idx_users_deleted; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_deleted ON public.users USING btree (deleted);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_phone; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_phone ON public.users USING btree (phone);


--
-- Name: idx_users_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_status ON public.users USING btree (status);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: uq_bill_meter_period_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_bill_meter_period_active ON public.bills USING btree (meter_id, billing_month, billing_year) WHERE ((deleted = false) AND ((bill_status)::text <> 'CANCELLED'::text));


--
-- Name: bills trg_bill_generated_notify; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_bill_generated_notify AFTER INSERT ON public.bills FOR EACH ROW EXECUTE FUNCTION public.fn_notify_bill_generated();


--
-- Name: payments trg_payment_insert; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_payment_insert AFTER INSERT ON public.payments FOR EACH ROW EXECUTE FUNCTION public.fn_on_payment_insert();


--
-- Name: bills fk_bills_approved_by; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fk_bills_approved_by FOREIGN KEY (approved_by) REFERENCES public.users(id);


--
-- Name: bills fk_bills_customer; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fk_bills_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: bills fk_bills_meter; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fk_bills_meter FOREIGN KEY (meter_id) REFERENCES public.meters(id);


--
-- Name: bills fk_bills_tariff; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fk_bills_tariff FOREIGN KEY (tariff_version_id) REFERENCES public.tariff_versions(id);


--
-- Name: customers fk_customers_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: email_verification_tokens fk_evt_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: meter_readings fk_meter_readings_captured_by; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT fk_meter_readings_captured_by FOREIGN KEY (captured_by) REFERENCES public.users(id);


--
-- Name: meter_readings fk_meter_readings_meter; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT fk_meter_readings_meter FOREIGN KEY (meter_id) REFERENCES public.meters(id);


--
-- Name: meters fk_meters_customer; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT fk_meters_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: notifications fk_notifications_bill; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk_notifications_bill FOREIGN KEY (bill_id) REFERENCES public.bills(id);


--
-- Name: notifications fk_notifications_customer; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk_notifications_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: payments fk_payments_bill; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_bill FOREIGN KEY (bill_id) REFERENCES public.bills(id);


--
-- Name: payments fk_payments_recorded_by; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_recorded_by FOREIGN KEY (recorded_by) REFERENCES public.users(id);


--
-- Name: password_reset_tokens fk_prt_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: tariff_tiers fk_tariff_tiers_version; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tariff_tiers
    ADD CONSTRAINT fk_tariff_tiers_version FOREIGN KEY (tariff_version_id) REFERENCES public.tariff_versions(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

