package lk.icbt.cis6003.dental.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * An entry in the clinic's treatment catalogue ("treatment type" in the
 * scenario), e.g. Scaling &amp; Polishing, Root Canal, Extraction.
 *
 * <p>Holding treatments as data rather than as a hard-coded enum was a
 * deliberate choice: prices change, and the clinic must be able to add a new
 * treatment without a redeploy. {@code pricingStrategy} names which billing
 * rule applies, which is the key that the Strategy factory resolves.</p>
 */
public class TreatmentDto {

    private Long id;

    @NotBlank(message = "Treatment code is required")
    @Size(max = 20, message = "Treatment code must not exceed 20 characters")
    private String code;

    @NotBlank(message = "Treatment name is required")
    @Size(max = 100, message = "Treatment name must not exceed 100 characters")
    private String name;

    @Size(max = 300, message = "Description must not exceed 300 characters")
    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.00", message = "Base price cannot be negative")
    private BigDecimal basePrice;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "A treatment must be at least 15 minutes")
    @Max(value = 480, message = "A treatment cannot exceed 8 hours")
    private Integer durationMinutes;

    /**
     * Key of the pricing rule to apply, e.g. {@code STANDARD}, {@code SURGICAL},
     * {@code COSMETIC}, {@code EMERGENCY}. Resolved at runtime by the pricing
     * strategy factory.
     */
    @NotBlank(message = "Pricing strategy is required")
    @Size(max = 30, message = "Pricing strategy must not exceed 30 characters")
    private String pricingStrategy = "STANDARD";

    private boolean active = true;

    public TreatmentDto() {
        // required by Jackson
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getPricingStrategy() {
        return pricingStrategy;
    }

    public void setPricingStrategy(String pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDisplayLabel() {
        return name + " - Rs. " + basePrice;
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}
