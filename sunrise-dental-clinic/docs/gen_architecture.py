"""Figure 6 - Three-tier / distributed architecture of the delivered system.

The tier names run down a gutter on the left so that nothing has to sit on
top of them, which is what keeps a banded diagram legible.
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from diagram_kit import *

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "diagrams")
os.makedirs(OUT, exist_ok=True)

TIER1 = "#fdf0e4"     # presentation
TIER2 = "#e8f4f5"     # business
TIER3 = "#eef3ea"     # data

L = 13.0              # left edge of all content, right of the label gutter


def band(ax, y, h, fc, name, sub, name_col):
    box(ax, 1.5, y, 119.0, h, "", fc=fc, ec="#9fb0bd", lw=2.0, radius=0.9,
        zorder=1)
    line(ax, 11.0, y + 0.8, 11.0, y + h - 0.8, lw=1.6, color="#b6c4ce",
         zorder=2)
    ax.text(5.0, y + h / 2, name, fontsize=11.4, weight="bold", rotation=90,
            ha="center", va="center", color=name_col, zorder=2)
    ax.text(8.6, y + h / 2, sub, fontsize=8.6, rotation=90, ha="center",
            va="center", color="#6d7f8c", style="italic", zorder=2,
            linespacing=1.35)


def build():
    with hand(scale=1.1, length=125, randomness=2.5):
        use_hand_font()
        fig, ax = new_canvas(A4_LANDSCAPE, xlim=(0, 122), ylim=(0, 84))

        title(ax, "Figure 6 - Three-Tier Architecture and Deployment",
              "CIS6003 WRIT1   |   Task B", fs=19)

        band(ax, 56.5, 17.5, TIER1, "PRESENTATION",
             "what the user sees", "#a35a1e")
        band(ax, 28.5, 26.0, TIER2, "BUSINESS",
             "every rule the clinic runs on", "#12626c")
        band(ax, 2.5, 23.0, TIER3, "DATA",
             "the only place data lives", "#3d5c37")

        # ==================== presentation tier ====================
        pres = [
            (L, "Web Browser Client", "«browser»",
             "Thymeleaf pages, hand-written CSS.\n"
             "No CDN, so it works with no internet."),
            (49.0, "Menu-Driven Desktop Client", "«separate process»",
             "Java Swing in its own JVM.\n"
             "Command pattern behind the menu."),
            (85.0, "Any Other Client", "«future»",
             "Swagger UI, curl, a mobile app -\nthe same versioned REST contract."),
        ]
        for x, head, st, body in pres:
            box(ax, x, 58.5, 34.0, 11.5, "", fc="white", lw=2.2, zorder=3)
            ax.text(x + 17.0, 68.6, st, fontsize=8.8, color="#5a6b7a",
                    ha="center", style="italic", zorder=4)
            ax.text(x + 17.0, 66.2, head, fontsize=10.6, weight="bold",
                    ha="center", color=INK, zorder=4)
            ax.text(x + 17.0, 62.2, body, fontsize=9.2, ha="center",
                    va="center", color=INK, zorder=4, linespacing=1.5)

        # ==================== the wire ====================
        for cx, lab in [(30.0, "HTTP session\n+ CSRF token"),
                        (66.0, "HTTP + JSON only\nno shared objects"),
                        (102.0, "HTTP Basic\n+ JSON")]:
            arrow(ax, cx, 58.5, cx, 54.2, color=ACCENT2, lw=2.2, head="<->",
                  zorder=4)
            ax.text(cx + 1.4, 56.4, lab, fontsize=8.8, color=ACCENT2,
                    ha="left", va="center", zorder=5, linespacing=1.3)

        # ==================== business tier ====================
        box(ax, L, 47.5, 106.0, 5.8,
            "Spring Boot server   -   dental-server   -   one deployable jar",
            fc="#d3e9eb", lw=2.2, fs=11.4, bold=True, zorder=3)

        col = [
            ("Web Controllers",
             "Appointment-, Billing-,\nPatient-, Report-\nWebController...\n\n"
             "Return HTML pages.\nRedirect to /login when\nnobody is signed in."),
            ("REST Controllers",
             "/api/v1/...\n\nOne response envelope\nand one error code for\n"
             "every failure.\nReturn JSON 401, never\nan HTML login page."),
            ("Security",
             "Two filter chains.\nBCrypt hashing.\nThree roles.\n"
             "Session + remember-me.\nCSRF on every form.\n"
             "Lock-out after 5 tries.\nEvery attempt audited."),
            ("Services + Facade",
             "AppointmentService\nBillingService\nReportService\nPatientService\n"
             "ClinicFacade\n\n@Transactional lives here."),
            ("Design Patterns",
             "Strategy, Template\nMethod, Factory, Chain\nof Responsibility,\n"
             "Observer, Adapter,\nFacade, Builder,\nCommand, Singleton,\nDAO, DTO, MVC."),
        ]
        for i, (head, body) in enumerate(col):
            x = L + i * 21.4
            box(ax, x, 30.0, 20.0, 16.5, "", fc="white", lw=2.0, zorder=3)
            ax.text(x + 10.0, 45.2, head, fontsize=10.0, weight="bold",
                    ha="center", color=INK, zorder=4)
            line(ax, x + 1.0, 43.9, x + 19.0, 43.9, lw=1.6, color="#9fb0bd",
                 zorder=4)
            ax.text(x + 1.2, 43.0, body, fontsize=8.4, ha="left", va="top",
                    color=INK, zorder=4, linespacing=1.45)

        # ==================== wire to the data tier ====================
        arrow(ax, 66.0, 30.0, 66.0, 25.5, color=ACCENT2, lw=2.2, head="<->",
              zorder=4)
        ax.text(67.4, 27.7, "JPA / Hibernate  +  JDBC     (ddl-auto = validate)",
                fontsize=9.2, color=ACCENT2, ha="left", va="center", zorder=5)

        # ==================== data tier ====================
        data = [
            (L, 33.0, "Repositories + DAO",
             "Spring Data JPA interfaces, plus\nReportingDao for native SQL"),
            (49.0, 34.0, "Flyway migrations",
             "V1 schema, V2 reference data,\nV3 views + functions, V4 triggers"),
            (86.0, 33.0, "H2 (default)  /  MySQL 8",
             "The same Java against two engines,\nchosen by a Spring profile"),
        ]
        for x, w, head, body in data:
            box(ax, x, 16.0, w, 8.0, "", fc="white", lw=2.0, zorder=3)
            ax.text(x + w / 2, 22.6, head, fontsize=10.0, weight="bold",
                    ha="center", color=INK, zorder=4)
            ax.text(x + w / 2, 19.2, body, fontsize=8.8, ha="center",
                    va="center", color=INK, zorder=4, linespacing=1.5)

        box(ax, L, 4.0, 106.0, 10.5, "", fc="#f5f9f2", ec="#8aa382", lw=2.0,
            zorder=3)
        ax.text(66.0, 13.1, "Work the database is trusted to do for itself",
                fontsize=10.4, weight="bold", ha="center", color="#3d5c37",
                zorder=4)
        items = [
            (14.5, "9 tables\nFK and CHECK\nconstraints"),
            (32.0, "uk_appointment_slot\n(dentist_id, slot_lock)\nno double booking"),
            (53.5, "5 views\nv_daily_schedule,\nv_revenue_daily, ..."),
            (72.5, "2 functions\nFN_INVOICE_TOTAL\nFN_AGEING_BUCKET"),
            (90.0, "2 procedures\nSP_SETTLE_INVOICE\nSP_DAILY_CLOSING"),
            (108.0, "4 triggers\naudit trail +\nworking hours"),
        ]
        for x, t in items:
            ax.text(x, 8.2, t, fontsize=8.4, ha="left", va="center",
                    color="#3d5c37", zorder=4, linespacing=1.5)
        for x in (31.0, 52.5, 71.5, 89.0, 107.0):
            line(ax, x, 5.2, x, 11.4, lw=1.4, color="#a8bda2", zorder=4)

        save(fig, os.path.join(OUT, "fig06_architecture.png"))


if __name__ == "__main__":
    build()
