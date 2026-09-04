"""Figure 2 - Domain Class diagram (drawn from the real entity classes)."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from diagram_kit import *

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "diagrams")
os.makedirs(OUT, exist_ok=True)

TOP1 = 57.5          # top edge of row 1
TOP2 = 32.5          # top edge of row 2
BUS1 = 59.5          # generalisation bus feeding row 1
BUS2 = 34.2          # generalisation bus feeding row 2


def enum_box(ax, x, y, w, name, values, fs=9.4):
    line_h = 1.28
    head_h = 3.6
    h = head_h + len(values) * line_h + 0.9
    y0 = y - h
    sharp_box(ax, x, y0, w, h, fc="#f5eef9", ec=INK)
    sharp_box(ax, x, y - head_h, w, head_h, fc="#e3d3ec", ec=INK)
    ax.text(x + w / 2, y - 1.15, "<<enumeration>>", ha="center", va="center",
            fontsize=fs - 0.6, color="#5a6b7a", style="italic", zorder=4)
    ax.text(x + w / 2, y - 2.7, name, ha="center", va="center",
            fontsize=fs + 1.6, color=INK, weight="bold", zorder=4)
    cy = y - head_h - 0.95
    for v in values:
        ax.text(x + 0.55, cy, v, ha="left", va="center", fontsize=fs,
                color=INK, zorder=4)
        cy -= line_h
    return x, y0, w, h


def build():
    with hand(scale=1.05, length=125, randomness=2.4):
        use_hand_font()
        fig, ax = new_canvas(A4_LANDSCAPE, xlim=(0, 122), ylim=(0, 84))

        title(ax, "Figure 2 - Class Diagram : Domain Model")

        # ===================== BaseEntity (top centre) =====================
        class_box(ax, 40.0, 76.0, 30.0, "BaseEntity",
                  ["# id : Long",
                   "# createdAt : LocalDateTime",
                   "# updatedAt : LocalDateTime",
                   "# version : long"],
                  ["+ getId() : Long",
                   "+ isNew() : boolean",
                   "# onCreate() : void"],
                  stereotype="<<abstract>>", header="#dfe8ee")

        # ===================== Row 1 =====================
        class_box(ax, 1.0, TOP1, 26.0, "Patient",
                  ["- patientCode : String",
                   "- fullName : String",
                   "- address : String",
                   "- contactNumber : String",
                   "- email : String",
                   "- nic : String",
                   "- gender : Gender",
                   "- dateOfBirth : LocalDate"],
                  ["+ isMinor() : boolean",
                   "+ isSeniorCitizen() : boolean",
                   "+ hasEmail() : boolean",
                   "+ addAppointment(a) : void"])

        class_box(ax, 33.0, TOP1, 27.0, "Appointment",
                  ["- appointmentNumber : String",
                   "- appointmentDate : LocalDate",
                   "- appointmentTime : LocalTime",
                   "- durationMinutes : int",
                   "- status : AppointmentStatus",
                   "- slotLock : String",
                   "- notes : String",
                   "- createdBy : String"],
                  ["+ changeStatus(s, reason, by)",
                   "+ reschedule(date, time, by)",
                   "+ getEndTime() : LocalTime",
                   "+ isBillable() : boolean",
                   "+ isUpcoming() : boolean",
                   "- syncSlotLock() : void"],
                  header="#d8ecee")

        class_box(ax, 66.0, TOP1, 26.0, "Dentist",
                  ["- dentistCode : String",
                   "- fullName : String",
                   "- specialization : String",
                   "- consultationFee : BigDecimal",
                   "- slmcRegistrationNo : String",
                   "- workStartTime : LocalTime",
                   "- workEndTime : LocalTime",
                   "- active : boolean"],
                  ["+ isWithinWorkingHours(s, e)",
                   "+ isActive() : boolean"])

        class_box(ax, 96.0, TOP1, 25.0, "User",
                  ["- username : String",
                   "- passwordHash : String",
                   "- fullName : String",
                   "- role : Role",
                   "- active : boolean",
                   "- failedLoginAttempts : int",
                   "- lockedUntil : LocalDateTime",
                   "- linkedDentistCode : String"],
                  ["+ isLocked() : boolean",
                   "+ registerFailedLogin()",
                   "+ registerSuccessfulLogin()",
                   "+ hasRole(r : Role) : boolean"])

        # ===================== Row 2 =====================
        class_box(ax, 1.0, TOP2, 26.0, "InvoiceLine",
                  ["- lineNumber : int",
                   "- description : String",
                   "- quantity : int",
                   "- unitPrice : BigDecimal",
                   "- lineTotal : BigDecimal",
                   "- lineType : String"],
                  ["+ InvoiceLine(desc, qty,",
                   "        price, type)"])

        class_box(ax, 33.0, TOP2, 27.0, "Invoice",
                  ["- invoiceNumber : String",
                   "- consultationFee : BigDecimal",
                   "- treatmentCost : BigDecimal",
                   "- surchargeAmount : BigDecimal",
                   "- discountAmount : BigDecimal",
                   "- taxAmount : BigDecimal",
                   "- totalAmount : BigDecimal",
                   "- amountPaid : BigDecimal",
                   "- paymentStatus : PaymentStatus"],
                  ["+ applyPayment(amt, method, ref)",
                   "+ addLine(line : InvoiceLine)",
                   "+ getBalanceDue() : BigDecimal",
                   "+ cancel(reason : String)"],
                  header="#d8ecee")

        class_box(ax, 66.0, TOP2, 26.0, "Treatment",
                  ["- code : String",
                   "- name : String",
                   "- category : String",
                   "- basePrice : BigDecimal",
                   "- durationMinutes : Integer",
                   "- pricingStrategyKey : String",
                   "- active : boolean"],
                  ["+ isActive() : boolean"])

        # ===================== generalisation tree =====================
        ax.add_patch(Polygon([(55.0, 62.0), (52.7, 59.5), (57.3, 59.5)],
                             closed=True, facecolor="white", edgecolor=INK,
                             linewidth=2.0, zorder=5))
        line(ax, 14.0, BUS1, 108.5, BUS1, lw=2.0, zorder=2)
        for cx in (14.0, 46.5, 79.0, 108.5):
            line(ax, cx, BUS1, cx, TOP1, lw=2.0, zorder=2)
        # branch down the left gutter to InvoiceLine + Invoice
        line(ax, 30.0, BUS1, 30.0, BUS2, lw=2.0, zorder=2)
        line(ax, 14.0, BUS2, 40.0, BUS2, lw=2.0, zorder=2)
        line(ax, 14.0, BUS2, 14.0, TOP2, lw=2.0, zorder=2)
        line(ax, 40.0, BUS2, 40.0, TOP2, lw=2.0, zorder=2)
        # branch down the right gutter to Treatment
        line(ax, 63.0, BUS1, 63.0, 33.6, lw=2.0, zorder=2)
        line(ax, 63.0, 33.6, 74.0, 33.6, lw=2.0, zorder=2)
        line(ax, 74.0, 33.6, 74.0, TOP2, lw=2.0, zorder=2)
        ax.text(58.5, 61.0, "generalisation", fontsize=9.2, color="#5a6b7a",
                ha="left", va="center", style="italic")

        # ===================== associations =====================
        def mult(x, y, t, ha="center"):
            ax.text(x, y, t, fontsize=9.6, color=INK, ha=ha, va="center",
                    zorder=6, bbox=dict(boxstyle="round,pad=0.15", fc="white",
                                        ec="none", alpha=0.95))

        def rel(x, y, t, color=ACCENT, rot=0, ha="center"):
            ax.text(x, y, t, fontsize=9.8, color=color, ha=ha, va="center",
                    style="italic", rotation=rot, zorder=6,
                    bbox=dict(boxstyle="round,pad=0.18", fc="white",
                              ec="none", alpha=0.95))

        # Patient  <>--  Appointment    (aggregation)
        line(ax, 27.0, 48.0, 33.0, 48.0, lw=2.0, zorder=3)
        diamond(ax, 28.4, 48.0, 1.35, 0.95, filled=False)
        mult(27.4, 50.0, "1", ha="left")
        mult(32.6, 50.0, "0..*", ha="right")
        rel(30.0, 45.6, "books")

        # Appointment  -->  Dentist
        arrow(ax, 60.0, 48.0, 66.0, 48.0, color=INK, lw=2.0, head="->",
              zorder=3)
        mult(60.4, 50.0, "0..*", ha="left")
        mult(65.6, 50.0, "1", ha="right")
        rel(63.0, 45.6, "attended by")

        # Appointment  -->  Treatment
        arrow(ax, 60.0, 40.0, 69.5, 32.5, color=INK, lw=2.0, head="->",
              zorder=3)
        mult(60.6, 40.8, "0..*", ha="left")
        mult(70.4, 33.8, "1", ha="left")
        rel(65.6, 35.4, "is for", rot=-32)

        # Appointment  -->  Invoice
        arrow(ax, 50.0, 35.7, 50.0, 32.5, color=INK, lw=2.0, head="->",
              zorder=3)
        mult(51.0, 35.3, "1", ha="left")
        mult(51.0, 33.0, "0..1", ha="left")
        rel(48.6, 34.1, "billed by", ha="right")

        # Invoice  <#>--  InvoiceLine   (composition)
        line(ax, 27.0, 22.0, 33.0, 22.0, lw=2.0, zorder=3)
        diamond(ax, 31.6, 22.0, 1.35, 0.95, filled=True)
        mult(32.6, 24.0, "1", ha="right")
        mult(27.4, 24.0, "1..*", ha="left")
        rel(30.0, 19.6, "consists of", color=ACCENT2)

        # User  ..>  Dentist
        arrow(ax, 96.0, 43.0, 92.0, 43.0, color="#5a6b7a", lw=1.8,
              ls=(0, (4, 3)), head="->", zorder=3)
        ax.text(94.0, 48.5, "<<linked by dentistCode>>", fontsize=8.4,
                color="#5a6b7a", ha="center", va="center", rotation=90,
                zorder=6,
                bbox=dict(boxstyle="round,pad=0.18", fc="white", ec="none"))

        # ===================== enumerations =====================
        enum_box(ax, 95.0, 37.5, 26.0, "Role",
                 ["+ ADMIN", "+ RECEPTIONIST", "+ DENTIST"])
        enum_box(ax, 95.0, 27.8, 26.0, "AppointmentStatus",
                 ["+ SCHEDULED", "+ CONFIRMED", "+ IN_PROGRESS",
                  "+ COMPLETED", "+ CANCELLED", "+ NO_SHOW"])
        enum_box(ax, 95.0, 14.2, 26.0, "PaymentStatus",
                 ["+ PENDING", "+ PARTIALLY_PAID", "+ PAID", "+ CANCELLED"])

        DEP = "#7a5a90"
        # User ..> Role
        arrow(ax, 108.0, 38.9, 108.0, 37.5, color=DEP, lw=1.8,
              ls=(0, (4, 3)), head="->", zorder=3)
        # Appointment ..> AppointmentStatus  (routed between the two rows)
        line(ax, 60.0, 37.0, 93.6, 37.0, lw=1.8, color=DEP, ls=(0, (4, 3)),
             zorder=3)
        line(ax, 93.6, 37.0, 93.6, 22.0, lw=1.8, color=DEP, ls=(0, (4, 3)),
             zorder=3)
        arrow(ax, 93.6, 22.0, 95.0, 22.0, color=DEP, lw=1.8, ls=(0, (4, 3)),
              head="->", zorder=3)
        ax.text(84.0, 38.1, "<<uses>>", fontsize=9.0, color=DEP, ha="center",
                va="center", zorder=6,
                bbox=dict(boxstyle="round,pad=0.18", fc="white", ec="none"))
        # Invoice ..> PaymentStatus
        arrow(ax, 60.0, 9.5, 95.0, 9.5, color=DEP, lw=1.8, ls=(0, (4, 3)),
              head="->", zorder=3)
        ax.text(77.0, 10.5, "<<uses>>", fontsize=9.0, color=DEP, ha="center",
                va="bottom", zorder=6,
                bbox=dict(boxstyle="round,pad=0.18", fc="white", ec="none"))

        # ===================== legend (top left) =====================
        lx, ly, lw_, lh = 1.5, 60.5, 36.0, 17.0
        box(ax, lx, ly, lw_, lh, "", fc="#fbfcfd", ec="#9fb0bd", lw=1.8)
        ax.text(lx + 1.2, ly + lh - 1.6, "Key", fontsize=11.5, weight="bold",
                color=INK, va="center")

        rows = ly + lh - 4.4
        line(ax, lx + 1.4, rows, lx + 7.0, rows, lw=2.0)
        diamond(ax, lx + 2.6, rows, 1.2, 0.85, filled=False)
        ax.text(lx + 8.2, rows, "aggregation  (part can live alone)",
                fontsize=9.2, va="center")

        rows -= 2.8
        line(ax, lx + 1.4, rows, lx + 7.0, rows, lw=2.0)
        diamond(ax, lx + 2.6, rows, 1.2, 0.85, filled=True)
        ax.text(lx + 8.2, rows, "composition  (part dies with the whole)",
                fontsize=9.2, va="center")

        rows -= 2.8
        arrow(ax, lx + 1.4, rows, lx + 7.0, rows, lw=2.0, head="->")
        ax.text(lx + 8.2, rows, "association, arrow = navigable way",
                fontsize=9.2, va="center")

        rows -= 2.9
        ax.add_patch(Polygon([(lx + 4.2, rows + 1.2), (lx + 3.1, rows + 0.2),
                              (lx + 5.3, rows + 0.2)], closed=True,
                             facecolor="white", edgecolor=INK, linewidth=1.8))
        line(ax, lx + 4.2, rows + 0.2, lx + 4.2, rows - 0.4, lw=2.0)
        line(ax, lx + 1.4, rows - 0.4, lx + 7.0, rows - 0.4, lw=2.0)
        ax.text(lx + 8.2, rows + 0.3, "generalisation  (inherits from)",
                fontsize=9.2, va="center")

        rows -= 3.1
        arrow(ax, lx + 1.4, rows, lx + 7.0, rows, lw=1.8, ls=(0, (4, 3)),
              head="->", color=DEP)
        ax.text(lx + 8.2, rows, "dependency   <<uses>>", fontsize=9.2,
                va="center")

        # ===================== notes (top right) =====================
        note(ax, 72.0, 60.5, 24.0, 17.0,
             "Visibility\n   +  public     -  private\n   #  protected\n"
             "Multiplicity\n   1  exactly one\n   0..1  none or one\n"
             "   0..*  none or many\n   1..*  at least one", fs=9.2)
        note(ax, 98.0, 60.5, 23.0, 17.0,
             "Only the fields that\nmatter to the business\nrules are shown.\n"
             "Getters and setters\nare left out to keep\nthe diagram readable.\n\n"
             "CIS6003 WRIT1 - Task A", fs=9.0)

        save(fig, os.path.join(OUT, "fig02_class.png"))


if __name__ == "__main__":
    build()
