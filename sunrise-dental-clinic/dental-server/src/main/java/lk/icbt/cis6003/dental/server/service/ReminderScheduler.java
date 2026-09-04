package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.server.config.ClinicProperties;
import lk.icbt.cis6003.dental.server.domain.Appointment;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEvent;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEventPublisher;
import lk.icbt.cis6003.dental.server.service.notification.AppointmentEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Sends the automatic appointment reminders.
 *
 * <p>This is the direct answer to the scenario's no-show problem. A patient who
 * booked five weeks ago has usually forgotten; a reminder the previous evening
 * converts a proportion of would-be no-shows into either an attendance or a
 * cancellation the clinic can resell. Both outcomes are better than an empty
 * chair.</p>
 *
 * <p>Runs at 18:00 daily, which is late enough that the day's cancellations
 * have been recorded and early enough that a patient who cannot attend
 * tomorrow can still telephone before the clinic closes at
 * {@code 20:00}.</p>
 *
 * <p>It publishes the same {@code REMINDER} event to the same observer
 * pipeline that booking uses - so reminders reach e-mail, SMS and the audit
 * trail with no code duplicated. Adding a channel adds it to reminders too.</p>
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final AppointmentService appointmentService;
    private final AppointmentEventPublisher eventPublisher;
    private final ClinicProperties properties;

    public ReminderScheduler(AppointmentService appointmentService,
                             AppointmentEventPublisher eventPublisher,
                             ClinicProperties properties) {
        this.appointmentService = appointmentService;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    /** 18:00 every day, clinic local time. */
    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Colombo")
    @Transactional(readOnly = true)
    public void sendTomorrowsReminders() {
        if (!properties.getNotifications().isEnabled()) {
            log.debug("Notifications are switched off - the reminder run is skipped");
            return;
        }

        int leadHours = properties.getNotifications().getReminderLeadHours();
        LocalDate target = LocalDate.now().plusDays(Math.max(leadHours / 24, 1));

        List<Appointment> due = appointmentService.dueForReminder(target);
        if (due.isEmpty()) {
            log.info("Reminder run for {}: no appointments to remind", target);
            return;
        }

        for (Appointment appointment : due) {
            eventPublisher.publish(AppointmentEvent
                    .from(appointment, AppointmentEventType.REMINDER, "system")
                    .build());
        }

        log.info("Reminder run for {}: {} reminders published", target, due.size());
    }

    /**
     * Runs the reminder job immediately, for the same date the scheduled run
     * would use.
     *
     * <p>Exposed so the feature can be demonstrated on demand instead of
     * requiring a marker to wait until 18:00, and so an administrator can
     * re-run it after fixing an SMS outage.</p>
     *
     * @return the number of reminders published
     */
    @Transactional(readOnly = true)
    public int runNow() {
        int leadHours = properties.getNotifications().getReminderLeadHours();
        LocalDate target = LocalDate.now().plusDays(Math.max(leadHours / 24, 1));

        List<Appointment> due = appointmentService.dueForReminder(target);
        for (Appointment appointment : due) {
            eventPublisher.publish(AppointmentEvent
                    .from(appointment, AppointmentEventType.REMINDER,
                          lk.icbt.cis6003.dental.server.security.SecurityUtils.getCurrentUsernameOrSystem())
                    .build());
        }
        log.info("Manual reminder run for {}: {} reminders published", target, due.size());
        return due.size();
    }
}
