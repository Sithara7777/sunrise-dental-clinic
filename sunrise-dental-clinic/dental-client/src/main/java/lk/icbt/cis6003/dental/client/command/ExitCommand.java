package lk.icbt.cis6003.dental.client.command;

import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.UiUtils;

import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Requirement 6 - "Exit System. Allow users to safely close the application."
 *
 * <p>"Safely" is doing real work here. The command confirms first, so an
 * accidental click cannot discard what a receptionist is in the middle of, and
 * then signs out on the server before the JVM stops. That releases the session
 * immediately rather than leaving it alive until it times out &mdash; which
 * matters on a shared front-desk machine where the next person to sit down
 * must not inherit the previous user's session.</p>
 */
public class ExitCommand extends AbstractMenuCommand {

    public ExitCommand(JFrame owner, ClinicApiClient api) {
        super(owner, api);
    }

    @Override
    public String getName() {
        return "Exit";
    }

    @Override
    public String getDescription() {
        return "Sign out on the server and close the application.";
    }

    @Override
    public int getMnemonic() {
        return KeyEvent.VK_X;
    }

    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK);
    }

    @Override
    protected void run() {
        boolean confirmed = UiUtils.confirm(owner, "Exit the application",
                "Sign out and close Sunrise Dental Clinic?\n\n"
                        + "Everything you have saved is already stored on the server.");
        if (!confirmed) {
            return;
        }

        // Release the session on the server. logout() swallows its own errors -
        // a network problem must not prevent the application from closing.
        api.logout();

        owner.dispose();
        System.exit(0);
    }
}
