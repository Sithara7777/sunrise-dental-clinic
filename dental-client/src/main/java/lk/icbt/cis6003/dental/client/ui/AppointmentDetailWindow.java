package lk.icbt.cis6003.dental.client.ui;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClientSession;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.common.dto.AppointmentDto;
import lk.icbt.cis6003.dental.common.dto.StatusUpdateRequest;
import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;

/**
 * Requirement 3 - "Show complete patient and appointment information."
 *
 * <p>Also the launch point for requirement 4: a completed visit can be billed
 * straight from here, using the Facade call that completes and bills in one
 * transaction.</p>
 */
public class AppointmentDetailWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final ClinicApiClient api;
    private AppointmentDto appointment;

    private final JPanel detailPanel = new JPanel(new GridBagLayout());
    private final JLabel statusBadge = new JLabel();
    private final JComboBox<AppointmentStatus> statusCombo = new JComboBox<>();
    private final JButton updateStatusButton = new JButton("Update status");
    private final JButton billButton = new JButton("Calculate & print bill");

    public AppointmentDetailWindow(Window owner, ClinicApiClient api, AppointmentDto appointment) {
        super("Appointment " + appointment.getAppointmentNumber());
        this.api = api;
        this.appointment = appointment;

        buildUi();
        populate();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(680, 560));
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));

        /* ---------------- header ---------------- */
        JPanel header = new JPanel(new BorderLayout());
        JLabel number = UiUtils.heading(appointment.getAppointmentNumber());
        number.setFont(new Font("Consolas", Font.BOLD, 20));
        header.add(number, BorderLayout.WEST);

        statusBadge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusBadge.setOpaque(true);
        statusBadge.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        header.add(statusBadge, BorderLayout.EAST);

        /* ---------------- details ---------------- */
        UiUtils.titleBorder(detailPanel, "  Complete appointment record  ");

        /* ---------------- actions ---------------- */
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        UiUtils.titleBorder(actions, "  What happened?  ");
        actions.add(new JLabel("Mark as:"));
        actions.add(statusCombo);
        actions.add(updateStatusButton);
        actions.add(billButton);

        billButton.setEnabled(ClientSession.getInstance().canHandleBilling());

        updateStatusButton.addActionListener(e -> updateStatus());
        billButton.addActionListener(e ->
                new BillingWindow(this, api, appointment.getAppointmentNumber()).setVisible(true));

        JPanel south = new JPanel(new BorderLayout());
        south.add(actions, BorderLayout.CENTER);

        JPanel closeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        closeRow.add(close);
        south.add(closeRow, BorderLayout.SOUTH);

        root.add(header, BorderLayout.NORTH);
        root.add(detailPanel, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void populate() {
        detailPanel.removeAll();
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        row = detail(gc, row, "Patient number", appointment.getPatientCode());
        row = detail(gc, row, "Patient name", appointment.getPatientName());
        row = detail(gc, row, "Address", appointment.getAddress());
        row = detail(gc, row, "Contact number", appointment.getContactNumber());
        row = detail(gc, row, "E-mail",
                UiUtils.nullSafe(appointment.getPatientEmail()).isEmpty()
                        ? "Not supplied" : appointment.getPatientEmail());
        row = detail(gc, row, " ", " ");
        row = detail(gc, row, "Dentist", "Dr " + appointment.getDentistName()
                + " (" + UiUtils.nullSafe(appointment.getDentistSpecialization()) + ")");
        row = detail(gc, row, "Treatment", appointment.getTreatmentName()
                + "  -  " + UiUtils.moneyWithCurrency(appointment.getTreatmentPrice()));
        row = detail(gc, row, "Appointment date", String.valueOf(appointment.getAppointmentDate()));
        row = detail(gc, row, "Appointment time", appointment.getAppointmentTime()
                + " to " + appointment.getAppointmentEndTime()
                + "  (" + appointment.getDurationMinutes() + " minutes)");
        row = detail(gc, row, "Status",
                appointment.getStatus() == null ? "" : appointment.getStatus().getDisplayName());
        row = detail(gc, row, "Notes",
                UiUtils.nullSafe(appointment.getNotes()).isEmpty() ? "None" : appointment.getNotes());
        row = detail(gc, row, " ", " ");
        row = detail(gc, row, "Registered by", appointment.getCreatedBy());
        row = detail(gc, row, "Bill",
                appointment.isInvoiced()
                        ? appointment.getInvoiceNumber() + " has been issued"
                        : "Not yet billed");

        /* Only transitions the server would actually accept are offered. */
        statusCombo.removeAllItems();
        if (appointment.getStatus() != null) {
            for (AppointmentStatus next : appointment.getStatus().allowedTransitions()) {
                statusCombo.addItem(next);
            }
        }
        boolean changeable = statusCombo.getItemCount() > 0;
        statusCombo.setEnabled(changeable);
        updateStatusButton.setEnabled(changeable);

        billButton.setEnabled(ClientSession.getInstance().canHandleBilling()
                && !appointment.isInvoiced()
                && appointment.getStatus() != AppointmentStatus.CANCELLED
                && appointment.getStatus() != AppointmentStatus.NO_SHOW);

        paintBadge();

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private int detail(GridBagConstraints gc, int row, String label, String value) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(UiUtils.SLATE_MUTED);
        labelComponent.setFont(new Font("Segoe UI", Font.BOLD, 12));
        detailPanel.add(labelComponent, gc);

        gc.gridx = 1; gc.weightx = 1;
        JLabel valueComponent = new JLabel("<html>" + UiUtils.nullSafe(value) + "</html>");
        valueComponent.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailPanel.add(valueComponent, gc);
        return row + 1;
    }

    private void paintBadge() {
        AppointmentStatus status = appointment.getStatus();
        if (status == null) {
            statusBadge.setText("");
            return;
        }
        statusBadge.setText(status.getDisplayName());
        switch (status) {
            case COMPLETED -> setBadgeColours(new Color(0xDB, 0xF1, 0xE3), UiUtils.GREEN);
            case CANCELLED -> setBadgeColours(new Color(0xEE, 0xF2, 0xF6), UiUtils.SLATE_MUTED);
            case NO_SHOW   -> setBadgeColours(new Color(0xFB, 0xDE, 0xDC), UiUtils.RED);
            case CONFIRMED -> setBadgeColours(UiUtils.TEAL_LIGHT, UiUtils.TEAL);
            default        -> setBadgeColours(new Color(0xDD, 0xE8, 0xF9), new Color(0x1F, 0x5A, 0xA8));
        }
    }

    private void setBadgeColours(Color background, Color foreground) {
        statusBadge.setBackground(background);
        statusBadge.setForeground(foreground);
    }

    private void updateStatus() {
        AppointmentStatus target = (AppointmentStatus) statusCombo.getSelectedItem();
        if (target == null) {
            return;
        }

        String reason = null;
        if (target == AppointmentStatus.CANCELLED) {
            reason = JOptionPane.showInputDialog(this,
                    "Why is this appointment being cancelled?", "Cancellation reason",
                    JOptionPane.QUESTION_MESSAGE);
            if (reason == null) {
                return;     // the user thought better of it
            }
        }

        StatusUpdateRequest request = new StatusUpdateRequest(target);
        request.setReason(reason);
        updateStatusButton.setEnabled(false);

        new SwingWorker<AppointmentDto, Void>() {
            @Override
            protected AppointmentDto doInBackground() throws ApiException {
                return api.updateStatus(appointment.getAppointmentNumber(), request);
            }

            @Override
            protected void done() {
                updateStatusButton.setEnabled(true);
                try {
                    // Re-read the whole record: completing a visit changes what
                    // else is possible, and stale buttons mislead.
                    appointment = api.findAppointment(get().getAppointmentNumber());
                    populate();
                    UiUtils.showSuccess(AppointmentDetailWindow.this, "Status updated",
                            "Appointment " + appointment.getAppointmentNumber() + " is now "
                                    + appointment.getStatus().getDisplayName() + ".");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    UiUtils.showError(AppointmentDetailWindow.this, "Status not changed",
                                      ex.getCause().getMessage());
                } catch (ApiException ex) {
                    UiUtils.showError(AppointmentDetailWindow.this, "Could not reload",
                                      ex.getMessage());
                }
            }
        }.execute();
    }
}
