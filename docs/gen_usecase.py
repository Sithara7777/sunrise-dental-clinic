"""Figure 1 - Use Case diagram for the Sunrise Dental Clinic system.

Layout rule that keeps it readable: column A holds every use case an actor
touches directly, column B holds only the supporting use cases reached through
<<include>> / <<extend>>. No actor line ever has to cross into column B.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from diagram_kit import *

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "diagrams")
os.makedirs(OUT, exist_ok=True)

CA, WA = 41.0, 23.0          # column A centre / width
CB, WB = 77.0, 21.0          # column B centre / width
HH = 7.4                     # bubble height

# key -> (centre x, centre y, width, label)
UC = {
    # ---- column A : actor facing --------------------------------------
    "login":    (CA, 66.0, WA, "Sign In\n(Authenticate)"),
    "register": (CA, 56.5, WA, "Register New\nAppointment"),
    "search":   (CA, 47.0, WA, "Display Appointment\nDetails"),
    "status":   (CA, 37.5, WA, "Update Visit\nStatus"),
    "bill":     (CA, 28.0, WA, "Calculate &\nPrint Bill"),
    "pay":      (CA, 18.5, WA, "Record\nPayment"),
    "reports":  (CA,  9.0, WA, "View Management\nReports"),
    # ---- column B : reached only by include / extend -------------------
    "newpat":   (CB, 61.0, WB, "Register New\nPatient"),
    "slots":    (CB, 51.5, WB, "Check Slot\nAvailability"),
    "notify":   (CB, 42.0, WB, "Send Confirmation\nMessage"),
    "resched":  (CB, 32.5, WB, "Reschedule\nAppointment"),
    "pricing":  (CB, 23.0, WB, "Apply Pricing\nRule"),
    "receipt":  (CB, 13.5, WB, "Print Receipt"),
}
SUB = {"newpat", "slots", "notify", "resched", "pricing", "receipt"}


def L(k):
    cx, cy, w, _ = UC[k]; return cx - w / 2, cy


def R(k):
    cx, cy, w, _ = UC[k]; return cx + w / 2, cy


def build():
    with hand(scale=1.25, length=115, randomness=2.9):
        use_hand_font()
        fig, ax = new_canvas(A4_LANDSCAPE, xlim=(0, 120), ylim=(0, 84))

        title(ax, "Figure 1 - Use Case Diagram : Sunrise Dental Clinic System",
              "Appointment & Patient Management   |   CIS6003 WRIT1   |   Task A")

        # ------------------------------------------------ system boundary
        box(ax, 26.0, 2.0, 66.0, 71.0, "", fc="#fdfefe", ec=ACCENT, lw=2.6,
            radius=1.0, zorder=1)
        ax.text(59.0, 71.2, "Sunrise Dental Clinic System", ha="center",
                va="center", fontsize=13.0, color=ACCENT, weight="bold",
                zorder=2)

        # ------------------------------------------------ use cases
        for k, (cx, cy, w, lab) in UC.items():
            oval(ax, cx, cy, w, HH, lab,
                 fc=FILL_C if k in SUB else FILL_B, fs=10.3, zorder=3)

        # ------------------------------------------------ actors
        actor(ax, 10.5, 66.0, "Receptionist", scale=1.05)
        actor(ax, 10.5, 46.0, "Dentist", scale=1.05)
        actor(ax, 10.5, 29.0, "Administrator", scale=1.05)
        actor(ax, 107.0, 51.0, "E-mail / SMS\nGateway", scale=0.95,
              color="#5a6b7a", fs=11)
        actor(ax, 107.0, 33.0, "Reminder\nScheduler", scale=0.95,
              color="#5a6b7a", fs=11)
        ax.text(107.0, 56.6, "<<system>>", ha="center", fontsize=9.8,
                color="#5a6b7a", style="italic")
        ax.text(107.0, 38.6, "<<system>>", ha="center", fontsize=9.8,
                color="#5a6b7a", style="italic")

        # ------------------------------------------------ associations
        REC, DEN, ADM = (14.0, 63.5), (14.0, 43.5), (14.0, 26.5)
        for a, k in [(REC, "login"), (REC, "register"), (REC, "search"),
                     (REC, "bill"), (REC, "pay"),
                     (DEN, "login"), (DEN, "search"), (DEN, "status"),
                     (ADM, "login"), (ADM, "bill"), (ADM, "reports")]:
            line(ax, a[0], a[1], *L(k), lw=1.55, color="#5b6f80", zorder=2)

        # ------------------------------------------------ <<include>>
        for a, b in [("register", "slots"), ("register", "notify"),
                     ("bill", "pricing")]:
            arrow(ax, *R(a), *L(b), color=ACCENT, lw=1.8, ls=(0, (5, 3)),
                  head="->", zorder=5)

        # ------------------------------------------------ <<extend>>
        for a, b in [("newpat", "register"), ("resched", "search"),
                     ("receipt", "pay")]:
            arrow(ax, *L(a), *R(b), color=ACCENT2, lw=1.8, ls=(0, (5, 3)),
                  head="->", zorder=5)

        def stereo(x, y, text, color, rot=0, fs=9.4):
            ax.text(x, y, text, fontsize=fs, color=color, rotation=rot,
                    rotation_mode="anchor", ha="center", va="center", zorder=6,
                    bbox=dict(boxstyle="round,pad=0.22", fc="white",
                              ec="none", alpha=0.95))

        stereo(59.0, 55.0, "<<include>>", ACCENT, rot=-11)
        stereo(59.0, 48.6, "<<include>>", ACCENT, rot=-27)
        stereo(59.0, 26.0, "<<include>>", ACCENT, rot=-11)
        stereo(59.0, 59.6, "<<extend>>", ACCENT2, rot=11)
        stereo(59.0, 40.6, "<<extend>>", ACCENT2, rot=27)
        stereo(59.0, 16.6, "<<extend>>", ACCENT2, rot=11)

        ax.text(62.5, 63.5, "[patient is not\n already on file]", fontsize=8.7,
                color=ACCENT2, ha="center", va="center", zorder=6,
                linespacing=1.35)
        ax.text(65.5, 36.4, "[slot must\n change]", fontsize=8.7,
                color=ACCENT2, ha="center", va="center", zorder=6,
                linespacing=1.35)

        # ------------------------------------------------ secondary actors
        line(ax, *R("notify"), 102.0, 48.0, lw=1.6, color="#5a6b7a")
        line(ax, *R("notify"), 102.0, 36.0, lw=1.6, color="#5a6b7a")
        ax.text(95.0, 46.6, "delivers", fontsize=9.3, color="#5a6b7a",
                rotation=25, ha="center", va="center",
                bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="none"))
        ax.text(95.5, 38.2, "wakes daily", fontsize=9.3, color="#5a6b7a",
                rotation=-19, ha="center", va="center",
                bbox=dict(boxstyle="round,pad=0.2", fc="white", ec="none"))

        # ------------------------------------------------ legend
        lx, ly = 1.5, 1.5
        box(ax, lx, ly, 22.5, 16.8, "", fc="#fbfcfd", ec="#9fb0bd", lw=1.8)
        ax.text(lx + 11.2, ly + 15.2, "Key", ha="center", fontsize=11.5,
                weight="bold", color=INK)
        line(ax, lx + 1.4, ly + 12.6, lx + 6.6, ly + 12.6, lw=1.6,
             color="#5b6f80")
        ax.text(lx + 7.4, ly + 12.6, "association", fontsize=9.5, va="center")
        arrow(ax, lx + 1.4, ly + 9.8, lx + 6.6, ly + 9.8, color=ACCENT,
              lw=1.7, ls=(0, (5, 3)), head="->")
        ax.text(lx + 7.4, ly + 9.8, "<<include>>  always", fontsize=9.5,
                va="center", color=ACCENT)
        arrow(ax, lx + 1.4, ly + 7.0, lx + 6.6, ly + 7.0, color=ACCENT2,
              lw=1.7, ls=(0, (5, 3)), head="->")
        ax.text(lx + 7.4, ly + 7.0, "<<extend>>  only if", fontsize=9.5,
                va="center", color=ACCENT2)
        oval(ax, lx + 4.0, ly + 4.1, 5.0, 2.6, "", fc=FILL_B)
        ax.text(lx + 7.4, ly + 4.1, "actor-facing", fontsize=9.5, va="center")
        oval(ax, lx + 4.0, ly + 1.4, 5.0, 2.6, "", fc=FILL_C)
        ax.text(lx + 7.4, ly + 1.4, "supporting", fontsize=9.5, va="center")

        # ------------------------------------------------ assumption note
        note(ax, 95.0, 3.0, 23.5, 17.0,
             "Assumption\nEvery use case inside the\nboundary needs a signed-in\n"
             "user. Sign In is joined to the\nthree actors once, instead of\n"
             "being <<include>>-ed by all\ntwelve use cases - that would\n"
             "hide the flow the diagram is\nmeant to show.", fs=9.2)

        save(fig, os.path.join(OUT, "fig01_use_case.png"))


if __name__ == "__main__":
    build()
