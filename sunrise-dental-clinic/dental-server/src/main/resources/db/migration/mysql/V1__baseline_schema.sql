-- =========================================================================
--  V1 : baseline schema (MySQL 8 profile)
--  Activated with:  --spring.profiles.active=mysql
-- =========================================================================
--  Structurally identical to the H2 baseline. The differences are only those
--  MySQL requires: AUTO_INCREMENT instead of GENERATED AS IDENTITY, DATETIME
--  instead of TIMESTAMP (MySQL's TIMESTAMP is limited to 2038 and carries
--  implicit auto-update behaviour that would silently rewrite created_at), and
--  an explicit InnoDB / utf8mb4 declaration so that foreign keys, CHECK
--  constraints and Sinhala or Tamil patient names all work.
-- =========================================================================


CREATE TABLE app_user (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    username              VARCHAR(30)  NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    full_name             VARCHAR(100) NOT NULL,
    email                 VARCHAR(120),
    role                  VARCHAR(20)  NOT NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at         DATETIME,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          DATETIME,
    linked_dentist_code   VARCHAR(20),
    created_at            DATETIME     NOT NULL,
    updated_at            DATETIME,
    version               BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT chk_app_user_role CHECK (role IN ('ADMIN', 'RECEPTIONIST', 'DENTIST'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


CREATE TABLE patient (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    patient_code   VARCHAR(20)  NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    address        VARCHAR(200) NOT NULL,
    contact_number VARCHAR(20)  NOT NULL,
    email          VARCHAR(120),
    nic            VARCHAR(20),
    gender         VARCHAR(20),
    date_of_birth  DATE,
    medical_notes  VARCHAR(500),
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME,
    version        BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_patient_code UNIQUE (patient_code),
    INDEX ix_patient_name    (full_name),
    INDEX ix_patient_contact (contact_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


CREATE TABLE dentist (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    dentist_code         VARCHAR(20)   NOT NULL,
    full_name            VARCHAR(100)  NOT NULL,
    specialization       VARCHAR(80)   NOT NULL,
    contact_number       VARCHAR(20)   NOT NULL,
    email                VARCHAR(120),
    consultation_fee     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    slmc_registration_no VARCHAR(40),
    work_start_time      TIME          NOT NULL DEFAULT '08:00:00',
    work_end_time        TIME          NOT NULL DEFAULT '20:00:00',
    active               BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           DATETIME      NOT NULL,
    updated_at           DATETIME,
    version              BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_dentist_code UNIQUE (dentist_code),
    CONSTRAINT chk_dentist_fee   CHECK (consultation_fee >= 0),
    CONSTRAINT chk_dentist_shift CHECK (work_end_time > work_start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


CREATE TABLE treatment (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    code             VARCHAR(20)   NOT NULL,
    name             VARCHAR(100)  NOT NULL,
    description      VARCHAR(300),
    category         VARCHAR(50)   NOT NULL,
    base_price       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    duration_minutes INT           NOT NULL DEFAULT 30,
    pricing_strategy VARCHAR(30)   NOT NULL DEFAULT 'STANDARD',
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       DATETIME      NOT NULL,
    updated_at       DATETIME,
    version          BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_treatment_code UNIQUE (code),
    CONSTRAINT chk_treatment_price    CHECK (base_price >= 0),
    CONSTRAINT chk_treatment_duration CHECK (duration_minutes BETWEEN 15 AND 480)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- The anti-double-booking rule. See the H2 baseline for the full rationale:
-- slot_lock is NULL once an appointment stops occupying the chair, and MySQL
-- unique indexes permit unlimited NULLs, so cancelled bookings never collide.
CREATE TABLE appointment (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    appointment_number  VARCHAR(20) NOT NULL,
    patient_id          BIGINT      NOT NULL,
    dentist_id          BIGINT      NOT NULL,
    treatment_id        BIGINT      NOT NULL,
    appointment_date    DATE        NOT NULL,
    appointment_time    TIME        NOT NULL,
    duration_minutes    INT         NOT NULL DEFAULT 30,
    status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    slot_lock           VARCHAR(30),
    notes               VARCHAR(500),
    cancellation_reason VARCHAR(300),
    created_by          VARCHAR(30) NOT NULL,
    updated_by          VARCHAR(30),
    created_at          DATETIME    NOT NULL,
    updated_at          DATETIME,
    version             BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_appointment_number UNIQUE (appointment_number),
    CONSTRAINT uk_appointment_slot   UNIQUE (dentist_id, slot_lock),
    CONSTRAINT fk_appointment_patient   FOREIGN KEY (patient_id)   REFERENCES patient (id),
    CONSTRAINT fk_appointment_dentist   FOREIGN KEY (dentist_id)   REFERENCES dentist (id),
    CONSTRAINT fk_appointment_treatment FOREIGN KEY (treatment_id) REFERENCES treatment (id),
    CONSTRAINT chk_appointment_status CHECK
        (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_appointment_duration CHECK (duration_minutes > 0),
    INDEX ix_appointment_date         (appointment_date),
    INDEX ix_appointment_patient      (patient_id),
    INDEX ix_appointment_status       (status),
    INDEX ix_appointment_dentist_date (dentist_id, appointment_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


CREATE TABLE invoice (
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_number           VARCHAR(20)   NOT NULL,
    appointment_id           BIGINT        NOT NULL,
    patient_name             VARCHAR(100)  NOT NULL,
    patient_address          VARCHAR(200)  NOT NULL,
    patient_contact          VARCHAR(20)   NOT NULL,
    dentist_name             VARCHAR(100)  NOT NULL,
    treatment_name           VARCHAR(100)  NOT NULL,
    consultation_fee         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    treatment_cost           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    surcharge_amount         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    sub_total                DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_percentage      DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    discount_amount          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_reason          VARCHAR(200),
    taxable_amount           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_rate                 DECIMAL(5,4)  NOT NULL DEFAULT 0.1800,
    tax_amount               DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount             DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    amount_paid              DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    pricing_strategy_applied VARCHAR(30),
    payment_status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    payment_method           VARCHAR(20),
    payment_reference        VARCHAR(100),
    issued_date              DATE          NOT NULL,
    issued_by                VARCHAR(30)   NOT NULL,
    paid_at                  DATETIME,
    remarks                  VARCHAR(300),
    created_at               DATETIME      NOT NULL,
    updated_at               DATETIME,
    version                  BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoice_number      UNIQUE (invoice_number),
    CONSTRAINT uk_invoice_appointment UNIQUE (appointment_id),
    CONSTRAINT fk_invoice_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id),
    CONSTRAINT chk_invoice_payment_status CHECK
        (payment_status IN ('PENDING', 'PARTIALLY_PAID', 'PAID', 'CANCELLED')),
    CONSTRAINT chk_invoice_discount_pct CHECK (discount_percentage BETWEEN 0 AND 50),
    CONSTRAINT chk_invoice_amounts      CHECK (total_amount >= 0 AND amount_paid >= 0),
    CONSTRAINT chk_invoice_amount_paid  CHECK (amount_paid <= total_amount),
    INDEX ix_invoice_issued_date    (issued_date),
    INDEX ix_invoice_payment_status (payment_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


CREATE TABLE invoice_line (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_id  BIGINT        NOT NULL,
    line_number INT           NOT NULL,
    description VARCHAR(200)  NOT NULL,
    quantity    INT           NOT NULL DEFAULT 1,
    unit_price  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    line_total  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    line_type   VARCHAR(20)   NOT NULL DEFAULT 'CHARGE',
    created_at  DATETIME      NOT NULL,
    updated_at  DATETIME,
    version     BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_line_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoice (id) ON DELETE CASCADE,
    CONSTRAINT chk_invoice_line_type CHECK (line_type IN ('CHARGE', 'SURCHARGE', 'DISCOUNT', 'TAX')),
    CONSTRAINT chk_invoice_line_qty  CHECK (quantity > 0),
    INDEX ix_invoice_line_invoice (invoice_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


CREATE TABLE audit_log (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)   NOT NULL,
    action      VARCHAR(60)   NOT NULL,
    entity_type VARCHAR(50),
    entity_key  VARCHAR(50),
    details     VARCHAR(1000),
    ip_address  VARCHAR(60),
    source      VARCHAR(20)   NOT NULL DEFAULT 'APPLICATION',
    occurred_at DATETIME      NOT NULL,
    PRIMARY KEY (id),
    INDEX ix_audit_log_occurred (occurred_at),
    INDEX ix_audit_log_username (username),
    INDEX ix_audit_log_entity   (entity_type, entity_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


CREATE TABLE notification_log (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    channel        VARCHAR(20)   NOT NULL,
    recipient      VARCHAR(150)  NOT NULL,
    subject        VARCHAR(200),
    body           VARCHAR(2000),
    status         VARCHAR(20)   NOT NULL DEFAULT 'SENT',
    failure_reason VARCHAR(500),
    reference_key  VARCHAR(30),
    created_at     DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_notification_channel CHECK (channel IN ('EMAIL', 'SMS', 'SYSTEM')),
    CONSTRAINT chk_notification_status  CHECK (status IN ('SENT', 'FAILED', 'SUPPRESSED')),
    INDEX ix_notification_log_sent      (created_at),
    INDEX ix_notification_log_reference (reference_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


CREATE TABLE number_sequence (
    sequence_key VARCHAR(40) NOT NULL,
    next_value   BIGINT      NOT NULL DEFAULT 1,
    PRIMARY KEY (sequence_key),
    CONSTRAINT chk_number_sequence_value CHECK (next_value > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
