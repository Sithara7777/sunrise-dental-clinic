package lk.icbt.cis6003.dental.client.command;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.HelpWindow;
import lk.icbt.cis6003.dental.common.dto.HelpTopicDto;

import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Requirement 5 - "Provide step-by-step instructions for new staff on how to
 * use the system."
 *
 * <p>The content is fetched from the server rather than compiled into this
 * client, so correcting a step does not require rebuilding and redistributing
 * the desktop application to every front-desk machine.</p>
 */
public class ShowHelpCommand extends AbstractMenuCommand {

    public ShowHelpCommand(JFrame owner, ClinicApiClient api) {
        super(owner, api);
    }

    @Override
    public String getName() {
        return "Help - How to use this system";
    }

    @Override
    public String getDescription() {
        return "Step-by-step instructions for new staff, served from the clinic server.";
    }

    @Override
    public int getMnemonic() {
        return KeyEvent.VK_H;
    }

    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
    }

    @Override
    protected void run() throws ApiException {
        List<HelpTopicDto> topics = api.help();
        new HelpWindow(owner, topics).setVisible(true);
    }
}
