package lk.icbt.cis6003.dental.client.command;

import lk.icbt.cis6003.dental.client.api.ApiException;
import lk.icbt.cis6003.dental.client.api.ClinicApiClient;
import lk.icbt.cis6003.dental.client.ui.ReportWindow;

import javax.swing.JFrame;
import java.awt.event.KeyEvent;
import java.time.LocalDate;

/**
 * Opens today's diary.
 *
 * <p>It runs the {@code DAILY_SCHEDULE} report rather than a bespoke query, so
 * the desktop client shows byte-for-byte the same schedule the web application
 * prints and the same free-slot count the practice manager reads at 07:45.</p>
 */
public class TodayScheduleCommand extends AbstractMenuCommand {

    public TodayScheduleCommand(JFrame owner, ClinicApiClient api) {
        super(owner, api);
    }

    @Override
    public String getName() {
        return "Today's Schedule";
    }

    @Override
    public String getDescription() {
        return "Every appointment booked for today, in time order, with the remaining capacity.";
    }

    @Override
    public int getMnemonic() {
        return KeyEvent.VK_T;
    }

    @Override
    protected void run() throws ApiException {
        ReportWindow window = new ReportWindow(owner, api, "DAILY_SCHEDULE");
        window.runReport(LocalDate.now(), LocalDate.now());
        window.setVisible(true);
    }
}
