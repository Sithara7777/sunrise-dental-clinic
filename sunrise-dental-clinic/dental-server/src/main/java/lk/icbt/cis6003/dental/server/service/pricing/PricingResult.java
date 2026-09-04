package lk.icbt.cis6003.dental.server.service.pricing;

import lk.icbt.cis6003.dental.common.dto.InvoiceLineDto;
import lk.icbt.cis6003.dental.server.domain.InvoiceLine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The complete, itemised outcome of a pricing calculation.
 *
 * <p>A strategy does not return "the total". It returns every intermediate
 * figure plus the narrative lines that will be printed. That is what allows the
 * receipt to explain itself - "Senior citizen concession 10%", "Sterilisation
 * and consumables surcharge" - instead of presenting a number the patient has
 * to take on trust.</p>
 */
public final class PricingResult {

    private final String strategyKey;
    private final String strategyName;
    private final BigDecimal consultationFee;
    private final BigDecimal treatmentCost;
    private final BigDecimal surchargeAmount;
    private final BigDecimal subTotal;
    private final BigDecimal discountPercentage;
    private final BigDecimal discountAmount;
    private final String discountReason;
    private final BigDecimal taxableAmount;
    private final BigDecimal taxRate;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;
    private final List<InvoiceLine> lines;
    private final List<String> explanations;

    PricingResult(Builder builder) {
        this.strategyKey = builder.strategyKey;
        this.strategyName = builder.strategyName;
        this.consultationFee = builder.consultationFee;
        this.treatmentCost = builder.treatmentCost;
        this.surchargeAmount = builder.surchargeAmount;
        this.subTotal = builder.subTotal;
        this.discountPercentage = builder.discountPercentage;
        this.discountAmount = builder.discountAmount;
        this.discountReason = builder.discountReason;
        this.taxableAmount = builder.taxableAmount;
        this.taxRate = builder.taxRate;
        this.taxAmount = builder.taxAmount;
        this.totalAmount = builder.totalAmount;
        this.lines = Collections.unmodifiableList(new ArrayList<>(builder.lines));
        this.explanations = Collections.unmodifiableList(new ArrayList<>(builder.explanations));
    }

    public String getStrategyKey() {
        return strategyKey;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public BigDecimal getTreatmentCost() {
        return treatmentCost;
    }

    public BigDecimal getSurchargeAmount() {
        return surchargeAmount;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public String getDiscountReason() {
        return discountReason;
    }

    public BigDecimal getTaxableAmount() {
        return taxableAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /** Persistable receipt lines, in print order. */
    public List<InvoiceLine> getLines() {
        return lines;
    }

    /** Human sentences describing every adjustment the strategy made. */
    public List<String> getExplanations() {
        return explanations;
    }

    /** Read-only view of the lines, for the "preview bill" endpoint. */
    public List<InvoiceLineDto> toLineDtos() {
        List<InvoiceLineDto> dtos = new ArrayList<>();
        int number = 1;
        for (InvoiceLine line : lines) {
            dtos.add(new InvoiceLineDto(number++, line.getDescription(), line.getQuantity(),
                                        line.getUnitPrice(), line.getLineTotal(), line.getLineType()));
        }
        return dtos;
    }

    @Override
    public String toString() {
        return strategyName + " -> total " + totalAmount;
    }

    static Builder builder() {
        return new Builder();
    }

    /** Package-private builder; only strategies assemble a result. */
    static final class Builder {

        private String strategyKey;
        private String strategyName;
        private BigDecimal consultationFee = BigDecimal.ZERO;
        private BigDecimal treatmentCost = BigDecimal.ZERO;
        private BigDecimal surchargeAmount = BigDecimal.ZERO;
        private BigDecimal subTotal = BigDecimal.ZERO;
        private BigDecimal discountPercentage = BigDecimal.ZERO;
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private String discountReason;
        private BigDecimal taxableAmount = BigDecimal.ZERO;
        private BigDecimal taxRate = BigDecimal.ZERO;
        private BigDecimal taxAmount = BigDecimal.ZERO;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private final List<InvoiceLine> lines = new ArrayList<>();
        private final List<String> explanations = new ArrayList<>();

        Builder strategyKey(String value) {
            this.strategyKey = value;
            return this;
        }

        Builder strategyName(String value) {
            this.strategyName = value;
            return this;
        }

        Builder consultationFee(BigDecimal value) {
            this.consultationFee = value;
            return this;
        }

        Builder treatmentCost(BigDecimal value) {
            this.treatmentCost = value;
            return this;
        }

        Builder surchargeAmount(BigDecimal value) {
            this.surchargeAmount = value;
            return this;
        }

        Builder subTotal(BigDecimal value) {
            this.subTotal = value;
            return this;
        }

        Builder discountPercentage(BigDecimal value) {
            this.discountPercentage = value;
            return this;
        }

        Builder discountAmount(BigDecimal value) {
            this.discountAmount = value;
            return this;
        }

        Builder discountReason(String value) {
            this.discountReason = value;
            return this;
        }

        Builder taxableAmount(BigDecimal value) {
            this.taxableAmount = value;
            return this;
        }

        Builder taxRate(BigDecimal value) {
            this.taxRate = value;
            return this;
        }

        Builder taxAmount(BigDecimal value) {
            this.taxAmount = value;
            return this;
        }

        Builder totalAmount(BigDecimal value) {
            this.totalAmount = value;
            return this;
        }

        Builder addLine(InvoiceLine line) {
            this.lines.add(line);
            return this;
        }

        Builder addExplanation(String text) {
            this.explanations.add(text);
            return this;
        }

        PricingResult build() {
            return new PricingResult(this);
        }
    }
}
