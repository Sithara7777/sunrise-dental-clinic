package lk.icbt.cis6003.dental.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Type-safe binding of every {@code clinic.*} setting in
 * {@code application.yml}.
 *
 * <p>Preferred over scattering {@code @Value("${...}")} annotations across the
 * codebase: the full set of knobs is visible in one file, the IDE
 * auto-completes them, and a typo in a property name fails at start-up rather
 * than at the moment the clinic tries to send its first reminder.</p>
 */
@Component
@ConfigurationProperties(prefix = "clinic")
public class ClinicProperties {

    private final Notifications notifications = new Notifications();
    private final Demo demo = new Demo();
    private final Security security = new Security();

    public Notifications getNotifications() {
        return notifications;
    }

    public Demo getDemo() {
        return demo;
    }

    public Security getSecurity() {
        return security;
    }

    /** Outbound alert settings. */
    public static class Notifications {

        /** Master switch. When false every channel logs SUPPRESSED. */
        private boolean enabled = true;

        /**
         * When true, e-mail goes to a real SMTP server via JavaMailSender.
         * When false (the default) it goes to the console gateway, so the
         * system is fully demonstrable with no mail server configured.
         */
        private boolean smtpEnabled = false;

        private boolean emailEnabled = true;
        private boolean smsEnabled = true;

        private String fromAddress = "no-reply@sunrisedental.lk";
        private String fromName = "Sunrise Dental Clinic";

        /** Hours before an appointment that the reminder job fires. */
        private int reminderLeadHours = 24;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isSmtpEnabled() {
            return smtpEnabled;
        }

        public void setSmtpEnabled(boolean smtpEnabled) {
            this.smtpEnabled = smtpEnabled;
        }

        public boolean isEmailEnabled() {
            return emailEnabled;
        }

        public void setEmailEnabled(boolean emailEnabled) {
            this.emailEnabled = emailEnabled;
        }

        public boolean isSmsEnabled() {
            return smsEnabled;
        }

        public void setSmsEnabled(boolean smsEnabled) {
            this.smsEnabled = smsEnabled;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public void setFromAddress(String fromAddress) {
            this.fromAddress = fromAddress;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public int getReminderLeadHours() {
            return reminderLeadHours;
        }

        public void setReminderLeadHours(int reminderLeadHours) {
            this.reminderLeadHours = reminderLeadHours;
        }
    }

    /** Sample data used to make the reports meaningful on a fresh install. */
    public static class Demo {

        private boolean seedEnabled = true;
        private int patientCount = 40;
        private int pastDays = 45;
        private int futureDays = 21;

        public boolean isSeedEnabled() {
            return seedEnabled;
        }

        public void setSeedEnabled(boolean seedEnabled) {
            this.seedEnabled = seedEnabled;
        }

        public int getPatientCount() {
            return patientCount;
        }

        public void setPatientCount(int patientCount) {
            this.patientCount = patientCount;
        }

        public int getPastDays() {
            return pastDays;
        }

        public void setPastDays(int pastDays) {
            this.pastDays = pastDays;
        }

        public int getFutureDays() {
            return futureDays;
        }

        public void setFutureDays(int futureDays) {
            this.futureDays = futureDays;
        }
    }

    /** Session and login settings. */
    public static class Security {

        /**
         * Initial passwords for the four seeded staff accounts. They are
         * hashed with BCrypt before storage and must be changed in any real
         * deployment - the application logs a warning while the defaults are
         * still in use.
         */
        private String defaultAdminPassword = "Admin@123";
        private String defaultReceptionPassword = "Reception@123";
        private String defaultDentistPassword = "Dentist@123";

        /** Idle session timeout, in minutes. */
        private int sessionTimeoutMinutes = 30;

        /** Lifetime of the "remember this device" cookie, in days. */
        private int rememberMeDays = 7;

        public String getDefaultAdminPassword() {
            return defaultAdminPassword;
        }

        public void setDefaultAdminPassword(String defaultAdminPassword) {
            this.defaultAdminPassword = defaultAdminPassword;
        }

        public String getDefaultReceptionPassword() {
            return defaultReceptionPassword;
        }

        public void setDefaultReceptionPassword(String defaultReceptionPassword) {
            this.defaultReceptionPassword = defaultReceptionPassword;
        }

        public String getDefaultDentistPassword() {
            return defaultDentistPassword;
        }

        public void setDefaultDentistPassword(String defaultDentistPassword) {
            this.defaultDentistPassword = defaultDentistPassword;
        }

        public int getSessionTimeoutMinutes() {
            return sessionTimeoutMinutes;
        }

        public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
            this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        }

        public int getRememberMeDays() {
            return rememberMeDays;
        }

        public void setRememberMeDays(int rememberMeDays) {
            this.rememberMeDays = rememberMeDays;
        }
    }
}
