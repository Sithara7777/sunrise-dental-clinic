package lk.icbt.cis6003.dental.server.service.pricing;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.server.testsupport.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the billing rules - the Strategy pattern.
 *
 * <p><b>Why these are the most important unit tests in the project.</b> Billing
 * errors are one of the four problems the clinic asked us to fix. Every one of
 * these calculations ends up on a printed receipt handed to a patient, so each
 * rule is verified against a figure worked out by hand in the test comment
 * rather than against whatever the code happens to produce.</p>
 *
 * <p>The strategies take a plain {@link PricingContext} and touch no database,
 * which is what allows them to be tested this directly. That was a design
 * decision made <em>for</em> testability, and this class is the payoff.</p>
 */
@DisplayName("Pricing strategies (Strategy pattern)")
class PricingStrategyTest {

    private final StandardPricingStrategy standard = new StandardPricingStrategy();
    private final SurgicalPricingStrategy surgical = new SurgicalPricingStrategy();
    private final CosmeticPricingStrategy cosmetic = new CosmeticPricingStrategy();
    private final EmergencyPricingStrategy emergency = new EmergencyPricingStrategy();

    /* ================================================================== */
    @Nested
    @DisplayName("Standard pricing")
    class Standard {

        @Test
        @DisplayName("charges consultation + treatment + VAT, with no surcharge")
        void chargesListPricePlusVat() {
            // Consultation 1,500 + treatment 6,500      = 8,000.00
            // Discount                          0%      =     0.00
            // VAT 18% of 8,000                          = 1,440.00
            // Total                                     = 9,440.00
            PricingResult result = standard.calculate(contextFor(
                    new BigDecimal("6500.00"), new BigDecimal("1500.00"),
                    BigDecimal.ZERO, false, false));

            assertThat(result.getSubTotal()).isEqualByComparingTo("8000.00");
            assertThat(result.getSurchargeAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getTaxAmount()).isEqualByComparingTo("1440.00");
            assertThat(result.getTotalAmount()).isEqualByComparingTo("9440.00");
            assertThat(result.getStrategyKey()).isEqualTo("STANDARD");
        }

        @Test
        @DisplayName("gives a senior citizen the 10% concession automatically")
        void appliesSeniorConcession() {
            // Sub-total 8,000 - 10% (800) = 7,200 taxable
            // VAT 18% of 7,200            = 1,296.00
            // Total                       = 8,496.00
            PricingResult result = standard.calculate(contextFor(
                    new BigDecimal("6500.00"), new BigDecimal("1500.00"),
                    BigDecimal.ZERO, false, true));

            assertThat(result.getDiscountPercentage()).isEqualByComparingTo("10.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("800.00");
            assertThat(result.getTotalAmount()).isEqualByComparingTo("8496.00");
            assertThat(result.getDiscountReason()).contains("Senior citizen");
        }

        @Test
        @DisplayName("gives a child patient the 5% concession automatically")
        void appliesChildConcession() {
            PricingResult result = standard.calculate(contextFor(
                    new BigDecimal("6500.00"), new BigDecimal("1500.00"),
                    BigDecimal.ZERO, true, false));

            assertThat(result.getDiscountPercentage()).isEqualByComparingTo("5.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("400.00");
            assertThat(result.getDiscountReason()).contains("Child");
        }

        @Test
        @DisplayName("applies the LARGER of the concession and the manual discount, never both")
        void discountsDoNotStack() {
            // Senior concession is 10%; the receptionist approved 20%.
            // Policy: highest single discount wins, so 20% - NOT 30%.
            PricingResult result = standard.calculate(contextFor(
                    new BigDecimal("6500.00"), new BigDecimal("1500.00"),
                    new BigDecimal("20"), false, true));

            assertThat(result.getDiscountPercentage()).isEqualByComparingTo("20.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("1600.00");
        }

        @Test
        @DisplayName("keeps the concession when it beats the manual discount")
        void concessionWinsWhenLarger() {
            PricingResult result = standard.calculate(contextFor(
                    new BigDecimal("6500.00"), new BigDecimal("1500.00"),
                    new BigDecimal("3"), false, true));

            assertThat(result.getDiscountPercentage()).isEqualByComparingTo("10.00");
            assertThat(result.getDiscountReason()).contains("Senior citizen");
        }

        @Test
        @DisplayName("caps any discount at 50%, however large a value is passed in")
        void capsDiscountAtFiftyPercent() {
            PricingResult result = standard.calculate(contextFor(
                    new BigDecimal("6500.00"), new BigDecimal("1500.00"),
                    new BigDecimal("95"), false, false));

            assertThat(result.getDiscountPercentage()).isEqualByComparingTo("50.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("4000.00");
        }

        @Test
        @DisplayName("treats a negative discount as zero rather than as a surcharge")
        void rejectsNegativeDiscount() {
            PricingResult result = standard.calculate(contextFor(
                    new BigDecimal("6500.00"), new BigDecimal("1500.00"),
                    new BigDecimal("-25"), false, false));

            assertThat(result.getDiscountPercentage()).isEqualByComparingTo("0.00");
            assertThat(result.getTotalAmount()).isEqualByComparingTo("9440.00");
        }
    }

    /* ================================================================== */
    @Nested
    @DisplayName("Surgical pricing")
    class Surgical {

        @Test
        @DisplayName("adds a 12% sterilisation surcharge on the treatment price")
        void addsProportionalSurcharge() {
            // Treatment 25,000, surcharge 12%           = 3,000.00
            // Sub-total 1,500 + 25,000 + 3,000          = 29,500.00
            // VAT 18%                                   = 5,310.00
            // Total                                     = 34,810.00
            PricingResult result = surgical.calculate(contextFor(
                    new BigDecimal("25000.00"), new BigDecimal("1500.00"),
                    BigDecimal.ZERO, false, false));

            assertThat(result.getSurchargeAmount()).isEqualByComparingTo("3000.00");
            assertThat(result.getSubTotal()).isEqualByComparingTo("29500.00");
            assertThat(result.getTotalAmount()).isEqualByComparingTo("34810.00");
        }

        @Test
        @DisplayName("applies the Rs. 1,500 minimum when 12% would be less")
        void appliesMinimumSurcharge() {
            // 12% of 7,500 is 900, which is below the 1,500 floor, so the
            // consumables tray is still covered.
            PricingResult result = surgical.calculate(contextFor(
                    new BigDecimal("7500.00"), new BigDecimal("1500.00"),
                    BigDecimal.ZERO, false, false));

            assertThat(result.getSurchargeAmount()).isEqualByComparingTo("1500.00");
        }

        @Test
        @DisplayName("still honours the senior concession - surgery is clinically necessary")
        void concessionsStillApply() {
            PricingResult result = surgical.calculate(contextFor(
                    new BigDecimal("25000.00"), new BigDecimal("1500.00"),
                    BigDecimal.ZERO, false, true));

            assertThat(result.getDiscountPercentage()).isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("names the surcharge on the receipt so the patient can see why")
        void surchargeIsExplainedOnTheReceipt() {
            PricingResult result = surgical.calculate(contextFor(
                    new BigDecimal("25000.00"), new BigDecimal("1500.00"),
                    BigDecimal.ZERO, false, false));

            assertThat(result.getLines())
                    .anyMatch(line -> line.getDescription().contains("Sterilisation"));
        }
    }

    /* ================================================================== */
    @Nested
    @DisplayName("Cosmetic pricing")
    class Cosmetic {

        @Test
        @DisplayName("withholds the senior concession - cosmetic work is elective")
        void withholdsAgeConcessionFromSenior() {
            PricingResult result = cosmetic.calculate(contextFor(
                    new BigDecimal("28000.00"), new BigDecimal("2500.00"),
                    BigDecimal.ZERO, false, true));

            assertThat(result.getDiscountPercentage()).isEqualByComparingTo("0.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("withholds the child concession too")
        void withholdsAgeConcessionFromChild() {
            PricingResult result = cosmetic.calculate(contextFor(
                    new BigDecimal("28000.00"), new BigDecimal("2500.00"),
                    BigDecimal.ZERO, true, false));

            assertThat(result.getDiscountPercentage()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("but still honours a manually approved discount")
        void honoursManualDiscount() {
            PricingResult result = cosmetic.calculate(contextFor(
                    new BigDecimal("28000.00"), new BigDecimal("2500.00"),
                    new BigDecimal("15"), false, true));

            assertThat(result.getDiscountPercentage()).isEqualByComparingTo("15.00");
            assertThat(result.getDiscountReason()).contains("elective");
        }

        @Test
        @DisplayName("adds no surcharge")
        void addsNoSurcharge() {
            PricingResult result = cosmetic.calculate(contextFor(
                    new BigDecimal("28000.00"), new BigDecimal("2500.00"),
                    BigDecimal.ZERO, false, false));

            assertThat(result.getSurchargeAmount()).isEqualByComparingTo("0.00");
        }
    }

    /* ================================================================== */
    @Nested
    @DisplayName("Emergency pricing")
    class Emergency {

        @Test
        @DisplayName("adds no loading during core hours on a weekday")
        void noLoadingInCoreHours() {
            PricingContext context = PricingContext.builder()
                    .treatmentBasePrice(new BigDecimal("5000.00"))
                    .consultationFee(new BigDecimal("3000.00"))
                    .taxRate(ClinicConstants.VAT_RATE)
                    .appointmentDate(TestDataFactory.nextWeekday())
                    .appointmentTime(LocalTime.of(11, 0))
                    .treatmentName("Emergency Pain Relief")
                    .build();

            assertThat(emergency.calculate(context).getSurchargeAmount())
                    .isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("adds a 35% loading on the consultation fee after 17:00")
        void addsLoadingAfterHours() {
            // 35% of a 3,000 consultation = 1,050.00
            PricingContext context = PricingContext.builder()
                    .treatmentBasePrice(new BigDecimal("5000.00"))
                    .consultationFee(new BigDecimal("3000.00"))
                    .taxRate(ClinicConstants.VAT_RATE)
                    .appointmentDate(TestDataFactory.nextWeekday())
                    .appointmentTime(LocalTime.of(18, 30))
                    .treatmentName("Emergency Pain Relief")
                    .build();

            assertThat(emergency.calculate(context).getSurchargeAmount())
                    .isEqualByComparingTo("1050.00");
        }

        @Test
        @DisplayName("adds the loading before 09:00 as well")
        void addsLoadingEarlyMorning() {
            PricingContext context = PricingContext.builder()
                    .treatmentBasePrice(new BigDecimal("5000.00"))
                    .consultationFee(new BigDecimal("3000.00"))
                    .taxRate(ClinicConstants.VAT_RATE)
                    .appointmentDate(TestDataFactory.nextWeekday())
                    .appointmentTime(LocalTime.of(8, 0))
                    .treatmentName("Emergency Pain Relief")
                    .build();

            assertThat(emergency.calculate(context).getSurchargeAmount())
                    .isEqualByComparingTo("1050.00");
        }

        @Test
        @DisplayName("adds the loading at a weekend even during core hours")
        void addsLoadingAtWeekend() {
            PricingContext context = PricingContext.builder()
                    .treatmentBasePrice(new BigDecimal("5000.00"))
                    .consultationFee(new BigDecimal("3000.00"))
                    .taxRate(ClinicConstants.VAT_RATE)
                    .appointmentDate(TestDataFactory.nextSaturday())
                    .appointmentTime(LocalTime.of(11, 0))
                    .treatmentName("Emergency Pain Relief")
                    .build();

            assertThat(emergency.calculate(context).getSurchargeAmount())
                    .isEqualByComparingTo("1050.00");
        }
    }

    /* ================================================================== */
    @Nested
    @DisplayName("Rules shared by every strategy (the Template Method skeleton)")
    class SharedSkeleton {

        @ParameterizedTest(name = "{0} produces a total equal to taxable + VAT")
        @CsvSource({
            "STANDARD,  6500.00, 1500.00",
            "SURGICAL, 25000.00, 3000.00",
            "COSMETIC, 28000.00, 2500.00"
        })
        @DisplayName("the total always equals taxable amount plus VAT")
        void totalIsAlwaysTaxablePlusVat(String key, BigDecimal price, BigDecimal fee) {
            PricingStrategy strategy = switch (key) {
                case "SURGICAL" -> surgical;
                case "COSMETIC" -> cosmetic;
                default -> standard;
            };

            PricingResult result = strategy.calculate(
                    contextFor(price, fee, new BigDecimal("5"), false, false));

            assertThat(result.getTotalAmount())
                    .isEqualByComparingTo(result.getTaxableAmount().add(result.getTaxAmount()));
        }

        @Test
        @DisplayName("discount is applied BEFORE VAT, as Sri Lankan VAT law requires")
        void discountIsAppliedBeforeVat() {
            PricingResult result = standard.calculate(contextFor(
                    new BigDecimal("6500.00"), new BigDecimal("1500.00"),
                    new BigDecimal("10"), false, false));

            // If VAT were charged before the discount the taxable amount would
            // still be 8,000. It must be 7,200.
            assertThat(result.getTaxableAmount()).isEqualByComparingTo("7200.00");
            assertThat(result.getTaxAmount()).isEqualByComparingTo("1296.00");
        }

        @Test
        @DisplayName("every amount is rounded to exactly two decimal places")
        void everyAmountIsScaledToTwoPlaces() {
            // A price and a discount chosen to produce a recurring decimal.
            PricingResult result = standard.calculate(contextFor(
                    new BigDecimal("999.99"), new BigDecimal("333.33"),
                    new BigDecimal("7.5"), false, false));

            assertThat(result.getSubTotal().scale()).isEqualTo(2);
            assertThat(result.getDiscountAmount().scale()).isEqualTo(2);
            assertThat(result.getTaxAmount().scale()).isEqualTo(2);
            assertThat(result.getTotalAmount().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("produces itemised receipt lines, not just a total")
        void producesItemisedLines() {
            PricingResult result = standard.calculate(contextFor(
                    new BigDecimal("6500.00"), new BigDecimal("1500.00"),
                    new BigDecimal("10"), false, false));

            // consultation, treatment, discount, VAT
            assertThat(result.getLines()).hasSize(4);
            assertThat(result.getLines()).extracting("lineType")
                    .containsExactly("CHARGE", "CHARGE", "DISCOUNT", "TAX");
        }

        @Test
        @DisplayName("explains every adjustment in words for the receipt")
        void explainsItsWorking() {
            PricingResult result = surgical.calculate(contextFor(
                    new BigDecimal("25000.00"), new BigDecimal("1500.00"),
                    BigDecimal.ZERO, false, true));

            assertThat(result.getExplanations()).isNotEmpty();
            assertThat(String.join(" ", result.getExplanations()))
                    .contains("Surgical Pricing")
                    .contains("Sterilisation")
                    .contains("Senior citizen")
                    .contains("VAT");
        }

        @Test
        @DisplayName("a patient with no recorded date of birth gets no age concession")
        void unknownAgeGetsNoConcession() {
            PricingResult result = standard.calculate(contextFor(
                    new BigDecimal("6500.00"), new BigDecimal("1500.00"),
                    BigDecimal.ZERO, false, false));

            assertThat(result.getDiscountAmount()).isEqualByComparingTo("0.00");
        }
    }

    /* ------------------------------------------------------------------ */

    private PricingContext contextFor(BigDecimal treatmentPrice, BigDecimal consultationFee,
                                      BigDecimal discount, boolean minor, boolean senior) {
        return PricingContext.builder()
                .treatmentBasePrice(treatmentPrice)
                .consultationFee(consultationFee)
                .requestedDiscountPercentage(discount)
                .taxRate(ClinicConstants.VAT_RATE)
                .patientIsMinor(minor)
                .patientIsSeniorCitizen(senior)
                .appointmentDate(TestDataFactory.nextWeekday())
                .appointmentTime(LocalTime.of(10, 0))
                .treatmentName("Test Treatment")
                .treatmentCategory("Test")
                .build();
    }
}
