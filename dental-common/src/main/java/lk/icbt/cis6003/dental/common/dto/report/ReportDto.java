package lk.icbt.cis6003.dental.common.dto.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envelope shared by every report the system produces.
 *
 * <p>This is the return type of the Template Method report skeleton: the
 * abstract generator fills in the metadata, the concrete subclass only
 * supplies rows and summary figures. Because all five reports share one
 * envelope, the web UI needs a single report table fragment and the Swing
 * client needs a single report window.</p>
 *
 * @param <R> the row type of this particular report
 */
public class ReportDto<R> {

    private String reportCode;
    private String title;
    private String description;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private List<String> columnHeaders = new ArrayList<>();
    private List<R> rows = new ArrayList<>();

    /**
     * The same rows, pre-formatted as display strings aligned with
     * {@link #columnHeaders}.
     *
     * <p>Carrying both shapes is deliberate. {@code rows} keeps the typed data
     * for any consumer that wants to compute with it; {@code cells} lets a
     * consumer <em>render</em> any report without knowing its row type. That is
     * what allows one Thymeleaf table and one Swing {@code JTable} to display
     * all five reports - and the sixth, when it is written - with no
     * report-specific code in either user interface.</p>
     */
    private List<List<String>> cells = new ArrayList<>();

    /** Named totals / KPIs rendered above the table, e.g. "Total revenue". */
    private Map<String, Object> summary = new LinkedHashMap<>();

    public ReportDto() {
        // required by Jackson
    }

    public String getReportCode() {
        return reportCode;
    }

    public void setReportCode(String reportCode) {
        this.reportCode = reportCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public List<String> getColumnHeaders() {
        return columnHeaders;
    }

    public void setColumnHeaders(List<String> columnHeaders) {
        this.columnHeaders = columnHeaders == null ? new ArrayList<>() : new ArrayList<>(columnHeaders);
    }

    public List<R> getRows() {
        return rows;
    }

    public void setRows(List<R> rows) {
        this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
    }

    public List<List<String>> getCells() {
        return cells;
    }

    public void setCells(List<List<String>> cells) {
        this.cells = cells == null ? new ArrayList<>() : new ArrayList<>(cells);
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Object> summary) {
        this.summary = summary == null ? new LinkedHashMap<>() : new LinkedHashMap<>(summary);
    }

    public void addSummary(String key, Object value) {
        this.summary.put(key, value);
    }

    public int getRowCount() {
        return rows == null ? 0 : rows.size();
    }

    public boolean isEmpty() {
        return getRowCount() == 0;
    }

    @Override
    public String toString() {
        return title + " (" + getRowCount() + " rows)";
    }
}
