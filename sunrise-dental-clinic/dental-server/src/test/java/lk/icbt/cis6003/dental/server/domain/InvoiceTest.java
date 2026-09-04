package lk.icbt.cis6003.dental.server.domain;

import lk.icbt.cis6003.dental.common.enums.PaymentMethod;
import lk.icbt.cis6003.dental.common.enums.PaymentStatus;
import lk.icbt.cis6003.dental.server.exception.BusinessException;
import lk.icbt.cis6003.dental.server.exception.ErrorCode;
import lk.icbt.cis6003.dental.server.testsupport.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for money handling on the {@link Invoice} entity.
 *
 * <p>Over-payment is refused here, again by the service, and again by the
 * {@code chk_invoice_amount_paid} database constraint. Three layers is not
 * paranoia: a clinic's cash position is exactly the thing a defect must not be
 * able to corrupt, and each layer catches a different class of mistake -
 * a coding error, a malformed request, and a direct database edit.</p>
 */
@DisplayName("Invoice entity")
class InvoiceTest {

    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoice = new Invoice();
        invoice.setInvoiceNumber("INV-2026-000001");
        invoice.setAppointment(TestDataFactory.completedAppointment());
        invoice.setPatientName("Kamala Perera");
        invoice.setPatientAddress("No. 45, Galle Road, Colombo 03");
        invoice.setPatientContact("0771234567");
        invoice.setDentistName("Nimal Perera");
        invoice.setTreatmentName("Scaling and Polishing");
        invoice.setTotalAmount(new BigDecimal("9440.00"));
        invoice.setIssuedBy("reception");
    }

    @Test
    @DisplayName("a newly issued bill is pending, with the full amount outstanding")
    void newInvoiceIsPending() {
        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo("9440.00");
    }

    @Test
    @DisplayName("paying the full amount settles the bill and stamps the time")
    void fullPaymentSettlesTheBill() {
        invoice.applyPayment(new BigDecimal("9440.00"), PaymentMethod.CASH, null);

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo("0.00");
        assertThat(invoice.getPaidAt()).isNotNull();
        assertThat(invoice.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    @DisplayName("a part payment leaves the bill partially paid with the correct balance")
    void partPaymentLeavesABalance() {
        invoice.applyPayment(new BigDecimal("4000.00"), PaymentMethod.CARD, "AUTH-123");

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo("5440.00");
        assertThat(invoice.getPaidAt()).as("not yet settled, so no settlement time").isNull();
    }

    @Test
    @DisplayName("successive part payments accumulate and eventually settle the bill")
    void successivePaymentsAccumulate() {
        invoice.applyPayment(new BigDecimal("4000.00"), PaymentMethod.CASH, null);
        invoice.applyPayment(new BigDecimal("3000.00"), PaymentMethod.CASH, null);
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo("2440.00");

        invoice.applyPayment(new BigDecimal("2440.00"), PaymentMethod.CASH, null);
        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("an over-payment is refused, naming the balance actually due")
    void overPaymentIsRefused() {
        assertThatThrownBy(() ->
                invoice.applyPayment(new BigDecimal("99999.00"), PaymentMethod.CASH, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_EXCEEDS_BALANCE);

        assertThat(invoice.getAmountPaid())
                .as("a refused payment must leave the bill untouched")
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("a second payment cannot push the total over, either")
    void overPaymentAcrossTwoPaymentsIsRefused() {
        invoice.applyPayment(new BigDecimal("9000.00"), PaymentMethod.CASH, null);

        assertThatThrownBy(() ->
                invoice.applyPayment(new BigDecimal("1000.00"), PaymentMethod.CASH, null))
                .isInstanceOf(BusinessException.class);

        assertThat(invoice.getAmountPaid()).isEqualByComparingTo("9000.00");
    }

    @Test
    @DisplayName("a zero or negative payment is refused")
    void zeroOrNegativePaymentIsRefused() {
        assertThatThrownBy(() -> invoice.applyPayment(BigDecimal.ZERO, PaymentMethod.CASH, null))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() ->
                invoice.applyPayment(new BigDecimal("-500.00"), PaymentMethod.CASH, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("a null payment amount is refused rather than treated as zero")
    void nullPaymentIsRefused() {
        assertThatThrownBy(() -> invoice.applyPayment(null, PaymentMethod.CASH, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("an unpaid bill can be voided with a reason")
    void unpaidInvoiceCanBeCancelled() {
        invoice.cancel("Raised against the wrong appointment");

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(invoice.getRemarks()).isEqualTo("Raised against the wrong appointment");
    }

    @Test
    @DisplayName("a fully paid bill cannot be voided - the money has already changed hands")
    void paidInvoiceCannotBeCancelled() {
        invoice.applyPayment(new BigDecimal("9440.00"), PaymentMethod.CASH, null);

        assertThatThrownBy(() -> invoice.cancel("Changed my mind"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    @DisplayName("a cancelled bill cannot take a payment")
    void cancelledInvoiceCannotTakePayment() {
        invoice.cancel("Raised in error");

        assertThatThrownBy(() ->
                invoice.applyPayment(new BigDecimal("100.00"), PaymentMethod.CASH, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    @DisplayName("the balance never reports as negative")
    void balanceNeverGoesNegative() {
        invoice.applyPayment(new BigDecimal("9440.00"), PaymentMethod.CASH, null);

        assertThat(invoice.getBalanceDue()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("adding lines numbers them in order and links them back to the bill")
    void linesAreNumberedAndLinked() {
        invoice.addLine(new InvoiceLine("Consultation fee", 1,
                new BigDecimal("1500.00"), InvoiceLine.TYPE_CHARGE));
        invoice.addLine(new InvoiceLine("Scaling and Polishing", 1,
                new BigDecimal("6500.00"), InvoiceLine.TYPE_CHARGE));

        assertThat(invoice.getLines()).hasSize(2);
        assertThat(invoice.getLines().get(0).getLineNumber()).isEqualTo(1);
        assertThat(invoice.getLines().get(1).getLineNumber()).isEqualTo(2);
        assertThat(invoice.getLines().get(0).getInvoice()).isSameAs(invoice);
    }
}
