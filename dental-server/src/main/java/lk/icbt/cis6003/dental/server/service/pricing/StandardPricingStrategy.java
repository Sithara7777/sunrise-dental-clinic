package lk.icbt.cis6003.dental.server.service.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Default billing rule: the catalogue price, plus the clinic's standard
 * concessions.
 *
 * <p>Applies to consultations, radiographs, scaling, fillings, crowns,
 * dentures and orthodontic fittings - anything with no special cost structure.
 * It is also the fallback the factory returns for an unrecognised key, so a
 * bad data value degrades to "charge the list price" rather than failing to
 * produce a bill at all.</p>
 */
@Component
public class StandardPricingStrategy extends AbstractPricingStrategy {

    public static final String KEY = "STANDARD";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getDisplayName() {
        return "Standard Pricing";
    }

    @Override
    public String getDescription() {
        return "Catalogue price with no surcharge; senior citizen and child concessions apply.";
    }

    /** Standard treatments carry no loading. */
    @Override
    protected BigDecimal calculateSurcharge(PricingContext context) {
        return BigDecimal.ZERO;
    }

    @Override
    protected DiscountDecision resolveDiscount(PricingContext context) {
        return standardConcession(context);
    }
}
