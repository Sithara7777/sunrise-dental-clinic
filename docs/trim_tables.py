"""One-off: shorten the wordiest table cells in the report content files.

Kept as a script rather than done by hand so the edits are reviewable and
repeatable if the content files are regenerated.
"""
import io, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))

EDITS = {
    "report_content_b.py": [
        # ---- tier table -------------------------------------------------
        ('"Business rules. A controller that decided a discount would put that "\n'
         '         "rule out of reach of the desktop client."',
         '"Business rules - a controller that decided a discount would put it "\n'
         '         "out of reach of the desktop client."'),
        ('"SQL and HTTP. This tier does not know it is reached over the web, "\n'
         '         "which is why the same rules serve both front ends."',
         '"SQL and HTTP. It does not know it is reached over the web, which is "\n'
         '         "why the same rules serve both front ends."'),
        ('"Decisions. It stores and constrains; it does not choose what the "\n'
         '         "clinic should do."',
         '"Decisions. It stores and constrains only."'),
        # ---- pattern table ----------------------------------------------
        ('"A new pricing rule is a new class, not an edit to a growing `if`."',
         '"A new rule is a new class, not an edit to a growing `if`."'),
        ('"The order of the billing steps is `final` and cannot be broken."',
         '"The order of the billing steps is `final`."'),
        ('"Callers name what they want; nobody writes `new` on a rule."',
         '"Callers name what they want; nobody writes `new`."'),
        ('"Each booking rule is independent and separately testable."',
         '"Each booking rule is independent and testable."'),
        ('"A real SMS provider can be added without touching a service."',
         '"A real SMS provider needs no change above the adapter."'),
        ('"Turns a multi-call sequence into one round trip for the client."',
         '"Turns a multi-call sequence into one round trip."'),
        ('"An appointment cannot be half-built; `slotLock` is always right."',
         '"An appointment cannot be half-built."'),
        ('"The menu bar and the button panel cannot drift apart."',
         '"Menu bar and button panel cannot drift apart."'),
        ('"Entities never leave the server; the wire format is separate."',
         '"Entities never leave the server."'),
        # ---- database features table ------------------------------------
        ('"Recomputes an invoice total in SQL; every bill is compared against "\n'
         '         "it, so a rounding mistake in Java is caught before the patient sees "\n'
         '         "it."',
         '"Recomputes an invoice total in SQL; every bill is checked against "\n'
         '         "it, so a rounding mistake in Java is caught first."'),
        ('"Places a debt in a 0-30 / 31-60 / 61-90 / 90+ day band; the report "\n'
         '         "calls it, so report and ad-hoc query cannot disagree."',
         '"Puts a debt in a 0-30 / 31-60 / 61-90 / 90+ day band; the report "\n'
         '         "calls it, so it cannot disagree with an ad-hoc query."'),
        ('"Refuses any booking outside the dentist\'s working hours - holds even "\n'
         '         "against a direct SQL insert (MySQL)."',
         '"Refuses a booking outside the dentist\'s hours, even from a direct "\n'
         '         "SQL insert (MySQL)."'),
        ('"Write the audit trail at database level, so a change made outside "\n'
         '         "the application is still recorded."',
         '"Audit at database level, so a change made outside the application "\n'
         '         "is still recorded."'),
        ('"Discounts cannot exceed 50%, payments cannot exceed the total, a "\n'
         '         "status must be a permitted value."',
         '"Discounts <= 50%, payments <= total, status must be permitted."'),
        # ---- reports table ----------------------------------------------
        ('"Which debts to chase first (bands from `FN_AGEING_BUCKET`)."',
         '"Which debts to chase first."'),
    ],

    "report_content_cd.py": [
        # ---- levels table -----------------------------------------------
        ('"One class behaves correctly in isolation - a pricing rule, a "\n'
         '         "handler, a status transition.",\n'
         '         "JUnit 5 with Mockito. No Spring context, no database, so a failure "\n'
         '         "points at one class."',
         '"One class behaves correctly in isolation.",\n'
         '         "JUnit 5 with Mockito - no Spring context, no database, so a "\n'
         '         "failure points at one class."'),
        ('"The parts work together against the real schema - constraints, "\n'
         '         "views, functions and triggers genuinely exercised.",\n'
         '         "`@SpringBootTest` with MockMvc against the real Flyway schema on "\n'
         '         "in-memory H2."',
         '"The parts work together against the real schema.",\n'
         '         "`@SpringBootTest` + MockMvc on the real Flyway schema, in-memory "\n'
         '         "H2."'),
        ('"The rules hold under a genuine race, and the interfaces behave for a "\n'
         '         "human being.",\n'
         '         "A multi-threaded client, plus scripted checks of every page and "\n'
         '         "endpoint."',
         '"The rules hold under a genuine race.",\n'
         '         "A multi-threaded client, plus scripted page and endpoint checks."'),
        # ---- defect table -----------------------------------------------
        ('"Two requests authenticating as the same account in the same instant "\n'
         '         "raced to update one versioned `User` row. The loser\'s "\n'
         '         "optimistic-locking failure escaped as a raw HTTP 500 instead of the "\n'
         '         "API envelope, and that sign-in\'s audit entry was lost."',
         '"Two requests authenticating as the same account in the same instant "\n'
         '         "raced to update one versioned `User` row. The loser\'s locking "\n'
         '         "failure escaped as a raw HTTP 500, and its audit entry was lost."'),
        ('"`SELECT ... FOR UPDATE` cannot lock a row that does not exist yet, so "\n'
         '         "the first appointment number of a new year could be requested by "\n'
         '         "several bookings at once, and every loser\'s booking was abandoned - "\n'
         '         "because of how a counter happened to be initialised."',
         '"`SELECT ... FOR UPDATE` cannot lock a row that does not exist yet, so "\n'
         '         "the first appointment number of a year could be claimed by several "\n'
         '         "bookings at once, and every loser\'s booking was abandoned."'),
        ('"The security-scan job referenced `trivy-action@0.24.0`, but that "\n'
         '         "action\'s tags carry a `v` prefix, so the job would have failed on its "\n'
         '         "first real run."',
         '"`trivy-action@0.24.0` was referenced, but that action\'s tags carry a "\n'
         '         "`v` prefix, so the job would have failed on its first run."'),
        # ---- version control table --------------------------------------
        ('"The subject says what changed and where, so the log is searchable "\n'
         '         "and a changelog can be assembled from it."',
         '"The subject says what changed and where, so the log is searchable."'),
        ('"One commit per layer or feature - scaffolding, contract, data tier, "\n'
         '         "each pattern, security, API, web UI, client, tests, CI, docs",\n'
         '         "A reviewer can follow the reasoning; a single “final "\n'
         '         "submission” commit hides all of it."',
         '"One commit per layer or feature",\n'
         '         "A reviewer can follow the reasoning; a single “final "\n'
         '         "submission” commit hides it."'),
        ('"A simplified Git Flow (Driessen, 2010). A full feature-branch model "\n'
         '         "would be ceremony for one developer."',
         '"A simplified Git Flow (Driessen, 2010); a full feature-branch model "\n'
         '         "would be ceremony for one developer."'),
        ('"The number states whether an upgrade is safe; `v1.0.1` is a patch "\n'
         '         "because it fixed defects without changing the API."',
         '"The number states whether an upgrade is safe."'),
        ('"Build output, IDE files, the H2 data directory and local credentials "\n'
         '         "are excluded",\n'
         '         "The repository must be public; patient data and secrets must never "\n'
         '         "reach it."',
         '"Build output, IDE files, the H2 data directory and credentials are "\n'
         '         "excluded",\n'
         '         "The repository is public; patient data must never reach it."'),
        # ---- commit history table ---------------------------------------
        ('"Three-module reactor, Java 17 baseline, pinned plugin versions."',
         '"Three-module reactor, Java 17, pinned plugin versions."'),
        ('"DTOs, enums and validation constraints shared by both sides."',
         '"DTOs, enums and validation constraints shared by both sides."'),
        ('"Flyway schema for H2 and MySQL, entities, views, functions, "\n'
         '         "triggers, the `slot_lock` design."',
         '"Flyway schema for H2 and MySQL, entities, views, functions, "\n'
         '         "triggers, `slot_lock`."'),
        ('"Services, `ClinicFacade`, sequence generation under a row lock."',
         '"Services, `ClinicFacade`, sequence generation under a lock."'),
        ('"Two filter chains, BCrypt, roles, remember-me, lock-out, auditing."',
         '"Two filter chains, BCrypt, roles, remember-me, lock-out."'),
        ('"All six required functions plus admin and report screens."',
         '"All six required functions plus admin and reports."'),
        ('"Build matrix, tests, packaging, smoke test, scan, release on tag."',
         '"Build matrix, tests, packaging, smoke test, release on tag."'),
    ],
}


def main():
    total = 0
    for fname, pairs in EDITS.items():
        path = os.path.join(HERE, fname)
        s = io.open(path, encoding="utf-8").read()
        applied = 0
        for old, new in pairs:
            if old in s:
                s = s.replace(old, new, 1)
                applied += 1
            else:
                print(f"  [skip] {fname}: {old[:56]!r}")
        io.open(path, "w", encoding="utf-8").write(s)
        print(f"{fname}: applied {applied}/{len(pairs)}")
        total += applied
    print("total edits:", total)


if __name__ == "__main__":
    main()
