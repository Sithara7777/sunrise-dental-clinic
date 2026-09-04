package lk.icbt.cis6003.dental.server.service.notification;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.server.util.MoneyUtils;
import org.springframework.stereotype.Component;

/**
 * Turns an {@link AppointmentEvent} into the words a patient reads.
 *
 * <p>Kept separate from the observers so that message wording is one class the
 * clinic manager can review, rather than string concatenation scattered through
 * the delivery code. The e-mail and SMS bodies for the same event are written
 * side by side here, which is how they stay consistent.</p>
 *
 * <p>SMS bodies are written deliberately short: the clinic is billed per
 * 160-character segment, so a two-line message costs twice a one-line
 * message.</p>
 */
@Component
public class NotificationComposer {

    /* ------------------------------------------------------------------ */
    /* E-mail                                                              */
    /* ------------------------------------------------------------------ */

    public String emailSubject(AppointmentEvent event) {
        return event.getType().getSubject() + " - " + event.getAppointmentNumber();
    }

    public String emailBody(AppointmentEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(safe(event.getPatientName())).append(",\n\n");
        sb.append(leadParagraph(event)).append("\n\n");

        sb.append("APPOINTMENT DETAILS\n");
        sb.append("-------------------------------------------------\n");
        sb.append(pair("Appointment No", event.getAppointmentNumber()));
        sb.append(pair("Date", format(event)));
        sb.append(pair("Dentist", event.getDentistName() == null ? null : "Dr " + event.getDentistName()));
        sb.append(pair("Treatment", event.getTreatmentName()));
        sb.append(pair("Status", event.getStatus()));
        if (event.getAmount() != null) {
            sb.append(pair("Amount", MoneyUtils.formatWithCurrency(event.getAmount())));
        }
        if (event.getReference() != null && !event.getReference().isBlank()) {
            sb.append(pair("Reference", event.getReference()));
        }
        if (event.getDetail() != null && !event.getDetail().isBlank()) {
            sb.append(pair("Note", event.getDetail()));
        }
        sb.append("-------------------------------------------------\n\n");

        sb.append(closingAdvice(event)).append("\n\n");
        sb.append("Kind regards,\n");
        sb.append(ClinicConstants.CLINIC_NAME).append('\n');
        sb.append(ClinicConstants.CLINIC_ADDRESS_LINE_1).append(", ")
          .append(ClinicConstants.CLINIC_ADDRESS_LINE_2).append('\n');
        sb.append("Telephone: ").append(ClinicConstants.CLINIC_PHONE).append('\n');
        sb.append("\nThis is an automated message. Please do not reply to this address.\n");
        return sb.toString();
    }

    private String leadParagraph(AppointmentEvent event) {
        switch (event.getType()) {
            case BOOKED:
                return "Your appointment at " + ClinicConstants.CLINIC_NAME
                        + " has been registered. Please keep the appointment number below - "
                        + "you will be asked for it at reception.";
            case RESCHEDULED:
                return "Your appointment has been moved. The new date and time are shown below.";
            case CANCELLED:
                return "Your appointment has been cancelled. If this was not at your request, "
                        + "please contact us and we will rebook you.";
            case CONFIRMED:
                return "Thank you for confirming your appointment. We look forward to seeing you.";
            case COMPLETED:
                return "Thank you for visiting us today. Your treatment has been recorded.";
            case NO_SHOW:
                return "We are sorry we missed you at your appointment. "
                        + "Please contact reception to arrange another time.";
            case REMINDER:
                return "This is a reminder about your appointment tomorrow. "
                        + "Please arrive ten minutes early.";
            case INVOICE_ISSUED:
                return "Your bill for today's treatment is ready. The details are set out below.";
            case PAYMENT_RECEIVED:
                return "We have received your payment. Thank you.";
            default:
                return "There has been an update to your appointment.";
        }
    }

    private String closingAdvice(AppointmentEvent event) {
        switch (event.getType()) {
            case BOOKED:
            case RESCHEDULED:
            case REMINDER:
                return "If you need to change or cancel, please telephone us at least 24 hours "
                        + "beforehand so that we can offer the slot to another patient.";
            case INVOICE_ISSUED:
                return "Payment can be made at reception by cash or card.";
            default:
                return "If you have any questions, please telephone us.";
        }
    }

    /* ------------------------------------------------------------------ */
    /* SMS - kept inside one 160 character segment wherever possible        */
    /* ------------------------------------------------------------------ */

    public String smsBody(AppointmentEvent event) {
        String when = format(event);
        String dentist = event.getDentistName() == null ? "" : " with Dr " + event.getDentistName();

        switch (event.getType()) {
            case BOOKED:
                return "Sunrise Dental: appointment " + event.getAppointmentNumber()
                        + " booked for " + when + dentist + ". Please quote this number at reception.";
            case RESCHEDULED:
                return "Sunrise Dental: appointment " + event.getAppointmentNumber()
                        + " moved to " + when + dentist + ".";
            case CANCELLED:
                return "Sunrise Dental: appointment " + event.getAppointmentNumber()
                        + " on " + when + " has been cancelled. Call " + ClinicConstants.CLINIC_PHONE
                        + " to rebook.";
            case NO_SHOW:
                return "Sunrise Dental: we missed you at " + when
                        + ". Please call " + ClinicConstants.CLINIC_PHONE + " to rebook.";
            case REMINDER:
                return "Sunrise Dental reminder: " + when + dentist
                        + ". Ref " + event.getAppointmentNumber() + ". Please arrive 10 minutes early.";
            case INVOICE_ISSUED:
                return "Sunrise Dental: bill " + event.getReference() + " for "
                        + MoneyUtils.formatWithCurrency(event.getAmount()) + " is ready at reception.";
            case PAYMENT_RECEIVED:
                return "Sunrise Dental: payment of " + MoneyUtils.formatWithCurrency(event.getAmount())
                        + " received against " + event.getReference() + ". Thank you.";
            default:
                return "Sunrise Dental: appointment " + event.getAppointmentNumber() + " updated.";
        }
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    private String format(AppointmentEvent event) {
        if (event.getAppointmentDate() == null) {
            return "-";
        }
        String date = event.getAppointmentDate().format(ClinicConstants.DISPLAY_DATE_FORMAT);
        if (event.getAppointmentTime() == null) {
            return date;
        }
        return date + " at " + event.getAppointmentTime().format(ClinicConstants.TIME_FORMAT);
    }

    private String pair(String label, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return String.format("  %-16s : %s%n", label, value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Patient" : value;
    }
}
