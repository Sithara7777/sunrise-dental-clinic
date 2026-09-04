package lk.icbt.cis6003.dental.common.dto.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The at-a-glance figures on the landing page of both the web UI and the
 * desktop client.
 *
 * <p>Everything here is a leading indicator the clinic manager can act on the
 * same morning: today's load, money still owed, and the no-show rate that
 * directly wastes chair time.</p>
 */
public class DashboardStatsDto {

    private long todayAppointments;
    private long todayCompleted;
    private long todayPending;
    private long todayCancelled;
    private long upcomingSevenDays;

    private long totalPatients;
    private long newPatientsThisMonth;
    private long activeDentists;
    private long activeTreatments;

    private BigDecimal todayRevenue = BigDecimal.ZERO;
    private BigDecimal monthRevenue = BigDecimal.ZERO;
    private BigDecimal outstandingBalance = BigDecimal.ZERO;
    private long outstandingInvoiceCount;

    private BigDecimal noShowRate = BigDecimal.ZERO;
    private BigDecimal chairUtilisationToday = BigDecimal.ZERO;

    /** Last 7 days of appointment counts, for the sparkline on the dashboard. */
    private List<TrendPoint> weeklyTrend = new ArrayList<>();

    public DashboardStatsDto() {
        // required by Jackson
    }

    public long getTodayAppointments() {
        return todayAppointments;
    }

    public void setTodayAppointments(long todayAppointments) {
        this.todayAppointments = todayAppointments;
    }

    public long getTodayCompleted() {
        return todayCompleted;
    }

    public void setTodayCompleted(long todayCompleted) {
        this.todayCompleted = todayCompleted;
    }

    public long getTodayPending() {
        return todayPending;
    }

    public void setTodayPending(long todayPending) {
        this.todayPending = todayPending;
    }

    public long getTodayCancelled() {
        return todayCancelled;
    }

    public void setTodayCancelled(long todayCancelled) {
        this.todayCancelled = todayCancelled;
    }

    public long getUpcomingSevenDays() {
        return upcomingSevenDays;
    }

    public void setUpcomingSevenDays(long upcomingSevenDays) {
        this.upcomingSevenDays = upcomingSevenDays;
    }

    public long getTotalPatients() {
        return totalPatients;
    }

    public void setTotalPatients(long totalPatients) {
        this.totalPatients = totalPatients;
    }

    public long getNewPatientsThisMonth() {
        return newPatientsThisMonth;
    }

    public void setNewPatientsThisMonth(long newPatientsThisMonth) {
        this.newPatientsThisMonth = newPatientsThisMonth;
    }

    public long getActiveDentists() {
        return activeDentists;
    }

    public void setActiveDentists(long activeDentists) {
        this.activeDentists = activeDentists;
    }

    public long getActiveTreatments() {
        return activeTreatments;
    }

    public void setActiveTreatments(long activeTreatments) {
        this.activeTreatments = activeTreatments;
    }

    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(BigDecimal todayRevenue) {
        this.todayRevenue = todayRevenue;
    }

    public BigDecimal getMonthRevenue() {
        return monthRevenue;
    }

    public void setMonthRevenue(BigDecimal monthRevenue) {
        this.monthRevenue = monthRevenue;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public long getOutstandingInvoiceCount() {
        return outstandingInvoiceCount;
    }

    public void setOutstandingInvoiceCount(long outstandingInvoiceCount) {
        this.outstandingInvoiceCount = outstandingInvoiceCount;
    }

    public BigDecimal getNoShowRate() {
        return noShowRate;
    }

    public void setNoShowRate(BigDecimal noShowRate) {
        this.noShowRate = noShowRate;
    }

    public BigDecimal getChairUtilisationToday() {
        return chairUtilisationToday;
    }

    public void setChairUtilisationToday(BigDecimal chairUtilisationToday) {
        this.chairUtilisationToday = chairUtilisationToday;
    }

    public List<TrendPoint> getWeeklyTrend() {
        return weeklyTrend;
    }

    public void setWeeklyTrend(List<TrendPoint> weeklyTrend) {
        this.weeklyTrend = weeklyTrend == null ? new ArrayList<>() : new ArrayList<>(weeklyTrend);
    }

    /** A single (label, value) pair on the dashboard trend chart. */
    public static class TrendPoint {

        private String label;
        private long value;

        public TrendPoint() {
            // required by Jackson
        }

        public TrendPoint(String label, long value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }
}
