"""
Assemble the CIS6003 WRIT1 report.

Run:  python build_report.py
Out:  docs/CIS6003_WRIT1_Report.docx   (and the PDF, via export_pdf.py)
"""
import sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from report_kit import *
import report_content_a as A
import report_content_b as B
import report_content_cd as CD

DIA = os.path.join(HERE, "diagrams")
SHOT = os.path.join(HERE, "screenshots")
OUT = os.path.join(HERE, "CIS6003_WRIT1_Report.docx")

# --------------------------------------------------------------------- #
STUDENT = {
    "name":       "[ Your Full Name ]",
    "cardiff_id": "[ stXXXXXXXX ]",
    "icbt_id":    "[ CL/BSCSD/XX/XX ]",
    "leader":     "Ms. Priyanga",
    "year":       "2024 / 2025  -  Semester 1",
    "submitted":  "August 2026",
}
REPO_URL = "https://github.com/[your-github-username]/sunrise-dental-clinic"

FIGURES = [
    ("Figure 1", "Use Case Diagram - Sunrise Dental Clinic System"),
    ("Figure 2", "Class Diagram - Domain Model"),
    ("Figure 3", "Sequence Diagram - Sign In (Authenticate)"),
    ("Figure 4", "Sequence Diagram - Register New Appointment"),
    ("Figure 5", "Sequence Diagram - Calculate the Bill"),
    ("Figure 6", "Three-Tier Architecture and Deployment"),
    ("Figure 7", "Entity-Relationship Diagram - Physical Schema"),
    ("Figure 8", "Sign-in screen"),
    ("Figure 9", "Dashboard showing live clinic figures"),
    ("Figure 10", "Registering a new appointment"),
    ("Figure 11", "Appointment details, found by appointment number"),
    ("Figure 12", "A generated bill and its pricing breakdown"),
    ("Figure 13", "The printable receipt"),
    ("Figure 14", "Revenue report, read from a database view"),
    ("Figure 15", "Audit trail with IP addresses"),
    ("Figure 16", "Notification history"),
    ("Figure 17", "OpenAPI documentation at /swagger-ui.html"),
]

TABLES = [
    ("Table 1", "Assumptions made during analysis, with reasons"),
    ("Table 2", "Use of <<include>> and <<extend>> stereotypes"),
    ("Table 3", "What belongs in each architectural tier"),
    ("Table 4", "The thirteen design patterns applied"),
    ("Table 5", "Advanced database features and the rules they implement"),
    ("Table 6", "The five management reports and the decisions they support"),
    ("Table 7", "The three levels of testing"),
    ("Table 8", "Test plan"),
    ("Table 9", "Test data, chosen at the boundaries"),
    ("Table 10", "Test results"),
    ("Table 11", "Defects found after the suite was already green"),
    ("Table 12", "Requirements traceability"),
    ("Table 13", "Version control techniques applied"),
    ("Table 14", "Commit history"),
]


def shot(name):
    """The cropped, bordered version prepared by prep_screenshots.py."""
    ready = os.path.join(SHOT, "report", name)
    return ready if os.path.exists(ready) else os.path.join(SHOT, name)


def screenshot(doc, name, caption):
    """One screenshot at the full text width, so the text in it is legible."""
    figure(doc, shot(name), caption, width=TEXT_W_PORTRAIT)


def build():
    doc = new_document()

    # ---------------- front matter ----------------
    A.title_page(doc, STUDENT)
    A.acknowledgement(doc, STUDENT)
    A.contents(doc, FIGURES, TABLES)

    # ---------------- Task A ----------------
    A.task_a(doc)

    full_page_figure(doc, os.path.join(DIA, "fig01_use_case.png"),
                     "Figure 1 - Use Case Diagram: Sunrise Dental Clinic "
                     "System. Actors, the system boundary, and the "
                     "<<include>> / <<extend>> relationships described in "
                     "section 1.3.", "landscape")
    full_page_figure(doc, os.path.join(DIA, "fig02_class.png"),
                     "Figure 2 - Class Diagram: the domain model, drawn from "
                     "the classes that were actually written. Access "
                     "modifiers, multiplicity, navigability, aggregation and "
                     "composition are all shown (section 1.4).", "landscape")
    full_page_figure(doc, os.path.join(DIA, "fig03_seq_signin.png"),
                     "Figure 3 - Sequence Diagram: Sign In. The alt fragment "
                     "shows both outcomes, and both end with an audit entry "
                     "(section 1.5.1).", "portrait")
    full_page_figure(doc, os.path.join(DIA, "fig04_seq_appointment.png"),
                     "Figure 4 - Sequence Diagram: Register New Appointment. "
                     "The loop fragment is the validation chain; the opt "
                     "fragment is the database having the last word on a race "
                     "(section 1.5.2).", "portrait")
    full_page_figure(doc, os.path.join(DIA, "fig05_seq_billing.png"),
                     "Figure 5 - Sequence Diagram: Calculate the Bill. The "
                     "Template Method fragment shows the six steps whose order "
                     "no subclass can change (section 1.5.3).", "portrait")
    resume_body(doc)

    # ---------------- Task B ----------------
    B.task_b(doc)

    full_page_figure(doc, os.path.join(DIA, "fig06_architecture.png"),
                     "Figure 6 - Three-tier architecture and deployment. The "
                     "desktop client reaches the server over HTTP and JSON "
                     "only, which is what makes the application genuinely "
                     "distributed (section 2.2).", "landscape")
    full_page_figure(doc, os.path.join(DIA, "fig07_er_diagram.png"),
                     "Figure 7 - Entity-Relationship diagram of the physical "
                     "schema. Note uk_appointment_slot on "
                     "(dentist_id, slot_lock), which is what actually prevents "
                     "double booking (section 2.4.1).", "landscape")
    resume_body(doc)

    heading(doc, "2.8 The system in use", 2)
    para(doc,
         "The screenshots below are of the running application with seeded "
         "clinic data. They are included because the marking criteria ask for "
         "documentation with screenshots and clear explanations, and because a "
         "description of an interface is no substitute for seeing it.")

    screenshot(doc, "01_login.png",
               "Figure 8 - The sign-in screen. The same message is shown for "
               "an unknown username and a wrong password, so the page cannot "
               "be used to discover which accounts exist.")
    screenshot(doc, "02_dashboard.png",
               "Figure 9 - The dashboard, showing live figures: today's "
               "appointments, outstanding invoices and revenue to date.")
    screenshot(doc, "04_appointment_new.png",
               "Figure 10 - Registering a new appointment. The dentist, "
               "treatment and slot are chosen from reference data, so an "
               "invalid combination cannot be typed in.")
    screenshot(doc, "05_appointment_view.png",
               "Figure 11 - The appointment detail screen, reached by "
               "searching for the appointment number as the brief requires.")
    screenshot(doc, "09_invoice_view.png",
               "Figure 12 - A generated bill, showing the full pricing "
               "breakdown: consultation fee, treatment cost, surcharge, "
               "discount, VAT and total.")
    screenshot(doc, "10_receipt.png",
               "Figure 13 - The printable receipt. The browser and the "
               "desktop client both print this same server-rendered page, so "
               "the two can never disagree.")
    screenshot(doc, "12_report_revenue.png",
               "Figure 14 - The revenue report, read from the "
               "v_revenue_daily database view.")
    screenshot(doc, "19_admin_audit.png",
               "Figure 15 - The audit trail, showing every sign-in and every "
               "change with the caller's IP address.")
    screenshot(doc, "20_admin_notifications.png",
               "Figure 16 - The notification history, recording every alert "
               "attempt, successful or failed.")
    screenshot(doc, "23_swagger.png",
               "Figure 17 - The OpenAPI documentation at /swagger-ui.html, "
               "which the desktop client and any future client are written "
               "against.")

    # ---------------- Tasks C and D ----------------
    page_break(doc)
    CD.task_c(doc)
    page_break(doc)
    CD.task_d(doc, REPO_URL)
    page_break(doc)
    CD.references(doc)

    # ---------------- finish ----------------
    page_numbers(doc, start_section=0)
    doc.save(OUT)
    print("wrote", OUT)
    return OUT


if __name__ == "__main__":
    build()
