package lk.icbt.cis6003.dental.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One printed line of a bill.
 *
 * <p>Storing the breakdown rather than only the total is what lets a patient
 * see <em>why</em> they are paying what they are paying, and lets the clinic
 * answer a billing query months later without re-deriving anything.</p>
 */
@Entity
@Table(name = "invoice_line")
public class InvoiceLine extends BaseEntity {

    public static final String TYPE_CHARGE = "CHARGE";
    public static final String TYPE_SURCHARGE = "SURCHARGE";
    public static final String TYPE_DISCOUNT = "DISCOUNT";
    public static final String TYPE_TAX = "TAX";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_invoice_line_invoice"))
    private Invoice invoice;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "line_type", nullable = false, length = 20)
    private String lineType = TYPE_CHARGE;

    public InvoiceLine() {
        // required by JPA
    }

    public InvoiceLine(String description, int quantity, BigDecimal unitPrice, String lineType) {
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineType = lineType;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    @Override
    public String toString() {
        return lineNumber + ". " + description + " = " + lineTotal;
    }
}
