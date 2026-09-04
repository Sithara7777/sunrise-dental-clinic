"""Figure 7 - Entity-Relationship diagram of the physical database.

Column names and types are copied from V1__baseline_schema.sql, so the
diagram describes the schema Flyway actually creates rather than an idealised
version of it.  Each column is drawn as name-left / type-right, which is what
keeps every box the same width no matter how long the type name is.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from diagram_kit import *

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "diagrams")
os.makedirs(OUT, exist_ok=True)

HEAD = "#d8ecee"
HEAD2 = "#c2e0e4"
PKC = "#12626c"
FKC = "#b3261e"
LH = 1.30
HEADH = 3.0


def th(n):
    """Height of a table box with n columns."""
    return HEADH + n * LH + 0.9


def table(ax, x, y, w, name, cols, fs=8.5, header=HEAD):
    """cols: list of (marker, column_name, type).  marker in PK/FK/UK/''."""
    h = th(len(cols))
    sharp_box(ax, x, y - h, w, h, fc="white", ec=INK, lw=2.0)
    sharp_box(ax, x, y - HEADH, w, HEADH, fc=header, ec=INK, lw=2.0)
    ax.text(x + w / 2, y - HEADH / 2, name, ha="center", va="center",
            fontsize=fs + 2.1, weight="bold", color=INK, zorder=4)
    cy = y - HEADH - 1.0
    for marker, cname, ctype in cols:
        col = PKC if marker in ("PK", "UK") else (FKC if marker == "FK" else INK)
        if marker:
            ax.text(x + 0.5, cy, marker, ha="left", va="center",
                    fontsize=fs - 1.0, color=col, weight="bold", zorder=4)
        ax.text(x + 3.7, cy, cname, ha="left", va="center", fontsize=fs,
                color=col, zorder=4)
        ax.text(x + w - 0.6, cy, ctype, ha="right", va="center", fontsize=fs,
                color=col, zorder=4)
        cy -= LH
    return h


def crow(ax, x, y, direction, color=INK, s=1.0):
    """A crow's foot.  direction: left / right / up / down."""
    v = {"left": (-1, 0), "right": (1, 0), "up": (0, 1), "down": (0, -1)}[direction]
    for k in (-1.0, 0.0, 1.0):
        if v[0]:
            line(ax, x, y, x + v[0] * 2.1 * s, y + k * 1.05 * s, lw=1.9,
                 color=color, zorder=5)
        else:
            line(ax, x, y, x + k * 1.05 * s, y + v[1] * 2.1 * s, lw=1.9,
                 color=color, zorder=5)


def one(ax, x, y, horizontal=True, color=INK, s=1.0):
    """The single tick marking the 'one' end."""
    if horizontal:
        line(ax, x, y - 1.0 * s, x, y + 1.0 * s, lw=1.9, color=color, zorder=5)
    else:
        line(ax, x - 1.0 * s, y, x + 1.0 * s, y, lw=1.9, color=color, zorder=5)


def italic(ax, x, y, t, color=INK, fs=8.7, rot=0):
    ax.text(x, y, t, fontsize=fs, color=color, ha="center", va="center",
            style="italic", rotation=rot, zorder=6, linespacing=1.35,
            bbox=dict(boxstyle="round,pad=0.22", fc="white", ec="none",
                      alpha=0.96))


def build():
    with hand(scale=1.05, length=125, randomness=2.3):
        use_hand_font()
        fig, ax = new_canvas(A4_LANDSCAPE, xlim=(0, 122), ylim=(0, 84))

        title(ax, "Figure 7 - Entity-Relationship Diagram : Physical Schema",
              "9 tables   |   5 views   |   2 functions   |   2 procedures   |   "
              "4 triggers        -        CIS6003 WRIT1   |   Task B",
              fs=18, sub_fs=10.2)

        # ============================ column A ============================
        table(ax, 1.5, 75.0, 27.0, "patient", [
            ("PK", "id", "BIGINT"),
            ("UK", "patient_code", "VARCHAR(20)"),
            ("", "full_name", "VARCHAR(120)"),
            ("", "address", "VARCHAR(200)"),
            ("", "contact_number", "VARCHAR(20)"),
            ("", "email", "VARCHAR(120)"),
            ("", "nic", "VARCHAR(20)"),
            ("", "gender", "VARCHAR(15)"),
            ("", "date_of_birth", "DATE"),
            ("", "medical_notes", "VARCHAR(500)"),
        ])
        table(ax, 1.5, 55.0, 27.0, "dentist", [
            ("PK", "id", "BIGINT"),
            ("UK", "dentist_code", "VARCHAR(20)"),
            ("", "full_name", "VARCHAR(120)"),
            ("", "specialization", "VARCHAR(80)"),
            ("", "consultation_fee", "DECIMAL(10,2)"),
            ("", "slmc_reg_no", "VARCHAR(30)"),
            ("", "work_start_time", "TIME"),
            ("", "work_end_time", "TIME"),
            ("", "active", "BOOLEAN"),
        ])
        table(ax, 1.5, 36.0, 27.0, "treatment", [
            ("PK", "id", "BIGINT"),
            ("UK", "code", "VARCHAR(20)"),
            ("", "name", "VARCHAR(120)"),
            ("", "category", "VARCHAR(40)"),
            ("", "base_price", "DECIMAL(10,2)"),
            ("", "duration_minutes", "INTEGER"),
            ("", "pricing_strategy_key", "VARCHAR(20)"),
            ("", "active", "BOOLEAN"),
        ])

        # ============================ column B ============================
        table(ax, 33.0, 75.0, 30.0, "appointment", [
            ("PK", "id", "BIGINT"),
            ("UK", "appointment_number", "VARCHAR(20)"),
            ("FK", "patient_id", "BIGINT"),
            ("FK", "dentist_id", "BIGINT"),
            ("FK", "treatment_id", "BIGINT"),
            ("", "appointment_date", "DATE"),
            ("", "appointment_time", "TIME"),
            ("", "duration_minutes", "INTEGER"),
            ("", "status", "VARCHAR(20)"),
            ("UK", "slot_lock", "VARCHAR(30)"),
            ("", "notes", "VARCHAR(500)"),
            ("", "cancellation_reason", "VARCHAR(300)"),
            ("", "created_by", "VARCHAR(30)"),
            ("", "version", "BIGINT"),
        ], header=HEAD2)
        table(ax, 33.0, 49.0, 30.0, "app_user", [
            ("PK", "id", "BIGINT"),
            ("UK", "username", "VARCHAR(40)"),
            ("", "password_hash", "VARCHAR(100)"),
            ("", "full_name", "VARCHAR(120)"),
            ("", "role", "VARCHAR(20)"),
            ("", "active", "BOOLEAN"),
            ("", "failed_login_attempts", "INTEGER"),
            ("", "locked_until", "TIMESTAMP"),
            ("", "linked_dentist_code", "VARCHAR(20)"),
        ])
        table(ax, 33.0, 30.0, 30.0, "audit_log", [
            ("PK", "id", "BIGINT"),
            ("", "occurred_at", "TIMESTAMP"),
            ("", "username", "VARCHAR(40)"),
            ("", "action", "VARCHAR(60)"),
            ("", "entity_type", "VARCHAR(40)"),
            ("", "entity_key", "VARCHAR(40)"),
            ("", "detail", "VARCHAR(500)"),
            ("", "ip_address", "VARCHAR(45)"),
        ])

        # ============================ column C ============================
        table(ax, 66.0, 75.0, 30.0, "invoice", [
            ("PK", "id", "BIGINT"),
            ("UK", "invoice_number", "VARCHAR(20)"),
            ("FK", "appointment_id", "BIGINT  UNIQUE"),
            ("", "patient_name", "VARCHAR(120)"),
            ("", "dentist_name", "VARCHAR(120)"),
            ("", "treatment_name", "VARCHAR(120)"),
            ("", "consultation_fee", "DECIMAL(10,2)"),
            ("", "treatment_cost", "DECIMAL(10,2)"),
            ("", "surcharge_amount", "DECIMAL(10,2)"),
            ("", "discount_amount", "DECIMAL(10,2)"),
            ("", "tax_amount", "DECIMAL(10,2)"),
            ("", "total_amount", "DECIMAL(10,2)"),
            ("", "amount_paid", "DECIMAL(10,2)"),
            ("", "payment_status", "VARCHAR(20)"),
        ], header=HEAD2)
        table(ax, 66.0, 49.0, 30.0, "invoice_line", [
            ("PK", "id", "BIGINT"),
            ("FK", "invoice_id", "BIGINT"),
            ("", "line_number", "INTEGER"),
            ("", "description", "VARCHAR(200)"),
            ("", "quantity", "INTEGER"),
            ("", "unit_price", "DECIMAL(10,2)"),
            ("", "line_total", "DECIMAL(10,2)"),
            ("", "line_type", "VARCHAR(20)"),
        ])

        # ============================ column D ============================
        table(ax, 99.0, 75.0, 22.0, "notification_log", [
            ("PK", "id", "BIGINT"),
            ("", "channel", "VARCHAR(10)"),
            ("", "recipient", "VARCHAR(120)"),
            ("", "subject", "VARCHAR(200)"),
            ("", "status", "VARCHAR(15)"),
            ("", "reference_key", "VARCHAR(40)"),
            ("", "created_at", "TIMESTAMP"),
        ])
        table(ax, 99.0, 57.0, 22.0, "number_sequence", [
            ("PK", "seq_key", "VARCHAR(40)"),
            ("", "next_value", "BIGINT"),
            ("", "updated_at", "TIMESTAMP"),
        ])

        # ========================= relationships =========================
        # patient (1) --< appointment (many)
        line(ax, 28.5, 66.0, 33.0, 66.0, lw=1.9, zorder=3)
        one(ax, 28.9, 66.0)
        crow(ax, 33.0, 66.0, "left", s=0.9)
        italic(ax, 30.7, 68.2, "books")

        # dentist (1) --< appointment (many)
        line(ax, 28.5, 47.0, 31.6, 47.0, lw=1.9, zorder=3)
        line(ax, 31.6, 47.0, 31.6, 60.0, lw=1.9, zorder=3)
        line(ax, 31.6, 60.0, 33.0, 60.0, lw=1.9, zorder=3)
        one(ax, 28.9, 47.0)
        crow(ax, 33.0, 60.0, "left", s=0.7)
        italic(ax, 31.6, 53.5, "attends", rot=90, fs=8.2)

        # treatment (1) --< appointment (many)
        line(ax, 28.5, 30.0, 30.0, 30.0, lw=1.9, zorder=3)
        line(ax, 30.0, 30.0, 30.0, 56.0, lw=1.9, zorder=3)
        line(ax, 30.0, 56.0, 33.0, 56.0, lw=1.9, zorder=3)
        one(ax, 28.9, 30.0)
        crow(ax, 33.0, 56.0, "left", s=0.7)
        italic(ax, 30.0, 38.0, "is for", rot=90, fs=8.2)

        # appointment (1) --- (0..1) invoice
        line(ax, 63.0, 66.0, 66.0, 66.0, lw=1.9, zorder=3)
        one(ax, 63.4, 66.0)
        one(ax, 65.6, 66.0)
        italic(ax, 64.5, 71.0, "billed by", rot=90, fs=8.2)
        italic(ax, 64.5, 60.5, "1 : 0..1", color="#5a6b7a", rot=90, fs=8.0)

        # invoice (1) --< invoice_line (many)   composition, cascade delete
        line(ax, 81.0, 52.9, 81.0, 49.0, lw=1.9, color=ACCENT2, zorder=3)
        one(ax, 81.0, 52.5, horizontal=False, color=ACCENT2)
        crow(ax, 81.0, 49.0, "up", color=ACCENT2, s=0.7)
        ax.text(82.6, 51.2, "consists of", fontsize=8.4, color=ACCENT2,
                ha="left", va="center", style="italic", zorder=6,
                bbox=dict(boxstyle="round,pad=0.22", fc="white", ec="none",
                          alpha=0.96))
        ax.text(82.6, 49.6, "ON DELETE CASCADE", fontsize=8.0, color=ACCENT2,
                ha="left", va="center", zorder=6,
                bbox=dict(boxstyle="round,pad=0.22", fc="white", ec="none",
                          alpha=0.96))

        # ============================ legend ============================
        lx, ly = 1.5, 3.0
        box(ax, lx, ly, 27.0, 13.0, "", fc="#fbfcfd", ec="#9fb0bd", lw=1.8)
        ax.text(lx + 1.2, ly + 11.4, "Key", fontsize=10.6, weight="bold",
                color=INK, va="center")
        ax.text(lx + 1.2, ly + 8.9, "PK   primary key", fontsize=8.8,
                va="center", color=PKC)
        ax.text(lx + 1.2, ly + 6.9, "FK   foreign key", fontsize=8.8,
                va="center", color=FKC)
        ax.text(lx + 1.2, ly + 4.9, "UK   unique business identifier",
                fontsize=8.8, va="center", color=PKC)
        line(ax, lx + 2.0, ly + 2.2, lx + 7.4, ly + 2.2, lw=1.9)
        one(ax, lx + 2.0, ly + 2.2)
        crow(ax, lx + 7.4, ly + 2.2, "left")
        ax.text(lx + 10.4, ly + 2.2, "one   to   many", fontsize=8.8,
                va="center")

        note(ax, 33.0, 3.0, 30.0, 11.5,
             "The constraint that matters most\n"
             "uk_appointment_slot (dentist_id, slot_lock)\n"
             "slot_lock holds \"date|time\" while a visit\n"
             "occupies the chair, and NULL once it is\n"
             "cancelled - and NULLs never clash in SQL.",
             fs=8.6)

        note(ax, 66.0, 3.0, 55.0, 30.0,
             "Also part of the schema, but not tables\n\n"
             "Views        v_daily_schedule, v_revenue_daily, v_dentist_workload,\n"
             "                  v_treatment_popularity, v_outstanding_invoice\n"
             "Functions   FN_INVOICE_TOTAL  -  re-computes an invoice total in SQL\n"
             "                  FN_AGEING_BUCKET  -  puts a debt in a 0-30 / 31-60 /\n"
             "                  61-90 / 90+ day band, so a report and an ad-hoc query\n"
             "                  can never disagree\n"
             "Procedures  SP_SETTLE_INVOICE, SP_DAILY_CLOSING_SUMMARY (MySQL)\n"
             "Triggers      audit an appointment on insert and on update, audit every\n"
             "                  payment, and - on MySQL - refuse any booking outside\n"
             "                  the dentist's own working hours\n\n"
             "app_user.linked_dentist_code matches dentist.dentist_code but is not a\n"
             "foreign key, so retiring a dentist never deletes the sign-in history\n"
             "that records the work they did.", fs=8.6)

        save(fig, os.path.join(OUT, "fig07_er_diagram.png"))


if __name__ == "__main__":
    build()
