"""
Editable draw.io sources for all seven figures.

These are the same diagrams as the PNGs in docs/diagrams, expressed as
.drawio files so a marker (or a future maintainer) can open, check and edit
them.  Every shape carries draw.io's sketch styling, so the hand-drawn look
survives the round trip.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from drawio_kit import *

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "drawio")
os.makedirs(OUT, exist_ok=True)

FILL_B = "#E8F4F5"
FILL_C = "#FDF3D8"
FILL_D = "#DFE8EE"


# =====================================================================
# Figure 1 - Use case
# =====================================================================
def use_case():
    d = Drawio("Fig 1 - Use Case", 122, 84)
    d.text(20, 82, 82, 4, "Figure 1 - Use Case Diagram : Sunrise Dental Clinic System",
           fs=22, bold=True)
    d.text(20, 77.5, 82, 3.5,
           "Appointment & Patient Management   |   CIS6003 WRIT1   |   Task A",
           fs=13, color=GREY)

    d.box(26, 73, 66, 71, "Sunrise Dental Clinic System", fill="#FDFEFE",
          stroke=ACCENT, fs=15, valign="top")

    CA, CB, W, H = 41.0, 77.0, 23.0, 7.4
    UC = {
        "login":    (CA, 66.0, W, "Sign In (Authenticate)", FILL_B),
        "register": (CA, 56.5, W, "Register New Appointment", FILL_B),
        "search":   (CA, 47.0, W, "Display Appointment Details", FILL_B),
        "status":   (CA, 37.5, W, "Update Visit Status", FILL_B),
        "bill":     (CA, 28.0, W, "Calculate & Print Bill", FILL_B),
        "pay":      (CA, 18.5, W, "Record Payment", FILL_B),
        "reports":  (CA,  9.0, W, "View Management Reports", FILL_B),
        "newpat":   (CB, 61.0, 21.0, "Register New Patient", FILL_C),
        "slots":    (CB, 51.5, 21.0, "Check Slot Availability", FILL_C),
        "notify":   (CB, 42.0, 21.0, "Send Confirmation Message", FILL_C),
        "resched":  (CB, 32.5, 21.0, "Reschedule Appointment", FILL_C),
        "pricing":  (CB, 23.0, 21.0, "Apply Pricing Rule", FILL_C),
        "receipt":  (CB, 13.5, 21.0, "Print Receipt", FILL_C),
    }
    ids = {}
    for k, (cx, cy, w, lab, fc) in UC.items():
        ids[k] = d.ellipse(cx - w / 2, cy + H / 2, w, H, lab, fill=fc)

    a_rec = d.actor(10.5, 66.0, 6, 8, "Receptionist")
    a_den = d.actor(10.5, 46.0, 6, 8, "Dentist")
    a_adm = d.actor(10.5, 29.0, 6, 8, "Administrator")
    a_gw = d.actor(107.0, 51.0, 6, 8, "E-mail / SMS Gateway", stroke=GREY)
    a_sch = d.actor(107.0, 33.0, 6, 8, "Reminder Scheduler", stroke=GREY)

    for a, k in [(a_rec, "login"), (a_rec, "register"), (a_rec, "search"),
                 (a_rec, "bill"), (a_rec, "pay"),
                 (a_den, "login"), (a_den, "search"), (a_den, "status"),
                 (a_adm, "login"), (a_adm, "bill"), (a_adm, "reports")]:
        d.edge(a, ids[k], style="edgeStyle=none;" + E_PLAIN + "strokeColor=#5B6F80;")

    for a, b in [("register", "slots"), ("register", "notify"),
                 ("bill", "pricing")]:
        d.edge(ids[a], ids[b], "<<include>>",
               style="edgeStyle=none;" + E_INCLUDE)
    for a, b in [("newpat", "register"), ("resched", "search"),
                 ("receipt", "pay")]:
        d.edge(ids[a], ids[b], "<<extend>>",
               style="edgeStyle=none;" + E_EXTEND)

    d.edge(ids["notify"], a_gw, "delivers",
           style="edgeStyle=none;" + E_PLAIN + "strokeColor=#5A6B7A;")
    d.edge(ids["notify"], a_sch, "wakes daily",
           style="edgeStyle=none;" + E_PLAIN + "strokeColor=#5A6B7A;")

    d.note(1.5, 18.5, 22.5, 17,
           "Key\n"
           "———  association\n"
           "- - >  <<include>>  always\n"
           "- - >  <<extend>>   only if\n"
           "blue oval = actor-facing\n"
           "cream oval = supporting", fs=11)
    d.note(95, 20, 23.5, 17,
           "Assumption\nEvery use case inside the boundary needs a "
           "signed-in user. Sign In is joined to the three actors once, "
           "instead of being <<include>>-ed by all twelve use cases.", fs=11)
    d.save(os.path.join(OUT, "fig01_use_case.drawio"))


# =====================================================================
# Figure 2 - Class diagram
# =====================================================================
def class_diagram():
    d = Drawio("Fig 2 - Class Diagram", 122, 84)
    d.text(20, 82, 82, 4, "Figure 2 - Class Diagram : Domain Model",
           fs=22, bold=True)

    base, _ = d.uml_class(40, 76, 30, "BaseEntity",
                          ["# id : Long", "# createdAt : LocalDateTime",
                           "# updatedAt : LocalDateTime", "# version : long"],
                          ["+ getId() : Long", "+ isNew() : boolean",
                           "# onCreate() : void"],
                          stereotype="abstract", header=FILL_D)

    cls = {}
    cls["patient"], _ = d.uml_class(1, 57.5, 26, "Patient", [
        "- patientCode : String", "- fullName : String", "- address : String",
        "- contactNumber : String", "- email : String", "- nic : String",
        "- gender : Gender", "- dateOfBirth : LocalDate"], [
        "+ isMinor() : boolean", "+ isSeniorCitizen() : boolean",
        "+ hasEmail() : boolean", "+ addAppointment(a) : void"])
    cls["appt"], _ = d.uml_class(33, 57.5, 27, "Appointment", [
        "- appointmentNumber : String", "- appointmentDate : LocalDate",
        "- appointmentTime : LocalTime", "- durationMinutes : int",
        "- status : AppointmentStatus", "- slotLock : String",
        "- notes : String", "- createdBy : String"], [
        "+ changeStatus(s, reason, by)", "+ reschedule(date, time, by)",
        "+ getEndTime() : LocalTime", "+ isBillable() : boolean",
        "+ isUpcoming() : boolean", "- syncSlotLock() : void"],
        header="#D8ECEE")
    cls["dentist"], _ = d.uml_class(66, 57.5, 26, "Dentist", [
        "- dentistCode : String", "- fullName : String",
        "- specialization : String", "- consultationFee : BigDecimal",
        "- slmcRegistrationNo : String", "- workStartTime : LocalTime",
        "- workEndTime : LocalTime", "- active : boolean"], [
        "+ isWithinWorkingHours(s, e)", "+ isActive() : boolean"])
    cls["user"], _ = d.uml_class(96, 57.5, 25, "User", [
        "- username : String", "- passwordHash : String",
        "- fullName : String", "- role : Role", "- active : boolean",
        "- failedLoginAttempts : int", "- lockedUntil : LocalDateTime",
        "- linkedDentistCode : String"], [
        "+ isLocked() : boolean", "+ registerFailedLogin()",
        "+ registerSuccessfulLogin()", "+ hasRole(r : Role) : boolean"])
    cls["line"], _ = d.uml_class(1, 32.5, 26, "InvoiceLine", [
        "- lineNumber : int", "- description : String", "- quantity : int",
        "- unitPrice : BigDecimal", "- lineTotal : BigDecimal",
        "- lineType : String"], ["+ InvoiceLine(desc, qty, price, type)"])
    cls["invoice"], _ = d.uml_class(33, 32.5, 27, "Invoice", [
        "- invoiceNumber : String", "- consultationFee : BigDecimal",
        "- treatmentCost : BigDecimal", "- surchargeAmount : BigDecimal",
        "- discountAmount : BigDecimal", "- taxAmount : BigDecimal",
        "- totalAmount : BigDecimal", "- amountPaid : BigDecimal",
        "- paymentStatus : PaymentStatus"], [
        "+ applyPayment(amt, method, ref)", "+ addLine(line : InvoiceLine)",
        "+ getBalanceDue() : BigDecimal", "+ cancel(reason : String)"],
        header="#D8ECEE")
    cls["treatment"], _ = d.uml_class(66, 32.5, 26, "Treatment", [
        "- code : String", "- name : String", "- category : String",
        "- basePrice : BigDecimal", "- durationMinutes : Integer",
        "- pricingStrategyKey : String", "- active : boolean"],
        ["+ isActive() : boolean"])

    en = {}
    en["role"] = d.uml_class(95, 37.5, 26, "Role",
                             ["+ ADMIN", "+ RECEPTIONIST", "+ DENTIST"], [],
                             stereotype="enumeration", header="#E3D3EC")[0]
    en["appt"] = d.uml_class(95, 27.8, 26, "AppointmentStatus",
                             ["+ SCHEDULED", "+ CONFIRMED", "+ IN_PROGRESS",
                              "+ COMPLETED", "+ CANCELLED", "+ NO_SHOW"], [],
                             stereotype="enumeration", header="#E3D3EC")[0]
    en["pay"] = d.uml_class(95, 14.2, 26, "PaymentStatus",
                            ["+ PENDING", "+ PARTIALLY_PAID", "+ PAID",
                             "+ CANCELLED"], [],
                            stereotype="enumeration", header="#E3D3EC")[0]

    for k in ("patient", "appt", "dentist", "user", "line", "invoice",
              "treatment"):
        d.edge(cls[k], base, style=E_GENERAL)

    d.edge(cls["patient"], cls["appt"], "books   1 .. 0..*",
           style="edgeStyle=none;" + E_AGGREG + f"fontColor={ACCENT};")
    d.edge(cls["appt"], cls["dentist"], "attended by   0..* .. 1",
           style="edgeStyle=none;" + E_OPEN + f"fontColor={ACCENT};")
    d.edge(cls["appt"], cls["treatment"], "is for   0..* .. 1",
           style="edgeStyle=none;" + E_OPEN + f"fontColor={ACCENT};")
    d.edge(cls["appt"], cls["invoice"], "billed by   1 .. 0..1",
           style="edgeStyle=none;" + E_OPEN + f"fontColor={ACCENT};")
    d.edge(cls["invoice"], cls["line"], "consists of   1 .. 1..*",
           style="edgeStyle=none;" + E_COMPOS + f"fontColor={ACCENT2};")
    d.edge(cls["user"], cls["dentist"], "<<linked by dentistCode>>",
           style="edgeStyle=none;" + E_DEP)
    d.edge(cls["user"], en["role"], "<<uses>>", style="edgeStyle=none;" + E_DEP)
    d.edge(cls["appt"], en["appt"], "<<uses>>", style="edgeStyle=none;" + E_DEP)
    d.edge(cls["invoice"], en["pay"], "<<uses>>",
           style="edgeStyle=none;" + E_DEP)

    d.note(1.5, 77.5, 36, 17,
           "Key\n"
           "hollow diamond = aggregation (part can live alone)\n"
           "filled diamond = composition (part dies with the whole)\n"
           "open arrow = association, arrow shows navigability\n"
           "hollow triangle = generalisation (inherits from)\n"
           "dashed arrow = dependency <<uses>>", fs=11)
    d.note(72, 77.5, 24, 17,
           "Visibility\n  + public   - private   # protected\n"
           "Multiplicity\n  1 exactly one\n  0..1 none or one\n"
           "  0..* none or many\n  1..* at least one", fs=11)
    d.save(os.path.join(OUT, "fig02_class.drawio"))


# =====================================================================
# Sequence diagrams (portrait)
# =====================================================================
def sequence(name, fname, subtitle, parts, msgs, frames, notes):
    """parts: [(key, x, label)]  msgs: [(src, dst, y, text, kind)]"""
    d = Drawio(name, 98, 138, scale=8.0, landscape=False)
    d.text(9, 136, 80, 4, name, fs=20, bold=True)
    d.text(9, 131, 80, 3.5, subtitle, fs=12, color=GREY)

    ids = {}
    for k, x, lab in parts:
        ids[k] = d.node(x - 7, 126, 14, 11, lab,
                        "shape=umlLifeline;perimeter=lifelinePerimeter;"
                        "container=1;collapsible=0;recursiveResize=0;"
                        f"outlineConnect=0;size=11;fillColor="
                        f"{'#FDF3D8' if k == 'user' else FILL_B};"
                        f"strokeColor={INK};strokeWidth=2;", fs=11)

    for x, y, w, h, tab, cond in frames:
        d.node(x, y + h, w, h, f"{tab}  {cond}",
               f"shape=umlFrame;width={w * 8 * 0.35};height=22;"
               f"fillColor=none;strokeColor={ACCENT};strokeWidth=2;dashed=1;",
               fs=11, valign="top", align="left")

    for src, dst, y, text, kind in msgs:
        style = {"call": E_ARROW,
                 "ret": "dashed=1;" + E_OPEN,
                 "self": E_ARROW}[kind]
        if kind == "self":
            d.line_edge(parts_x(parts, src), y, parts_x(parts, src) + 4, y,
                        text, style="edgeStyle=orthogonalEdgeStyle;" + style,
                        fs=11)
        else:
            d.line_edge(parts_x(parts, src), y, parts_x(parts, dst), y, text,
                        style="edgeStyle=none;" + style, fs=11)

    for x, y, w, h, t in notes:
        d.note(x, y + h, w, h, t, fs=11)
    d.save(os.path.join(OUT, fname))


def parts_x(parts, key):
    for k, x, _ in parts:
        if k == key:
            return x
    raise KeyError(key)


def sign_in():
    parts = [("user", 9, "Receptionist\n:Actor"),
             ("ui", 25, "Login Page\n:Thymeleaf view"),
             ("filter", 41, "Security Chain\n:Spring Security"),
             ("uds", 57, "User Details\n:ClinicUserDetailsService"),
             ("repo", 73, "User Store\n:UserRepository"),
             ("audit", 89, "Audit Listener\n:AuthenticationAuditListener")]
    msgs = [
        ("user", "ui", 112, "1: opens /login", "call"),
        ("ui", "user", 107, "2: sign-in form", "ret"),
        ("user", "filter", 101, "3: POST /login (username, password, CSRF)", "call"),
        ("filter", "uds", 95, "4: loadUserByUsername(name)", "call"),
        ("uds", "repo", 90, "5: findByUsername(name)", "call"),
        ("repo", "uds", 84, "6: Optional<User>", "ret"),
        ("uds", "uds", 79, "7: active? still locked out?", "self"),
        ("uds", "filter", 70, "8: ClinicUserDetails", "ret"),
        ("filter", "filter", 65, "9: BCryptPasswordEncoder.matches()", "self"),
        ("filter", "audit", 54, "10: AuthenticationSuccessEvent", "call"),
        ("audit", "repo", 48, "11: registerSuccessfulLogin()", "call"),
        ("filter", "audit", 38, "12: AuthenticationFailureEvent", "call"),
        ("audit", "repo", 32, "13: registerFailedLogin() - locks after 5", "call"),
        ("filter", "ui", 23, "14: redirect /dashboard or /login?error", "ret"),
        ("ui", "user", 19, "15: dashboard or the error message", "ret"),
    ]
    frames = [(15, 26, 80, 34, "alt", "[ matches / does not ]")]
    notes = [(3, 2, 44, 13,
              "Task C - security. A password is never stored or compared as "
              "plain text; only its BCrypt hash is held."),
             (50, 2, 45, 13,
              "Every attempt, successful or not, is written to audit_log with "
              "the caller's IP address.")]
    sequence("Figure 3 - Sequence Diagram : Sign In",
             "fig03_seq_signin.drawio",
             "Use case UC-01   |   CIS6003 WRIT1   |   Task A",
             parts, msgs, frames, notes)


def register_appointment():
    parts = [("user", 7.5, "Receptionist\n:Actor"),
             ("ctrl", 21, "Controller\n:AppointmentWebController"),
             ("svc", 34.5, "Service\n:AppointmentService"),
             ("chain", 48, "Validation\n:BookingValidationChain"),
             ("seq", 61.5, "Numbering\n:SequenceGeneratorService"),
             ("repo", 75, "Repository\n:AppointmentRepository"),
             ("pub", 88.5, "Observers\n:AppointmentEventPublisher")]
    msgs = [
        ("user", "ctrl", 112, "1: submits the booking form", "call"),
        ("ctrl", "svc", 106, "2: register(AppointmentRequest)", "call"),
        ("svc", "chain", 100, "3: validateOrThrow(request)", "call"),
        ("chain", "svc", 60.5, "4: ValidationOutcome.ok()", "ret"),
        ("svc", "seq", 57, "5: nextAppointmentNumber()", "call"),
        ("seq", "seq", 52.5, "6: SELECT ... FOR UPDATE", "self"),
        ("seq", "svc", 49, "7: APT-2026-000123", "ret"),
        ("svc", "repo", 41.5, "8: saveAndFlush(appointment)", "call"),
        ("repo", "svc", 31.5, "9: the saved Appointment", "ret"),
        ("svc", "pub", 28.5, "10: publish(BOOKED)", "call"),
        ("pub", "pub", 25, "11: e-mail, SMS, audit observers", "self"),
        ("svc", "ctrl", 22.5, "12: AppointmentDto", "ret"),
        ("ctrl", "user", 19.5, "13: confirmation page", "ret"),
    ]
    frames = [(38, 63, 58, 32, "loop", "[ each handler, cheapest first ]"),
              (64, 33, 32, 9.5, "opt", "[ slot just taken -> HTTP 409 ]")]
    notes = [(38, 66, 56, 12,
              "3.1 BookingWindowHandler - not in the past, at most 90 days\n"
              "3.2 ClinicHoursHandler - 08:00 to 20:00, inside the dentist's hours\n"
              "3.3 SlotAlignmentHandler - on a 30-minute boundary\n"
              "3.4 Dentist / Patient DoubleBookingHandler"),
             (3, 1.5, 92, 13.5,
              "Two independent defences stop the same slot being sold twice. "
              "Step 3.4 asks the question; step 8 settles it, because the unique "
              "key on (dentist_id, slot_lock) cannot be talked round.")]
    sequence("Figure 4 - Sequence Diagram : Register Appointment",
             "fig04_seq_appointment.drawio",
             "Use case UC-02   |   CIS6003 WRIT1   |   Task A",
             parts, msgs, frames, notes)


def calculate_bill():
    parts = [("user", 7.5, "Receptionist\n:Actor"),
             ("ctrl", 21, "Controller\n:BillingWebController"),
             ("svc", 34.5, "Service\n:BillingService"),
             ("fact", 48, "Factory\n:PricingStrategyFactory"),
             ("strat", 61.5, "Strategy\n:AbstractPricingStrategy"),
             ("dao", 75, "Database\n:ReportingDao"),
             ("repo", 88.5, "Repository\n:InvoiceRepository")]
    msgs = [
        ("user", "ctrl", 112, "1: clicks Generate Bill", "call"),
        ("ctrl", "svc", 105, "2: generateBill(BillingRequest)", "call"),
        ("svc", "svc", 100, "3: isBillable? already invoiced?", "self"),
        ("svc", "fact", 92, "4: resolve(pricingStrategyKey)", "call"),
        ("fact", "svc", 88, "5: the matching PricingStrategy", "ret"),
        ("svc", "strat", 85, "6: calculate(PricingContext)", "call"),
        ("strat", "svc", 44, "7: PricingResult", "ret"),
        ("svc", "dao", 41, "8: fn_invoice_total(...)", "call"),
        ("dao", "svc", 36, "9: total - must agree", "ret"),
        ("svc", "repo", 32, "10: saveAndFlush(invoice + lines)", "call"),
        ("repo", "svc", 28, "11: the saved Invoice", "ret"),
        ("svc", "ctrl", 24, "12: InvoiceDto", "ret"),
        ("ctrl", "user", 20, "13: printable receipt", "ret"),
    ]
    frames = [(52, 46, 44, 34, "Template Method",
               "calculate() is final - the order cannot change")]
    notes = [(53, 50, 42, 26,
              "6.1 consultation fee + treatment base price\n"
              "6.2 calculateSurcharge()  - subclass decides\n"
              "6.3 resolveDiscount()     - subclass decides\n"
              "6.4 cap the total discount at 50%\n"
              "6.5 VAT 15% on the discounted amount\n"
              "6.6 build the invoice lines"),
             (3, 1.5, 92, 15.5,
              "Strategy in action. BillingService never learns which rule ran. "
              "STANDARD, SURGICAL, COSMETIC and EMERGENCY each answer 6.2 and "
              "6.3 differently, yet all four are called through the same line "
              "of code. Step 8 is deliberate duplication: the Java total and "
              "the SQL total are compared before the bill is issued.")]
    sequence("Figure 5 - Sequence Diagram : Calculate the Bill",
             "fig05_seq_billing.drawio",
             "Use case UC-06   |   CIS6003 WRIT1   |   Task A",
             parts, msgs, frames, notes)


# =====================================================================
# Figure 6 - Architecture
# =====================================================================
def architecture():
    d = Drawio("Fig 6 - Architecture", 122, 84)
    d.text(20, 82, 82, 4, "Figure 6 - Three-Tier Architecture and Deployment",
           fs=21, bold=True)
    d.text(20, 77.5, 82, 3.5, "CIS6003 WRIT1   |   Task B", fs=13, color=GREY)

    for y, h, fc, nm in [(74, 17.5, "#FDF0E4", "PRESENTATION TIER"),
                         (54.5, 26, "#E8F4F5", "BUSINESS TIER"),
                         (25.5, 23, "#EEF3EA", "DATA TIER")]:
        d.box(1.5, y, 119, h, nm, fill=fc, stroke="#9FB0BD", fs=14,
              valign="top")

    for x, lab in [(13, "Web Browser Client\n\nThymeleaf pages, hand-written "
                        "CSS.\nNo CDN, so it works offline."),
                   (49, "Menu-Driven Desktop Client\n\nJava Swing in its own "
                        "JVM.\nCommand pattern behind the menu."),
                   (85, "Any Other Client\n\nSwagger UI, curl, a mobile app -\n"
                        "the same versioned REST contract.")]:
        d.box(x, 70, 34, 11.5, lab, fs=12)

    for cx, lab in [(30, "HTTP session + CSRF"),
                    (66, "HTTP + JSON only"),
                    (102, "HTTP Basic + JSON")]:
        d.line_edge(cx, 58.5, cx, 53.5, lab,
                    style="edgeStyle=none;startArrow=block;startFill=1;"
                          f"endArrow=block;endFill=1;strokeColor={ACCENT2};"
                          f"fontColor={ACCENT2};", fs=11)

    d.box(13, 53.3, 106, 5.8,
          "Spring Boot server   -   dental-server   -   one deployable jar",
          fill="#D3E9EB", fs=14)

    cols = [
        ("Web Controllers", "Appointment-, Billing-, Patient-, Report-\n"
                            "WebController. Return HTML pages and redirect to "
                            "/login when nobody is signed in."),
        ("REST Controllers", "/api/v1/... One response envelope and one error "
                             "code per failure. Return JSON 401, never an HTML "
                             "login page."),
        ("Security", "Two filter chains. BCrypt. Three roles. Session + "
                     "remember-me. CSRF. Lock-out after five tries. Every "
                     "attempt audited."),
        ("Services + Facade", "AppointmentService, BillingService, "
                              "ReportService, PatientService, ClinicFacade. "
                              "@Transactional lives here."),
        ("Design Patterns", "Strategy, Template Method, Factory, Chain of "
                            "Responsibility, Observer, Adapter, Facade, "
                            "Builder, Command, Singleton, DAO, DTO, MVC."),
    ]
    for i, (head, body) in enumerate(cols):
        d.box(13 + i * 21.4, 46.5, 20, 16.5, f"{head}\n\n{body}", fs=11,
              valign="top")

    d.line_edge(66, 30, 66, 25.5, "JPA / Hibernate + JDBC  (ddl-auto=validate)",
                style="edgeStyle=none;startArrow=block;startFill=1;"
                      f"endArrow=block;endFill=1;strokeColor={ACCENT2};"
                      f"fontColor={ACCENT2};", fs=11)

    for x, w, lab in [(13, 33, "Repositories + DAO\n\nSpring Data JPA "
                               "interfaces, plus ReportingDao for native SQL"),
                      (49, 34, "Flyway migrations\n\nV1 schema, V2 reference "
                               "data, V3 views + functions, V4 triggers"),
                      (86, 33, "H2 (default) / MySQL 8\n\nThe same Java "
                               "against two engines, chosen by a profile")]:
        d.box(x, 24, w, 8, lab, fs=11)

    d.box(13, 14.5, 106, 10.5,
          "Work the database is trusted to do for itself\n"
          "9 tables with FK and CHECK constraints   |   uk_appointment_slot "
          "(dentist_id, slot_lock)   |   5 views   |   "
          "2 functions (FN_INVOICE_TOTAL, FN_AGEING_BUCKET)   |   "
          "2 procedures   |   4 triggers",
          fill="#F5F9F2", stroke="#8AA382", fs=11)
    d.save(os.path.join(OUT, "fig06_architecture.drawio"))


# =====================================================================
# Figure 7 - ER diagram
# =====================================================================
def er_diagram():
    d = Drawio("Fig 7 - ER Diagram", 122, 84)
    d.text(20, 82, 82, 4,
           "Figure 7 - Entity-Relationship Diagram : Physical Schema",
           fs=21, bold=True)
    d.text(20, 77.5, 82, 3.5,
           "9 tables  |  5 views  |  2 functions  |  2 procedures  |  "
           "4 triggers      -      CIS6003 WRIT1  |  Task B", fs=12, color=GREY)

    T = {}

    def tbl(key, x, y, w, name, cols, header="#D8ECEE"):
        T[key], _ = d.uml_class(x, y, w, name, cols, [], header=header,
                                fs=10, line_h=1.35)

    tbl("patient", 1.5, 75, 27, "patient", [
        "PK  id : BIGINT", "UK  patient_code : VARCHAR(20)",
        "      full_name : VARCHAR(120)", "      address : VARCHAR(200)",
        "      contact_number : VARCHAR(20)", "      email : VARCHAR(120)",
        "      nic : VARCHAR(20)", "      gender : VARCHAR(15)",
        "      date_of_birth : DATE", "      medical_notes : VARCHAR(500)"])
    tbl("dentist", 1.5, 55, 27, "dentist", [
        "PK  id : BIGINT", "UK  dentist_code : VARCHAR(20)",
        "      full_name : VARCHAR(120)", "      specialization : VARCHAR(80)",
        "      consultation_fee : DECIMAL(10,2)",
        "      slmc_reg_no : VARCHAR(30)", "      work_start_time : TIME",
        "      work_end_time : TIME", "      active : BOOLEAN"])
    tbl("treatment", 1.5, 36, 27, "treatment", [
        "PK  id : BIGINT", "UK  code : VARCHAR(20)",
        "      name : VARCHAR(120)", "      category : VARCHAR(40)",
        "      base_price : DECIMAL(10,2)",
        "      duration_minutes : INTEGER",
        "      pricing_strategy_key : VARCHAR(20)", "      active : BOOLEAN"])
    tbl("appt", 33, 75, 30, "appointment", [
        "PK  id : BIGINT", "UK  appointment_number : VARCHAR(20)",
        "FK  patient_id : BIGINT", "FK  dentist_id : BIGINT",
        "FK  treatment_id : BIGINT", "      appointment_date : DATE",
        "      appointment_time : TIME", "      duration_minutes : INTEGER",
        "      status : VARCHAR(20)", "UK  slot_lock : VARCHAR(30)",
        "      notes : VARCHAR(500)",
        "      cancellation_reason : VARCHAR(300)",
        "      created_by : VARCHAR(30)", "      version : BIGINT"],
        header="#C2E0E4")
    tbl("user", 33, 49, 30, "app_user", [
        "PK  id : BIGINT", "UK  username : VARCHAR(40)",
        "      password_hash : VARCHAR(100)", "      full_name : VARCHAR(120)",
        "      role : VARCHAR(20)", "      active : BOOLEAN",
        "      failed_login_attempts : INTEGER",
        "      locked_until : TIMESTAMP",
        "      linked_dentist_code : VARCHAR(20)"])
    tbl("audit", 33, 30, 30, "audit_log", [
        "PK  id : BIGINT", "      occurred_at : TIMESTAMP",
        "      username : VARCHAR(40)", "      action : VARCHAR(60)",
        "      entity_type : VARCHAR(40)", "      entity_key : VARCHAR(40)",
        "      detail : VARCHAR(500)", "      ip_address : VARCHAR(45)"])
    tbl("invoice", 66, 75, 30, "invoice", [
        "PK  id : BIGINT", "UK  invoice_number : VARCHAR(20)",
        "FK  appointment_id : BIGINT UNIQUE",
        "      patient_name : VARCHAR(120)",
        "      dentist_name : VARCHAR(120)",
        "      treatment_name : VARCHAR(120)",
        "      consultation_fee : DECIMAL(10,2)",
        "      treatment_cost : DECIMAL(10,2)",
        "      surcharge_amount : DECIMAL(10,2)",
        "      discount_amount : DECIMAL(10,2)",
        "      tax_amount : DECIMAL(10,2)",
        "      total_amount : DECIMAL(10,2)",
        "      amount_paid : DECIMAL(10,2)",
        "      payment_status : VARCHAR(20)"], header="#C2E0E4")
    tbl("line", 66, 49, 30, "invoice_line", [
        "PK  id : BIGINT", "FK  invoice_id : BIGINT",
        "      line_number : INTEGER", "      description : VARCHAR(200)",
        "      quantity : INTEGER", "      unit_price : DECIMAL(10,2)",
        "      line_total : DECIMAL(10,2)", "      line_type : VARCHAR(20)"])
    tbl("notif", 99, 75, 22, "notification_log", [
        "PK  id : BIGINT", "      channel : VARCHAR(10)",
        "      recipient : VARCHAR(120)", "      subject : VARCHAR(200)",
        "      status : VARCHAR(15)", "      reference_key : VARCHAR(40)",
        "      created_at : TIMESTAMP"])
    tbl("seq", 99, 57, 22, "number_sequence", [
        "PK  seq_key : VARCHAR(40)", "      next_value : BIGINT",
        "      updated_at : TIMESTAMP"])

    d.edge(T["patient"], T["appt"], "books", style="edgeStyle=none;" + E_ONE_MANY)
    d.edge(T["dentist"], T["appt"], "attends", style=E_ONE_MANY)
    d.edge(T["treatment"], T["appt"], "is for", style=E_ONE_MANY)
    d.edge(T["appt"], T["invoice"], "billed by  1 : 0..1",
           style="edgeStyle=none;" + E_ONE_ONE)
    d.edge(T["invoice"], T["line"], "consists of - ON DELETE CASCADE",
           style="edgeStyle=none;" + E_ONE_MANY +
                 f"strokeColor={ACCENT2};fontColor={ACCENT2};")

    d.note(1.5, 16, 27, 13,
           "Key\nPK  primary key\nFK  foreign key\n"
           "UK  unique business identifier\n|—<  one to many", fs=11)
    d.note(33, 14.5, 30, 11.5,
           "The constraint that matters most: uk_appointment_slot "
           "(dentist_id, slot_lock). slot_lock holds \"date|time\" while a "
           "visit occupies the chair and NULL once cancelled - and NULLs "
           "never clash in SQL.", fs=11)
    d.note(66, 33, 55, 30,
           "Also part of the schema, but not tables\n\n"
           "Views: v_daily_schedule, v_revenue_daily, v_dentist_workload, "
           "v_treatment_popularity, v_outstanding_invoice\n\n"
           "Functions: FN_INVOICE_TOTAL, FN_AGEING_BUCKET\n\n"
           "Procedures: SP_SETTLE_INVOICE, SP_DAILY_CLOSING_SUMMARY (MySQL)\n\n"
           "Triggers: audit an appointment on insert and update, audit every "
           "payment, and on MySQL refuse any booking outside the dentist's "
           "working hours.\n\n"
           "app_user.linked_dentist_code matches dentist.dentist_code but is "
           "deliberately not a foreign key.", fs=11)
    d.save(os.path.join(OUT, "fig07_er_diagram.drawio"))


if __name__ == "__main__":
    use_case()
    class_diagram()
    sign_in()
    register_appointment()
    calculate_bill()
    architecture()
    er_diagram()
