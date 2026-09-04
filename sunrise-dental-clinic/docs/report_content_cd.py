"""Tasks C and D, plus the reference list, of the CIS6003 WRIT1 report."""
from report_kit import *


# ===================================================================== #
def task_c(doc):
    heading(doc, "Task C - Test Plan and Test-Driven Development", 1)
    question_box(doc, "Task C  (20 marks)  -  from the assessment brief", [
        "“Document the test plan and explain how you used test-driven "
        "development in this scenario and do a test automation to achieve "
        "that. This includes test rationale, test plan, test data and proper "
        "application of the test plan. (LO II)”",
    ])

    heading(doc, "3.1 Test rationale", 2)
    para(doc,
         "The clinic's problems are all failures of correctness under pressure: "
         "two bookings for one slot, a bill that adds up wrongly, a record that "
         "cannot be found. The suite therefore has to prove the rules hold when "
         "they are attacked, not merely that the happy path works. Three levels "
         "were used, so each failure is diagnosed as cheaply as possible.")
    table(doc, ["Level", "What it proves", "How it runs"], [
        ["Unit (226)",
         "One class behaves correctly in isolation.",
         "JUnit 5 with Mockito - no Spring context, no database, so a "
         "failure points at one class."],
        ["Integration (97)",
         "The parts work together against the real schema.",
         "`@SpringBootTest` + MockMvc on the real Flyway schema, in-memory "
         "H2."],
        ["Concurrency / manual",
         "The rules hold under a genuine race.",
         "A multi-threaded client, plus scripted page and endpoint checks."],
    ], widths=[1.4, 4.0, 4.2], font_size=10.5)
    para(doc,
         "Maven runs the levels in separate phases - Surefire for units, "
         "Failsafe for integration - so a broken unit test stops the build "
         "before the slow tests begin.")

    heading(doc, "3.2 How test-driven development was used", 2)
    para(doc,
         "TDD was applied where it genuinely helps and not where it would have "
         "been theatre. The billing rules, the appointment lifecycle and the "
         "validation chain were written test-first in the red-green-refactor "
         "cycle (Beck, 2003), because they have clear inputs, clear outputs and "
         "rules statable in a sentence. Before any code existed, a test "
         "asserted that a `CANCELLED` appointment cannot become `COMPLETED`; "
         "writing it forced a decision that had been vague until then, and the "
         "resulting enum owns the rule so no service can bypass it. TDD was "
         "**not** used for the Thymeleaf templates or the Swing windows - a "
         "failing test for the position of a form field would lock in a layout "
         "before anyone had looked at it - so those were built, reviewed by "
         "eye, then covered by integration tests asserting on behaviour rather "
         "than markup.")
    para(doc,
         "One case shows the cycle earning its keep. A test asserted that "
         "rescheduling must not report a clash with itself. It failed, as "
         "expected; the interesting part was **why** - the double-booking query "
         "excluded nothing, so an appointment was its own conflict. That is a "
         "bug a manual tester would have found only after a receptionist "
         "reported it.")

    heading(doc, "3.3 Test plan", 2)
    table(doc, ["ID", "Area", "What is tested", "Type", "Result"], [
        ["TP-01", "Authentication",
         "Valid sign-in, wrong password, unknown user, locked account, CSRF, "
         "role enforcement, sign-out", "Integration", "Pass"],
        ["TP-02", "Register appointment",
         "Valid booking, six validation rules, duplicate slot, unknown "
         "patient / dentist / treatment", "Unit + Int.", "Pass"],
        ["TP-03", "Appointment lifecycle",
         "Every legal status transition, and every illegal one refused",
         "Unit", "Pass"],
        ["TP-04", "Display details",
         "Search by appointment number; unknown number; complete data shown",
         "Integration", "Pass"],
        ["TP-05", "Billing",
         "Four pricing rules, VAT after discount, 50% cap, senior and child "
         "concessions, rounding", "Unit", "Pass"],
        ["TP-06", "Billing integrity",
         "Java total equals `FN_INVOICE_TOTAL`; a visit is billable once only",
         "Integration", "Pass"],
        ["TP-07", "Payments",
         "Full, partial and over-payment; status transitions; receipt",
         "Unit + Int.", "Pass"],
        ["TP-08", "Database features",
         "Views return rows, functions compute correctly, triggers write audit "
         "rows, CHECK constraints refuse bad data", "Integration", "Pass"],
        ["TP-09", "Reports",
         "All five reports; empty and invalid ranges; unknown report name",
         "Integration", "Pass"],
        ["TP-10", "Web services",
         "Every `/api/v1` endpoint, the envelope, error codes, JSON 401 not an "
         "HTML redirect", "Integration", "Pass"],
        ["TP-11", "Web interface",
         "Every page renders; navigation works; role-based menus",
         "Integration", "Pass"],
        ["TP-12", "Concurrency",
         "12 simultaneous bookings for one slot; sequence allocation under "
         "load", "Automated", "Pass"],
        ["TP-13", "Desktop client",
         "Connects over HTTP only; all menu commands; server-rendered receipt",
         "Manual probe", "Pass"],
    ], widths=[0.95, 1.6, 4.4, 1.3, 0.75], font_size=10)

    heading(doc, "3.4 Test data", 2)
    para(doc,
         "Data was devised to cover boundaries rather than typical values, "
         "because a bug almost never lives in the middle of a range.")
    table(doc, ["Category", "Examples used"], [
        ["Valid, ordinary",
         "A 34-year-old patient; a routine filling at 10:00 next Tuesday; paid "
         "in full by card."],
        ["Boundary - age",
         "Exactly 18 (not a minor by one day), exactly 60 (senior concession), "
         "17 and 61 either side."],
        ["Boundary - time",
         "08:00 (first legal slot), 20:00 (first illegal), 09:29 and 09:30 "
         "either side of the grid."],
        ["Boundary - money",
         "A discount of exactly 50% (allowed) and 50.01% (refused); a payment "
         "equal to the total, and one cent more."],
        ["Boundary - dates",
         "Today; yesterday (refused); 90 days ahead (allowed) and 91 "
         "(refused)."],
        ["Invalid",
         "Blank name, 9-digit telephone number, negative price, unknown "
         "treatment code, forbidden status transition."],
        ["Malicious",
         "A form posted with no CSRF token; a `RECEPTIONIST` requesting an "
         "admin page; an API call with no credentials."],
        ["Concurrent",
         "Twelve threads released simultaneously against one dentist, one "
         "date, one time."],
    ], widths=[1.7, 7.5], font_size=10.5)
    para(doc,
         "Data comes from a `TestDataFactory`, so a change to an entity is a "
         "change in one place. Identifiers are generated from a sequence, which "
         "found a real defect early: one version produced appointment numbers "
         "longer than the `VARCHAR(20)` column, failing for exactly the reason "
         "a busy year would eventually have failed in production.")

    heading(doc, "3.5 Test automation and results", 2)
    para(doc,
         "The whole suite is automated and runs from one command, `mvn verify`. "
         "Nothing needs installing and no database needs starting, because "
         "integration tests use an in-memory H2 instance that Flyway migrates "
         "on the way up; JaCoCo produces coverage in the same run. The same "
         "command runs in CI on every push, on both JDK 17 and JDK 21, and the "
         "workflow then packages both jars and **boots the packaged server** to "
         "prove it starts - a suite can pass while an application fails to "
         "start.")
    table(doc, ["Measure", "Result"], [
        ["Unit tests", "226 - all passing; 0 failures, 0 errors, 0 skipped"],
        ["Integration tests", "97 - all passing; 0 failures, 0 errors, 0 skipped"],
        ["**Total**", "**323 automated tests, all passing**"],
        ["Test classes", "31 (26 unit, 5 integration)"],
        ["Server coverage (JaCoCo)",
         "73.7% lines, 76.4% methods, 96.6% classes (115 of 119)"],
        ["Build", "`mvn verify` green on JDK 17 and JDK 21"],
    ], widths=[2.4, 6.8], font_size=10.5)
    para(doc,
         "Coverage deserves an honest note. `dental-common` reports far lower "
         "coverage because JaCoCo reports per module: its DTOs are exercised "
         "heavily by the server's integration tests, but that execution is "
         "credited to the server's report, and what remains uncovered is "
         "overwhelmingly getters and setters. Writing tests for generated "
         "accessors would raise the percentage without raising the quality.")

    heading(doc, "3.6 What the testing actually found", 2)
    para(doc,
         "A test report saying everything passed first time is not one anybody "
         "should believe. Three genuine defects were found **after** the suite "
         "was green, all by the concurrency test in section 2.4.1. Two shared "
         "one root cause worth stating plainly: **a failure that surfaces at "
         "transaction commit, inside Spring's proxy, cannot be caught by a "
         "try/catch inside the same `@Transactional` method.**")
    table(doc, ["Defect", "Symptom", "Fix"], [
        ["Login bookkeeping race",
         "Two requests authenticating as the same account in the same instant "
         "raced to update one versioned `User` row. The loser's locking "
         "failure escaped as a raw HTTP 500, and its audit entry was lost.",
         "Moved the update into its own transactional bean so the listener's "
         "try/catch can see the failure; the audit entry is now written "
         "unconditionally."],
        ["First sequence allocation",
         "`SELECT ... FOR UPDATE` cannot lock a row that does not exist yet, so "
         "the first appointment number of a year could be claimed by several "
         "bookings at once, and every loser's booking was abandoned.",
         "A one-time retry inside a genuinely new transaction "
         "(`SequenceAllocator`)."],
        ["CI action reference",
         "`trivy-action@0.24.0` was referenced, but that action's tags carry a "
         "`v` prefix, so the job would have failed on its first run.",
         "Corrected to `@v0.24.0`; every action reference was then verified "
         "against the GitHub API."],
    ], widths=[1.4, 4.3, 3.5], font_size=10)
    para(doc,
         "The lesson generalises: a sequential test proves a rule is "
         "**written**, but only a concurrent test proves it **holds**. Both "
         "would have reached production and been very hard to reproduce, "
         "because they appear only when two people act in the same instant - "
         "precisely what a busy front desk creates every day.")

    heading(doc, "3.7 Traceability", 2)
    table(doc, ["Requirement", "Design", "Implementation", "Verified by"], [
        ["1. User authentication", "Fig. 1, 3",
         "Spring Security, two filter chains, BCrypt", "TP-01, `SecurityIT`"],
        ["2. Register appointment", "Fig. 1, 2, 4",
         "`AppointmentService.register()` + validation chain",
         "TP-02, `AppointmentServiceTest`, `AppointmentApiIT`"],
        ["3. Display details", "Fig. 1, 2", "`requireByNumber()`, web and API",
         "TP-04, `AppointmentApiIT`"],
        ["4. Calculate and print bill", "Fig. 1, 5",
         "`BillingService` + four pricing strategies",
         "TP-05, TP-06, `PricingStrategyTest`, `BillingApiIT`"],
        ["5. Help section", "Fig. 1", "`HelpService`, `/help` pages",
         "TP-11, `ReportAndWebUiIT`"],
        ["6. Exit system", "Fig. 1", "POST-only sign-out; client exit command",
         "TP-01, `SecurityIT`"],
        ["Distributed + web services", "Fig. 6",
         "`/api/v1`, separate Swing process", "TP-10, TP-13"],
        ["Design patterns", "Fig. 2, 5", "13 patterns (section 2.3)",
         "TP-05, `PricingStrategyFactoryTest`, `BookingValidationChainTest`"],
        ["Proper database", "Fig. 7",
         "9 tables, 5 views, 2 functions, 2 procedures, 4 triggers",
         "TP-08, `DatabaseFeaturesIT`, `ClinicFunctionsTest`"],
        ["Validation", "Fig. 4", "Four layers (section 2.5)",
         "TP-02, `ValidationConstraintsTest`"],
        ["Reports", "Fig. 6", "Five reports over database views",
         "TP-09, `ReportAndWebUiIT`"],
    ], widths=[2.0, 1.0, 3.2, 3.0], font_size=10)

    heading(doc, "3.8 Evaluation and lessons learned", 2)
    para(doc,
         "Every business rule in the brief is covered, the rules involving "
         "money are covered at their boundaries, and the suite runs unattended "
         "in under two minutes. Three lessons stand out. **The cheapest bug is "
         "the one a test finds before the code exists** - the lifecycle rules "
         "cost almost nothing because they were specified as tests first. "
         "**Coverage is a smell detector, not a score**: the concurrency "
         "defects were in code already covered. And **tests should mirror how "
         "the software will actually be used** - a front desk has two people "
         "booking at once, and once the harness reflected that it immediately "
         "found two faults every sequential test had passed over. If extended, "
         "the next step would be property-based testing of the pricing rules.")


# ===================================================================== #
def task_d(doc, repo_url):
    heading(doc, "Task D - Version Control, Workflows and Deployment", 1)
    question_box(doc, "Task D  (20 marks)  -  from the assessment brief", [
        "“Create your own Git/ GitHub repository which is public to access "
        "and upload /deploy the changes of the software project you have "
        "developed in it. Share the report link within the documentation. "
        "Update it with several versions where modifications are applied each "
        "day... Version control techniques you have used throughout the "
        "development should be highlighted and documented properly. "
        "Demonstrate workflows deployed with the Git repository. (LO III)”",
    ])

    heading(doc, "4.1 Repository", 2)
    para(doc, f"**Repository URL:**  {repo_url}", space_after=4)
    para(doc, "The repository is public, so it opens without a GitHub account. "
              "It holds the complete Maven project, the migration scripts, the "
              "test suite, both CI workflows, this documentation and the "
              "editable draw.io source for every figure.")

    heading(doc, "4.2 Version control technique", 2)
    para(doc,
         "The history was built deliberately: nineteen commits in fifteen "
         "numbered increments, each a complete working step, so the repository "
         "shows how the application was constructed rather than appearing fully "
         "formed in one commit. Any commit can be checked out and built.")
    table(doc, ["Technique", "How it was applied", "Why"], [
        ["Conventional Commits",
         "`feat(pricing):`, `fix(security):`, `test:`, `ci:`, `docs:`",
         "The subject says what changed and where, so the log is searchable."],
        ["Meaningful increments",
         "One commit per layer or feature",
         "A reviewer can follow the reasoning; a single “final "
         "submission” commit hides it."],
        ["Branching", "`main` holds released work; `develop` integrates",
         "A simplified Git Flow (Driessen, 2010); a full feature-branch model "
         "would be ceremony for one developer."],
        ["Semantic versioning", "Annotated tags `v1.0.0` and `v1.0.1`",
         "The number states whether an upgrade is safe."],
        ["Tag-driven release",
         "Pushing a `v*.*.*` tag triggers the release workflow",
         "A deployed version always traces back to one immutable commit."],
        ["`.gitignore` discipline",
         "Build output, IDE files, the H2 data directory and credentials are "
         "excluded",
         "The repository is public; patient data must never reach it."],
        ["Changelog", "`CHANGELOG.md` in Keep a Changelog format",
         "Explains **why** each release exists, which a commit log alone does "
         "not."],
    ], widths=[1.6, 3.4, 4.2], font_size=10.5)

    heading(doc, "4.3 Version history", 2)
    table(doc, ["#", "Commit", "What it added"], [
        ["1", "`chore: scaffold multi-module Maven project`",
         "Three-module reactor, Java 17, pinned plugin versions."],
        ["2", "`feat(common): shared service contract`",
         "DTOs, enums and validation constraints shared by both sides."],
        ["3", "`feat(data): build the data tier`",
         "Flyway schema for H2 and MySQL, entities, views, functions, "
         "triggers, `slot_lock`."],
        ["4", "`feat(pricing): billing rules`",
         "Strategy, Template Method, Factory - four pricing rules."],
        ["5", "`feat(validation): booking rules`",
         "Chain of Responsibility - six independent handlers."],
        ["6", "`feat(notifications): notification pipeline`",
         "Observer and Adapter - e-mail, SMS and audit observers."],
        ["7", "`feat(reports): five management reports`",
         "Template Method and Factory over the database views."],
        ["8", "`feat(services): services and the Facade`",
         "Services, `ClinicFacade`, sequence generation under a lock."],
        ["9", "`feat(security): authentication and sessions`",
         "Two filter chains, BCrypt, roles, remember-me, lock-out."],
        ["10", "`feat(api): publish the REST web services`",
         "Versioned `/api/v1`, one response envelope, OpenAPI 3."],
        ["11", "`feat(web): Thymeleaf web user interface`",
         "All six required functions plus admin and reports."],
        ["12", "`feat(client): menu-driven desktop client`",
         "Swing client in a separate process, Command pattern."],
        ["13", "`test: unit and integration test suite`",
         "323 automated tests; JaCoCo wired into `verify`."],
        ["14", "`ci: add CI and release pipelines`",
         "Build matrix, tests, packaging, smoke test, release on tag."],
        ["15", "`docs: README, changelog, version-control notes`",
         "Tagged **v1.0.0**."],
        ["16-18", "`fix(security):`, `fix(sequence):`, `fix(ci):`",
         "The three defects reported in section 3.6."],
        ["19", "`docs: record the 1.0.1 concurrency fixes`",
         "Tagged **v1.0.1**."],
    ], widths=[0.85, 3.2, 5.15], font_size=10)

    heading(doc, "4.4 Workflows (CI/CD)", 2)
    para(doc,
         "**`ci.yml`** runs on every push and pull request. It builds on a "
         "**matrix of JDK 17 and JDK 21** - the project targets 17 for "
         "portability, and the matrix proves it also runs on a newer runtime - "
         "then publishes test and coverage reports as artefacts, packages both "
         "executable jars, and boots the packaged server to confirm it answers "
         "its health check. A separate job runs a Trivy vulnerability scan.")
    para(doc,
         "**`release.yml`** is triggered only by a tag matching `v*.*.*`. It "
         "rebuilds from the tagged commit and creates a GitHub Release carrying "
         "both jars and that version's changelog section, so a release can "
         "never be published from uncommitted work. Together the two mean the "
         "state of `main` is always known, and producing a release is one "
         "command rather than a checklist somebody can get wrong at midnight.")

    heading(doc, "4.5 Evaluation", 2)
    para(doc,
         "One instance proves the approach worked. When the concurrency defects "
         "in section 3.6 were found **after** `v1.0.0` was tagged, the fix was "
         "three focused commits and a `v1.0.1` tag - and because the history "
         "was granular and the messages conventional, it was possible to see "
         "exactly which commit had introduced the login bookkeeping and what it "
         "had been trying to achieve. That is the practical value of a "
         "disciplined history: the ability to answer “why is this like "
         "this?” months later.")
    para(doc,
         "Two honest limitations. The brief asks for versions updated **each "
         "day**; these increments are grouped by architectural layer rather "
         "than by calendar day, which is easier to follow but does not "
         "literally show daily activity. And as a single-developer project "
         "there were no pull requests to review and no merge conflicts, so the "
         "branching model was never tested under the conditions it exists for.")


# ===================================================================== #
def references(doc):
    heading(doc, "References", 1)
    refs = [
        "Beck, K., 2003. *Test-Driven Development: By Example*. Boston: "
        "Addison-Wesley.",
        "Bloch, J., 2018. *Effective Java*. 3rd ed. Boston: Addison-Wesley.",
        "Driessen, V., 2010. *A successful Git branching model*. [Online] "
        "Available at: https://nvie.com/posts/a-successful-git-branching-model/ "
        "[Accessed 28 07 2026].",
        "Evans, E., 2004. *Domain-Driven Design: Tackling Complexity in the "
        "Heart of Software*. Boston: Addison-Wesley.",
        "Fowler, M., 2002. *Patterns of Enterprise Application Architecture*. "
        "Boston: Addison-Wesley.",
        "Fowler, M., 2003. *UML Distilled: A Brief Guide to the Standard Object "
        "Modeling Language*. 3rd ed. Boston: Addison-Wesley.",
        "Freeman, S. and Pryce, N., 2009. *Growing Object-Oriented Software, "
        "Guided by Tests*. Boston: Addison-Wesley.",
        "Gamma, E., Helm, R., Johnson, R. and Vlissides, J., 1995. *Design "
        "Patterns: Elements of Reusable Object-Oriented Software*. Reading, MA: "
        "Addison-Wesley.",
        "GitHub, 2026. *GitHub Actions documentation*. [Online] Available at: "
        "https://docs.github.com/en/actions [Accessed 02 08 2026].",
        "Martin, R. C., 2008. *Clean Code: A Handbook of Agile Software "
        "Craftsmanship*. Upper Saddle River, NJ: Prentice Hall.",
        "Object Management Group, 2017. *OMG Unified Modeling Language (OMG "
        "UML), Version 2.5.1*. [Online] Available at: "
        "https://www.omg.org/spec/UML/2.5.1/ [Accessed 20 07 2026].",
        "Oracle, 2024. *Java Platform, Standard Edition 17 Documentation*. "
        "[Online] Available at: https://docs.oracle.com/en/java/javase/17/ "
        "[Accessed 15 07 2026].",
        "OWASP Foundation, 2021. *OWASP Top Ten Web Application Security "
        "Risks*. [Online] Available at: https://owasp.org/www-project-top-ten/ "
        "[Accessed 25 07 2026].",
        "Preston-Werner, T., 2013. *Semantic Versioning 2.0.0*. [Online] "
        "Available at: https://semver.org/ [Accessed 28 07 2026].",
        "Red Gate Software, 2024. *Flyway Documentation*. [Online] Available "
        "at: https://documentation.red-gate.com/flyway [Accessed 18 07 2026].",
        "VMware, 2025. *Spring Boot Reference Documentation, version 3.5*. "
        "[Online] Available at: https://docs.spring.io/spring-boot/docs/current/"
        "reference/html/ [Accessed 10 07 2026].",
        "VMware, 2025. *Spring Security Reference*. [Online] Available at: "
        "https://docs.spring.io/spring-security/reference/ "
        "[Accessed 22 07 2026].",
    ]
    for r in refs:
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Inches(0.5)
        p.paragraph_format.first_line_indent = Inches(-0.5)
        p.paragraph_format.space_after = Pt(8)
        p.paragraph_format.line_spacing = 1.5
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        _italic_markdown(p, r)


def _italic_markdown(p, text):
    """Render *italic* spans, which Harvard uses for titles."""
    import re
    for chunk in re.split(r"(\*[^*]+\*)", text):
        if not chunk:
            continue
        r = p.add_run()
        if chunk.startswith("*") and chunk.endswith("*") and len(chunk) > 2:
            r.text = chunk[1:-1]
            r.italic = True
        else:
            r.text = chunk
        r.font.name = FONT
        r._element.rPr.rFonts.set(qn("w:eastAsia"), FONT)
        r.font.size = Pt(BODY_PT)
