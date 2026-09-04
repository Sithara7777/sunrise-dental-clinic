package lk.icbt.cis6003.dental.common.dto;

import java.math.BigDecimal;

/**
 * A single printed line on the patient's bill.
 *
 * <p>Lines are produced by the billing tier rather than stored ad hoc, so the
 * receipt shows exactly how the total was reached - consultation fee,
 * treatment charge, any surcharge, discount and VAT each appear separately.
 * That transparency is the direct answer to the "billing errors" complaint in
 * the scenario.</p>
 */
public class InvoiceLineDto {

    private Long id;
    private int lineNumber;
    private String description;
    private int quantity = 1;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal lineTotal = BigDecimal.ZERO;

    /** {@code CHARGE}, {@code SURCHARGE}, {@code DISCOUNT} or {@code TAX}. */
    private String lineType = "CHARGE";

    public InvoiceLineDto() {
        // required by Jackson
    }

    public InvoiceLineDto(int lineNumber, String description, int quantity,
                          BigDecimal unitPrice, BigDecimal lineTotal, String lineType) {
        this.lineNumber = lineNumber;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
        this.lineType = lineType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        return description + " x" + quantity + " = " + lineTotal;
    }
}
