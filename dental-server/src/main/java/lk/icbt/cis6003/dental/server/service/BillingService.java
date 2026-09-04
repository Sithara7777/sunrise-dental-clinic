package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.common.dto.BillingRequest;
import lk.icbt.cis6003.dental.common.dto.InvoiceDto;
import lk.icbt.cis6003.dental.common.dto.PageResponse;
import lk.icbt.cis6003.dental.common.dto.PaymentRequest;
import lk.icbt.cis6003.dental.common.enums.PaymentStatus;
import lk.icbt.cis6003.dental.server.domain.Appointment;
import lk.icbt.cis6003.dental.server.domain.Invoice;
import lk.icbt.cis6003.dental.server.domain.InvoiceLine;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import lk.icbt.cis6003.dental.server.exception.ResourceNotFoundException;
import lk.icbt.cis6003.dental.server.mapper.InvoiceMapper;
import lk.icbt.cis6003.dental.server.repository.InvoiceRepository;
import lk.icbt.cis6003.dental.server.repository.dao.ReportingDao;
import lk.icbt.cis6003.dental.server.security.SecurityUtils;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEvent;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEventPublisher;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEventType;
import lk.icbt.cis6003.dental.server.service.pricing.PricingContext;
import lk.icbt.cis6003.dental.server.service.pricing.PricingResult;
import lk.icbt.cis6003.dental.server.service.pricing.PricingStrategy;
import lk.icbt.cis6003.dental.server.service.pricing.PricingStrategyFactory;
import lk.icbt.cis6003.dental.server.util.MoneyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Requirement 4 - "Calculate the total treatment cost based on treatment type
 * and consultation fee. Print the patient bill/receipt."
 *
 * <p><b>Three deliberate defences against the clinic's "billing errors"
 * complaint:</b></p>
 *
 * <ol>
 *   <li><b>The client never supplies a price.</b> Consultation fee comes from
 *       the dentist record and treatment cost from the catalogue. Only the
 *       discount - a genuine human decision - is accepted as input, and it is
 *       capped at 50% by the DTO, by the pricing tier and by a database CHECK
 *       constraint.</li>
 *   <li><b>Every figure is stored, never recomputed.</b> Reprinting a receipt
 *       from six months ago reads the stored columns, so a price rise since
 *       then cannot rewrite history.</li>
 *   <li><b>The arithmetic is verified against the database.</b> After the Java
 *       pricing strategy produces a total, {@code FN_INVOICE_TOTAL} computes it
 *       independently in SQL and the two are compared. A mismatch is logged
 *       loudly rather than printed silently.</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final InvoiceRepository invoiceRepository;
    private final AppointmentService appointmentService;
    private final PricingStrategyFactory pricingStrategyFactory;
    private final ReportingDao reportingDao;
    private final SequenceGeneratorService sequenceGenerator;
    private final AppointmentEventPublisher eventPublisher;
    private final InvoiceMapper invoiceMapper;

    public BillingService(InvoiceRepository invoiceRepository,
                          AppointmentService appointmentService,
                          PricingStrategyFactory pricingStrategyFactory,
                          ReportingDao reportingDao,
                          SequenceGeneratorService sequenceGenerator,
                          AppointmentEventPublisher eventPublisher,
                          InvoiceMapper invoiceMapper) {
        this.invoiceRepository = invoiceRepository;
        this.appointmentService = appointmentService;
        this.pricingStrategyFactory = pricingStrategyFactory;
        this.reportingDao = reportingDao;
        this.sequenceGenerator = sequenceGenerator;
        this.eventPublisher = eventPublisher;
        this.invoiceMapper = invoiceMapper;
    }

    /* ================================================================== */
    /* Calculate                                                           */
    /* ================================================================== */

    /**
     * Calculates what a bill <em>would</em> come to, without issuing it.
     *
     * <p>Lets the receptionist quote a figure, and try a discount, before
     * committing anything. Nothing is persisted and no invoice number is
     * consumed.</p>
     */
    public InvoiceDto previewBill(String appointmentNumber, BigDecimal discountPercentage) {
        Appointment appointment = appointmentService.requireByNumber(appointmentNumber);
        PricingResult pricing = price(appointment, discountPercentage);

        InvoiceDto preview = new InvoiceDto();
        preview.setInvoiceNumber("(not yet issued)");
        preview.setAppointmentNumber(appointment.getAppointmentNumber());
        preview.setPatientCode(appointment.getPatient().getPatientCode());
        preview.setPatientName(appointment.getPatient().getFullName());
        preview.setPatientAddress(appointment.getPatient().getAddress());
        preview.setPatientContact(appointment.getPatient().getContactNumber());
        preview.setDentistName(appointment.getDentist().getFullName());
        preview.setTreatmentName(appointment.getTreatment().getName());
        preview.setAppointmentDate(appointment.getAppointmentDate());
        preview.setAppointmentTime(appointment.getAppointmentTime());

        copyPricingOnto(preview, pricing);
        preview.setBalanceDue(pricing.getTotalAmount());
        preview.setLines(pricing.toLineDtos());
        preview.setRemarks(String.join(" | ", pricing.getExplanations()));
        return preview;
    }

    /**
     * Issues the bill for a completed visit.
     *
     * @throws BusinessException if the visit is not completed, or has already
     *         been billed
     */
    @Transactional
    public InvoiceDto generateBill(BillingRequest request) {
        String actor = SecurityUtils.getCurrentUsernameOrSystem();
        Appointment appointment = appointmentService.requireByNumber(request.getAppointmentNumber());

        // Billing an appointment that has not happened would invoice a patient
        // for treatment they have not received.
        if (!appointment.isBillable()) {
            throw new BusinessException(ErrorCode.NOT_BILLABLE,
                    "Appointment " + appointment.getAppointmentNumber() + " is "
                            + appointment.getStatus().getDisplayName()
                            + ". Only a completed appointment can be billed - mark the visit as "
                            + "completed first.");
        }

        // uk_invoice_appointment also enforces this; the check is here so the
        // user gets a sentence rather than a constraint violation.
        if (invoiceRepository.existsByAppointmentAppointmentNumber(appointment.getAppointmentNumber())) {
            Invoice existing = invoiceRepository
                    .findByAppointmentAppointmentNumber(appointment.getAppointmentNumber())
                    .orElseThrow();
            throw new BusinessException(ErrorCode.ALREADY_INVOICED,
                    "Bill " + existing.getInvoiceNumber() + " has already been issued for appointment "
                            + appointment.getAppointmentNumber() + ". Open it to reprint the receipt.");
        }

        PricingResult pricing = price(appointment, request.getDiscountPercentage());
        verifyAgainstDatabaseFunction(appointment.getAppointmentNumber(), pricing);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(sequenceGenerator.nextInvoiceNumber());
        invoice.setAppointment(appointment);

        // Snapshot: a bill is a record of what was charged, to whom, that day.
        invoice.setPatientName(appointment.getPatient().getFullName());
        invoice.setPatientAddress(appointment.getPatient().getAddress());
        invoice.setPatientContact(appointment.getPatient().getContactNumber());
        invoice.setDentistName(appointment.getDentist().getFullName());
        invoice.setTreatmentName(appointment.getTreatment().getName());

        invoice.setConsultationFee(pricing.getConsultationFee());
        invoice.setTreatmentCost(pricing.getTreatmentCost());
        invoice.setSurchargeAmount(pricing.getSurchargeAmount());
        invoice.setSubTotal(pricing.getSubTotal());
        invoice.setDiscountPercentage(pricing.getDiscountPercentage());
        invoice.setDiscountAmount(pricing.getDiscountAmount());
        invoice.setDiscountReason(firstNonBlank(request.getDiscountReason(), pricing.getDiscountReason()));
        invoice.setTaxableAmount(pricing.getTaxableAmount());
        invoice.setTaxRate(pricing.getTaxRate());
        invoice.setTaxAmount(pricing.getTaxAmount());
        invoice.setTotalAmount(pricing.getTotalAmount());
        invoice.setPricingStrategyApplied(pricing.getStrategyKey());

        invoice.setIssuedDate(LocalDate.now());
        invoice.setIssuedBy(actor);
        invoice.setPaymentStatus(PaymentStatus.PENDING);
        invoice.setRemarks(request.getRemarks());

        for (InvoiceLine line : pricing.getLines()) {
            invoice.addLine(new InvoiceLine(line.getDescription(), line.getQuantity(),
                                            line.getUnitPrice(), line.getLineType()));
        }

        Invoice saved = invoiceRepository.saveAndFlush(invoice);
        log.info("Bill {} issued for appointment {} - total {} ({} rule)",
                 saved.getInvoiceNumber(), appointment.getAppointmentNumber(),
                 saved.getTotalAmount(), pricing.getStrategyKey());

        eventPublisher.publish(AppointmentEvent
                .from(appointment, AppointmentEventType.INVOICE_ISSUED, actor)
                .reference(saved.getInvoiceNumber())
                .amount(saved.getTotalAmount())
                .build());

        return invoiceMapper.toDto(saved);
    }

    /**
     * Applies the pricing rule that this treatment's catalogue entry names.
     * The Strategy pattern in action - this method never knows which rule ran.
     */
    private PricingResult price(Appointment appointment, BigDecimal requestedDiscount) {
        PricingStrategy strategy = pricingStrategyFactory.resolve(
                appointment.getTreatment().getPricingStrategyKey());

        PricingContext context = PricingContext.builder()
                .treatmentBasePrice(appointment.getTreatment().getBasePrice())
                .consultationFee(appointment.getDentist().getConsultationFee())
                .requestedDiscountPercentage(requestedDiscount)
                .taxRate(ClinicConstants.VAT_RATE)
                .patientIsMinor(appointment.getPatient().isMinor())
                .patientIsSeniorCitizen(appointment.getPatient().isSeniorCitizen())
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .treatmentName(appointment.getTreatment().getName())
                .treatmentCategory(appointment.getTreatment().getCategory())
                .build();

        return strategy.calculate(context);
    }

    /**
     * Cross-checks the Java total against the database stored function.
     *
     * <p>Two independent implementations of one formula agreeing is real
     * evidence the bill is right. Disagreeing is a defect worth an alarm in the
     * log, and it is caught before the patient is handed a receipt rather than
     * during an audit months later.</p>
     */
    private void verifyAgainstDatabaseFunction(String appointmentNumber, PricingResult pricing) {
        try {
            BigDecimal fromDatabase = reportingDao.calculateInvoiceTotalInDatabase(
                    pricing.getConsultationFee(), pricing.getTreatmentCost(),
                    pricing.getSurchargeAmount(), pricing.getDiscountPercentage(),
                    pricing.getTaxRate());

            if (fromDatabase != null
                    && fromDatabase.compareTo(pricing.getTotalAmount()) != 0) {
                log.error("BILLING RECONCILIATION MISMATCH on appointment {}: "
                          + "Java pricing produced {} but FN_INVOICE_TOTAL produced {}. "
                          + "The stored figures follow the Java calculation - investigate immediately.",
                          appointmentNumber, pricing.getTotalAmount(), fromDatabase);
            }
        } catch (RuntimeException ex) {
            // A reconciliation check that fails must not stop a patient paying.
            log.warn("Could not reconcile the bill for {} against FN_INVOICE_TOTAL: {}",
                     appointmentNumber, ex.getMessage());
        }
    }

    /* ================================================================== */
    /* Settlement                                                          */
    /* ================================================================== */

    /** Records a full or partial payment. The rules live on the entity. */
    @Transactional
    public InvoiceDto recordPayment(String invoiceNumber, PaymentRequest request) {
        String actor = SecurityUtils.getCurrentUsernameOrSystem();
        Invoice invoice = requireByInvoiceNumber(invoiceNumber);

        invoice.applyPayment(request.getAmount(), request.getPaymentMethod(), request.getReference());
        Invoice saved = invoiceRepository.saveAndFlush(invoice);

        log.info("Payment of {} recorded against {} by {} - balance now {}",
                 request.getAmount(), invoiceNumber, actor, saved.getBalanceDue());

        eventPublisher.publish(AppointmentEvent
                .from(saved.getAppointment(), AppointmentEventType.PAYMENT_RECEIVED, actor)
                .reference(saved.getInvoiceNumber())
                .amount(request.getAmount())
                .detail("Balance remaining: " + MoneyUtils.formatWithCurrency(saved.getBalanceDue()))
                .build());

        return invoiceMapper.toDto(saved);
    }

    /** Voids a bill raised in error. Paid bills cannot be voided. */
    @Transactional
    public InvoiceDto cancelInvoice(String invoiceNumber, String reason) {
        Invoice invoice = requireByInvoiceNumber(invoiceNumber);
        invoice.cancel(reason);
        log.warn("Bill {} cancelled by {} - {}", invoiceNumber,
                 SecurityUtils.getCurrentUsernameOrSystem(), reason);
        return invoiceMapper.toDto(invoiceRepository.saveAndFlush(invoice));
    }

    /* ================================================================== */
    /* Lookup                                                              */
    /* ================================================================== */

    public InvoiceDto findByInvoiceNumber(String invoiceNumber) {
        return invoiceMapper.toDto(requireByInvoiceNumber(invoiceNumber));
    }

    public InvoiceDto findByAppointmentNumber(String appointmentNumber) {
        return invoiceMapper.toDto(invoiceRepository
                .findByAppointmentAppointmentNumber(appointmentNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No bill has been issued for appointment " + appointmentNumber)));
    }

    public Invoice requireByInvoiceNumber(String invoiceNumber) {
        String key = invoiceNumber == null ? null : invoiceNumber.trim().toUpperCase();
        return invoiceRepository.findByInvoiceNumber(key)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceNumber));
    }

    public PageResponse<InvoiceDto> search(String term, PaymentStatus status,
                                           LocalDate fromDate, LocalDate toDate,
                                           int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "issuedDate").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Invoice> result = invoiceRepository.search(
                term == null ? "" : term.trim(), status, fromDate, toDate, pageable);

        return new PageResponse<>(invoiceMapper.toDtoList(result.getContent()),
                                  result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public List<InvoiceDto> listOutstanding() {
        return invoiceMapper.toDtoList(invoiceRepository.findOutstanding(
                List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID)));
    }

    public BigDecimal revenueOn(LocalDate date) {
        return MoneyUtils.nullSafe(invoiceRepository.sumTotalForDate(date));
    }

    public BigDecimal revenueBetween(LocalDate from, LocalDate to) {
        return MoneyUtils.nullSafe(invoiceRepository.sumTotalBetween(from, to));
    }

    public BigDecimal totalOutstanding() {
        return MoneyUtils.nullSafe(invoiceRepository.sumOutstanding(
                List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID)));
    }

    public long countOutstanding() {
        return invoiceRepository.countByPaymentStatusIn(
                List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID));
    }

    /* ================================================================== */
    /* Helpers                                                             */
    /* ================================================================== */

    private void copyPricingOnto(InvoiceDto dto, PricingResult pricing) {
        dto.setConsultationFee(pricing.getConsultationFee());
        dto.setTreatmentCost(pricing.getTreatmentCost());
        dto.setSurchargeAmount(pricing.getSurchargeAmount());
        dto.setSubTotal(pricing.getSubTotal());
        dto.setDiscountPercentage(pricing.getDiscountPercentage());
        dto.setDiscountAmount(pricing.getDiscountAmount());
        dto.setDiscountReason(pricing.getDiscountReason());
        dto.setTaxableAmount(pricing.getTaxableAmount());
        dto.setTaxRate(pricing.getTaxRate());
        dto.setTaxAmount(pricing.getTaxAmount());
        dto.setTotalAmount(pricing.getTotalAmount());
        dto.setPricingStrategyApplied(pricing.getStrategyKey());
    }

    private String firstNonBlank(String preferred, String fallback) {
        return (preferred != null && !preferred.isBlank()) ? preferred : fallback;
    }
}
