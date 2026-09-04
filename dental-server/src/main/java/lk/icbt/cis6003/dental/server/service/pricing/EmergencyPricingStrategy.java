package lk.icbt.cis6003.dental.server.service.pricing;

import lk.icbt.cis6003.dental.server.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Billing rule for same-day emergency attendance.
 *
 * <p>An emergency slot is not simply another appointment: it displaces booked
 * work or requires staff outside their shift. The clinic recovers that as a
 * loading on the consultation fee - but <em>only</em> when the visit actually
 * falls outside core hours (before 09:00, from 17:00, or at a weekend). An
 * emergency seen at 11 a.m. on a Tuesday costs the clinic no more than any
 * other visit and is charged accordingly.</p>
 *
 * <p>Concessions still apply. Someone in pain should not be penalised for
 * their age.</p>
 */
@Component
public class EmergencyPricingStrategy extends AbstractPricingStrategy {

    public static final String KEY = "EMERGENCY";

    /** Loading applied to the consultation fee for out-of-hours attendance. */
    private static final BigDecimal OUT_OF_HOURS_RATE = new BigDecimal("0.35");

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getDisplayName() {
        return "Emergency Pricing";
    }

    @Override
    public String getDescription() {
        return "Adds a 35% out-of-hours loading on the consultation fee for visits before 09:00, "
                + "from 17:00, or at a weekend; concessions still apply.";
    }

    @Override
    protected BigDecimal calculateSurcharge(PricingContext context) {
        boolean outOfHours = context.isOutsideCoreHours() || context.isWeekend();
        if (!outOfHours) {
            return BigDecimal.ZERO;
        }
        return MoneyUtils.rateOf(context.getConsultationFee(), OUT_OF_HOURS_RATE);
    }

    @Override
    protected String surchargeDescription() {
        return "Out-of-hours emergency attendance";
    }

    @Override
    protected DiscountDecision resolveDiscount(PricingContext context) {
        return standardConcession(context);
    }
}
