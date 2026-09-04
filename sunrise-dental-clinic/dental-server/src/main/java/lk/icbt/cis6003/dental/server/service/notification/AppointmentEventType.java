package lk.icbt.cis6003.dental.server.service.notification;

/**
 * The things that can happen to an appointment which anyone might want to know
 * about.
 *
 * <p>Each type carries whether it is worth telling the <em>patient</em>.
 * Everything is audited; only patient-relevant events reach their inbox or
 * phone. A clinic that texts a patient every time a receptionist opens their
 * record teaches them to ignore its messages.</p>
 */
public enum AppointmentEventType {

    BOOKED("Appointment Confirmation", true),
    RESCHEDULED("Appointment Rescheduled", true),
    CANCELLED("Appointment Cancelled", true),
    CONFIRMED("Appointment Confirmed", false),
    COMPLETED("Treatment Completed", false),
    NO_SHOW("Missed Appointment", true),
    REMINDER("Appointment Reminder", true),
    INVOICE_ISSUED("Your Bill from Sunrise Dental Clinic", true),
    PAYMENT_RECEIVED("Payment Receipt", true);

    private final String subject;
    private final boolean notifyPatient;

    AppointmentEventType(String subject, boolean notifyPatient) {
        this.subject = subject;
        this.notifyPatient = notifyPatient;
    }

    public String getSubject() {
        return subject;
    }

    public boolean isNotifyPatient() {
        return notifyPatient;
    }
}
