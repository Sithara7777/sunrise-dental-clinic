"""Figures 3, 4 and 5 - Sequence diagrams for the three core operations.

Portrait, because a sequence diagram grows downwards: on a portrait page the
messages get the room they need and the whole diagram still fills the sheet.

Every message below is traced from the real code, not invented - the class
names, the method names and the order of the calls all match what actually
runs in the security filter chain, AppointmentService and BillingService.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from diagram_kit import *

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "diagrams")
os.makedirs(OUT, exist_ok=True)

XMAX, YMAX = 98.0, 138.0     # keeps one x-unit the same size as one y-unit
HEAD_TOP, HEAD_H = 126.0, 11.0
LIFE_BOTTOM = 18.0


def canvas(fig_title, sub, fs=19):
    fig, ax = new_canvas(A4_PORTRAIT, xlim=(0, XMAX), ylim=(0, YMAX))
    title(ax, fig_title, sub, y=YMAX - 3.0, fs=fs, sub_fs=11.5, sub_dy=3.6)
    return fig, ax


def participants(ax, spec, w):
    """spec: list of (key, x, name, stereotype).  Returns {key: x}."""
    for k, cx, name, st in spec:
        lifeline(ax, cx, HEAD_TOP, LIFE_BOTTOM, name, w=w, h=HEAD_H,
                 sub=st, fs=9.4, fc=FILL_C if k == "user" else FILL_B)
    return {k: cx for k, cx, _, _ in spec}


# ===================================================================== #
#  Figure 3 - Sign In                                                    #
# ===================================================================== #
def sign_in():
    with hand(scale=1.0, length=130, randomness=2.2):
        use_hand_font()
        fig, ax = canvas("Figure 3 - Sequence Diagram : Sign In",
                         "Use case UC-01   |   CIS6003 WRIT1   |   Task A")

        X = participants(ax, [
            ("user",   9.0, "Receptionist",   ":Actor"),
            ("ui",    25.0, "Login Page",     ":Thymeleaf view"),
            ("filter", 41.0, "Security Chain", ":Spring Security"),
            ("uds",   57.0, "User Details",   ":ClinicUserDetails\nService"),
            ("repo",  73.0, "User Store",     ":UserRepository"),
            ("audit", 89.0, "Audit Listener", ":Authentication\nAuditListener"),
        ], w=15.2)

        activation(ax, X["ui"],     111.0, 19.0)
        activation(ax, X["filter"], 101.0, 23.0)
        activation(ax, X["uds"],     95.0, 70.0)
        activation(ax, X["repo"],    90.0, 84.0)
        activation(ax, X["repo"],    49.0, 45.0)
        activation(ax, X["repo"],    33.0, 29.0)
        activation(ax, X["audit"],   54.0, 46.0)
        activation(ax, X["audit"],   38.0, 30.0)

        msg(ax, X["user"], X["ui"],     112.0, "1: opens /login")
        msg(ax, X["ui"], X["user"],     107.0, "2: sign-in form", ret=True)
        msg(ax, X["user"], X["filter"], 101.0,
            "3: POST /login\n(username, password, CSRF token)")
        msg(ax, X["filter"], X["uds"],   95.0, "4: loadUserByUsername(name)")
        msg(ax, X["uds"], X["repo"],     90.0, "5: findByUsername(name)")
        msg(ax, X["repo"], X["uds"],     84.0, "6: Optional<User>", ret=True)
        msg(ax, X["uds"], X["uds"],      79.0,
            "7: is the account active?\n    is it still locked out?", self_call=True)
        msg(ax, X["uds"], X["filter"],   70.0, "8: ClinicUserDetails", ret=True)
        msg(ax, X["filter"], X["filter"], 65.0,
            "9: BCryptPasswordEncoder\n     .matches(raw, hash)", self_call=True)

        # ---------------- alt : right password / wrong password ----------
        frame(ax, 15.0, 26.0, 80.0, 34.0, "alt",
              "[ matches  /  does not ]", fs=9.6)
        msg(ax, X["filter"], X["audit"], 54.0,
            "10: AuthenticationSuccessEvent")
        msg(ax, X["audit"], X["repo"],   48.0,
            "11: registerSuccessfulLogin()\n      clears the failure count")
        line(ax, 15.0, 42.0, 95.0, 42.0, lw=1.6, ls=(0, (5, 4)), color=ACCENT,
             zorder=4)
        ax.text(16.0, 40.8, "[ the password is wrong ]", fontsize=9.4,
                color=ACCENT2, va="top", zorder=6)
        msg(ax, X["filter"], X["audit"], 38.0,
            "12: AuthenticationFailureEvent")
        msg(ax, X["audit"], X["repo"],   32.0,
            "13: registerFailedLogin()\n      locks the account after 5 tries")

        msg(ax, X["filter"], X["ui"],   23.0,
            "14: redirect /dashboard  or  /login?error", ret=True)
        msg(ax, X["ui"], X["user"],     19.0,
            "15: dashboard  or  the error message", ret=True)

        note(ax, 3.0, 2.0, 44.0, 13.0,
             "Task C - security\nA password is never stored or compared as plain\n"
             "text. Only its BCrypt hash is held, and the same wrong\n"
             "message is shown whether the username exists or not, so\n"
             "the login page cannot be used to discover valid accounts.",
             fs=9.0)
        note(ax, 50.0, 2.0, 45.0, 13.0,
             "Both branches are audited\nEvery attempt, successful or not, is written to\n"
             "audit_log with the caller's IP address. The update\nruns in its own transaction so that two people\n"
             "signing in at once cannot corrupt each other.", fs=9.0)

        save(fig, os.path.join(OUT, "fig03_seq_signin.png"))


# ===================================================================== #
#  Figure 4 - Register New Appointment                                   #
# ===================================================================== #
def register_appointment():
    with hand(scale=1.0, length=130, randomness=2.2):
        use_hand_font()
        fig, ax = canvas(
            "Figure 4 - Sequence Diagram : Register Appointment",
            "Use case UC-02   |   CIS6003 WRIT1   |   Task A", fs=17.5)

        X = participants(ax, [
            ("user",  7.5,  "Receptionist", ":Actor"),
            ("ctrl",  21.0, "Controller",   ":Appointment\nWebController"),
            ("svc",   34.5, "Service",      ":Appointment\nService"),
            ("chain", 48.0, "Validation",   ":BookingValidation\nChain"),
            ("seq",   61.5, "Numbering",    ":SequenceGenerator\nService"),
            ("repo",  75.0, "Repository",   ":Appointment\nRepository"),
            ("pub",   88.5, "Observers",    ":AppointmentEvent\nPublisher"),
        ], w=13.2)

        activation(ax, X["ctrl"],  111.0, 20.0)
        activation(ax, X["svc"],   106.0, 23.0)
        activation(ax, X["chain"], 100.0, 61.0)
        activation(ax, X["seq"],    57.0, 49.0)
        activation(ax, X["repo"],   41.5, 32.0)
        activation(ax, X["pub"],    28.5, 23.0)

        msg(ax, X["user"], X["ctrl"], 112.0, "1: submits the booking form")
        msg(ax, X["ctrl"], X["svc"],  106.0, "2: register(AppointmentRequest)")
        msg(ax, X["svc"], X["chain"], 100.0, "3: validateOrThrow(request)")

        # ---------------- loop over the six handlers ---------------------
        frame(ax, 38.0, 63.0, 58.0, 32.0, "loop",
              "[ each handler, cheapest first ]", fs=9.6)
        for y, t in [(90.5, "3.1  BookingWindowHandler"),
                     (87.2, "        not in the past, at most 90 days ahead"),
                     (83.0, "3.2  ClinicHoursHandler"),
                     (79.7, "        08:00 - 20:00, and inside the dentist's hours"),
                     (75.5, "3.3  SlotAlignmentHandler"),
                     (72.2, "        must start on a 30-minute boundary"),
                     (68.0, "3.4  Dentist / Patient DoubleBookingHandler")]:
            ax.text(39.2, y, t, fontsize=9.0, color=INK, va="center", zorder=6)
        ax.text(39.2, 64.6,
                "the first failure stops the chain and is what the user sees",
                fontsize=8.8, color=ACCENT2, va="center", zorder=6,
                style="italic")

        msg(ax, X["chain"], X["svc"], 60.5, "4: ValidationOutcome.ok()", ret=True)
        msg(ax, X["svc"], X["seq"],   57.0, "5: nextAppointmentNumber()")
        msg(ax, X["seq"], X["seq"],   52.5,
            "6: SELECT ... FOR UPDATE\n     on number_sequence", self_call=True)
        msg(ax, X["seq"], X["svc"],   49.0, "7: \"APT-2026-000123\"", ret=True)
        msg(ax, X["svc"], X["repo"],  41.5,
            "8: saveAndFlush(appointment)\n     slot_lock = \"2026-08-20|09:30\"")

        frame(ax, 64.0, 33.0, 32.0, 9.5, "opt",
              "[ slot just taken ]", fs=9.4)
        ax.text(65.2, 36.6, "8.1  the unique key on\n"
                            "        (dentist_id, slot_lock)\n"
                            "        refuses it  ->  HTTP 409",
                fontsize=8.8, color=ACCENT2, va="center", zorder=6)

        msg(ax, X["repo"], X["svc"], 31.5, "9: the saved Appointment", ret=True)
        msg(ax, X["svc"], X["pub"],  28.5, "10: publish(BOOKED)")
        msg(ax, X["pub"], X["pub"],  25.0,
            "11: e-mail, SMS and audit observers\none failing cannot stop the others      ",
            self_call=True, self_side="left")
        msg(ax, X["svc"], X["ctrl"], 22.5, "12: AppointmentDto", ret=True)
        msg(ax, X["ctrl"], X["user"], 19.5, "13: confirmation page", ret=True)

        note(ax, 3.0, 1.5, 92.0, 13.5,
             "Two independent defences stop the same slot being sold twice.  Step 3.4 asks the question;\n"
             "step 8 settles it.  The unique key on (dentist_id, slot_lock) cannot be talked round, so even\n"
             "two receptionists pressing Save in the very same instant end up with one booking and one\n"
             "clear \"slot unavailable\" message.  Cancelling a visit sets slot_lock back to NULL, and because\n"
             "NULLs never compare equal in SQL, the freed slot becomes bookable again straight away.",
             fs=8.7)

        save(fig, os.path.join(OUT, "fig04_seq_appointment.png"))


# ===================================================================== #
#  Figure 5 - Calculate and Print Bill                                   #
# ===================================================================== #
def calculate_bill():
    with hand(scale=1.0, length=130, randomness=2.2):
        use_hand_font()
        fig, ax = canvas(
            "Figure 5 - Sequence Diagram : Calculate the Bill",
            "Use case UC-06   |   CIS6003 WRIT1   |   Task A", fs=17.5)

        X = participants(ax, [
            ("user",  7.5,  "Receptionist", ":Actor"),
            ("ctrl",  21.0, "Controller",   ":Billing\nWebController"),
            ("svc",   34.5, "Service",      ":BillingService"),
            ("fact",  48.0, "Factory",      ":PricingStrategy\nFactory"),
            ("strat", 61.5, "Strategy",     ":AbstractPricing\nStrategy"),
            ("dao",   75.0, "Database",     ":ReportingDao\n(H2 / MySQL)"),
            ("repo",  88.5, "Repository",   ":Invoice\nRepository"),
        ], w=13.2)

        activation(ax, X["ctrl"],  111.0, 20.0)
        activation(ax, X["svc"],   105.0, 24.0)
        activation(ax, X["fact"],   92.0, 88.0)
        activation(ax, X["strat"],  85.0, 45.0)
        activation(ax, X["dao"],    41.0, 36.0)
        activation(ax, X["repo"],   32.0, 27.0)

        msg(ax, X["user"], X["ctrl"], 112.0,
            "1: clicks \"Generate Bill\" on a\ncompleted appointment")
        msg(ax, X["ctrl"], X["svc"],  105.0, "2: generateBill(BillingRequest)")
        msg(ax, X["svc"], X["svc"],   100.0,
            "3: appointment.isBillable()   and\n     \"has it been invoiced already?\"",
            self_call=True)
        msg(ax, X["svc"], X["fact"],   92.0,
            "4: resolve(treatment.pricingStrategyKey)")
        msg(ax, X["fact"], X["svc"],   88.0,
            "5: the matching PricingStrategy", ret=True)
        msg(ax, X["svc"], X["strat"],  85.0, "6: calculate(PricingContext)")

        # ---------------- the template method ----------------------------
        frame(ax, 52.0, 46.0, 44.0, 34.0, "Template Method", fs=9.4)
        ax.text(53.2, 75.4, "calculate() is final, so a subclass can never\n"
                            "change the order of the six steps below",
                fontsize=8.8, color=ACCENT, va="center", zorder=6,
                style="italic")
        for y, t in [(70.0, "6.1  consultation fee + treatment base price"),
                     (66.3, "6.2  calculateSurcharge()        subclass decides"),
                     (62.6, "6.3  resolveDiscount()              subclass decides"),
                     (58.9, "6.4  cap the total discount at 50%"),
                     (55.2, "6.5  VAT 15% on the discounted amount"),
                     (51.5, "6.6  build the invoice lines")]:
            ax.text(53.2, y, t, fontsize=9.0, color=INK, va="center", zorder=6)

        msg(ax, X["strat"], X["svc"], 44.0, "7: PricingResult", ret=True)
        msg(ax, X["svc"], X["dao"],   41.0,
            "8: fn_invoice_total(...)\n     the same sum, worked out in SQL")
        msg(ax, X["dao"], X["svc"],   36.0,
            "9: total  -  must agree, or the bill is refused", ret=True)
        msg(ax, X["svc"], X["repo"],  32.0, "10: saveAndFlush(invoice + lines)")
        msg(ax, X["repo"], X["svc"],  28.0, "11: the saved Invoice", ret=True)
        msg(ax, X["svc"], X["ctrl"],  24.0, "12: InvoiceDto", ret=True)
        msg(ax, X["ctrl"], X["user"], 20.0, "13: printable receipt", ret=True)

        note(ax, 3.0, 1.5, 92.0, 15.5,
             "Strategy in action.  BillingService never learns which rule ran.  STANDARD, SURGICAL, COSMETIC\n"
             "and EMERGENCY each answer steps 6.2 and 6.3 differently - a surgical case adds a theatre\n"
             "surcharge, an emergency adds an out-of-hours loading, a cosmetic case gets no age concession -\n"
             "yet all four are called through the same line of code.  Adding a fifth rule means writing one new\n"
             "class, and not one line of this diagram changes.  Step 8 is deliberate duplication: the Java total\n"
             "and the SQL total are worked out separately and compared, so a rounding mistake in either one\n"
             "is caught long before a patient is ever handed the bill.", fs=8.7)

        save(fig, os.path.join(OUT, "fig05_seq_billing.png"))


if __name__ == "__main__":
    sign_in()
    register_appointment()
    calculate_bill()
