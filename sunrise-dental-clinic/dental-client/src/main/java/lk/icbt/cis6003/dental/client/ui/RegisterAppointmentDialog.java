package lk.icbt.cis6003.dental.client.ui;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.common.dto.AppointmentDto;
import lk.icbt.cis6003.dental.common.dto.AppointmentRequest;
import lk.icbt.cis6003.dental.common.dto.DentistDto;
import lk.icbt.cis6003.dental.common.dto.SlotDto;
import lk.icbt.cis6003.dental.common.dto.TreatmentDto;
import lk.icbt.cis6003.dental.common.enums.Gender;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Requirement 2 - "Register New Appointment" - as a desktop form.
 *
 * <p><b>There is no field for the appointment number.</b> The server issues it.
 * Letting a receptionist type it is precisely how the clinic's paper system
 * produced duplicates, so the field simply does not exist to be typed into.</p>
 *
 * <p>The reference lists and the free slots arrive in a single call to
 * {@code /api/v1/booking-form-data} - the Facade operation - so opening this
 * dialog costs one network round trip rather than three.</p>
 */
public class RegisterAppointmentDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final ClinicApiClient api;

    private final JTextField patientCodeField = new JTextField(16);
    private final JTextField patientNameField = new JTextField(24);
    private final JTextField addressField = new JTextField(24);
    private final JTextField contactField = new JTextField(16);
    private final JTextField emailField = new JTextField(20);
    private final JTextField nicField = new JTextField(16);
    private final JTextField dobField = new JTextField(12);
    private final JComboBox<Gender> genderCombo = new JComboBox<>(Gender.values());

    private final JComboBox<DentistDto> dentistCombo = new JComboBox<>();
    private final JComboBox<TreatmentDto> treatmentCombo = new JComboBox<>();
    private final JTextField dateField = new JTextField(12);
    private final JComboBox<String> timeCombo = new JComboBox<>();
    private final JTextArea notesArea = new JTextArea(2, 24);

    private final JLabel statusLabel = new JLabel(" ");
    private final JButton checkSlotsButton = new JButton("Check availability");
    private final JButton saveButton = new JButton("Save appointment");
    private final JButton cancelButton = new JButton("Cancel");

    private AppointmentDto createdAppointment;

    public RegisterAppointmentDialog(Window owner, ClinicApiClient api) {
        super(owner, "Register New Appointment", ModalityType.APPLICATION_MODAL);
        this.api = api;

        dateField.setText(LocalDate.now().plusDays(1).toString());
        genderCombo.setSelectedItem(Gender.UNSPECIFIED);

        buildUi();
        loadReferenceData();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(saveButton);
        pack();
        setMinimumSize(new Dimension(720, getHeight()));
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));

        JLabel banner = new JLabel("<html><b>The appointment number is issued automatically once "
                + "you save.</b><br>Leave the patient number blank for a new patient &mdash; they "
                + "will be registered at the same time.</html>");
        banner.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        banner.setForeground(UiUtils.SLATE_MUTED);
        banner.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.add(buildPatientPanel(), BorderLayout.NORTH);
        form.add(buildBookingPanel(), BorderLayout.CENTER);

        statusLabel.setForeground(UiUtils.RED);
        statusLabel.setPreferredSize(new Dimension(600, 40));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        buttons.add(checkSlotsButton);
        buttons.add(cancelButton);
        buttons.add(saveButton);

        JPanel south = new JPanel(new BorderLayout());
        south.add(statusLabel, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        checkSlotsButton.addActionListener(e -> loadSlots());
        cancelButton.addActionListener(e -> dispose());
        saveButton.addActionListener(e -> save());

        root.add(banner, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildPatientPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        UiUtils.titleBorder(panel, "  Patient details  ");

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 4, 3, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addField(panel, gc, row, 0, "Existing patient no", patientCodeField);
        addField(panel, gc, row, 2, "Patient name *", patientNameField);

        row++;
        gc.gridx = 0; gc.gridy = row;
        panel.add(new JLabel("Address *"), gc);
        gc.gridx = 1; gc.gridwidth = 3; gc.weightx = 1;
        panel.add(addressField, gc);
        gc.gridwidth = 1; gc.weightx = 0;

        row++;
        addField(panel, gc, row, 0, "Contact number *", contactField);
        addField(panel, gc, row, 2, "E-mail", emailField);

        row++;
        addField(panel, gc, row, 0, "NIC", nicField);
        addField(panel, gc, row, 2, "Date of birth", dobField);

        row++;
        gc.gridx = 0; gc.gridy = row;
        panel.add(new JLabel("Gender"), gc);
        gc.gridx = 1;
        panel.add(genderCombo, gc);

        gc.gridx = 2; gc.gridwidth = 2;
        JLabel hint = UiUtils.muted("Date of birth drives the senior / child concession");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(hint, gc);

        dobField.setToolTipText("yyyy-MM-dd, for example 1958-03-14");
        contactField.setToolTipText("0771234567 or +94771234567");
        patientCodeField.setToolTipText("PAT-000042. Leave blank to register a new patient.");

        return panel;
    }

    private JPanel buildBookingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        UiUtils.titleBorder(panel, "  Appointment details  ");

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 4, 3, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gc.gridx = 0; gc.gridy = row;
        panel.add(new JLabel("Dentist *"), gc);
        gc.gridx = 1; gc.weightx = 1;
        panel.add(dentistCombo, gc);
        gc.weightx = 0;

        gc.gridx = 2;
        panel.add(new JLabel("Treatment *"), gc);
        gc.gridx = 3; gc.weightx = 1;
        panel.add(treatmentCombo, gc);
        gc.weightx = 0;

        row++;
        gc.gridx = 0; gc.gridy = row;
        panel.add(new JLabel("Date *"), gc);
        gc.gridx = 1;
        panel.add(dateField, gc);

        gc.gridx = 2;
        panel.add(new JLabel("Time *"), gc);
        gc.gridx = 3;
        panel.add(timeCombo, gc);

        row++;
        gc.gridx = 0; gc.gridy = row;
        panel.add(new JLabel("Notes"), gc);
        gc.gridx = 1; gc.gridwidth = 3; gc.weightx = 1;
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(notesArea), gc);

        dateField.setToolTipText("yyyy-MM-dd");
        timeCombo.setToolTipText("Click 'Check availability' to list only the free slots");
        timeCombo.setEditable(true);

        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints gc, int row, int col,
                          String label, JTextField field) {
        gc.gridx = col; gc.gridy = row; gc.weightx = 0;
        panel.add(new JLabel(label), gc);
        gc.gridx = col + 1; gc.weightx = 1;
        panel.add(field, gc);
        gc.weightx = 0;
    }

    /* ------------------------------------------------------------------ */
    /* Reference data - the Facade call                                    */
    /* ------------------------------------------------------------------ */

    private void loadReferenceData() {
        setBusy(true, "Loading dentists and treatments...");

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws ApiException {
                return new Object[] { api.listDentists(), api.listTreatments() };
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                setBusy(false, " ");
                try {
                    Object[] result = get();
                    dentistCombo.setModel(new DefaultComboBoxModel<>(
                            ((List<DentistDto>) result[0]).toArray(new DentistDto[0])));
                    treatmentCombo.setModel(new DefaultComboBoxModel<>(
                            ((List<TreatmentDto>) result[1]).toArray(new TreatmentDto[0])));
                    loadSlots();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showStatus("Could not load the dentist and treatment lists: "
                            + ex.getCause().getMessage());
                }
            }
        }.execute();
    }

    /**
     * Loads the chosen dentist's free slots.
     *
     * <p>The receptionist picks from slots the server has already confirmed are
     * free, which is the clinic's primary defence against double booking: a
     * taken slot is never offered, so it is never requested.</p>
     */
    private void loadSlots() {
        DentistDto dentist = (DentistDto) dentistCombo.getSelectedItem();
        LocalDate date = parseDate();
        if (dentist == null || date == null) {
            return;
        }

        setBusy(true, "Checking availability...");

        new SwingWorker<List<SlotDto>, Void>() {
            @Override
            protected List<SlotDto> doInBackground() throws ApiException {
                return api.availableSlots(dentist.getDentistCode(), date);
            }

            @Override
            protected void done() {
                setBusy(false, " ");
                try {
                    List<SlotDto> slots = get();
                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                    int free = 0;
                    for (SlotDto slot : slots) {
                        if (slot.isAvailable()) {
                            model.addElement(slot.getStartTime().toString());
                            free++;
                        }
                    }
                    timeCombo.setModel(model);
                    if (free == 0) {
                        showStatus("Dr " + dentist.getFullName() + " has no free slots on " + date
                                + ". Try another date or another dentist.");
                    } else {
                        statusLabel.setForeground(UiUtils.GREEN);
                        showStatus(free + " free slot(s) for Dr " + dentist.getFullName()
                                + " on " + date + ".");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    showStatus("Could not check availability: " + ex.getCause().getMessage());
                }
            }
        }.execute();
    }

    /* ------------------------------------------------------------------ */
    /* Save                                                                */
    /* ------------------------------------------------------------------ */

    private void save() {
        statusLabel.setForeground(UiUtils.RED);

        // Cheap local checks first, so an obviously incomplete form does not
        // cost a network round trip. The server validates everything again -
        // this is a courtesy, not the guarantee.
        if (patientNameField.getText().trim().isEmpty()) {
            showStatus("Enter the patient's name.");
            patientNameField.requestFocusInWindow();
            return;
        }
        if (addressField.getText().trim().isEmpty()) {
            showStatus("Enter the patient's address.");
            addressField.requestFocusInWindow();
            return;
        }
        if (contactField.getText().trim().isEmpty()) {
            showStatus("Enter the patient's contact number.");
            contactField.requestFocusInWindow();
            return;
        }

        LocalDate date = parseDate();
        if (date == null) {
            showStatus("Enter the appointment date as yyyy-MM-dd, for example "
                    + LocalDate.now().plusDays(1) + ".");
            dateField.requestFocusInWindow();
            return;
        }

        LocalTime time = parseTime();
        if (time == null) {
            showStatus("Choose an appointment time, or type one such as 09:30.");
            timeCombo.requestFocusInWindow();
            return;
        }

        DentistDto dentist = (DentistDto) dentistCombo.getSelectedItem();
        TreatmentDto treatment = (TreatmentDto) treatmentCombo.getSelectedItem();
        if (dentist == null || treatment == null) {
            showStatus("Select both a dentist and a treatment.");
            return;
        }

        AppointmentRequest request = AppointmentRequest.builder()
                .patientCode(blankToNull(patientCodeField.getText()))
                .patientName(patientNameField.getText().trim())
                .address(addressField.getText().trim())
                .contactNumber(contactField.getText().trim())
                .email(blankToNull(emailField.getText()))
                .nic(blankToNull(nicField.getText()))
                .gender((Gender) genderCombo.getSelectedItem())
                .dateOfBirth(parseDateOfBirth())
                .dentistCode(dentist.getDentistCode())
                .treatmentCode(treatment.getCode())
                .appointmentDate(date)
                .appointmentTime(time)
                .notes(blankToNull(notesArea.getText()))
                .build();

        setBusy(true, "Saving...");

        new SwingWorker<AppointmentDto, Void>() {
            @Override
            protected AppointmentDto doInBackground() throws ApiException {
                return api.registerAppointment(request);
            }

            @Override
            protected void done() {
                setBusy(false, " ");
                try {
                    createdAppointment = get();
                    UiUtils.showSuccess(RegisterAppointmentDialog.this, "Appointment registered",
                            "Appointment number: " + createdAppointment.getAppointmentNumber()
                                    + "\n\nPatient : " + createdAppointment.getPatientName()
                                    + "\nDentist : Dr " + createdAppointment.getDentistName()
                                    + "\nWhen    : " + createdAppointment.getAppointmentDate()
                                    + " at " + createdAppointment.getAppointmentTime()
                                    + "\n\nGive this number to the patient - they will be asked "
                                    + "for it when they arrive.");
                    dispose();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    // A refused booking rule is normal traffic, not a crash:
                    // show the reason and leave everything typed in place.
                    showStatus("<html>" + cause.getMessage().replace("\n", "<br>") + "</html>");
                    loadSlots();
                }
            }
        }.execute();
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    private LocalDate parseDate() {
        try {
            return LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDate parseDateOfBirth() {
        String value = dobField.getText().trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalTime parseTime() {
        Object selected = timeCombo.getSelectedItem();
        if (selected == null) {
            return null;
        }
        try {
            String value = selected.toString().trim();
            return LocalTime.parse(value.length() == 5 ? value + ":00" : value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private void setBusy(boolean busy, String message) {
        saveButton.setEnabled(!busy);
        checkSlotsButton.setEnabled(!busy);
        if (busy) {
            statusLabel.setForeground(new Color(0x3D, 0x4A, 0x58));
        }
        showStatus(message);
    }

    /** @return the appointment created, or {@code null} if the dialog was cancelled */
    public AppointmentDto getCreatedAppointment() {
        return createdAppointment;
    }
}
