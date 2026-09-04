package lk.icbt.cis6003.dental.server.service.pricing;

import lk.icbt.cis6003.dental.server.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Billing rule for elective cosmetic work - whitening and veneers.
 *
 * <p>The clinic's senior-citizen and child concessions exist to keep
 * <em>clinically necessary</em> care affordable. Cosmetic treatment is
 * elective, so the automatic concessions are deliberately withheld here; a
 * receptionist can still apply a manual, approved discount, and that discount
 * is recorded with its reason.</p>
 *
 * <p>This is the clearest illustration of why the Strategy pattern earns its
 * place: the rule is not "a different number", it is a different
 * <em>policy</em>, and a single parameterised formula could not express it
 * without a conditional that would grow with every future exception.</p>
 */
@Component
public class CosmeticPricingStrategy extends AbstractPricingStrategy {

    public static final String KEY = "COSMETIC";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getDisplayName() {
        return "Cosmetic Pricing";
    }

    @Override
    public String getDescription() {
        return "Elective treatment: automatic age concessions do not apply; "
                + "only an approved manual discount is honoured.";
    }

    @Override
    protected BigDecimal calculateSurcharge(PricingContext context) {
        return BigDecimal.ZERO;
    }

    /** Manual discount only - age concessions are intentionally ignored. */
    @Override
    protected DiscountDecision resolveDiscount(PricingContext context) {
        BigDecimal manual = MoneyUtils.nullSafe(context.getRequestedDiscountPercentage());
        if (MoneyUtils.isPositive(manual)) {
            return new DiscountDecision(manual, "Approved discount on elective treatment");
        }
        return DiscountDecision.none();
    }
}
