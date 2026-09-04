package lk.icbt.cis6003.dental.server.service;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import lk.icbt.cis6003.dental.common.dto.HelpTopicDto;
import lk.icbt.cis6003.dental.server.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Requirement 5 - "Provide step-by-step instructions for new staff on how to
 * use the system."
 *
 * <p>The help text is served by the API rather than being duplicated in the
 * Thymeleaf templates and again in the Swing client. That means the web
 * application and the desktop client show identical instructions, and correcting
 * a step does not require rebuilding and redistributing the client.</p>
 *
 * <p>Each topic is written as numbered steps a receptionist can follow on their
 * first morning, with the traps called out in the tips.</p>
 */
@Service
public class HelpService {

    private final List<HelpTopicDto> topics;

    public HelpService() {
        this.topics = buildTopics();
    }

    public List<HelpTopicDto> listTopics() {
        return topics;
    }

    public HelpTopicDto findTopic(String topicId) {
        return topics.stream()
                .filter(t -> t.getTopicId().equalsIgnoreCase(topicId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Help topic", topicId));
    }

    private List<HelpTopicDto> buildTopics() {
        List<HelpTopicDto> list = new ArrayList<>();

        /* ------------------------------------------------------------ */
        list.add(new HelpTopicDto("login", "1. Signing in",
                "How to access the system, and what to do when it will not let you in.",
                "Getting started", 1)
            .addStep("Open the clinic system in your web browser, or start the Sunrise Dental "
                     + "desktop application from the shortcut on your machine.")
            .addStep("Type the username your manager issued you. Usernames are not case sensitive.")
            .addStep("Type your password. It IS case sensitive.")
            .addStep("Tick 'Remember this device' only on the machine at your own desk - never on "
                     + "a shared computer.")
            .addStep("Click Sign in. You will arrive at the Dashboard.")
            .addTip("After five wrong passwords in a row the account locks itself for 15 minutes. "
                    + "This protects patient data; wait, or ask an administrator to help.")
            .addTip("If you are away from the screen for 30 minutes you will be signed out "
                    + "automatically. Sign in again - nothing you saved is lost.")
            .addTip("Always sign out before leaving the front desk. Everything you do is recorded "
                    + "against your name."));

        /* ------------------------------------------------------------ */
        list.add(new HelpTopicDto("register-appointment", "2. Registering a new appointment",
                "Booking a visit for a new or a returning patient.",
                "Daily work", 2)
            .addStep("Choose Appointments, then 'New appointment'.")
            .addStep("FOR A RETURNING PATIENT: type their name, telephone number or patient number "
                     + "in the patient search box and select them from the list. Their details fill "
                     + "in automatically.")
            .addStep("FOR A NEW PATIENT: leave the patient search empty and type their name, address "
                     + "and mobile number. The system registers them and issues a patient number.")
            .addStep("Choose the dentist. Only dentists currently practising appear in the list.")
            .addStep("Choose the treatment. The list shows the price and how long it takes.")
            .addStep("Choose the date, then click 'Check availability'. The free slots for that "
                     + "dentist on that day are shown.")
            .addStep("Click a free slot, then click Save.")
            .addStep("The system displays the appointment number, for example APT-2026-000137. "
                     + "Write it on the patient's card - they will be asked for it when they arrive.")
            .addTip("You never type the appointment number yourself. The system issues it, which is "
                    + "why two patients can no longer be given the same one.")
            .addTip("Appointments start on the hour or the half hour, between "
                    + ClinicConstants.CLINIC_OPENING_TIME + " and " + ClinicConstants.CLINIC_CLOSING_TIME + ".")
            .addTip("If the system says the slot is taken, somebody booked it seconds before you. "
                    + "Click 'Check availability' again to see the refreshed list.")
            .addTip("Add the patient's e-mail address if they have one - they then get a written "
                    + "confirmation as well as the text message."));

        /* ------------------------------------------------------------ */
        list.add(new HelpTopicDto("find-appointment", "3. Finding an appointment",
                "Looking up a booking when the patient arrives or telephones.",
                "Daily work", 3)
            .addStep("Choose Appointments, then 'Search'.")
            .addStep("Type the appointment number, for example APT-2026-000137, and press Enter.")
            .addStep("The full record appears: patient name, address, contact number, dentist, "
                     + "treatment, date, time and current status.")
            .addStep("If the patient has lost their number, search instead by their name or "
                     + "telephone number and pick the right visit from the list.")
            .addTip("The date filters are the fastest way to answer 'who is coming in tomorrow?'.")
            .addTip("Use 'Today's schedule' on the Dashboard for the whole day at a glance."));

        /* ------------------------------------------------------------ */
        list.add(new HelpTopicDto("appointment-status", "4. Marking what happened",
                "Confirming, completing, cancelling and recording a no-show.",
                "Daily work", 4)
            .addStep("Open the appointment.")
            .addStep("When the patient rings to confirm, click 'Confirm'.")
            .addStep("When the treatment has been carried out, click 'Complete'. Only then can the "
                     + "bill be produced.")
            .addStep("If the patient cancels, click 'Cancel' and type the reason. The slot is "
                     + "immediately released for somebody else.")
            .addStep("If the patient simply did not arrive, click 'No show'.")
            .addTip("Always cancel rather than delete. A cancelled appointment frees the slot but "
                    + "keeps the history, and the no-show report depends on it.")
            .addTip("Completed, cancelled and no-show are final. If you mark the wrong one, ask an "
                    + "administrator - the change is recorded against your name."));

        /* ------------------------------------------------------------ */
        list.add(new HelpTopicDto("billing", "5. Calculating and printing the bill",
                "Producing the patient's bill and taking payment.",
                "Money", 5)
            .addStep("Open the appointment and check that it is marked Completed.")
            .addStep("Click 'Generate bill'. The system calculates the total from the dentist's "
                     + "consultation fee and the treatment price - you never type an amount.")
            .addStep("If a discount has been approved, enter the percentage and the reason. "
                     + "The maximum is 50%.")
            .addStep("Check the preview. Every line is shown separately: consultation, treatment, "
                     + "any surcharge, the discount and VAT.")
            .addStep("Click 'Issue bill'. The bill number, for example INV-2026-000137, is issued.")
            .addStep("Click 'Print receipt' and hand the printout to the patient.")
            .addStep("Take payment, then click 'Record payment', enter the amount and choose cash, "
                     + "card, bank transfer or insurance.")
            .addTip("Part payments are allowed. The balance appears on the Outstanding Payments "
                    + "report until it is settled.")
            .addTip("A bill can only be issued once per appointment. To reprint, open the existing "
                    + "bill and click 'Print receipt' again.")
            .addTip("Senior citizens (65+) and children (under 18) receive an automatic concession "
                    + "on clinical treatment. It is applied for you and printed on the receipt. "
                    + "Cosmetic treatment is excluded."));

        /* ------------------------------------------------------------ */
        list.add(new HelpTopicDto("patients", "6. Patient records",
                "Keeping the patient master file accurate.",
                "Records", 6)
            .addStep("Choose Patients to search by name, patient number, telephone number or NIC.")
            .addStep("Open a patient to see their details and their complete visit history.")
            .addStep("Click Edit to correct an address or telephone number.")
            .addTip("Before registering somebody as new, always search first. One patient should "
                    + "have one record - that is what makes their history retrievable.")
            .addTip("Record the NIC when the patient has it with them. It is the only identifier "
                    + "that reliably distinguishes two people with the same name."));

        /* ------------------------------------------------------------ */
        list.add(new HelpTopicDto("reports", "7. Reports",
                "The five management reports and what each one is for.",
                "Management", 7)
            .addStep("Choose Reports and pick one from the list.")
            .addStep("Set the date range where one is offered, then click Generate.")
            .addStep("DAILY SCHEDULE - print at 07:45. Who is expected today and how many slots "
                     + "are still free.")
            .addStep("REVENUE ANALYSIS - invoiced against collected income. Watch the collection "
                     + "rate.")
            .addStep("DENTIST WORKLOAD - who is at capacity and who has room. Use it before "
                     + "recruiting.")
            .addStep("TREATMENT POPULARITY - which treatments earn their chair time.")
            .addStep("OUTSTANDING PAYMENTS - unpaid bills oldest first. Work the 90+ day band "
                     + "first.")
            .addTip("A report can cover at most one year at a time."));

        /* ------------------------------------------------------------ */
        list.add(new HelpTopicDto("exit", "8. Signing out and closing down",
                "Leaving the system safely at the end of your shift.",
                "Getting started", 8)
            .addStep("Finish or save whatever you are working on.")
            .addStep("Click your name at the top right, then 'Sign out'. In the desktop "
                     + "application, choose File, then Exit.")
            .addStep("Confirm when you are asked. The desktop application closes its connection to "
                     + "the server tidily.")
            .addTip("Signing out ends your session immediately. Simply closing the browser window "
                    + "leaves it alive until it times out - on a shared machine, sign out properly.")
            .addTip("Nothing is lost by signing out: every change is saved to the database the "
                    + "moment you click Save."));

        return list;
    }
}
