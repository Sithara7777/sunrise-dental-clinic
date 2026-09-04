package lk.icbt.cis6003.dental.client.command;

import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.MainFrame;

import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

/** Reloads the dashboard tiles from the server. */
public class RefreshDashboardCommand extends AbstractMenuCommand {

    public RefreshDashboardCommand(JFrame owner, ClinicApiClient api) {
        super(owner, api);
    }

    @Override
    public String getName() {
        return "Refresh Dashboard";
    }

    @Override
    public String getDescription() {
        return "Reload today's figures from the clinic server.";
    }

    @Override
    public int getMnemonic() {
        return KeyEvent.VK_D;
    }

    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    }

    @Override
    protected void run() {
        if (owner instanceof MainFrame frame) {
            frame.refreshDashboard();
        }
    }
}
