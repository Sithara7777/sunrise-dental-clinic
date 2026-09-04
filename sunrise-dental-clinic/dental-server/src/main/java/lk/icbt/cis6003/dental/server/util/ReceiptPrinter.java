package lk.icbt.cis6003.dental.server.util;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.common.dto.InvoiceDto;
import lk.icbt.cis6003.dental.common.dto.InvoiceLineDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Renders a bill as the plain-text receipt a thermal printer produces.
 *
 * <p>Requirement 4 asks the system to "print the patient bill/receipt". A
 * 48-column monospaced layout is what a counter-top receipt printer actually
 * accepts, and the same text is what the desktop client shows on screen and
 * what the web UI offers as a download - one renderer, three destinations.</p>
 *
 * <p>Kept out of the service tier deliberately: presentation belongs in the
 * presentation layer, and {@code BillingService} should not know how wide a
 * receipt is.</p>
 */
@Component
public class ReceiptPrinter {

    /** Standard 80mm thermal receipt width in characters. */
    private static final int WIDTH = 48;

    private static final String DOUBLE_RULE = "=".repeat(WIDTH);
    private static final String SINGLE_RULE = "-".repeat(WIDTH);

    /**
     * @param invoice the bill to render
     * @return the complete receipt, ready to write to a printer or a file
     */
    public String render(InvoiceDto invoice) {
        StringBuilder sb = new StringBuilder();

        sb.append(DOUBLE_RULE).append('\n');
        sb.append(centre(ClinicConstants.CLINIC_NAME.toUpperCase())).append('\n');
        sb.append(centre(ClinicConstants.CLINIC_ADDRESS_LINE_1)).append('\n');
        sb.append(centre(ClinicConstants.CLINIC_ADDRESS_LINE_2)).append('\n');
        sb.append(centre("Tel: " + ClinicConstants.CLINIC_PHONE)).append('\n');
        sb.append(centre("Reg. No: " + ClinicConstants.CLINIC_REGISTRATION_NO)).append('\n');
        sb.append(DOUBLE_RULE).append('\n');
        sb.append(centre("PATIENT BILL / RECEIPT")).append('\n');
        sb.append(DOUBLE_RULE).append('\n');

        sb.append(field("Bill No", invoice.getInvoiceNumber()));
        sb.append(field("Appointment No", invoice.getAppointmentNumber()));
        sb.append(field("Date", formatDateTime(invoice.getIssuedAt())));
        sb.append(field("Issued By", invoice.getIssuedBy()));
        sb.append(SINGLE_RULE).append('\n');

        sb.append(field("Patient", invoice.getPatientName()));
        sb.append(field("Patient No", invoice.getPatientCode()));
        sb.append(wrappedField("Address", invoice.getPatientAddress()));
        sb.append(field("Contact", invoice.getPatientContact()));
        sb.append(SINGLE_RULE).append('\n');

        sb.append(field("Dentist", prefixDoctor(invoice.getDentistName())));
        sb.append(field("Treatment", invoice.getTreatmentName()));
        if (invoice.getAppointmentDate() != null) {
            sb.append(field("Visit Date",
                    invoice.getAppointmentDate().format(ClinicConstants.DISPLAY_DATE_FORMAT)
                            + (invoice.getAppointmentTime() == null ? ""
                               : " " + invoice.getAppointmentTime())));
        }
        sb.append(DOUBLE_RULE).append('\n');

        sb.append(String.format("%-32s%16s%n", "DESCRIPTION", "AMOUNT (" + ClinicConstants.CURRENCY_CODE + ")"));
        sb.append(SINGLE_RULE).append('\n');

        for (InvoiceLineDto line : invoice.getLines()) {
            sb.append(moneyRow(truncate(line.getDescription(), 32), MoneyUtils.format(line.getLineTotal())));
        }

        sb.append(SINGLE_RULE).append('\n');
        sb.append(moneyRow("Sub-total", MoneyUtils.format(invoice.getSubTotal())));
        if (MoneyUtils.isPositive(invoice.getDiscountAmount())) {
            sb.append(moneyRow("Discount (" + strip(invoice.getDiscountPercentage()) + "%)",
                               "-" + MoneyUtils.format(invoice.getDiscountAmount())));
        }
        sb.append(moneyRow("Taxable amount", MoneyUtils.format(invoice.getTaxableAmount())));
        sb.append(moneyRow("VAT @ " + strip(percentage(invoice)) + "%",
                           MoneyUtils.format(invoice.getTaxAmount())));
        sb.append(DOUBLE_RULE).append('\n');
        sb.append(moneyRow("TOTAL PAYABLE", MoneyUtils.format(invoice.getTotalAmount())));
        sb.append(DOUBLE_RULE).append('\n');

        sb.append(moneyRow("Amount paid", MoneyUtils.format(invoice.getAmountPaid())));
        sb.append(moneyRow("Balance due", MoneyUtils.format(invoice.getBalanceDue())));
        sb.append(field("Status", invoice.getPaymentStatus() == null
                ? "-" : invoice.getPaymentStatus().getDisplayName()));
        if (invoice.getPaymentMethod() != null) {
            sb.append(field("Paid by", invoice.getPaymentMethod().getDisplayName()));
        }

        if (invoice.getDiscountReason() != null && !invoice.getDiscountReason().isBlank()) {
            sb.append(SINGLE_RULE).append('\n');
            sb.append(wrappedField("Discount", invoice.getDiscountReason()));
        }
        if (invoice.getPricingStrategyApplied() != null) {
            sb.append(field("Pricing rule", invoice.getPricingStrategyApplied()));
        }

        sb.append(DOUBLE_RULE).append('\n');
        sb.append(centre("Thank you for choosing")).append('\n');
        sb.append(centre(ClinicConstants.CLINIC_NAME)).append('\n');
        sb.append('\n');
        sb.append(centre("This is a computer generated receipt.")).append('\n');
        sb.append(centre("Please retain it for your records.")).append('\n');
        sb.append(DOUBLE_RULE).append('\n');

        return sb.toString();
    }

    /* ------------------------------------------------------------------ */
    /* Layout helpers                                                      */
    /* ------------------------------------------------------------------ */

    private String centre(String text) {
        if (text == null) {
            return "";
        }
        String value = truncate(text, WIDTH);
        int padding = Math.max((WIDTH - value.length()) / 2, 0);
        return " ".repeat(padding) + value;
    }

    private String field(String label, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return String.format("%-16s: %s%n", truncate(label, 16), truncate(value, WIDTH - 18));
    }

    /** Wraps a long value (an address) onto continuation lines. */
    private String wrappedField(String label, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int available = WIDTH - 18;
        StringBuilder sb = new StringBuilder();
        String remaining = value.trim();
        boolean first = true;

        while (!remaining.isEmpty()) {
            String chunk = remaining.length() <= available ? remaining : remaining.substring(0, available);
            sb.append(String.format("%-16s: %s%n", first ? truncate(label, 16) : "", chunk));
            remaining = remaining.length() <= available ? "" : remaining.substring(available).trim();
            first = false;
        }
        return sb.toString();
    }

    private String moneyRow(String label, String amount) {
        return String.format("%-32s%16s%n", truncate(label, 32), amount);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String prefixDoctor(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.startsWith("Dr") ? name : "Dr " + name;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null
                ? LocalDateTime.now().format(ClinicConstants.DISPLAY_DATETIME_FORMAT)
                : value.format(ClinicConstants.DISPLAY_DATETIME_FORMAT);
    }

    private java.math.BigDecimal percentage(InvoiceDto invoice) {
        return MoneyUtils.nullSafe(invoice.getTaxRate()).multiply(MoneyUtils.HUNDRED);
    }

    private String strip(java.math.BigDecimal value) {
        return MoneyUtils.nullSafe(value).stripTrailingZeros().toPlainString();
    }
}
