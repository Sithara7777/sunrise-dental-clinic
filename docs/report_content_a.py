"""Front matter and Task A of the CIS6003 WRIT1 report."""
from report_kit import *
import os

ASSET = os.path.join(os.path.dirname(os.path.abspath(__file__)), "assets")


# ===================================================================== #
def title_page(doc, student):
    """Same layout as the reference report: logos, module line, ID block."""
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.0
    icbt = os.path.join(ASSET, "logo_icbt.png")
    cardiff = os.path.join(ASSET, "logo_cardiff.png")
    if os.path.exists(icbt):
        p.add_run().add_picture(icbt, height=Inches(1.15))
    p.add_run("          ")
    if os.path.exists(cardiff):
        p.add_run().add_picture(cardiff, height=Inches(1.15))

    para(doc, "", space_after=40)
    para(doc, "CIS6003 - Advanced Programming", size=20, bold=True,
         align="center", space_after=10)
    para(doc, "Assessment WRIT1", size=16, align="center", space_after=36)
    para(doc, "Sunrise Dental Clinic", size=18, bold=True, align="center",
         space_after=6)
    para(doc, "Appointment and Patient Management System",
         size=15, align="center", space_after=8)
    para(doc, "A distributed, three-tier Java application", size=12,
         italic=True, align="center", space_after=56)

    para(doc, f"Student Name: {student['name']}", size=12.5, align="center",
         space_after=4)
    para(doc, f"Cardiff Student ID: {student['cardiff_id']}", size=12.5,
         align="center", space_after=4)
    para(doc, f"ICBT Student ID: {student['icbt_id']}", size=12.5,
         align="center", space_after=4)
    para(doc, f"Module Leader: {student['leader']}", size=12.5,
         align="center", space_after=4)
    para(doc, f"Academic Year: {student['year']}", size=12.5, align="center",
         space_after=4)
    para(doc, f"Submitted: {student['submitted']}", size=12.5, align="center")
    page_break(doc)


# ===================================================================== #
def acknowledgement(doc, student):
    para(doc, "ACKNOWLEDGEMENT", size=16, bold=True, align="center",
         space_after=24)
    for t in [
        "I would like to express my sincere gratitude to everyone who helped me "
        "complete this appointment and patient management system for Sunrise "
        "Dental Clinic.",

        f"First and foremost, I extend my deepest thanks to {student['leader']}, "
        "my Module Leader for the Advanced Programming module, whose teaching on "
        "design patterns, layered architecture and test-driven development shaped "
        "almost every decision in this project. The habit of asking \"which class "
        "should own this rule?\" before writing any code came directly from those "
        "sessions, and it is the single thing that kept the system tidy as it "
        "grew.",

        "I am grateful to ICBT Campus and the School of Technologies for "
        "providing an excellent academic environment, and to Cardiff "
        "Metropolitan University for the structure of the programme, which "
        "pushed me to think about professional standards rather than only about "
        "getting the program to run.",

        "I also want to thank the open-source community. This project stands on "
        "work I did not have to write myself: the Spring Boot, Hibernate and "
        "Flyway projects, the JUnit 5, Mockito and AssertJ maintainers, and the "
        "H2 and MySQL teams. Their documentation was often clearer than any "
        "textbook.",

        "Finally, I would like to thank my family and my classmates. Explaining "
        "a problem out loud to somebody else was, more than once, how I found "
        "the answer to it.",
    ]:
        para(doc, t, space_after=12)
    page_break(doc)


# ===================================================================== #
def contents(doc, figures, tables_list):
    heading(doc, "Table of Contents", 1)
    toc_field(doc)
    page_break(doc)

    heading(doc, "Table of Figures", 1)
    table(doc, ["Figure", "Description"],
          [[f, d] for f, d in figures], widths=[1.3, 7.5], font_size=11)
    page_break(doc)

    heading(doc, "Table of Tables", 1)
    table(doc, ["Table", "Description"],
          [[t, d] for t, d in tables_list], widths=[1.3, 7.5], font_size=11)
    page_break(doc)


# ===================================================================== #
def task_a(doc):
    heading(doc, "Task A - System Design with UML Diagrams", 1)
    question_box(doc, "Task A  (20 marks)  -  from the assessment brief", [
        "“Provide the UML diagrams for the given problem with clear "
        "explanations of the design decisions. Derive detailed Use Case "
        "diagram, Class diagram and sequence diagram. Whenever necessary "
        "document the relevant assumptions you made. (LO I)”",
    ])

    heading(doc, "1.1 Understanding the problem", 2)
    para(doc,
         "The brief lists four symptoms of running a clinic on paper: double "
         "bookings, lost records, long waiting times and billing errors. They "
         "are four consequences of one thing - nobody can see, at the moment "
         "they need to, what is already true. So the job is not to store the "
         "same information in a computer, but to make the conflicting cases "
         "**impossible** rather than merely unlikely. That is why a database "
         "constraint, not an `if` statement, does the final work of preventing "
         "a double booking (2.4.1), and why an invoice total is calculated "
         "twice and compared (2.4.2).")

    heading(doc, "1.2 Assumptions made", 2)
    table(doc, ["#", "Assumption", "Reason"], [
        ["A1", "Only staff use the system; patients do not log in.",
         "The brief says only authorised staff may use it."],
        ["A2", "Three roles: Receptionist, Dentist, Administrator.",
         "One shared login makes the audit trail meaningless - “who "
         "cancelled this?” could never be answered."],
        ["A3", "One appointment = one visit, one dentist, one treatment.",
         "Keeps the appointment number a genuine unique identifier."],
        ["A4", "Fixed 30-minute slots, 08:00 to 20:00.",
         "No hours are given. A fixed grid makes “is this slot "
         "free?” a question with one correct answer."],
        ["A5", "Only a completed visit can be billed.",
         "Billing a visit that has not happened is exactly the billing error "
         "the clinic wants to escape."],
        ["A6", "Prices in LKR; VAT 15%.",
         "The clinic is in Colombo. One constant, so a tax change is a "
         "one-line change."],
        ["A7", "Cancelling or missing a visit frees the slot at once.",
         "A slot held by a cancelled visit cannot be sold, recreating the "
         "waiting-time problem."],
    ], widths=[0.9, 3.4, 5.3], font_size=10.5)

    heading(doc, "1.3 Use Case diagram", 2)
    para(doc,
         "Figure 1 shows three human actors and two system actors. The gateway "
         "is an actor because it sits outside the boundary and can fail "
         "independently; the scheduler because something must start the daily "
         "reminder run when no human is present. The two-column layout is "
         "deliberate - the left column holds every use case an actor touches "
         "directly, the right only those reached through a stereotype.")

    heading(doc, "1.3.1 Choosing between <<include>> and <<extend>>", 3)
    para(doc,
         "These two are often used interchangeably, which empties them of "
         "meaning. The rule applied is the standard one (Fowler, 2003): "
         "`<<include>>` happens **every** time; `<<extend>>` happens **only "
         "under a condition**, written on the arrow.")
    table(doc, ["Relationship", "Type", "Why"], [
        ["Register Appointment → Check Slot Availability", "<<include>>",
         "Checked on every booking; skipping it is the double-booking bug the "
         "clinic already has."],
        ["Register Appointment → Send Confirmation", "<<include>>",
         "Every booking confirms; if the gateway is down the attempt is still "
         "logged."],
        ["Calculate Bill → Apply Pricing Rule", "<<include>>",
         "Every bill runs exactly one pricing rule."],
        ["Register New Patient → Register Appointment", "<<extend>>",
         "[patient not already on file] - a returning patient is looked up."],
        ["Reschedule → Display Appointment Details", "<<extend>>",
         "[slot must change] - most lookups change nothing."],
        ["Print Receipt → Record Payment", "<<extend>>",
         "A patient paying by transfer may want no printed receipt."],
    ], widths=[3.4, 1.4, 4.4], font_size=10.5)
    para(doc,
         "One omission is deliberate and noted on the diagram: every use case "
         "inside the boundary needs a signed-in user, so each strictly includes "
         "Sign In, but twelve arrows to one bubble would hide the flow the "
         "diagram exists to show.")

    heading(doc, "1.4 Class diagram", 2)
    para(doc,
         "Figure 2 is drawn from the classes actually written - the attribute "
         "names, types and signatures are the ones in the source. Getters and "
         "setters are omitted, because listing seventy would bury the eight "
         "methods carrying the business rules. Every attribute is `private` "
         "(minus), every business method `public` (plus), and the fields "
         "inherited from `BaseEntity` are `protected` (hash). One method is "
         "deliberately private: `syncSlotLock()` maintains the field that "
         "prevents double booking, and a slot lock disagreeing with the "
         "appointment date would silently break that safeguard.")

    heading(doc, "1.4.1 Multiplicity, navigability, aggregation, composition", 3)
    bullet(doc,
           "**Multiplicity.** A Patient books `0..*` Appointments; an "
           "Appointment belongs to exactly `1`. An Appointment has `0..1` "
           "Invoice - zero while scheduled, one once billed - enforced by a "
           "unique key on `invoice.appointment_id`, so a visit cannot be billed "
           "twice.")
    bullet(doc,
           "**Navigability.** The arrow to Dentist means an Appointment knows "
           "its Dentist but not the reverse; a two-way link would drag a "
           "dentist's whole history into memory on every load. "
           "Patient-Appointment has no arrowhead because it genuinely is "
           "two-way - the patient screen shows their history.")
    bullet(doc,
           "**Aggregation** (hollow diamond) joins Patient to Appointment: "
           "deleting a patient must not silently delete the clinical record of "
           "visits they attended.")
    bullet(doc,
           "**Composition** (filled diamond) joins Invoice to InvoiceLine. A "
           "line reading “VAT 15%” means nothing apart from its "
           "invoice, so the parts die with the whole - and the foreign key "
           "carries `ON DELETE CASCADE`, so diagram and database agree.")
    para(doc,
         "The contrast between the last two is the point. Both read as "
         "“has many” and both would be a `List` in Java, but they "
         "behave differently on deletion, and the diagram is where that "
         "difference is decided.")

    heading(doc, "1.5 Sequence diagrams", 2)
    para(doc,
         "Three use cases are shown, because between them they cover the three "
         "hardest parts of the system - security, concurrency and business "
         "rules.")
    bullet(doc,
           "**Figure 3 (Sign In)** uses an `alt` fragment because sign-in has "
           "two real outcomes, and both end in the same place: an `audit_log` "
           "entry with the caller's IP. The same message is shown whether the "
           "username exists or not, so the page cannot be used to discover real "
           "accounts.")
    bullet(doc,
           "**Figure 4 (Register Appointment)** is the most important here. The "
           "`loop` fragment runs six handlers cheapest-first, so a booking in "
           "the past is rejected without touching the database; the `opt` "
           "fragment shows the slot being taken in the few milliseconds before "
           "the save - a window a paper diary cannot even express.")
    bullet(doc,
           "**Figure 5 (Calculate Bill)** shows the Template Method fragment: "
           "steps 6.1-6.6 always run in that order because the method is "
           "`final`, and only 6.2 and 6.3 differ per subclass.")

    heading(doc, "1.6 Critical reflection", 2)
    para(doc,
         "The three diagram types answer three questions - **who wants what**, "
         "**what the system is made of**, **what happens in what order** - and "
         "the design holds together only because they agree. One example is "
         "worth reporting honestly. The first class diagram had no `slotLock`; "
         "the anti-double-booking rule lived only in the sequence diagram, as a "
         "check before the save. Drawing that sequence carefully exposed the "
         "gap between step 3.4 and step 8 - a gap only something the class "
         "owned could close - so `slotLock` was added to the class and the "
         "schema as a direct result. The diagrams found a concurrency bug "
         "before a line of the service was written.")
    para(doc,
         "The main limitation is that the design models one clinic on one "
         "server. “One Appointment, one Dentist” would need "
         "revisiting if Sunrise opened a second branch, and the fixed 30-minute "
         "grid in A4 is a simplification a real orthodontic practice would "
         "outgrow.")
