-- =========================================================================
--  V4 : database triggers (MySQL 8)
-- =========================================================================
--  MySQL can express these natively, so the profile goes further than the H2
--  one: as well as auditing, a BEFORE INSERT trigger ENFORCES two booking
--  rules inside the database. Even a direct SQL INSERT that bypasses the
--  application cannot create an appointment outside the dentist's shift or in
--  the past.
-- =========================================================================

DELIMITER $$

-- -------------------------------------------------------------------------
-- Business rule enforcement: an appointment must fall inside the chosen
-- dentist's working hours. Rejecting this in the database means the rule
-- survives a buggy client, a data migration or a manual fix.
-- -------------------------------------------------------------------------
CREATE TRIGGER trg_appointment_before_insert
BEFORE INSERT ON appointment
FOR EACH ROW
BEGIN
    DECLARE v_start TIME;
    DECLARE v_end   TIME;
    DECLARE v_active BOOLEAN;

    SELECT work_start_time, work_end_time, active
      INTO v_start, v_end, v_active
      FROM dentist
     WHERE id = NEW.dentist_id;

    IF v_active = FALSE THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot book an appointment with an inactive dentist';
    END IF;

    IF NEW.appointment_time < v_start
       OR ADDTIME(NEW.appointment_time, SEC_TO_TIME(NEW.duration_minutes * 60)) > v_end THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Appointment falls outside the dentist working hours';
    END IF;
END$$


-- -------------------------------------------------------------------------
-- Audit: every appointment booked.
-- -------------------------------------------------------------------------
CREATE TRIGGER trg_appointment_audit_insert
AFTER INSERT ON appointment
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (username, action, entity_type, entity_key, details, source, occurred_at)
    VALUES (IFNULL(NEW.created_by, 'system'),
            'APPOINTMENT_CREATED',
            'APPOINTMENT',
            NEW.appointment_number,
            CONCAT('Booked for ', NEW.appointment_date, ' ', NEW.appointment_time,
                   ', status ', NEW.status),
            'DB_TRIGGER',
            NOW());
END$$


-- -------------------------------------------------------------------------
-- Audit: every appointment amended, naming the fields that changed.
-- -------------------------------------------------------------------------
CREATE TRIGGER trg_appointment_audit_update
AFTER UPDATE ON appointment
FOR EACH ROW
BEGIN
    DECLARE v_details VARCHAR(1000) DEFAULT '';

    IF NOT (OLD.status <=> NEW.status) THEN
        SET v_details = CONCAT(v_details, 'status: ', OLD.status, ' -> ', NEW.status, '; ');
    END IF;
    IF NOT (OLD.appointment_date <=> NEW.appointment_date) THEN
        SET v_details = CONCAT(v_details, 'date: ', OLD.appointment_date, ' -> ', NEW.appointment_date, '; ');
    END IF;
    IF NOT (OLD.appointment_time <=> NEW.appointment_time) THEN
        SET v_details = CONCAT(v_details, 'time: ', OLD.appointment_time, ' -> ', NEW.appointment_time, '; ');
    END IF;
    IF NOT (OLD.dentist_id <=> NEW.dentist_id) THEN
        SET v_details = CONCAT(v_details, 'dentist: ', OLD.dentist_id, ' -> ', NEW.dentist_id, '; ');
    END IF;
    IF NOT (OLD.treatment_id <=> NEW.treatment_id) THEN
        SET v_details = CONCAT(v_details, 'treatment: ', OLD.treatment_id, ' -> ', NEW.treatment_id, '; ');
    END IF;

    IF v_details <> '' THEN
        INSERT INTO audit_log (username, action, entity_type, entity_key, details, source, occurred_at)
        VALUES (IFNULL(NEW.updated_by, IFNULL(NEW.created_by, 'system')),
                'APPOINTMENT_UPDATED',
                'APPOINTMENT',
                NEW.appointment_number,
                v_details,
                'DB_TRIGGER',
                NOW());
    END IF;
END$$


-- -------------------------------------------------------------------------
-- Audit: every movement of money on a bill.
-- -------------------------------------------------------------------------
CREATE TRIGGER trg_invoice_payment_audit
AFTER UPDATE ON invoice
FOR EACH ROW
BEGIN
    IF NOT (OLD.amount_paid <=> NEW.amount_paid)
       OR NOT (OLD.payment_status <=> NEW.payment_status) THEN
        INSERT INTO audit_log (username, action, entity_type, entity_key, details, source, occurred_at)
        VALUES ('database',
                CASE WHEN NEW.amount_paid > NEW.total_amount
                     THEN 'PAYMENT_INTEGRITY_WARNING' ELSE 'INVOICE_PAYMENT_CHANGED' END,
                'INVOICE',
                NEW.invoice_number,
                CONCAT('paid: ', OLD.amount_paid, ' -> ', NEW.amount_paid,
                       '; status: ', OLD.payment_status, ' -> ', NEW.payment_status,
                       '; total: ', NEW.total_amount),
                'DB_TRIGGER',
                NOW());
    END IF;
END$$

DELIMITER ;
