package lk.icbt.cis6003.dental.client.command;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClientSession;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.BillingWindow;

import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Requirement 4 - "Calculate and Print Bill".
 *
 * <p>Only offered to administrators and receptionists. The server enforces the
 * same rule; disabling the control simply avoids presenting an action that
 * would be refused, which is a better experience than an error message.</p>
 */
public class GenerateBillCommand extends AbstractMenuCommand {

    public GenerateBillCommand(JFrame owner, ClinicApiClient api) {
        super(owner, api);
    }

    @Override
    public String getName() {
        return "Calculate & Print Bill";
    }

    @Override
    public String getDescription() {
        return "Calculate the total from the treatment and consultation fee, issue the bill, "
                + "and print the receipt.";
    }

    @Override
    public int getMnemonic() {
        return KeyEvent.VK_C;
    }

    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK);
    }

    @Override
    public boolean isPermitted() {
        return ClientSession.getInstance().canHandleBilling();
    }

    @Override
    protected void run() throws ApiException {
        new BillingWindow(owner, api, null).setVisible(true);
    }
}
