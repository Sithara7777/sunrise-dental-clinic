package lk.icbt.cis6003.dental.server.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.common.enums.PaymentMethod;
import lk.icbt.cis6003.dental.common.enums.PaymentStatus;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The patient bill produced by "Calculate and Print Bill".
 *
 * <p>Two deliberate design decisions:</p>
 *
 * <p><b>1. Every figure is stored, not recomputed.</b> Consultation fee,
 * treatment cost, discount, VAT and total are all columns. Re-deriving a
 * historic bill from today's price list would silently rewrite the past the
 * next time a treatment price changed - a classic source of the "billing
 * errors" the clinic complained about.</p>
 *
 * <p><b>2. Patient details are copied onto the invoice.</b> The invoice is the
 * legal record of what was charged, to whom, at that address, on that date.</p>
 *
 * <p>All money is {@link BigDecimal}. Using {@code double} for currency is the
 * canonical rounding bug and would produce receipts that do not add up.</p>
 */
@Entity
@Table(name = "invoice",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_invoice_number", columnNames = "invoice_number"),
           @UniqueConstraint(name = "uk_invoice_appointment", columnNames = "appointment_id")
       },
       indexes = {
           @Index(name = "ix_invoice_issued_date", columnList = "issued_date"),
           @Index(name = "ix_invoice_payment_status", columnList = "payment_status")
       })
public class Invoice extends BaseEntity {

    @Column(name = "invoice_number", nullable = false, length = 20)
    private String invoiceNumber;

    /**
     * One bill per completed visit. The unique constraint on
     * {@code appointment_id} is what makes "print the bill twice" impossible.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_invoice_appointment"))
    private Appointment appointment;

    /* ---- denormalised snapshot of the patient at the time of billing ---- */

    @Column(name = "patient_name", nullable = false, length = 100)
    private String patientName;

    @Column(name = "patient_address", nullable = false, length = 200)
    private String patientAddress;

    @Column(name = "patient_contact", nullable = false, length = 20)
    private String patientContact;

    @Column(name = "dentist_name", nullable = false, length = 100)
    private String dentistName;

    @Column(name = "treatment_name", nullable = false, length = 100)
    private String treatmentName;

    /* ------------------------------ money ------------------------------ */

    @Column(name = "consultation_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Column(name = "treatment_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal treatmentCost = BigDecimal.ZERO;

    /** Strategy-driven loading, e.g. surgical sterilisation surcharge. */
    @Column(name = "surcharge_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal surchargeAmount = BigDecimal.ZERO;

    @Column(name = "sub_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_reason", length = 200)
    private String discountReason;

    @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate = ClinicConstants.VAT_RATE;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    /** Which Strategy produced the figures above - printed on the receipt. */
    @Column(name = "pricing_strategy_applied", length = 30)
    private String pricingStrategyApplied;

    /* ------------------------- settlement ------------------------------ */

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate = LocalDate.now();

    @Column(name = "issued_by", nullable = false, length = 30)
    private String issuedBy;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "remarks", length = 300)
    private String remarks;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    public Invoice() {
        // required by JPA
    }

    /* ------------------------- behaviour ------------------------------- */

    /** Money still owed on this bill. Never negative. */
    public BigDecimal getBalanceDue() {
        BigDecimal balance = totalAmount.subtract(amountPaid);
        return balance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : balance;
    }

    /**
     * Records a (possibly partial) payment and re-derives the payment status.
     *
     * @throws BusinessException if the payment would exceed the balance, or the
     *         invoice has been cancelled
     */
    public void applyPayment(BigDecimal amount, PaymentMethod method, String reference) {
        if (paymentStatus == PaymentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Invoice " + invoiceNumber + " has been cancelled and cannot take a payment");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Payment amount must be greater than zero");
        }
        if (amount.compareTo(getBalanceDue()) > 0) {
            throw new BusinessException(ErrorCode.PAYMENT_EXCEEDS_BALANCE,
                    "Payment of " + amount + " exceeds the outstanding balance of " + getBalanceDue());
        }

        this.amountPaid = this.amountPaid.add(amount).setScale(ClinicConstants.MONEY_SCALE, RoundingMode.HALF_UP);
        this.paymentMethod = method;
        this.paymentReference = reference;

        if (getBalanceDue().compareTo(BigDecimal.ZERO) == 0) {
            this.paymentStatus = PaymentStatus.PAID;
            this.paidAt = LocalDateTime.now();
        } else {
            this.paymentStatus = PaymentStatus.PARTIALLY_PAID;
        }
    }

    /** Voids an unpaid bill (e.g. issued against the wrong appointment). */
    public void cancel(String reason) {
        if (paymentStatus == PaymentStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Invoice " + invoiceNumber + " is already paid and cannot be cancelled");
        }
        this.paymentStatus = PaymentStatus.CANCELLED;
        this.remarks = reason;
    }

    /** Days since the bill was issued - drives the debtor ageing report. */
    public long getDaysOutstanding() {
        return java.time.temporal.ChronoUnit.DAYS.between(issuedDate, LocalDate.now());
    }

    /** Keeps both sides of the association consistent. */
    public void addLine(InvoiceLine line) {
        line.setInvoice(this);
        line.setLineNumber(lines.size() + 1);
        this.lines.add(line);
    }

    public LocalDate getAppointmentDate() {
        return appointment == null ? null : appointment.getAppointmentDate();
    }

    public LocalTime getAppointmentTime() {
        return appointment == null ? null : appointment.getAppointmentTime();
    }

    /* ------------------------- accessors ------------------------------- */

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public void setPatientAddress(String patientAddress) {
        this.patientAddress = patientAddress;
    }

    public String getPatientContact() {
        return patientContact;
    }

    public void setPatientContact(String patientContact) {
        this.patientContact = patientContact;
    }

    public String getDentistName() {
        return dentistName;
    }

    public void setDentistName(String dentistName) {
        this.dentistName = dentistName;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public BigDecimal getTreatmentCost() {
        return treatmentCost;
    }

    public void setTreatmentCost(BigDecimal treatmentCost) {
        this.treatmentCost = treatmentCost;
    }

    public BigDecimal getSurchargeAmount() {
        return surchargeAmount;
    }

    public void setSurchargeAmount(BigDecimal surchargeAmount) {
        this.surchargeAmount = surchargeAmount;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getDiscountReason() {
        return discountReason;
    }

    public void setDiscountReason(String discountReason) {
        this.discountReason = discountReason;
    }

    public BigDecimal getTaxableAmount() {
        return taxableAmount;
    }

    public void setTaxableAmount(BigDecimal taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPricingStrategyApplied() {
        return pricingStrategyApplied;
    }

    public void setPricingStrategyApplied(String pricingStrategyApplied) {
        this.pricingStrategyApplied = pricingStrategyApplied;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : lines;
    }

    @Override
    public String toString() {
        return invoiceNumber + " | " + patientName + " | " + totalAmount + " | " + paymentStatus;
    }
}
