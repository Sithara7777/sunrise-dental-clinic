package lk.icbt.cis6003.dental.common.dto.report;

import java.math.BigDecimal;

/**
 * One treatment's line in the Treatment Popularity &amp; Yield report.
 *
 * <p>Decision it supports: which treatments to promote and which to retire.
 * A treatment with high volume but low revenue share is consuming chair time
 * that a higher-yield treatment could use.</p>
 */
public class TreatmentPopularityRow {

    private String treatmentCode;
    private String treatmentName;
    private String category;
    private long timesPerformed;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal averageRevenue = BigDecimal.ZERO;
    private BigDecimal revenueSharePercentage = BigDecimal.ZERO;

    public TreatmentPopularityRow() {
        // required by Jackson
    }

    public String getTreatmentCode() {
        return treatmentCode;
    }

    public void setTreatmentCode(String treatmentCode) {
        this.treatmentCode = treatmentCode;
    }

    public String getTreatmentName() {
        return treatmentName;
    }

    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getTimesPerformed() {
        return timesPerformed;
    }

    public void setTimesPerformed(long timesPerformed) {
        this.timesPerformed = timesPerformed;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getAverageRevenue() {
        return averageRevenue;
    }

    public void setAverageRevenue(BigDecimal averageRevenue) {
        this.averageRevenue = averageRevenue;
    }

    public BigDecimal getRevenueSharePercentage() {
        return revenueSharePercentage;
    }

    public void setRevenueSharePercentage(BigDecimal revenueSharePercentage) {
        this.revenueSharePercentage = revenueSharePercentage;
    }
}
