package lk.icbt.cis6003.dental.client.command;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.AppointmentDetailWindow;
import lk.icbt.cis6003.dental.client.ui.RegisterAppointmentDialog;
import lk.icbt.cis6003.dental.common.dto.AppointmentDto;

import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Requirement 2 - "Register New Appointment".
 *
 * <p>Opens the booking dialog and, on success, opens the new appointment in its
 * own window so the receptionist can read the issued appointment number to the
 * patient straight away.</p>
 */
public class RegisterAppointmentCommand extends AbstractMenuCommand {

    public RegisterAppointmentCommand(JFrame owner, ClinicApiClient api) {
        super(owner, api);
    }

    @Override
    public String getName() {
        return "Register New Appointment";
    }

    @Override
    public String getDescription() {
        return "Book a visit for a new or returning patient. The appointment number is "
                + "issued by the server.";
    }

    @Override
    public int getMnemonic() {
        return KeyEvent.VK_R;
    }

    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK);
    }

    @Override
    protected void run() throws ApiException {
        RegisterAppointmentDialog dialog = new RegisterAppointmentDialog(owner, api);
        dialog.setVisible(true);

        AppointmentDto created = dialog.getCreatedAppointment();
        if (created != null) {
            new AppointmentDetailWindow(owner, api, created).setVisible(true);
        }
    }
}
