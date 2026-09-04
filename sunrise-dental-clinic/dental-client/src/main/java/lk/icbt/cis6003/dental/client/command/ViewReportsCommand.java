package lk.icbt.cis6003.dental.client.command;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.ReportWindow;

import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Opens the report window.
 *
 * <p>The window lists whatever reports the <em>server</em> advertises and
 * renders them from the pre-formatted cell grid in the response, so a sixth
 * report written on the server appears in this client with no change here and
 * no new release of the client.</p>
 */
public class ViewReportsCommand extends AbstractMenuCommand {

    public ViewReportsCommand(JFrame owner, ClinicApiClient api) {
        super(owner, api);
    }

    @Override
    public String getName() {
        return "Management Reports";
    }

    @Override
    public String getDescription() {
        return "Daily schedule, revenue, dentist workload, treatment popularity and debtor "
                + "ageing.";
    }

    @Override
    public int getMnemonic() {
        return KeyEvent.VK_M;
    }

    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK);
    }

    @Override
    protected void run() throws ApiException {
        new ReportWindow(owner, api, null).setVisible(true);
    }
}
