package lk.icbt.cis6003.dental.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * An entry in the treatment catalogue - the "treatment type" of the scenario.
 *
 * <p>Modelled as a table rather than a Java enum so that the clinic can add
 * "Teeth Whitening (LED)" or change the price of a root canal without a code
 * change and redeploy. {@code pricingStrategyKey} is the discriminator the
 * Strategy factory uses to pick the billing rule for this treatment.</p>
 */
@Entity
@Table(name = "treatment",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_treatment_code", columnNames = "code")
       })
public class Treatment extends BaseEntity {

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 30;

    /** {@code STANDARD}, {@code SURGICAL}, {@code COSMETIC} or {@code EMERGENCY}. */
    @Column(name = "pricing_strategy", nullable = false, length = 30)
    private String pricingStrategyKey = "STANDARD";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Treatment() {
        // required by JPA
    }

    public Treatment(String code, String name, String category,
                     BigDecimal basePrice, Integer durationMinutes, String pricingStrategyKey) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
        this.durationMinutes = durationMinutes;
        this.pricingStrategyKey = pricingStrategyKey;
    }

    /**
     * How many 30-minute diary slots this treatment consumes. Rounded up, so a
     * 45-minute treatment correctly blocks two slots rather than one and a
     * half.
     */
    public int getSlotCount() {
        int slot = lk.icbt.cis6003.dental.common.ClinicConstants.SLOT_DURATION_MINUTES;
        return (int) Math.ceil((double) durationMinutes / slot);
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

    public String getPricingStrategyKey() {
        return pricingStrategyKey;
    }

    public void setPricingStrategyKey(String pricingStrategyKey) {
        this.pricingStrategyKey = pricingStrategyKey;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return code + " - " + name;
    }
}
