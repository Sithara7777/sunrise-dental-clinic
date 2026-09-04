# Changelog

All notable changes to the Sunrise Dental Clinic Management System.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Every release below corresponds to an annotated Git tag, which the
`release.yml` workflow turns into a GitHub Release carrying both executable
jars. A deployed version can therefore always be traced back to one immutable
commit.

---

## [1.0.1] — Concurrency hardening

Found by a deliberate, genuine concurrency stress test after the initial
release: firing a dozen truly simultaneous HTTP requests at the same
dentist/date/time slot, using a thread pool sized to fire them at once
rather than the sequential "book, then try again" test that only proves a
pre-check works. That is the difference between testing that a rule is
*written* and testing that it *holds under a real race* - and it found two
genuine defects that no sequential test, however thorough, could have
caught.

### Fixed
- **Login bookkeeping could corrupt an unrelated request's response.**
  Two requests authenticating as the same account in the same instant
  (Swagger UI's "Authorize" button re-sends Basic auth on every call; a
  shared front-desk login in two tabs does the same) raced to update the
  same versioned `User` row. The loser's optimistic-locking failure
  surfaced at transaction commit - inside Spring Security's filter chain,
  outside `RestExceptionHandler`'s reach - producing a raw HTTP 500 instead
  of the API's envelope, and silently discarding that sign-in's audit
  entry. Fixed by isolating the bookkeeping update in its own
  transactional bean (`UserLoginBookkeepingService`) so the listener's
  try/catch can actually see the failure; the audit entry is now written
  unconditionally regardless.
- **The first-ever allocation of a business-identifier sequence could fail
  under concurrency.** `SELECT ... FOR UPDATE` cannot lock a row that does
  not exist yet, so the very first appointment or invoice number of a new
  year could be requested by several concurrent bookings at once, all
  finding no row, all attempting to create it - and every loser's booking
  was abandoned entirely, not because their slot was taken, but because of
  how the counter happened to be initialised. Fixed with a one-time retry
  in a genuinely new transaction (`SequenceAllocator`), on the same
  reasoning as the login fix above.
- Raised the HikariCP pool from 10 to 20 connections and added a specific
  503 `SERVICE_BUSY` response for connection-pool exhaustion, both surfaced
  by the same stress test once the two defects above were fixed.
- **CI:** `aquasecurity/trivy-action` was referenced without the `v` prefix
  its tags actually use (`@0.24.0` instead of `@v0.24.0`), which would have
  failed the security-scan job on its first real run. Every action
  reference in both workflows was then verified against the GitHub API.

### Verified after the fix
Twelve truly simultaneous booking requests for one slot now produce
exactly one success and eleven clean `409 SLOT_UNAVAILABLE` responses,
with zero unhandled exceptions anywhere in the server log - re-run
against three consecutive fresh databases to rule out a fluke. The MySQL
migration profile (previously untested against a real database engine)
was run end to end against MariaDB 10.4: all four migrations, both
stored functions, both stored procedures and all triggers - including
the native `BEFORE INSERT` trigger that enforces working hours - verified
with real queries and real data, not merely reviewed as text.

---

## [1.0.0] — Initial release

The complete system, delivered in the increments listed below. Each increment
was committed separately so the history shows how the application was built up
rather than appearing fully formed in one commit.

### Increment 1 — Project scaffolding
- Three-module Maven reactor (`dental-common`, `dental-server`, `dental-client`)
  so the physical structure mirrors the distributed, three-tier architecture.
- Java 17 baseline, Spring Boot 3.5.16 BOM, all plugin versions pinned.
- `.gitignore` written to keep patient data and credentials out of a repository
  that Task D requires to be public.

### Increment 2 — The shared contract
- DTOs, enums and constants in `dental-common`, depended on by both the server
  and the desktop client.
- Bean Validation constraints travel with the DTOs, so client and server enforce
  the same rules.
- `AppointmentStatus` owns the appointment lifecycle, including "only a
  completed visit is billable" and "a cancelled visit releases its slot".

### Increment 3 — The data tier
- Flyway-managed schema for H2 and for MySQL 8; Hibernate runs
  `ddl-auto=validate`, so the schema is owned by reviewable SQL.
- The anti-double-booking design: `slot_lock` plus a unique constraint on
  `(dentist_id, slot_lock)`.
- Five reporting views, two stored functions, two triggers, and CHECK
  constraints on statuses, discounts and payments.
- MySQL profile adds two native stored procedures and triggers that *enforce*
  working hours at database level.

### Increment 4 — Billing rules (Strategy + Template Method + Factory)
- Four pricing strategies: standard, surgical, cosmetic, emergency.
- The invariant billing sequence sealed in a `final` template method.
- A factory that registers strategies automatically and degrades to the standard
  rule, with a warning, if reference data carries an unrecognised key.

### Increment 5 — Booking rules (Chain of Responsibility)
- Six independent validation handlers, ordered so the cheap checks run before
  the two that cost a database query.
- The chain short-circuits on the first failure, so the user is told the most
  fundamental problem rather than a confusing downstream one.

### Increment 6 — Notifications (Observer + Adapter)
- E-mail, SMS and audit-trail observers on an event publisher that isolates
  failures, so a gateway outage cannot prevent a patient being booked.
- Gateways behind one clinic-shaped interface; console e-mail by default so the
  feature is demonstrable with no SMTP server.
- Every attempt persisted to the notification log, successes and failures alike.

### Increment 7 — Reports (Template Method + Factory)
- Five decision-support reports sharing one envelope, so one web page and one
  desktop window display all of them.
- Reports read database views; the debtor ageing bands come from the stored
  function, so a report and an ad-hoc SQL query cannot disagree.

### Increment 8 — Services and the Facade
- Appointment, patient, dentist, treatment, billing and report services.
- `SequenceGeneratorService` allocates business identifiers under a pessimistic
  row lock, so two receptionists saving simultaneously cannot be issued the same
  appointment number.
- `ClinicFacade` collapses multi-call sequences into one round trip and one
  transaction for the remote client.

### Increment 9 — Security
- Spring Security with BCrypt, three roles, session management, an opt-in
  remember-me cookie, and lock-out after five failed attempts.
- Two filter chains: JSON 401s for the API, HTML redirects for the browser.
- Every sign-in attempt audited with its IP address.

### Increment 10 — REST web services
- Versioned `/api/v1` contract with a uniform response envelope.
- OpenAPI 3 published at `/swagger-ui.html`.
- One global exception handler mapping every failure to a stable error code,
  and never echoing internal detail to a caller.

### Increment 11 — Web user interface
- Thymeleaf application covering all six required functions plus patient,
  dentist, treatment, report and administration screens.
- Hand-written stylesheet with no CDN dependency, so the application is fully
  usable offline.
- Session-scoped "recently viewed" trail and a cookie-backed density preference.

### Increment 12 — Menu-driven desktop client
- Swing application in a separate process, reaching the server only over HTTP.
- Command pattern behind the menu bar and the button panel, so the two cannot
  drift apart.
- Server-rendered receipts, so the desktop and the browser print identically.

### Increment 13 — Automated tests
- 175 unit tests and 97 integration tests, all passing.
- Integration tests run against the real Flyway schema, so constraints, views,
  functions and triggers are genuinely exercised.
- JaCoCo coverage reporting wired into `verify`.

### Increment 14 — CI/CD
- CI on every push and pull request: builds on JDK 17 and 21, runs the whole
  suite, publishes reports and coverage, packages both jars and boots the
  packaged server to prove it starts.
- Release pipeline triggered by a `v*.*.*` tag.

### Increment 15 — Documentation
- README covering architecture, patterns, database features, testing and
  operations.

---

## Planned

### [1.1.0]
- Real SMS aggregator integration behind the existing `MessageGateway` adapter —
  no change above the adapter layer.
- Patient-facing appointment confirmation by reply link.

### [1.2.0]
- Treatment plans spanning multiple visits.
- Dentist leave calendar feeding the availability check.
