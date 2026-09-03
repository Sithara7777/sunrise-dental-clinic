package lk.icbt.cis6003.dental.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Input to "Calculate and Print Bill".
 *
 * <p>Only the discretionary parts of a bill are accepted from the user. The
 * consultation fee and treatment cost are read from the dentist and treatment
 * records on the server, never sent by the client - otherwise a tampered
 * client could invoice any amount it liked.</p>
 */
public class BillingRequest {

    @NotBlank(message = "Appointment number is required")
    private String appointmentNumber;

    /** Manual, discretionary discount. Capped so a slip cannot zero a bill. */
    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    @DecimalMax(value = "50.00", message = "Discount cannot exceed 50%")
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Size(max = 200, message = "Discount reason must not exceed 200 characters")
    private String discountReason;

    @Size(max = 300, message = "Remarks must not exceed 300 characters")
    private String remarks;

    public BillingRequest() {
        // required by Jackson
    }

    public BillingRequest(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public String getDiscountReason() {
        return discountReason;
    }

    public void setDiscountReason(String discountReason) {
        this.discountReason = discountReason;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Override
    public String toString() {
        return "BillingRequest{" + appointmentNumber + ", discount=" + discountPercentage + "%}";
    }
}
