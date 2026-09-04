package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.common.dto.BillingRequest;
import lk.icbt.cis6003.dental.common.dto.DentistDto;
import lk.icbt.cis6003.dental.common.dto.HelpTopicDto;
import lk.icbt.cis6003.dental.common.dto.InvoiceDto;
import lk.icbt.cis6003.dental.common.dto.SlotDto;
import lk.icbt.cis6003.dental.common.dto.StatusUpdateRequest;
import lk.icbt.cis6003.dental.common.dto.TreatmentDto;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.server.util.ReceiptPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * <b>Facade pattern</b> - one coarse-grained entry point for the operations a
 * remote client performs as a single logical step.
 *
 * <p><b>The problem it solves.</b> The desktop client is on the far side of a
 * network. Opening the booking window with fine-grained services means three
 * separate HTTP calls (dentists, treatments, then slots), each with its own
 * latency and its own failure mode, and the client has to sequence them
 * correctly. Completing and billing a visit means two calls that must both
 * succeed or neither should - a transaction the client cannot enforce.</p>
 *
 * <p><b>How this is better.</b> Each method here is one network round trip and
 * one database transaction. {@link #completeAndBill} either marks the visit
 * completed <em>and</em> issues the bill, or does neither - so a network drop
 * halfway through can no longer leave a completed visit with no bill, which is
 * precisely the kind of gap the clinic's paper system produced.</p>
 *
 * <p><b>What this facade is not.</b> It is not a pass-through wrapper around
 * every service method. The individual services remain the API for anything
 * that genuinely is one operation; only the compositions live here. A facade
 * that mirrored every method would add indirection and buy nothing.</p>
 */
@Service
public class ClinicFacade {

    private final AppointmentService appointmentService;
    private final BillingService billingService;
    private final DentistService dentistService;
    private final TreatmentService treatmentService;
    private final HelpService helpService;
    private final ReceiptPrinter receiptPrinter;

    public ClinicFacade(AppointmentService appointmentService,
                        BillingService billingService,
                        DentistService dentistService,
                        TreatmentService treatmentService,
                        HelpService helpService,
                        ReceiptPrinter receiptPrinter) {
        this.appointmentService = appointmentService;
        this.billingService = billingService;
        this.dentistService = dentistService;
        this.treatmentService = treatmentService;
        this.helpService = helpService;
        this.receiptPrinter = receiptPrinter;
    }

    /**
     * Everything the "New appointment" screen needs, in one call.
     *
     * <p>Three round trips become one. On a clinic's ADSL connection that is
     * the difference between a form that appears instantly and one that fills
     * in visibly in stages.</p>
     */
    @Transactional(readOnly = true)
    public BookingFormData bookingFormData(String dentistCode, LocalDate date) {
        List<DentistDto> dentists = dentistService.listActive();
        List<TreatmentDto> treatments = treatmentService.listActive();

        List<SlotDto> slots = List.of();
        if (dentistCode != null && !dentistCode.isBlank() && date != null) {
            slots = appointmentService.availableSlots(dentistCode, date);
        }
        return new BookingFormData(dentists, treatments, slots);
    }

    /**
     * Marks a visit completed and issues its bill as one atomic step.
     *
     * <p>This is what the front desk actually does when a patient walks out of
     * the surgery: it is one action to them, so it is one transaction here.
     * If billing fails - an unknown treatment, a rejected discount - the
     * completion is rolled back too, and the appointment is left in a state
     * the receptionist can retry from.</p>
     */
    @Transactional
    public InvoiceDto completeAndBill(String appointmentNumber,
                                      BigDecimal discountPercentage,
                                      String discountReason) {

        var appointment = appointmentService.requireByNumber(appointmentNumber);
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            StatusUpdateRequest completion = new StatusUpdateRequest(AppointmentStatus.COMPLETED);
            appointmentService.updateStatus(appointmentNumber, completion);
        }

        BillingRequest billingRequest = new BillingRequest(appointmentNumber);
        billingRequest.setDiscountPercentage(
                discountPercentage == null ? BigDecimal.ZERO : discountPercentage);
        billingRequest.setDiscountReason(discountReason);

        return billingService.generateBill(billingRequest);
    }

    /**
     * The printable receipt for a bill, as plain text.
     *
     * <p>Returned pre-rendered so the desktop client does not have to reproduce
     * the clinic's receipt layout - and cannot drift out of step with the web
     * application's version of it.</p>
     */
    @Transactional(readOnly = true)
    public String receiptText(String invoiceNumber) {
        InvoiceDto invoice = billingService.findByInvoiceNumber(invoiceNumber);
        return receiptPrinter.render(invoice);
    }

    /** The full help contents, shared by both user interfaces. */
    public List<HelpTopicDto> help() {
        return helpService.listTopics();
    }

    /**
     * The three lists the booking screen needs.
     *
     * @param dentists   practising dentists
     * @param treatments bookable treatments
     * @param slots      the chosen dentist's diary for the chosen day, or empty
     */
    public record BookingFormData(List<DentistDto> dentists,
                                  List<TreatmentDto> treatments,
                                  List<SlotDto> slots) {
    }
}
