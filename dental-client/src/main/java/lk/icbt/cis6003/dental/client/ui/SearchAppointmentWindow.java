package lk.icbt.cis6003.dental.client.ui;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.common.dto.AppointmentDto;
import lk.icbt.cis6003.dental.common.dto.PageResponse;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutionException;

/**
 * Requirement 3 - "Display Appointment Details. Search using the appointment
 * number."
 *
 * <p>A separate window rather than a panel inside the main frame, so a
 * receptionist can leave a search open while working elsewhere &mdash; the
 * "separate UI windows" the assessment criteria call for.</p>
 *
 * <p>Searching by name or telephone number is offered alongside the appointment
 * number because patients routinely arrive having lost the slip it was written
 * on.</p>
 */
public class SearchAppointmentWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {
        "Appointment No", "Date", "Time", "Patient", "Contact", "Dentist", "Treatment", "Status"
    };

    private final ClinicApiClient api;

    private final JTextField termField = new JTextField(26);
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[] {
        "All statuses", "SCHEDULED", "CONFIRMED", "COMPLETED", "CANCELLED", "NO_SHOW"
    });
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;   // a results grid is for reading, not editing
        }
    };
    private final JTable resultsTable = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel("Enter a search term and press Search.");
    private final JButton searchButton = new JButton("Search");
    private final JButton openButton = new JButton("Open selected");

    public SearchAppointmentWindow(JFrame owner, ClinicApiClient api) {
        super("Find Appointment - Sunrise Dental Clinic");
        this.api = api;

        buildUi();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(940, 520));
        setLocationRelativeTo(owner);

        search();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));

        /* ---------------- search bar ---------------- */
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        UiUtils.titleBorder(searchPanel, "  Search  ");
        searchPanel.add(new JLabel("Appointment number, patient name or telephone:"));
        searchPanel.add(termField);
        searchPanel.add(new JLabel("Status:"));
        searchPanel.add(statusCombo);
        searchPanel.add(searchButton);

        termField.setToolTipText("For example APT-2026-000137, Perera, or 0771234567");

        /* ---------------- results ---------------- */
        UiUtils.styleTable(resultsTable);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelected();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(resultsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UiUtils.BORDER));

        /* ---------------- footer ---------------- */
        JPanel footer = new JPanel(new BorderLayout());
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(UiUtils.SLATE_MUTED);
        footer.add(statusLabel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttons.add(closeButton);
        buttons.add(openButton);
        footer.add(buttons, BorderLayout.EAST);

        searchButton.addActionListener(e -> search());
        termField.addActionListener(e -> search());
        statusCombo.addActionListener(e -> search());
        openButton.addActionListener(e -> openSelected());

        root.add(searchPanel, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void search() {
        String term = termField.getText().trim();
        String status = statusCombo.getSelectedIndex() == 0
                ? null : (String) statusCombo.getSelectedItem();

        searchButton.setEnabled(false);
        statusLabel.setText("Searching...");

        new SwingWorker<PageResponse<AppointmentDto>, Void>() {
            @Override
            protected PageResponse<AppointmentDto> doInBackground() throws ApiException {
                return api.searchAppointments(term, status, null, null, 0, 100);
            }

            @Override
            protected void done() {
                searchButton.setEnabled(true);
                try {
                    PageResponse<AppointmentDto> page = get();
                    populate(page);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    statusLabel.setText("Search failed.");
                    UiUtils.showError(SearchAppointmentWindow.this, "Search failed",
                                      ex.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void populate(PageResponse<AppointmentDto> page) {
        tableModel.setRowCount(0);
        for (AppointmentDto a : page.getContent()) {
            tableModel.addRow(new Object[] {
                a.getAppointmentNumber(),
                a.getAppointmentDate(),
                a.getAppointmentTime(),
                a.getPatientName(),
                a.getContactNumber(),
                a.getDentistName() == null ? "" : "Dr " + a.getDentistName(),
                a.getTreatmentName(),
                a.getStatus() == null ? "" : a.getStatus().getDisplayName()
            });
        }
        UiUtils.autoSizeColumns(resultsTable);

        statusLabel.setText(page.getTotalElements() + " appointment(s) found"
                + (page.getTotalElements() > page.getContent().size()
                   ? " - showing the first " + page.getContent().size() : "")
                + ".  Double-click a row to open it.");
    }

    private void openSelected() {
        int row = resultsTable.getSelectedRow();
        if (row < 0) {
            UiUtils.showInfo(this, "Nothing selected",
                             "Select an appointment in the list first.");
            return;
        }

        String appointmentNumber = String.valueOf(tableModel.getValueAt(row, 0));

        new SwingWorker<AppointmentDto, Void>() {
            @Override
            protected AppointmentDto doInBackground() throws ApiException {
                return api.findAppointment(appointmentNumber);
            }

            @Override
            protected void done() {
                try {
                    new AppointmentDetailWindow(SearchAppointmentWindow.this, api, get())
                            .setVisible(true);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    UiUtils.showError(SearchAppointmentWindow.this,
                                      "Could not open the appointment", ex.getCause().getMessage());
                }
            }
        }.execute();
    }
}
