package lk.icbt.cis6003.dental.common.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One unpaid or part-paid bill in the Outstanding Payments (debtor ageing)
 * report.
 *
 * <p>Decision it supports: who to chase first. {@code ageingBucket} groups the
 * debt into 0-30 / 31-60 / 61-90 / 90+ days so the front desk can work the
 * oldest money first.</p>
 */
public class OutstandingInvoiceRow {

    private String invoiceNumber;
    private String appointmentNumber;
    private String patientName;
    private String contactNumber;
    private LocalDate issuedDate;
    private long daysOutstanding;
    private String ageingBucket;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal amountPaid = BigDecimal.ZERO;
    private BigDecimal balanceDue = BigDecimal.ZERO;
    private String paymentStatus;

    public OutstandingInvoiceRow() {
        // required by Jackson
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public long getDaysOutstanding() {
        return daysOutstanding;
    }

    public void setDaysOutstanding(long daysOutstanding) {
        this.daysOutstanding = daysOutstanding;
    }

    public String getAgeingBucket() {
        return ageingBucket;
    }

    public void setAgeingBucket(String ageingBucket) {
        this.ageingBucket = ageingBucket;
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

    public BigDecimal getBalanceDue() {
        return balanceDue;
    }

    public void setBalanceDue(BigDecimal balanceDue) {
        this.balanceDue = balanceDue;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
