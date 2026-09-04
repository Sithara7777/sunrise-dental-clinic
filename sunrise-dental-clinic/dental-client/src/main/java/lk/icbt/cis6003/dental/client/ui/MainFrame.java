package lk.icbt.cis6003.dental.client.ui;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClientSession;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.command.AboutCommand;
import lk.icbt.cis6003.dental.client.command.ExitCommand;
import lk.icbt.cis6003.dental.client.command.FindBillCommand;
import lk.icbt.cis6003.dental.client.command.GenerateBillCommand;
import lk.icbt.cis6003.dental.client.command.MenuCommand;
import lk.icbt.cis6003.dental.client.command.RefreshDashboardCommand;
import lk.icbt.cis6003.dental.client.command.RegisterAppointmentCommand;
import lk.icbt.cis6003.dental.client.command.SearchAppointmentCommand;
import lk.icbt.cis6003.dental.client.command.ShowHelpCommand;
import lk.icbt.cis6003.dental.client.command.TodayScheduleCommand;
import lk.icbt.cis6003.dental.client.command.ViewReportsCommand;
import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.common.dto.report.DashboardStatsDto;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * The menu-driven main window the scenario asks for.
 *
 * <p><b>Every action exists exactly once.</b> The menu bar and the large button
 * panel are both built by iterating the same {@link MenuCommand} list, so the
 * two can never offer different things or behave differently. Adding a feature
 * to this client is one new command class plus one line in
 * {@link #buildCommands()}.</p>
 *
 * <p>The window follows the six functions of the scenario directly:
 * 1&nbsp;sign in (before this window opens), 2&nbsp;register, 3&nbsp;display,
 * 4&nbsp;bill, 5&nbsp;help, 6&nbsp;exit.</p>
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final ClinicApiClient api;

    private final List<MenuCommand> appointmentCommands = new ArrayList<>();
    private final List<MenuCommand> billingCommands = new ArrayList<>();
    private final List<MenuCommand> reportCommands = new ArrayList<>();
    private final List<MenuCommand> helpCommands = new ArrayList<>();
    private final List<MenuCommand> fileCommands = new ArrayList<>();

    private final JPanel tilePanel = new JPanel(new GridLayout(2, 4, 10, 10));
    private final JLabel statusLabel = new JLabel("Ready");

    public MainFrame(ClinicApiClient api) {
        super("Sunrise Dental Clinic - Desktop Client");
        this.api = api;

        buildCommands();
        setJMenuBar(buildMenuBar());
        setContentPane(buildContent());

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Requirement 6: closing the window must sign out tidily, not
                // simply abandon the session on the server.
                new ExitCommand(MainFrame.this, api).execute();
            }
        });

        setSize(new Dimension(980, 660));
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);

        refreshDashboard();
    }

    /* ------------------------------------------------------------------ */
    /* Command registration - the single source of truth                   */
    /* ------------------------------------------------------------------ */

    private void buildCommands() {
        appointmentCommands.add(new RegisterAppointmentCommand(this, api));
        appointmentCommands.add(new SearchAppointmentCommand(this, api));
        appointmentCommands.add(new TodayScheduleCommand(this, api));

        billingCommands.add(new GenerateBillCommand(this, api));
        billingCommands.add(new FindBillCommand(this, api));

        reportCommands.add(new ViewReportsCommand(this, api));
        reportCommands.add(new RefreshDashboardCommand(this, api));

        helpCommands.add(new ShowHelpCommand(this, api));
        helpCommands.add(new AboutCommand(this, api));

        fileCommands.add(new ExitCommand(this, api));
    }

    /* ------------------------------------------------------------------ */
    /* Menu bar                                                            */
    /* ------------------------------------------------------------------ */

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.add(menu("Appointments", 'A', appointmentCommands));
        bar.add(menu("Billing", 'B', billingCommands));
        bar.add(menu("Reports", 'R', reportCommands));
        bar.add(menu("Help", 'H', helpCommands));
        bar.add(menu("File", 'F', fileCommands));
        return bar;
    }

    private JMenu menu(String title, char mnemonic, List<MenuCommand> commands) {
        JMenu menu = new JMenu(title);
        menu.setMnemonic(mnemonic);
        for (MenuCommand command : commands) {
            JMenuItem item = new JMenuItem(command.getName());
            item.setMnemonic(command.getMnemonic());
            item.setToolTipText(command.getDescription());
            if (command.getAccelerator() != null) {
                item.setAccelerator(command.getAccelerator());
            }
            item.setEnabled(command.isPermitted());
            item.addActionListener(e -> command.execute());
            menu.add(item);
        }
        return menu;
    }

    /* ------------------------------------------------------------------ */
    /* Content                                                             */
    /* ------------------------------------------------------------------ */

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiUtils.SLATE_BG);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCentre(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiUtils.TEAL_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(ClinicConstants.CLINIC_NAME);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Appointment & Patient Management System");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(0xD7, 0xEE, 0xF0));

        left.add(title);
        left.add(subtitle);

        ClientSession session = ClientSession.getInstance();
        JLabel who = new JLabel("<html><div style='text-align:right'><b>"
                + session.getDisplayName() + "</b><br>"
                + (session.getRole() == null ? "" : session.getRole().getDisplayName())
                + " &nbsp;|&nbsp; " + session.getServerBaseUrl() + "</div></html>");
        who.setForeground(new Color(0xD7, 0xEE, 0xF0));
        who.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        header.add(left, BorderLayout.WEST);
        header.add(who, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCentre() {
        JPanel centre = new JPanel(new BorderLayout(0, 12));
        centre.setBackground(UiUtils.SLATE_BG);
        centre.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        tilePanel.setOpaque(false);
        UiUtils.titleBorder(tilePanel, "  Today at a glance  ");
        centre.add(tilePanel, BorderLayout.NORTH);

        /* Large buttons - the same commands as the menu bar, nothing duplicated. */
        JPanel actions = new JPanel(new GridLayout(0, 3, 10, 10));
        actions.setOpaque(false);
        UiUtils.titleBorder(actions, "  What would you like to do?  ");

        List<MenuCommand> all = new ArrayList<>();
        all.addAll(appointmentCommands);
        all.addAll(billingCommands);
        all.addAll(reportCommands);
        all.addAll(helpCommands);
        all.addAll(fileCommands);

        for (MenuCommand command : all) {
            actions.add(actionButton(command));
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(actions, BorderLayout.NORTH);
        centre.add(wrapper, BorderLayout.CENTER);

        return centre;
    }

    private JButton actionButton(MenuCommand command) {
        JButton button = new JButton("<html><div style='text-align:center'><b>"
                + command.getName() + "</b></div></html>");
        button.setToolTipText(command.getDescription());
        button.setEnabled(command.isPermitted());
        button.setPreferredSize(new Dimension(190, 52));
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.addActionListener(e -> command.execute());

        if (!command.isPermitted()) {
            button.setToolTipText(command.getDescription()
                    + "  (not available to a " + ClientSession.getInstance().getRole().getDisplayName() + ")");
        }
        return button;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiUtils.BORDER),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        bar.setBackground(Color.WHITE);

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(UiUtils.SLATE_MUTED);

        JLabel connection = new JLabel("Connected over HTTP to " + api.getBaseUrl());
        connection.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        connection.setForeground(UiUtils.SLATE_MUTED);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(connection, BorderLayout.EAST);
        return bar;
    }

    /* ------------------------------------------------------------------ */
    /* Dashboard                                                           */
    /* ------------------------------------------------------------------ */

    /**
     * Reloads the tiles.
     *
     * <p>On a {@link SwingWorker} because it is a network call: doing it on the
     * event dispatch thread would freeze the window for the duration of the
     * round trip.</p>
     */
    public void refreshDashboard() {
        setStatus("Loading today's figures...");

        new SwingWorker<DashboardStatsDto, Void>() {
            @Override
            protected DashboardStatsDto doInBackground() throws ApiException {
                return api.dashboard();
            }

            @Override
            protected void done() {
                try {
                    populateTiles(get());
                    setStatus("Dashboard updated at "
                            + java.time.LocalTime.now().withNano(0));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    setStatus("Could not load the dashboard - " + ex.getCause().getMessage());
                    tilePanel.removeAll();
                    tilePanel.add(UiUtils.muted("Dashboard unavailable. Use Reports > Refresh to retry."));
                    tilePanel.revalidate();
                    tilePanel.repaint();
                }
            }
        }.execute();
    }

    private void populateTiles(DashboardStatsDto stats) {
        tilePanel.removeAll();
        tilePanel.add(tile("Appointments today", String.valueOf(stats.getTodayAppointments()),
                stats.getTodayPending() + " still to be seen", UiUtils.TEAL));
        tilePanel.add(tile("Completed today", String.valueOf(stats.getTodayCompleted()),
                stats.getTodayCancelled() + " cancelled", UiUtils.GREEN));
        tilePanel.add(tile("Invoiced today", UiUtils.moneyWithCurrency(stats.getTodayRevenue()),
                "Month: " + UiUtils.moneyWithCurrency(stats.getMonthRevenue()), UiUtils.GREEN));
        tilePanel.add(tile("Outstanding", UiUtils.moneyWithCurrency(stats.getOutstandingBalance()),
                stats.getOutstandingInvoiceCount() + " unpaid bills", UiUtils.RED));
        tilePanel.add(tile("Next 7 days", String.valueOf(stats.getUpcomingSevenDays()),
                "appointments booked", UiUtils.TEAL));
        tilePanel.add(tile("Patients", String.valueOf(stats.getTotalPatients()),
                stats.getNewPatientsThisMonth() + " new this month", UiUtils.TEAL));
        tilePanel.add(tile("No-show rate", stats.getNoShowRate() + "%",
                "wasted chair time", UiUtils.AMBER));
        tilePanel.add(tile("Chair utilisation", stats.getChairUtilisationToday() + "%",
                stats.getActiveDentists() + " dentists practising", UiUtils.TEAL));

        tilePanel.revalidate();
        tilePanel.repaint();
    }

    private JPanel tile(String label, String value, String hint, Color accent) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UiUtils.BORDER),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10))));

        JLabel labelText = new JLabel(label.toUpperCase());
        labelText.setFont(new Font("Segoe UI", Font.BOLD, 10));
        labelText.setForeground(UiUtils.SLATE_MUTED);

        JLabel valueText = new JLabel(value);
        valueText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueText.setForeground(UiUtils.TEAL_DARK);

        JLabel hintText = new JLabel(hint);
        hintText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hintText.setForeground(UiUtils.SLATE_MUTED);

        panel.add(labelText);
        panel.add(Box.createVerticalStrut(2));
        panel.add(valueText);
        panel.add(Box.createVerticalStrut(1));
        panel.add(hintText);
        return panel;
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}
