package lk.icbt.cis6003.dental.client.command;

import lk.icbt.cis6003.dental.client.api.ClientSession;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.UiUtils;
import lk.icbt.cis6003.dental.common.ClinicConstants;

import javax.swing.JFrame;
import java.awt.event.KeyEvent;

/** Identifies the application, the signed-in user and the server it is talking to. */
public class AboutCommand extends AbstractMenuCommand {

    public AboutCommand(JFrame owner, ClinicApiClient api) {
        super(owner, api);
    }

    @Override
    public String getName() {
        return "About";
    }

    @Override
    public String getDescription() {
        return "Version information, and which server this client is connected to.";
    }

    @Override
    public int getMnemonic() {
        return KeyEvent.VK_A;
    }

    @Override
    protected void run() {
        ClientSession session = ClientSession.getInstance();

        UiUtils.showInfo(owner, "About this application",
                ClinicConstants.CLINIC_NAME + "\n"
                        + "Appointment & Patient Management System - Desktop Client\n"
                        + "Version 1.0.0\n\n"
                        + "This client holds no database of its own. It is a separate process\n"
                        + "that reaches the clinic system only over HTTP web services, which is\n"
                        + "what makes the solution a distributed application.\n\n"
                        + "Connected to : " + api.getBaseUrl() + "\n"
                        + "Signed in as : " + session.getDisplayName()
                        + " (" + session.getUsername() + ")\n"
                        + "Role         : "
                        + (session.getRole() == null ? "-" : session.getRole().getDisplayName()) + "\n\n"
                        + "Module: CIS6003 Advanced Programming - WRIT1\n"
                        + "ICBT Campus / Cardiff Metropolitan University");
    }
}
