package lk.icbt.cis6003.dental.server.service.pricing;

import lk.icbt.cis6003.dental.server.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Billing rule for invasive procedures - extractions, surgical extractions,
 * root canal treatment and implants.
 *
 * <p>Surgical work consumes single-use instruments, sutures, sterile drapes and
 * a full autoclave cycle that a check-up does not. The clinic recovers that as
 * a percentage of the treatment price, with a floor so that a low-value
 * procedure still covers the fixed cost of the consumables tray.</p>
 *
 * <p>Concessions still apply: surgical work is clinically necessary, so a
 * senior citizen should not lose their concession because they needed an
 * extraction rather than a filling.</p>
 */
@Component
public class SurgicalPricingStrategy extends AbstractPricingStrategy {

    public static final String KEY = "SURGICAL";

    /** Sterilisation and consumables recovery, as a fraction of treatment price. */
    private static final BigDecimal SURCHARGE_RATE = new BigDecimal("0.12");

    /** Floor, so a low-value procedure still covers the consumables tray. */
    private static final BigDecimal MINIMUM_SURCHARGE = new BigDecimal("1500.00");

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getDisplayName() {
        return "Surgical Pricing";
    }

    @Override
    public String getDescription() {
        return "Adds a 12% sterilisation and consumables surcharge (minimum Rs. 1,500); "
                + "concessions still apply.";
    }

    @Override
    protected BigDecimal calculateSurcharge(PricingContext context) {
        BigDecimal proportional = MoneyUtils.rateOf(context.getTreatmentBasePrice(), SURCHARGE_RATE);
        return MoneyUtils.max(proportional, MINIMUM_SURCHARGE);
    }

    @Override
    protected String surchargeDescription() {
        return "Sterilisation and surgical consumables";
    }

    @Override
    protected DiscountDecision resolveDiscount(PricingContext context) {
        return standardConcession(context);
    }
}
