package lk.icbt.cis6003.dental.client.ui;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.common.dto.report.ReportDto;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.print.PrinterException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * <b>One window displays every report.</b>
 *
 * <p>It can, because the server returns every report in the same envelope:
 * a title, the column headings, a grid of pre-formatted cells and a set of
 * named totals. This class never mentions a report by name and never knows a
 * row type &mdash; so a sixth report written on the server appears here with no
 * change to this client and no new client release.</p>
 *
 * <p>The report list itself is fetched from the server for the same reason.</p>
 */
public class ReportWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final ClinicApiClient api;

    private final JComboBox<ReportChoice> reportCombo = new JComboBox<>();
    private final JTextField fromField = new JTextField(10);
    private final JTextField toField = new JTextField(10);
    private final JButton generateButton = new JButton("Generate");
    private final JButton printButton = new JButton("Print");

    private final DefaultTableModel tableModel = new DefaultTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextArea summaryArea = new JTextArea(7, 30);
    private final JLabel titleLabel = UiUtils.subHeading("Select a report and click Generate");
    private final JLabel statusLabel = new JLabel(" ");

    public ReportWindow(Window owner, ClinicApiClient api, String preselectedReportCode) {
        super("Management Reports - Sunrise Dental Clinic");
        this.api = api;

        fromField.setText(LocalDate.now().withDayOfMonth(1).toString());
        toField.setText(LocalDate.now().toString());

        buildUi();
        loadReportList(preselectedReportCode);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(1040, 620));
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));

        /* ---------------- controls ---------------- */
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        UiUtils.titleBorder(controls, "  Report and period  ");
        controls.add(new JLabel("Report:"));
        controls.add(reportCombo);
        controls.add(new JLabel("From:"));
        controls.add(fromField);
        controls.add(new JLabel("To:"));
        controls.add(toField);
        controls.add(generateButton);
        controls.add(printButton);

        fromField.setToolTipText("yyyy-MM-dd");
        toField.setToolTipText("yyyy-MM-dd");
        reportCombo.setPreferredSize(new Dimension(280, reportCombo.getPreferredSize().height));

        /* ---------------- data ---------------- */
        UiUtils.styleTable(table);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(UiUtils.BORDER));

        summaryArea.setEditable(false);
        summaryArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        summaryArea.setBackground(UiUtils.SLATE_BG);
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiUtils.BORDER), "  Summary  "));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, summaryScroll);
        split.setResizeWeight(0.72);
        split.setBorder(null);

        /* ---------------- header and footer ---------------- */
        JPanel header = new JPanel(new BorderLayout());
        header.add(controls, BorderLayout.NORTH);
        header.add(titleLabel, BorderLayout.SOUTH);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 2, 4, 2));

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(UiUtils.SLATE_MUTED);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(statusLabel, BorderLayout.WEST);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel closeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        closeRow.add(close);
        footer.add(closeRow, BorderLayout.EAST);

        generateButton.addActionListener(e -> generate());
        printButton.addActionListener(e -> printTable());
        printButton.setEnabled(false);

        root.add(header, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    /* ------------------------------------------------------------------ */
    /* The report list comes from the server                               */
    /* ------------------------------------------------------------------ */

    private void loadReportList(String preselectedCode) {
        statusLabel.setText("Loading the list of reports...");

        new SwingWorker<List<Map<String, String>>, Void>() {
            @Override
            protected List<Map<String, String>> doInBackground() throws ApiException {
                return api.listReports();
            }

            @Override
            protected void done() {
                try {
                    DefaultComboBoxModel<ReportChoice> model = new DefaultComboBoxModel<>();
                    for (Map<String, String> descriptor : get()) {
                        model.addElement(new ReportChoice(
                                descriptor.get("code"),
                                descriptor.get("title"),
                                descriptor.get("description")));
                    }
                    reportCombo.setModel(model);

                    if (preselectedCode != null) {
                        for (int i = 0; i < model.getSize(); i++) {
                            if (preselectedCode.equals(model.getElementAt(i).code())) {
                                reportCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                    statusLabel.setText(model.getSize() + " report(s) available.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    statusLabel.setText("Could not load the report list.");
                    UiUtils.showError(ReportWindow.this, "Reports unavailable",
                                      ex.getCause().getMessage());
                }
            }
        }.execute();
    }

    /** Runs a specific report with a specific period - used by "Today's Schedule". */
    public void runReport(LocalDate from, LocalDate to) {
        fromField.setText(from.toString());
        toField.setText(to.toString());
        generate();
    }

    private void generate() {
        ReportChoice choice = (ReportChoice) reportCombo.getSelectedItem();
        if (choice == null) {
            statusLabel.setText("Select a report first.");
            return;
        }

        LocalDate from = parse(fromField.getText());
        LocalDate to = parse(toField.getText());
        if (from == null || to == null) {
            statusLabel.setText("Enter both dates as yyyy-MM-dd.");
            return;
        }

        generateButton.setEnabled(false);
        printButton.setEnabled(false);
        statusLabel.setText("Generating " + choice.title() + "...");

        new SwingWorker<ReportDto<Object>, Void>() {
            @Override
            protected ReportDto<Object> doInBackground() throws ApiException {
                return api.runReport(choice.code(), from, to);
            }

            @Override
            protected void done() {
                generateButton.setEnabled(true);
                try {
                    render(get());
                    printButton.setEnabled(true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    statusLabel.setText("The report could not be generated.");
                    UiUtils.showError(ReportWindow.this, "Report failed",
                                      ex.getCause().getMessage());
                }
            }
        }.execute();
    }

    /**
     * Renders any report.
     *
     * <p>Uses {@code getColumnHeaders()} and {@code getCells()} only - no field
     * of any row type is ever named here.</p>
     */
    private void render(ReportDto<Object> report) {
        titleLabel.setText(report.getTitle());

        List<String> headers = report.getColumnHeaders();
        tableModel.setColumnIdentifiers(headers.toArray());
        tableModel.setRowCount(0);

        for (List<String> row : report.getCells()) {
            tableModel.addRow(row.toArray());
        }
        UiUtils.autoSizeColumns(table);

        StringBuilder summary = new StringBuilder();
        summary.append(report.getDescription()).append("\n\n");
        summary.append("Period : ").append(report.getFromDate())
               .append("  to  ").append(report.getToDate()).append('\n');
        summary.append("Run at : ").append(report.getGeneratedAt())
               .append("  by ").append(report.getGeneratedBy()).append("\n\n");

        int widest = report.getSummary().keySet().stream()
                .mapToInt(String::length).max().orElse(20);
        for (Map.Entry<String, Object> entry : report.getSummary().entrySet()) {
            summary.append(String.format("%-" + (widest + 2) + "s : %s%n",
                                         entry.getKey(), entry.getValue()));
        }
        summaryArea.setText(summary.toString());
        summaryArea.setCaretPosition(0);

        statusLabel.setText(report.getRowCount() + " row(s).");
    }

    private void printTable() {
        try {
            table.print(JTable.PrintMode.FIT_WIDTH,
                        new java.text.MessageFormat(titleLabel.getText()),
                        new java.text.MessageFormat("Page {0} - Sunrise Dental Clinic"));
        } catch (PrinterException ex) {
            UiUtils.showError(this, "Could not print",
                    "The report could not be sent to the printer.\n\nTechnical detail: "
                            + ex.getMessage());
        }
    }

    private LocalDate parse(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /** One entry in the report drop-down, as advertised by the server. */
    private record ReportChoice(String code, String title, String description) {
        @Override
        public String toString() {
            return title;
        }
    }
}
