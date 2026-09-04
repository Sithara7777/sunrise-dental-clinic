-- =========================================================================
--  V3 : stored functions, stored procedures and reporting views (MySQL 8)
-- =========================================================================
--  The MySQL profile implements the same two functions as the H2 profile,
--  natively, plus two stored PROCEDURES that have no H2 equivalent. The
--  function names and semantics are identical on both engines, so
--  JdbcReportingDao runs unchanged against either.
-- =========================================================================

DELIMITER $$

-- -------------------------------------------------------------------------
-- FN_INVOICE_TOTAL - the clinic's billing formula, owned by the database.
--   subTotal = consultation + treatment + surcharge
--   discount = subTotal x pct / 100          (applied BEFORE VAT, because
--   taxable  = subTotal - discount            Sri Lankan VAT is charged on the
--   tax      = taxable x rate                 consideration actually received)
--   total    = taxable + tax
-- -------------------------------------------------------------------------
CREATE FUNCTION FN_INVOICE_TOTAL(
    p_consultation_fee DECIMAL(12,2),
    p_treatment_cost   DECIMAL(12,2),
    p_surcharge        DECIMAL(12,2),
    p_discount_pct     DECIMAL(5,2),
    p_tax_rate         DECIMAL(5,4)
) RETURNS DECIMAL(12,2)
DETERMINISTIC
NO SQL
BEGIN
    DECLARE v_sub_total DECIMAL(12,2);
    DECLARE v_discount  DECIMAL(12,2);
    DECLARE v_taxable   DECIMAL(12,2);
    DECLARE v_tax       DECIMAL(12,2);

    SET v_sub_total = IFNULL(p_consultation_fee, 0) + IFNULL(p_treatment_cost, 0) + IFNULL(p_surcharge, 0);
    SET v_discount  = ROUND(v_sub_total * IFNULL(p_discount_pct, 0) / 100, 2);
    SET v_taxable   = v_sub_total - v_discount;
    SET v_tax       = ROUND(v_taxable * IFNULL(p_tax_rate, 0), 2);

    RETURN ROUND(v_taxable + v_tax, 2);
END$$


-- -------------------------------------------------------------------------
-- FN_AGEING_BUCKET - how a debt is aged for the debtor report.
-- -------------------------------------------------------------------------
CREATE FUNCTION FN_AGEING_BUCKET(p_days BIGINT)
RETURNS VARCHAR(10)
DETERMINISTIC
NO SQL
BEGIN
    IF p_days <= 30 THEN
        RETURN '0-30';
    ELSEIF p_days <= 60 THEN
        RETURN '31-60';
    ELSEIF p_days <= 90 THEN
        RETURN '61-90';
    ELSE
        RETURN '90+';
    END IF;
END$$


-- -------------------------------------------------------------------------
-- SP_SETTLE_INVOICE - takes a payment atomically.
--
-- The whole read-check-write cycle happens inside one statement on the
-- server, so two cashiers settling the same bill at the same moment cannot
-- both see a balance of Rs. 5,000 and both accept Rs. 5,000. The row is locked
-- FOR UPDATE and any over-payment is rejected with SQLSTATE '45000'.
-- -------------------------------------------------------------------------
CREATE PROCEDURE SP_SETTLE_INVOICE(
    IN  p_invoice_number VARCHAR(20),
    IN  p_amount         DECIMAL(12,2),
    IN  p_method         VARCHAR(20),
    IN  p_reference      VARCHAR(100),
    OUT p_balance_due    DECIMAL(12,2)
)
MODIFIES SQL DATA
BEGIN
    DECLARE v_id      BIGINT;
    DECLARE v_total   DECIMAL(12,2);
    DECLARE v_paid    DECIMAL(12,2);
    DECLARE v_status  VARCHAR(20);
    DECLARE v_balance DECIMAL(12,2);

    SELECT id, total_amount, amount_paid, payment_status
      INTO v_id, v_total, v_paid, v_status
      FROM invoice
     WHERE invoice_number = p_invoice_number
     FOR UPDATE;

    IF v_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice not found';
    END IF;

    IF v_status = 'CANCELLED' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice has been cancelled';
    END IF;

    SET v_balance = v_total - v_paid;

    IF p_amount <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Payment amount must be greater than zero';
    END IF;

    IF p_amount > v_balance THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Payment exceeds the outstanding balance';
    END IF;

    UPDATE invoice
       SET amount_paid       = amount_paid + p_amount,
           payment_method    = p_method,
           payment_reference = p_reference,
           payment_status    = CASE WHEN (amount_paid + p_amount) >= total_amount
                                    THEN 'PAID' ELSE 'PARTIALLY_PAID' END,
           paid_at           = CASE WHEN (amount_paid + p_amount) >= total_amount
                                    THEN NOW() ELSE paid_at END,
           updated_at        = NOW()
     WHERE id = v_id;

    SELECT total_amount - amount_paid INTO p_balance_due FROM invoice WHERE id = v_id;
END$$


-- -------------------------------------------------------------------------
-- SP_DAILY_CLOSING_SUMMARY - the end-of-day figures the practice manager
-- reads before locking up. One round trip instead of six.
-- -------------------------------------------------------------------------
CREATE PROCEDURE SP_DAILY_CLOSING_SUMMARY(IN p_date DATE)
READS SQL DATA
BEGIN
    SELECT
        p_date AS closing_date,
        (SELECT COUNT(*) FROM appointment WHERE appointment_date = p_date)                        AS booked,
        (SELECT COUNT(*) FROM appointment WHERE appointment_date = p_date AND status = 'COMPLETED') AS completed,
        (SELECT COUNT(*) FROM appointment WHERE appointment_date = p_date AND status = 'CANCELLED') AS cancelled,
        (SELECT COUNT(*) FROM appointment WHERE appointment_date = p_date AND status = 'NO_SHOW')   AS no_shows,
        (SELECT IFNULL(SUM(total_amount), 0) FROM invoice
          WHERE issued_date = p_date AND payment_status <> 'CANCELLED')                           AS invoiced,
        (SELECT IFNULL(SUM(amount_paid), 0) FROM invoice
          WHERE issued_date = p_date AND payment_status <> 'CANCELLED')                           AS collected,
        (SELECT IFNULL(SUM(total_amount - amount_paid), 0) FROM invoice
          WHERE payment_status IN ('PENDING', 'PARTIALLY_PAID'))                                  AS total_outstanding;
END$$

DELIMITER ;


-- =========================================================================
--  Reporting views - identical in meaning to the H2 profile.
-- =========================================================================

-- REPORT 1 - Daily Appointment Schedule
CREATE VIEW v_daily_schedule AS
SELECT a.appointment_date   AS appointment_date,
       a.appointment_time   AS appointment_time,
       a.appointment_number AS appointment_number,
       p.full_name          AS patient_name,
       p.contact_number     AS contact_number,
       d.dentist_code       AS dentist_code,
       d.full_name          AS dentist_name,
       t.name               AS treatment_name,
       a.status             AS status
FROM appointment a
JOIN patient   p ON p.id = a.patient_id
JOIN dentist   d ON d.id = a.dentist_id
JOIN treatment t ON t.id = a.treatment_id;


-- REPORT 2 - Daily Revenue
CREATE VIEW v_revenue_daily AS
SELECT i.issued_date                                    AS issued_date,
       COUNT(i.id)                                      AS invoice_count,
       IFNULL(SUM(i.sub_total),       0)                AS gross_amount,
       IFNULL(SUM(i.discount_amount), 0)                AS discount_amount,
       IFNULL(SUM(i.tax_amount),      0)                AS tax_amount,
       IFNULL(SUM(i.total_amount),    0)                AS net_amount,
       IFNULL(SUM(i.amount_paid),     0)                AS collected_amount,
       IFNULL(SUM(i.total_amount - i.amount_paid), 0)   AS outstanding_amount
FROM invoice i
WHERE i.payment_status <> 'CANCELLED'
GROUP BY i.issued_date;


-- REPORT 3 - Dentist Workload and Utilisation
CREATE VIEW v_dentist_workload AS
SELECT d.dentist_code     AS dentist_code,
       d.full_name        AS dentist_name,
       d.specialization   AS specialization,
       a.appointment_date AS work_date,
       COUNT(a.id)        AS total_appointments,
       IFNULL(SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed_appointments,
       IFNULL(SUM(CASE WHEN a.status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled_appointments,
       IFNULL(SUM(CASE WHEN a.status = 'NO_SHOW'   THEN 1 ELSE 0 END), 0) AS no_show_appointments,
       IFNULL(SUM(CASE WHEN a.status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED')
                       THEN a.duration_minutes ELSE 0 END), 0)            AS booked_minutes,
       IFNULL(SUM(i.total_amount), 0)                                     AS revenue_generated
FROM dentist d
LEFT JOIN appointment a ON a.dentist_id     = d.id
LEFT JOIN invoice     i ON i.appointment_id = a.id AND i.payment_status <> 'CANCELLED'
GROUP BY d.dentist_code, d.full_name, d.specialization, a.appointment_date;


-- REPORT 4 - Treatment Popularity and Yield
CREATE VIEW v_treatment_popularity AS
SELECT t.code             AS treatment_code,
       t.name             AS treatment_name,
       t.category         AS category,
       a.appointment_date AS performed_date,
       COUNT(a.id)        AS times_performed,
       IFNULL(SUM(i.total_amount), 0) AS total_revenue
FROM treatment t
LEFT JOIN appointment a ON a.treatment_id   = t.id AND a.status = 'COMPLETED'
LEFT JOIN invoice     i ON i.appointment_id = a.id AND i.payment_status <> 'CANCELLED'
GROUP BY t.code, t.name, t.category, a.appointment_date;


-- REPORT 5 - Outstanding Payments (debtor ageing)
-- NOTE: MySQL's DATEDIFF takes (later, earlier); H2's takes (unit, earlier, later).
CREATE VIEW v_outstanding_invoice AS
SELECT i.invoice_number                                                  AS invoice_number,
       a.appointment_number                                              AS appointment_number,
       i.patient_name                                                    AS patient_name,
       i.patient_contact                                                 AS patient_contact,
       i.issued_date                                                     AS issued_date,
       CAST(DATEDIFF(CURRENT_DATE, i.issued_date) AS SIGNED)             AS days_outstanding,
       FN_AGEING_BUCKET(CAST(DATEDIFF(CURRENT_DATE, i.issued_date) AS SIGNED)) AS ageing_bucket,
       i.total_amount                                                    AS total_amount,
       i.amount_paid                                                     AS amount_paid,
       (i.total_amount - i.amount_paid)                                  AS balance_due,
       i.payment_status                                                  AS payment_status
FROM invoice i
JOIN appointment a ON a.id = i.appointment_id
WHERE i.payment_status IN ('PENDING', 'PARTIALLY_PAID');
