# Sunrise Dental Clinic — Appointment & Patient Management System

**CIS6003 Advanced Programming — WRIT1**
ICBT Campus / Cardiff Metropolitan University

A distributed, three-tier Java system that replaces the paper appointment book at
Sunrise Dental Clinic, Colombo.

> **A note on the brief.** The assessment header names the assignment
> *"Online vehicle reservation System"*, but the scenario, the six required
> functions and the entire marking grid describe **Sunrise Dental Clinic**, an
> appointment and patient management system. The scenario governs; this project
> implements the dental clinic. The discrepancy is recorded here and in the
> report so it is not mistaken for a misread requirement.

> **New here?** [**SETUP.md**](SETUP.md) is a step-by-step guide for getting this
> running on a machine that has never seen the project before — including
> exactly which files to hand over.

---

## Contents

1. [What problem this solves](#1-what-problem-this-solves)
2. [How to run it](#2-how-to-run-it)
3. [Architecture](#3-architecture)
4. [Design patterns](#4-design-patterns)
5. [Database features](#5-database-features)
6. [Testing](#6-testing)
7. [The six required functions](#7-the-six-required-functions)
8. [Project layout](#8-project-layout)
9. [Documentation and diagrams](#9-documentation-and-diagrams)
10. [Git and CI/CD](#10-git-and-cicd)
11. [Configuration](#11-configuration)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. What problem this solves

The clinic's paper system produced four specific failures. Every one of them is
answered by a concrete mechanism, not by "the system is computerised now":

| Clinic's complaint | How this system prevents it |
|---|---|
| **Double bookings** | A dentist's diary slot is held by a `slot_lock` column and a **unique database constraint** on `(dentist_id, slot_lock)`. The constraint — not application code — is the guarantee. Cancelling sets the column to `NULL`, which releases the slot, because SQL never treats two `NULL`s as equal. Proven in `DatabaseFeaturesIT` by writing straight through the repository and bypassing the service layer entirely, and again by firing twelve genuinely simultaneous bookings at one slot. |
| **Lost patient records** | A patient exists **once**. Every visit points at that record, so a patient's complete history is a single query. Booking either matches an existing patient (by code, NIC, or name + telephone) or registers exactly one new record — never a duplicate. |
| **Long waiting times** | Each dentist has their own shift hours. The booking validation chain refuses an appointment outside them, so a patient can no longer be booked with somebody who went home an hour earlier. Free slots are offered up front, so a taken slot is never even requested. |
| **Billing errors** | The client can never send a price. Consultation fee and treatment cost come from the dentist and catalogue records; only a capped discount is accepted. Every figure is stored, so a historic receipt does not change when the price list does. The Java total is then **cross-checked against the database stored function** `FN_INVOICE_TOTAL`, and a mismatch is refused. |

---

## 2. How to run it

### 2.1 What you need

| | Version | Why |
|---|---|---|
| **JDK** | **17 or newer** | The only hard requirement. Tested on 17 and 21. |
| Maven | *not required* | The repository ships the Maven wrapper (`mvnw`), which downloads Maven itself on first use. |
| Database | *not required* | The default profile uses a file-backed H2 database created on first start. |

Check your Java before anything else:

```bash
java -version      # must report 17 or higher
```

### 2.2 Build

From the project root:

```bash
# Linux / macOS
./mvnw clean package

# Windows (Command Prompt or PowerShell)
mvnw.cmd clean package
```

That produces two runnable jars:

| Jar | Size | What it is |
|---|---|---|
| `dental-server/target/sunrise-dental-server.jar` | ~68 MB | The whole server: web UI, REST API, business rules, database |
| `dental-client/target/sunrise-dental-client.jar` | ~2.6 MB | The menu-driven desktop client |

To build **and** run the full 323-test suite, use `verify` instead of `package`:

```bash
./mvnw clean verify
```

### 2.3 Start the server

```bash
java -jar dental-server/target/sunrise-dental-server.jar
```

Wait for the banner ending with `Sunrise Dental Clinic Management System is
running`, then open:

**<http://localhost:8080/login>**

On first start the application creates the staff accounts, applies the four
Flyway migrations, and seeds about six weeks of trading history (40 patients with
their appointments and bills) so the reports and dashboard show real
distributions immediately rather than empty tables.

### 2.4 Start the desktop client

This is a **separate process** and can be on a different machine. Open a second
terminal:

```bash
java -jar dental-client/target/sunrise-dental-client.jar http://localhost:8080
```

The argument is the server's address. Point it at another machine's IP address to
see that the application is genuinely distributed:

```bash
java -jar dental-client/target/sunrise-dental-client.jar http://192.168.1.42:8080
```

### 2.5 Sign in

| Username | Password | Role | Can do |
|---|---|---|---|
| `admin` | `Admin@123` | Administrator | Everything, including the treatment catalogue, staff accounts and the audit trail |
| `reception` | `Reception@123` | Receptionist | Book, search, bill, take payment |
| `reception2` | `Reception@123` | Receptionist | As above — a second account, useful for demonstrating the concurrency safeguards |
| `nperera` | `Dentist@123` | Dentist | View the diary and patient history |

These are created at first start-up by `StaffAccountInitializer`, which hashes
them with BCrypt at run time. They are **not** stored in the repository — this
repo is public, and committing password hashes to a public repository publishes
them permanently. The application logs a warning while the defaults are still in
use.

### 2.6 Where everything lives

| What | Where |
|---|---|
| Web application | <http://localhost:8080/login> |
| REST API documentation (Swagger UI) | <http://localhost:8080/swagger-ui.html> |
| OpenAPI contract | <http://localhost:8080/v3/api-docs> |
| Database console (H2) | <http://localhost:8080/h2-console> — JDBC URL `jdbc:h2:file:./data/sunrisedental`, user `sa`, no password |
| Health check | <http://localhost:8080/actuator/health> |
| System diagnostics (which patterns actually loaded) | <http://localhost:8080/admin/system> |

### 2.7 Common options

```bash
# Different port (then point the client at the same port)
java -jar dental-server/target/sunrise-dental-server.jar --server.port=8081

# Start with an empty database - no demonstration history
java -jar dental-server/target/sunrise-dental-server.jar --clinic.demo.seed-enabled=false

# Run against MySQL 8 instead of H2
java -jar dental-server/target/sunrise-dental-server.jar --spring.profiles.active=mysql

# Run from source without packaging first
./mvnw -pl dental-server spring-boot:run
```

To start completely fresh, stop the server and delete the `data/` directory. It
is recreated, migrated and re-seeded on the next start.

---

## 3. Architecture

```
+------------------------------+        +--------------------------------------+
|  dental-client               |        |  dental-server                       |
|  (separate JVM process)      |        |                                      |
|                              | HTTP   |  +--------------------------------+  |
|  Menu-driven Swing UI        | + JSON |  | PRESENTATION                   |  |
|  Command pattern menus       |------->|  | Thymeleaf MVC  |  REST API v1  |  |
|  Singleton session holder    |        |  +----------------+---------------+  |
|  NO database, NO Spring      |        |  +----------------v---------------+  |
|  NO business rules           |        |  | BUSINESS                       |  |
+---------------+--------------+        |  | Services - Pricing strategies  |  |
                |                       |  | Validation chain - Observers   |  |
                | depends on            |  | Report generators - Facade     |  |
                v                       |  +----------------+---------------+  |
+------------------------------+        |  +----------------v---------------+  |
|  dental-common               |<-------|  | DATA                           |  |
|  DTOs - enums - constants    | depends|  | JPA entities - Repositories    |  |
|  The wire contract only      |   on   |  | JDBC reporting DAO - Flyway    |  |
+------------------------------+        |  +----------------+---------------+  |
                                        +-------------------+------------------+
                                                            v
                                                   H2 (default) or MySQL 8
                                             5 views - 2 stored functions
                                           triggers - unique + CHECK constraints
```

**Why this is genuinely distributed rather than one program in two windows:**
`dental-client` has no JDBC driver, no Spring, and no service class on its
classpath. Its only route into the system is HTTP against the published web
services. Client and server can run on different machines, operating systems and
JVM versions; the only thing they share is the `dental-common` DTO jar.

**Why the tiers are real rather than nominal:** the boundaries are package and
interface boundaries. The presentation tier talks only to services, the business
tier talks only to repository interfaces, and no JPA entity is ever serialised
onto the wire. Deploying all three tiers in one process is a *deployment*
decision that could be reversed without changing the tier above.

---

## 4. Design patterns

Each was chosen for a specific problem in this system, and each is documented in
its own source file with what it costs as well as what it buys.

| Pattern | Where | Problem it solves |
|---|---|---|
| **Strategy** | `service/pricing/*PricingStrategy` | Four genuinely different billing *policies* — surgical surcharge, cosmetic exclusion from concessions, emergency out-of-hours loading. A `switch` would put every future rule inside the one method that prints every bill in the clinic. |
| **Template Method** | `AbstractPricingStrategy`, `AbstractReportGenerator` | The billing sequence (subtotal → discount → VAT → total) is fixed by tax law and is `final`; only the two genuinely variable steps are overridable. Reports share the same fixed skeleton. |
| **Factory** | `PricingStrategyFactory`, `ReportGeneratorFactory` | Turns a database value into an object without any caller naming a concrete class. Registration is automatic from the Spring context, so adding a rule cannot mean forgetting to register it. |
| **Chain of Responsibility** | `service/validation/*Handler` | Six independent booking rules, each testable alone, short-circuiting on the first failure so the user sees the most fundamental problem — and stopping *before* the two rules that cost a database query. |
| **Observer** | `AppointmentEventPublisher` + 3 observers | Booking must trigger e-mail, SMS and audit. Failure is isolated per observer, so an SMS outage cannot prevent a patient being booked. |
| **Adapter** | `MessageGateway` + SMTP / console / SMS gateways | Presents one clinic-shaped interface over transports with nothing in common. Switching from the console gateway to real SMTP is one property. |
| **Facade** | `ClinicFacade` | Collapses multi-call sequences into one network round trip and one transaction for the remote client — `completeAndBill` either does both or neither. |
| **Builder** | `Appointment.Builder`, `AppointmentRequest.Builder`, `PricingContext.Builder` | Objects with six-plus fields, several of the same type sitting next to each other — the exact shape that produces silent argument-order bugs. |
| **Singleton** | `ClientSession` (desktop client) | One user at one keyboard for the life of the process. Uses the initialisation-on-demand holder idiom. The server's equivalent is correctly *not* a singleton — it is thread-bound session state. |
| **Command** | `client/command/*Command` | Every menu action is an object carrying its own label, accelerator and role rule. The menu bar and the button panel are built from the same list, so they cannot drift apart. |
| **DAO / Repository** | `repository/*`, `JdbcReportingDao` | Spring Data for the transactional model; a hand-written JDBC DAO for reporting reads, which aggregate thousands of rows and belong in SQL. |
| **DTO** | `dental-common/dto/*` | No JPA entity ever crosses the network. Adding a column cannot leak it onto a published contract. |
| **MVC** | Thymeleaf controllers | Standard Spring MVC separation. |

The live registries are visible at runtime on **`/admin/system`** — that page is
generated from the running application, not from documentation, which is the
mitigation for the main drawback of auto-registering patterns.

---

## 5. Database features

Default profile is **H2 file mode** (zero installation). A **MySQL 8** profile is
included with its own migration set.

```bash
java -jar dental-server/target/sunrise-dental-server.jar --spring.profiles.active=mysql
```

| Feature | Detail |
|---|---|
| **Versioned migrations** | Flyway, 4 scripts per engine. Hibernate runs `ddl-auto=validate`, so the schema is owned by reviewable SQL and the Java model is *checked* against it at start-up. |
| **9 tables** | `patient`, `dentist`, `treatment`, `appointment`, `invoice`, `invoice_line`, `app_user`, `audit_log`, `notification_log`, plus `number_sequence` |
| **5 reporting views** | `v_daily_schedule`, `v_revenue_daily`, `v_dentist_workload`, `v_treatment_popularity`, `v_outstanding_invoice` |
| **2 stored functions** | `FN_INVOICE_TOTAL` (the billing formula) and `FN_AGEING_BUCKET` (debtor ageing bands). Used *by the views*, so a report and an accountant's own SQL cannot disagree with the printed receipt. |
| **Triggers** | 2 on H2, 4 on MySQL. `trg_appointment_audit` and `trg_invoice_payment_audit` fire for any change, including one made over a SQL console. MySQL adds a `BEFORE INSERT` trigger that *enforces* working hours and rejects an inactive dentist at database level. |
| **2 stored procedures** (MySQL) | `SP_SETTLE_INVOICE` (atomic payment under `FOR UPDATE`), `SP_DAILY_CLOSING_SUMMARY` |
| **Constraints** | Unique appointment number, unique slot lock, one bill per appointment, plus CHECK constraints on status values, discount ≤ 50% and `amount_paid ≤ total_amount` |
| **Concurrency** | Pessimistic row lock on the identifier sequence; JPA optimistic locking (`@Version`) on every entity |

---

## 6. Testing

```bash
./mvnw clean verify       # everything - unit and integration
./mvnw test               # unit tests only, no database
```

| | Count | What they cover |
|---|---:|---|
| **Unit tests** | 226 | Pricing rules verified against hand-worked figures, the appointment state machine, money arithmetic, the validation chain (including proof it short-circuits *before* querying the database), Observer failure isolation, the SMS adapter, and the stored-function bodies |
| **Integration tests** | 97 | Real Flyway schema with real constraints, triggers and views; the full HTTP stack via MockMvc; authentication, roles, CSRF and sessions; every report; every web page |
| **Total** | **323** | All passing |

**Coverage (JaCoCo, `dental-server`):** 73.7% lines, 76.4% methods, 96.6% classes.
`dental-common` reports lower, and honestly so — it is a DTO contract module
whose accessors are exercised across the wire by the server's integration tests,
which JaCoCo attributes to the module that owns the `.exec` file. Its
*behaviour-bearing* classes (`AppointmentStatus`, `ApiResponse`, the validation
constraints) are directly unit tested.

Reports land in `dental-server/target/site/jacoco/index.html` after `verify`.

The desktop client's networking layer was additionally verified headlessly
against a live server, and the anti-double-booking design was verified under a
genuine race: twelve simultaneous requests for one slot produce exactly one
success and eleven clean `409 SLOT_UNAVAILABLE` responses. That test found two
real defects **after** the suite was already green; both are described in
[`CHANGELOG.md`](CHANGELOG.md) under 1.0.1.

---

## 7. The six required functions

| # | Requirement | Where |
|---|---|---|
| 1 | **User authentication** | Spring Security, BCrypt, roles, session + remember-me cookie, lock-out after 5 failures, every attempt audited. Web: `/login`. API: `POST /api/v1/auth/login`. Desktop: `LoginDialog`. |
| 2 | **Register new appointment** | `/appointments/new`, `POST /api/v1/appointments`, `RegisterAppointmentDialog`. Collects every field the scenario names. The appointment number is **issued by the server** — there is deliberately no field to type it into. |
| 3 | **Display appointment details** | `/appointments/{no}`, `GET /api/v1/appointments/{no}`, `SearchAppointmentWindow`. Search by number, name or telephone. |
| 4 | **Calculate and print bill** | Two-step calculate-then-issue at `/billing/calculate/{no}`, `POST /api/v1/invoices`, `BillingWindow`. 48-column plain-text receipt rendered **on the server**, so both front ends print identically. |
| 5 | **Help section** | `/help`, `GET /api/v1/help`, `HelpWindow`. Eight topics, 42 numbered steps. Served from the API so both UIs always agree. |
| 6 | **Exit system** | `ExitCommand` confirms, signs out on the server to release the session, then closes. Web: POST-only logout. |

Beyond the brief: patient master file, dentist roster, treatment catalogue,
availability checking, five management reports, dashboard with trend chart,
e-mail and SMS alerts with a delivery log, automatic 24-hour reminders, audit
trail, and workstation preferences in a cookie.

---

## 8. Project layout

```
sunrise-dental-clinic/
├── mvnw, mvnw.cmd, .mvn/       Maven wrapper - no Maven install needed
├── pom.xml                     Aggregator: Java 17, Spring Boot 3.5.16 BOM
├── SETUP.md                    Step-by-step guide for a fresh machine
├── CHANGELOG.md                What changed in each release, and why
├── dental-common/              The wire contract - DTOs, enums, constants
├── dental-server/
│   └── src/main/
│       ├── java/.../server/
│       │   ├── api/            REST controllers (the published web services)
│       │   ├── web/            Thymeleaf controllers + session/cookie state
│       │   ├── service/        Business tier
│       │   │   ├── pricing/      Strategy + Template Method + Factory
│       │   │   ├── validation/   Chain of Responsibility
│       │   │   ├── notification/ Observer + Adapter gateways
│       │   │   └── report/       Template Method + Factory
│       │   ├── domain/         JPA entities
│       │   ├── repository/     Spring Data + JDBC reporting DAO
│       │   ├── security/       Spring Security integration
│       │   └── bootstrap/      Staff accounts and demonstration data
│       └── resources/
│           ├── db/migration/   Flyway scripts - h2/ and mysql/
│           └── templates/      Thymeleaf pages
├── dental-client/              Menu-driven Swing desktop application
├── docs/                       Report, diagrams, screenshots, build scripts
└── .github/workflows/          CI and Release pipelines
```

---

## 9. Documentation and diagrams

Everything under `docs/` is generated from the code and is rebuildable.

| Path | What it is |
|---|---|
| `docs/CIS6003_WRIT1_Report.pdf` | The 39-page assessment report, organised by Task A–D |
| `docs/diagrams/*.png` | Seven hand-drawn UML diagrams: use case, class, three sequence diagrams, architecture, ER schema |
| `docs/drawio/*.drawio` | The same seven diagrams as editable draw.io sources |
| `docs/screenshots/` | Screenshots of the running system |
| `docs/build_report.py` | Rebuilds the report; `verify_report.py` checks it against the brief's format rules |

To regenerate the diagrams and the report (needs Python with `python-docx`,
`matplotlib`, `Pillow` and `PyMuPDF`; the PDF step needs Microsoft Word):

```bash
cd docs
python gen_usecase.py && python gen_class.py && python gen_sequence.py
python gen_architecture.py && python gen_er.py && python gen_drawio.py
python build_report.py && python export_pdf.py && python verify_report.py
```

---

## 10. Git and CI/CD

| Workflow | Trigger | What it does |
|---|---|---|
| **CI** (`ci.yml`) | Every push and pull request | Builds on **JDK 17 and 21**, runs all 323 tests, publishes test and coverage reports as artefacts, writes a results table to the run summary, packages both jars, **boots the packaged server and waits for `/actuator/health` to report UP**, and fails on a CRITICAL dependency advisory. |
| **Release** (`release.yml`) | Pushing a `v*.*.*` tag | Rebuilds from the tag, re-runs the full suite, generates release notes from the commit log since the previous tag, and publishes a GitHub Release with both versioned jars. |

Branching follows a simple Git Flow: `develop` integrates, `main` holds released
work, and `main` is tagged for every release (`v1.0.0`, `v1.0.1`).

---

## 11. Configuration

Everything is bound to `ClinicProperties` and overridable on the command line
with `--property=value`.

| Property | Default | Purpose |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `clinic.demo.seed-enabled` | `true` | Seeds ~6 weeks of history so reports are meaningful |
| `clinic.demo.patient-count` | `40` | How many demonstration patients to create |
| `clinic.notifications.smtp-enabled` | `false` | `false` logs e-mail to the console gateway; `true` sends real SMTP |
| `clinic.notifications.reminder-lead-hours` | `24` | How far ahead the 18:00 reminder job looks |
| `clinic.security.session-timeout-minutes` | `30` | Idle session timeout |
| `clinic.security.remember-me-days` | `7` | Lifetime of the opt-in device cookie |
| `spring.profiles.active` | *(none)* | `mysql` switches to MySQL 8 and its migration set |

---

## 12. Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `UnsupportedClassVersionError` on start-up | The JDK is older than 17. Check with `java -version` and install a newer one. |
| `Web server failed to start. Port 8080 was already in use` | Something else holds the port. Start with `--server.port=8081` and point the client at the same port. |
| The client cannot reach the server | The server is not running, or the address is wrong. Confirm <http://localhost:8080/actuator/health> returns `{"status":"UP"}` first. |
| `Table "APPOINTMENT" not found`, or a Flyway validation error | The `data/` directory is from an older schema. Stop the server, delete `data/`, start again. |
| Reports and dashboard are empty | The database was created with seeding switched off. Delete `data/` and restart without `--clinic.demo.seed-enabled=false`. |
| Sign-in fails with the passwords above | The accounts already exist with changed passwords — `StaffAccountInitializer` never resets an existing account. Delete `data/` for a clean start. |
| `mvnw: Permission denied` on Linux or macOS | `chmod +x mvnw` |
| The build cannot download dependencies | The first build needs internet. Afterwards it works offline from `~/.m2`. |

---

*Built for CIS6003 Advanced Programming, WRIT1.*
