-- =========================================================================
--  V4 : database triggers (H2 profile)
-- =========================================================================
--  The application already audits through its observer pipeline. These
--  triggers exist to catch everything the application does NOT see: a
--  correction applied over a SQL console, a bulk update from a future
--  integration, a maintenance script. Because the rule sits with the data
--  rather than with one client of it, nothing can touch these two tables
--  without leaving a trace.
--
--  Rows written here carry source = 'DB_TRIGGER', so the audit screen can
--  always tell an application action from a direct database change.
--
--  In H2 a trigger body is a Java class implementing org.h2.api.Trigger.
--  The equivalent MySQL profile uses native SQL trigger bodies.
-- =========================================================================


-- Every appointment booked or amended is recorded, including the specific
-- fields that changed (status, date, time, dentist, treatment).
CREATE TRIGGER trg_appointment_audit
    AFTER INSERT, UPDATE ON appointment
    FOR EACH ROW
    CALL 'lk.icbt.cis6003.dental.server.db.trigger.AppointmentAuditTrigger';


-- Every movement of money on a bill is recorded. If a row ever appears with
-- amount_paid greater than total_amount the trigger logs a
-- PAYMENT_INTEGRITY_WARNING; the chk_invoice_amount_paid CHECK constraint
-- declared in V1 is what actually refuses such a row.
CREATE TRIGGER trg_invoice_payment_audit
    AFTER UPDATE ON invoice
    FOR EACH ROW
    CALL 'lk.icbt.cis6003.dental.server.db.trigger.InvoicePaymentAuditTrigger';
