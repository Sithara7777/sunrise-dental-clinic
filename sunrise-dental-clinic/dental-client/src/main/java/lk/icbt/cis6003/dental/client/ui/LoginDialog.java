package lk.icbt.cis6003.dental.client.ui;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.common.dto.UserDto;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Requirement 1 as the desktop client's front door.
 *
 * <p>Two details that matter beyond appearance:</p>
 *
 * <ul>
 *   <li><b>The password is held in a {@code char[]} and wiped</b> after use.
 *       {@code JPasswordField.getPassword()} returns a character array
 *       precisely so the value need not linger in the string pool where a heap
 *       dump would reveal it. Calling {@code getText()} would defeat that.</li>
 *   <li><b>The call runs on a {@link SwingWorker}</b>, not on the event
 *       dispatch thread. A network round trip on the EDT freezes the entire
 *       window, and a receptionist looking at a frozen application concludes it
 *       has crashed and force-quits it.</li>
 * </ul>
 *
 * <p>The server address is editable, because the front-desk machine and the
 * server are different computers in any real deployment - which is the whole
 * point of the application being distributed.</p>
 */
public class LoginDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField serverField = new JTextField(22);
    private final JTextField usernameField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton signInButton = new JButton("Sign in");
    private final JButton cancelButton = new JButton("Exit");

    private ClinicApiClient apiClient;
    private UserDto authenticatedUser;

    public LoginDialog(Window owner, String defaultServerUrl) {
        super(owner, "Sign in - Sunrise Dental Clinic", ModalityType.APPLICATION_MODAL);

        serverField.setText(defaultServerUrl);
        buildUi();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(signInButton);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        /* ---------------- banner ---------------- */
        JPanel banner = new JPanel();
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));
        banner.setBackground(UiUtils.TEAL_DARK);
        banner.setBorder(BorderFactory.createEmptyBorder(18, 24, 16, 24));

        JLabel title = new JLabel("Sunrise Dental Clinic");
        title.setFont(new Font("Segoe UI", Font.BOLD, 19));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Appointment & Patient Management - Desktop Client");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(0xD7, 0xEE, 0xF0));
        subtitle.setAlignmentX(CENTER_ALIGNMENT);

        banner.add(title);
        banner.add(Box.createVerticalStrut(4));
        banner.add(subtitle);

        /* ---------------- form ---------------- */
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(18, 24, 6, 24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 5, 5, 5);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        form.add(new JLabel("Server address"), gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(serverField, gc);

        gc.gridx = 1; gc.gridy = ++row;
        JLabel serverHint = UiUtils.muted("The machine running the clinic server");
        serverHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        form.add(serverHint, gc);

        gc.gridx = 0; gc.gridy = ++row; gc.weightx = 0;
        form.add(new JLabel("Username"), gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(usernameField, gc);

        gc.gridx = 0; gc.gridy = ++row; gc.weightx = 0;
        form.add(new JLabel("Password"), gc);
        gc.gridx = 1; gc.weightx = 1;
        form.add(passwordField, gc);

        gc.gridx = 0; gc.gridy = ++row; gc.gridwidth = 2;
        statusLabel.setForeground(UiUtils.RED);
        statusLabel.setPreferredSize(new Dimension(320, 34));
        statusLabel.setVerticalAlignment(SwingConstants.TOP);
        form.add(statusLabel, gc);

        /* ---------------- buttons ---------------- */
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
        buttons.add(cancelButton);
        buttons.add(signInButton);

        signInButton.addActionListener(e -> attemptSignIn());
        cancelButton.addActionListener(e -> {
            authenticatedUser = null;
            dispose();
        });

        /* ---------------- demo credentials ---------------- */
        JPanel hint = new JPanel(new BorderLayout());
        hint.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiUtils.BORDER),
                BorderFactory.createEmptyBorder(8, 24, 12, 24)));
        hint.setBackground(UiUtils.SLATE_BG);
        JLabel demo = new JLabel("<html><b>Demonstration accounts</b><br>"
                + "admin / Admin@123 &nbsp;&middot;&nbsp; reception / Reception@123 "
                + "&nbsp;&middot;&nbsp; nperera / Dentist@123</html>");
        demo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        demo.setForeground(UiUtils.SLATE_MUTED);
        hint.add(demo, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.add(buttons, BorderLayout.NORTH);
        south.add(hint, BorderLayout.SOUTH);

        root.add(banner, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
    }

    /**
     * Validates locally, then authenticates off the event dispatch thread.
     */
    private void attemptSignIn() {
        String server = serverField.getText().trim();
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();

        if (server.isEmpty()) {
            showStatus("Enter the address of the clinic server.");
            return;
        }
        if (username.isEmpty()) {
            showStatus("Enter your username.");
            return;
        }
        if (password.length == 0) {
            showStatus("Enter your password.");
            return;
        }

        setBusy(true);
        showStatus(" ");

        ClinicApiClient client = new ClinicApiClient(server);

        new SwingWorker<UserDto, Void>() {
            @Override
            protected UserDto doInBackground() throws ApiException {
                try {
                    return client.login(username, new String(password));
                } finally {
                    // Do not leave the password sitting in memory.
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    authenticatedUser = get();
                    apiClient = client;
                    dispose();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showStatus("Sign-in was interrupted.");
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof ApiException apiEx) {
                        showStatus("<html>" + apiEx.getMessage().replace("\n", "<br>") + "</html>");
                    } else {
                        showStatus("Sign-in failed: " + cause);
                    }
                    passwordField.setText("");
                    passwordField.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        signInButton.setEnabled(!busy);
        signInButton.setText(busy ? "Signing in..." : "Sign in");
        serverField.setEnabled(!busy);
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    /** @return the signed-in user, or {@code null} if the dialog was cancelled */
    public UserDto getAuthenticatedUser() {
        return authenticatedUser;
    }

    /** @return the connected client, valid only after a successful sign-in */
    public ClinicApiClient getApiClient() {
        return apiClient;
    }
}
