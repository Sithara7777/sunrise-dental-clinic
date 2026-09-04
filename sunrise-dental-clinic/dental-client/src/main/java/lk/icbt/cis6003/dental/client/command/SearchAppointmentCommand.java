package lk.icbt.cis6003.dental.client.command;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.SearchAppointmentWindow;

import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Requirement 3 - "Display Appointment Details. Search using the appointment
 * number. Show complete patient and appointment information."
 */
public class SearchAppointmentCommand extends AbstractMenuCommand {

    public SearchAppointmentCommand(JFrame owner, ClinicApiClient api) {
        super(owner, api);
    }

    @Override
    public String getName() {
        return "Find Appointment";
    }

    @Override
    public String getDescription() {
        return "Search by appointment number, patient name or telephone number, and display "
                + "the full record.";
    }

    @Override
    public int getMnemonic() {
        return KeyEvent.VK_F;
    }

    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
    }

    @Override
    protected void run() throws ApiException {
        new SearchAppointmentWindow(owner, api).setVisible(true);
    }
}
