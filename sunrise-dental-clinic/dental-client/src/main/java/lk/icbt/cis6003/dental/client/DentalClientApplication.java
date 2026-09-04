package lk.icbt.cis6003.dental.client;

import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.LoginDialog;
import lk.icbt.cis6003.dental.client.ui.MainFrame;
import lk.icbt.cis6003.dental.client.ui.UiUtils;
import lk.icbt.cis6003.dental.common.dto.UserDto;

import javax.swing.SwingUtilities;

/**
 * Entry point for the menu-driven desktop client.
 *
 * <p>Run it with:</p>
 * <pre>
 *   java -jar sunrise-dental-client.jar [server-url]
 * </pre>
 *
 * <p>The server URL defaults to {@code http://localhost:8080} and can be given
 * on the command line, changed on the sign-in dialog, or set through the
 * {@code CLINIC_SERVER_URL} environment variable. It is configurable because
 * in any real deployment the front-desk machine and the server are different
 * computers - which is exactly what makes this a distributed application rather
 * than one program in two windows.</p>
 *
 * <p>The client holds no database, no ORM and no business rules. Every rule is
 * enforced by the server, and this process reaches it only over HTTP.</p>
 */
public final class DentalClientApplication {

    private static final String DEFAULT_SERVER_URL = "http://localhost:8080";

    private DentalClientApplication() {
        // entry point only
    }

    public static void main(String[] args) {
        String serverUrl = resolveServerUrl(args);

        // Swing is not thread-safe: every component must be created and touched
        // on the event dispatch thread.
        SwingUtilities.invokeLater(() -> {
            UiUtils.installLookAndFeel();

            LoginDialog login = new LoginDialog(null, serverUrl);
            login.setVisible(true);        // modal - blocks until signed in or cancelled

            UserDto user = login.getAuthenticatedUser();
            if (user == null) {
                // The user chose Exit at the sign-in dialog. Requirement 6:
                // close cleanly rather than leaving a headless process behind.
                System.exit(0);
                return;
            }

            ClinicApiClient api = login.getApiClient();
            new MainFrame(api).setVisible(true);
        });
    }

    /**
     * @param args the command line; the first argument, if present, is the
     *             server URL
     */
    private static String resolveServerUrl(String[] args) {
        if (args.length > 0 && !args[0].isBlank()) {
            return args[0].trim();
        }
        String fromEnvironment = System.getenv("CLINIC_SERVER_URL");
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment.trim();
        }
        return DEFAULT_SERVER_URL;
    }
}
