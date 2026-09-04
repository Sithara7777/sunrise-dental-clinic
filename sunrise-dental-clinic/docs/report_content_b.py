"""Task B of the CIS6003 WRIT1 report - design patterns and architecture."""
from report_kit import *


def task_b(doc):
    heading(doc, "Task B - Design Patterns and Architecture Development", 1)
    question_box(doc, "Task B  (40 marks)  -  from the assessment brief", [
        "“Develop an interactive System with set of interfaces to get the "
        "necessary user inputs. Make sure to implement proper validation "
        "mechanisms in order to restrict invalid entries to the system. Come up "
        "with a suitable set of reports, which you think add more value to your "
        "system.  i. Your program must be a distributed application with web "
        "services  ii. Appropriate design patterns must be implemented in your "
        "system  iii. Your program should make use of a proper database to "
        "store information. (LO II)”",
    ])

    heading(doc, "2.1 Three-tier architecture", 2)
    para(doc,
         "Figure 6 shows the architecture. The tiers are not three packages "
         "with tidy names; they are separated by a rule applied consistently "
         "and open to checking: **a class may only call downwards.** The "
         "practical test of a layered design is whether you can delete the top "
         "layer and still compile the rest - and here you can, because the "
         "desktop client is a different Maven module that does not depend on "
         "the server at compile time at all.")
    table(doc, ["Tier", "What lives there", "What is forbidden there"], [
        ["Presentation",
         "Thymeleaf controllers, REST controllers, the Swing client, templates, "
         "CSS.",
         "Business rules - a controller that decided a discount would put it "
         "out of reach of the desktop client."],
        ["Business",
         "Services, `ClinicFacade`, pricing strategies, the validation chain, "
         "observers, security.",
         "SQL and HTTP. It does not know it is reached over the web, which is "
         "why the same rules serve both front ends."],
        ["Data",
         "Spring Data repositories, `ReportingDao`, Flyway migrations, views, "
         "functions, procedures, triggers.",
         "Decisions. It stores and constrains only."],
    ], widths=[1.2, 4.2, 4.3], font_size=10.5)
    para(doc,
         "Three Maven modules - `dental-common`, `dental-server` and "
         "`dental-client` - make the build mirror the architecture. Because the "
         "client depends only on `dental-common`, reaching into a server class "
         "from the client is impossible: the compiler stops it. Architecture "
         "enforced by the build is worth more than architecture enforced by "
         "good intentions.")

    heading(doc, "2.2 A genuinely distributed application with web services", 2)
    para(doc,
         "This requirement is easy to satisfy in appearance only, by bolting a "
         "REST controller onto an ordinary web application. Here it is taken "
         "literally: `dental-client` is a Swing application in its own process, "
         "with its own JVM, started with the server's address as an argument "
         "and able to run on a different machine. It has no compile-time "
         "knowledge of any server class, no shared session and no shared object "
         "graph - only HTTP and JSON.")
    para(doc,
         "The contract is versioned at `/api/v1`, so a future `/api/v2` can "
         "change shape without breaking an installed client, and every response "
         "uses one envelope - success flag, payload, error code, message - so "
         "the client handles failure in one place. It is documented with "
         "OpenAPI 3 at `/swagger-ui.html` (Figure 17). One consequence deserves "
         "a mention: an API caller must get `401` with a JSON body while a "
         "browser user must be redirected to the login page, so Spring Security "
         "runs **two separate filter chains**, ordered so `/api/**` matches "
         "first. A single chain would have sent HTML login pages to the desktop "
         "client.")

    heading(doc, "2.3 Design patterns applied", 2)
    table(doc, ["Pattern", "Where", "What it buys"], [
        ["Strategy", "`PricingStrategy` + 4 implementations",
         "A new rule is a new class, not an edit to a growing `if`."],
        ["Template Method", "`AbstractPricingStrategy.calculate()`",
         "The order of the billing steps is `final`."],
        ["Factory", "`PricingStrategyFactory`, `ReportGeneratorFactory`",
         "Callers name what they want; nobody writes `new`."],
        ["Chain of Responsibility", "`BookingValidationChain` (6 handlers)",
         "Each booking rule is independent and testable."],
        ["Observer", "`AppointmentEventPublisher` + 3 observers",
         "A failing SMS gateway cannot stop a patient being booked."],
        ["Adapter", "`MessageGateway` (console / SMTP / SMS)",
         "A real SMS provider needs no change above the adapter."],
        ["Facade", "`ClinicFacade`",
         "Turns a multi-call sequence into one round trip."],
        ["Builder", "`Appointment.Builder`",
         "An appointment cannot be half-built."],
        ["Singleton", "Spring-managed beans", "One managed instance per service."],
        ["Command", "Desktop menu and toolbar actions",
         "Menu bar and button panel cannot drift apart."],
        ["DAO / Repository", "Spring Data repositories, `ReportingDao`",
         "Business code never contains SQL."],
        ["DTO", "Everything in `dental-common`",
         "Entities never leave the server."],
        ["MVC", "Thymeleaf controllers, templates, domain model",
         "Request handling is separated from rendering."],
    ], widths=[1.8, 3.2, 4.5], font_size=10.5)

    heading(doc, "2.3.1 Strategy and Template Method together", 3)
    para(doc,
         "Billing was the natural home for Strategy: four treatment categories "
         "have different surcharges and concession rules, and as one method "
         "that is a long `if/else` chain growing with every new category - the "
         "classic case the pattern exists to solve (Gamma et al., 1995). What "
         "makes it more than a textbook exercise is the combination with "
         "Template Method. As pure Strategy each rule would compute its own "
         "total, and nothing would stop one applying VAT before the discount "
         "instead of after. `calculate()` is therefore `final` and performs the "
         "invariant sequence: fees, surcharge, discount, a 50% cap, VAT, then "
         "the invoice lines, with only `calculateSurcharge()` and "
         "`resolveDiscount()` left abstract. A subclass decides **how much** "
         "but never **in what order** - and since VAT on a discounted amount "
         "differs from VAT on an undiscounted one, that distinction is money.")

    heading(doc, "2.3.2 Chain of Responsibility, and Observer", 3)
    para(doc,
         "Six conditions must hold before a booking is accepted. As one method "
         "that is six nested conditions with a single failure message; as a "
         "chain it is six small classes in a fixed order, and the order is a "
         "deliberate performance decision logged at start-up: `BOOKING_WINDOW → "
         "CLINIC_HOURS → SLOT_ALIGNMENT → DENTIST_AVAILABILITY → "
         "DENTIST_DOUBLE_BOOKING → PATIENT_DOUBLE_BOOKING`. The first three "
         "need no database access, so a booking for last Tuesday is rejected "
         "without a query, and the chain short-circuits on the first failure so "
         "staff are told the most fundamental thing that is wrong.")
    para(doc,
         "Observer solves a different problem. Calling the e-mail, SMS and "
         "audit code directly from `AppointmentService` means that if the SMS "
         "gateway is unreachable the exception propagates and the booking rolls "
         "back - the patient loses their appointment because a text message "
         "failed. `AppointmentEventPublisher` isolates each observer's failure, "
         "logs it and carries on, and every attempt is written to "
         "`notification_log` so staff can see the SMS failed and telephone "
         "instead.")

    heading(doc, "2.3.3 Critical evaluation - what the patterns cost", 3)
    bullet(doc,
           "**Indirection.** Following a bill now means opening four files "
           "where a beginner would have written one. For four pricing rules "
           "that is worth it; for one it would not have been.")
    bullet(doc,
           "**Debugging is harder.** With Observer a stack trace no longer "
           "shows who will react to an event, which is why the registered "
           "observers are logged at start-up.")
    bullet(doc,
           "**A wrong key can fail quietly.** If reference data names a "
           "strategy that does not exist, the factory could return `null` and "
           "fail far from the cause. It instead falls back to the standard rule "
           "**and logs a warning**.")
    bullet(doc,
           "**Patterns can be over-applied.** Singleton appears only because "
           "Spring manages bean lifecycles; hand-writing one would add global "
           "state for no benefit. Naming a pattern is not the same as needing "
           "it.")

    heading(doc, "2.4 The database", 2)
    para(doc,
         "Figure 7 shows the physical schema: nine tables, five views, two "
         "functions, two stored procedures and four triggers. The schema is "
         "owned by versioned Flyway scripts and Hibernate runs "
         "`ddl-auto=validate`, so the application refuses to start if the "
         "entities and the real schema disagree. Letting Hibernate generate the "
         "schema would make the database a by-product of the code; this way it "
         "is reviewable SQL under version control.")

    heading(doc, "2.4.1 How double booking is actually prevented", 3)
    para(doc,
         "The validation chain checks whether a slot is free, but between that "
         "check and the insert there is a window of a few milliseconds in which "
         "another transaction can commit the same slot. Any solution living "
         "only in Java has this window. The fix is a column `slot_lock` and a "
         "unique constraint `uk_appointment_slot (dentist_id, slot_lock)`: "
         "while an appointment occupies a chair `slot_lock` holds "
         "`\"date|time\"`, and when the visit is cancelled `syncSlotLock()` "
         "sets it back to `NULL`. The trick is that **SQL never treats two "
         "NULLs as equal**, so any number of cancelled appointments can share a "
         "dentist and slot while two live ones cannot. A database constraint "
         "has no race window - and this was verified rather than assumed: "
         "twelve genuinely simultaneous requests for one slot gave exactly one "
         "success and eleven clean `409` responses, against three fresh "
         "databases.")

    heading(doc, "2.4.2 Advanced database features", 3)
    table(doc, ["Feature", "Name", "Business rule it implements"], [
        ["Function", "`FN_INVOICE_TOTAL`",
         "Recomputes an invoice total in SQL; every bill is checked against "
         "it, so a rounding mistake in Java is caught first."],
        ["Function", "`FN_AGEING_BUCKET`",
         "Puts a debt in a 0-30 / 31-60 / 61-90 / 90+ day band; the report "
         "calls it, so it cannot disagree with an ad-hoc query."],
        ["Procedure", "`SP_SETTLE_INVOICE`",
         "Applies a payment and updates the status atomically."],
        ["Procedure", "`SP_DAILY_CLOSING_SUMMARY`",
         "Produces the end-of-day figures needed to cash up."],
        ["Trigger", "`trg_appointment_before_insert`",
         "Refuses a booking outside the dentist's hours, even from a direct "
         "SQL insert (MySQL)."],
        ["Triggers", "`trg_appointment_audit_*`, `trg_invoice_payment_audit`",
         "Audit at database level, so a change made outside the application "
         "is still recorded."],
        ["Views", "5 reporting views",
         "Reports read views, so “revenue” is defined once."],
        ["Constraints", "FK, UNIQUE, CHECK on every table",
         "Discounts <= 50%, payments <= total, status must be permitted."],
    ], widths=[1.0, 2.8, 5.5], font_size=10.5)
    para(doc,
         "Two engines are supported from one codebase: H2 in file mode by "
         "default, so the application runs with nothing to install, and MySQL 8 "
         "through a Spring profile. The MySQL path was tested end to end "
         "against a real MariaDB 10.4 server - all four migrations, both "
         "functions, both procedures and every trigger exercised with real "
         "data, not merely read through.")

    heading(doc, "2.5 Interfaces and validation", 2)
    para(doc,
         "Two complete front ends were built. The browser application "
         "(Figures 8 to 17) covers all six required functions plus patient, "
         "dentist, treatment, report and administration screens, using a "
         "hand-written stylesheet with no CDN dependency so the clinic stays "
         "usable if its internet drops. The desktop client is menu-driven, as "
         "the brief asks, with the Command pattern behind the menu bar and the "
         "button panel.")
    para(doc,
         "Validation is applied in four places, on the principle that each "
         "layer should refuse what it can prove is wrong: HTML5 `required`, "
         "`type` and `pattern` attributes in the **browser**; Bean Validation "
         "annotations on the **DTOs**, so the desktop client enforces identical "
         "rules with no duplicated code; the six-handler chain in the "
         "**business tier**; and CHECK, UNIQUE and FOREIGN KEY constraints plus "
         "the MySQL trigger in the **database**, which hold even against a "
         "direct SQL statement. Messages are written for a receptionist, not a "
         "developer: instead of a constraint violation the user is told which "
         "slot is taken and what to do about it.")

    heading(doc, "2.6 Reports that add value", 2)
    table(doc, ["Report", "Question it answers", "Decision it supports"], [
        ["Daily Schedule", "Who is coming in today, to which dentist?",
         "Staffing the desk; preparing surgeries."],
        ["Revenue", "What did the clinic take, by day and treatment?",
         "Whether a treatment is worth continuing to offer."],
        ["Outstanding Invoices", "Who owes money, and for how long?",
         "Which debts to chase first."],
        ["Dentist Workload", "How busy is each dentist?",
         "Rebalancing the rota; whether to recruit."],
        ["Treatment Popularity", "Which treatments are most in demand?",
         "Stock ordering; continuing-education choices."],
    ], widths=[1.8, 3.7, 4.2], font_size=10.5)
    para(doc,
         "Each is justified by the decision it supports rather than the data it "
         "contains. All five share one envelope class, which is why one web "
         "page and one desktop window display any of them, and why a sixth "
         "report would need no user-interface work at all.")

    heading(doc, "2.7 Sessions and cookies", 2)
    para(doc,
         "Three uses are implemented, each solving a real problem rather than "
         "demonstrating a mechanism. The **HTTP session** carries the signed-in "
         "user and a “recently viewed” trail, so a receptionist "
         "interrupted mid-task can return to where they were. An opt-in "
         "**remember-me cookie** saves the front-desk machine signing in every "
         "morning, and a **preference cookie** stores the chosen table density. "
         "A CSRF token is required on every form, so a stolen session cookie "
         "alone cannot submit one.")
