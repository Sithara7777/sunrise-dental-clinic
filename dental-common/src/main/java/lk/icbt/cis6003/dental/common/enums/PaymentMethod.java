package lk.icbt.cis6003.dental.common.enums;

import java.math.BigDecimal;

/**
 * How a patient settles a bill.
 *
 * <p>Assumption documented in the report: card settlements carry a 1.5%
 * merchant fee that the clinic absorbs, and insurance settlements are recorded
 * but only 90% is recognised immediately (the balance is claimed later). The
 * surcharge factor lives here so the billing tier does not need a switch
 * statement over payment methods.</p>
 */
public enum PaymentMethod {

    CASH("Cash", BigDecimal.ZERO),
    CARD("Credit / Debit Card", new BigDecimal("0.015")),
    BANK_TRANSFER("Bank Transfer", BigDecimal.ZERO),
    INSURANCE("Insurance Claim", BigDecimal.ZERO);

    private final String displayName;
    private final BigDecimal merchantFeeRate;

    PaymentMethod(String displayName, BigDecimal merchantFeeRate) {
        this.displayName = displayName;
        this.merchantFeeRate = merchantFeeRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getMerchantFeeRate() {
        return merchantFeeRate;
    }
}
