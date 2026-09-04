# Version control and workflow

**CIS6003 Advanced Programming — WRIT1, Task D**

This document records the version-control techniques used on the project and how
to demonstrate each of them.

---

## 1. Repository

| | |
|---|---|
| Hosting | GitHub, **public** (Task D requires public access) |
| Default branch | `main` |
| Integration branch | `develop` |
| Licence / use | Academic — ICBT Campus / Cardiff Metropolitan University |

### Publishing this repository

```bash
# From the project root
git remote add origin https://github.com/<your-username>/sunrise-dental-clinic.git
git push -u origin main
git push origin develop
git push origin --tags
```

Then paste the repository URL into the report, as Task D asks.

---

## 2. Branching model

A trimmed-down Git Flow. Full Git Flow has five branch types; a project this
size needs four, and adding the fifth would be ceremony rather than control.

```
main ──────●────────────────────●──────────────▶   production; tagged, protected
            \                  /
develop ─────●───●───●───●───●─────────────────▶   integration; always buildable
              \     /     \ /
feature/*      ●───●       ●                       one branch per unit of work
```

| Branch | Purpose | Merges into |
|---|---|---|
| `main` | Released code only. Every commit is tagged and has passed CI. | — |
| `develop` | Integration. Must always build and pass tests. | `main` via `release/*` |
| `feature/<name>` | One feature or fix. Short-lived. | `develop` |
| `release/<version>` | Version bump, final checks, changelog. | `main` **and** `develop` |

**Why `main` is not committed to directly:** it is the branch the clinic would
deploy from. Anything reaching it must have been through CI on a pull request,
which is what makes "the tagged version works" a fact rather than a hope.

### Working on a feature

```bash
git switch develop
git pull
git switch -c feature/appointment-reminders

# ... work, committing in small logical steps ...
git add -p                      # stage deliberately, not with `git add .`
git commit -m "feat(notifications): send reminders 24 hours before a visit"

git push -u origin feature/appointment-reminders
# then open a pull request into develop; CI runs automatically
```

---

## 3. Commit message convention

[Conventional Commits](https://www.conventionalcommits.org/): `type(scope): summary`.

| Type | Used for |
|---|---|
| `feat` | New behaviour a user would notice |
| `fix` | A defect corrected |
| `test` | Tests added or changed |
| `refactor` | Behaviour unchanged, structure improved |
| `docs` | Documentation |
| `ci` | Build or pipeline |
| `chore` | Tooling, dependencies, scaffolding |

**Why it earns its keep here:** the release workflow generates release notes
straight from `git log` since the previous tag. A history of "update" and "fix
stuff" produces release notes nobody can read; a conventional history produces
usable ones for free.

Each commit in this repository states *why* in its body, not only *what* — the
diff already says what changed.

---

## 4. Tagging and releases

Annotated tags, never lightweight:

```bash
git tag -a v1.0.0 -m "Initial release: all six required functions, 272 tests passing"
git push origin v1.0.0
```

An annotated tag is a real object carrying its author, date and message, so it
can be signed and verified. A lightweight tag is just a moveable pointer, which
is precisely the wrong property for something a clinic will deploy from.

Pushing a `v*.*.*` tag triggers `release.yml`, which rebuilds from the tag,
re-runs all 272 tests, and publishes a GitHub Release with both executable jars.

Versions follow SemVer: `MAJOR.MINOR.PATCH`.

---

## 5. Workflows (CI/CD)

Both live in `.github/workflows/`.

### `ci.yml` — Continuous Integration

Runs on every push and pull request to `main`, `develop`, `feature/**` and
`release/**`.

| Job | What it proves |
|---|---|
| **build-and-test** | The project compiles and all 272 tests pass on **JDK 17 *and* JDK 21** — so it cannot quietly depend on one developer's JDK. Publishes test reports and the JaCoCo coverage report as downloadable artefacts, and writes a results table into the run summary. |
| **package** | Both executable jars build, **and the packaged server actually boots** — the job starts it and polls `/actuator/health` until it reports `UP`. A jar that builds but will not start is worse than a build failure, because it looks like success. |
| **security-scan** | No dependency carries a known CRITICAL advisory. The clinic stores patient data, so this fails the build rather than warning. |

`concurrency` cancels a superseded run, so the badge always reflects the newest
commit.

### `release.yml` — Continuous Delivery

Triggered by pushing a version tag. Rebuilds from the tag, re-runs the full
suite (a tag can be moved, so CI's earlier pass is not sufficient evidence),
generates release notes from the commit log, and publishes the release.

---

## 6. What is deliberately not committed

`.gitignore` is written for a **public** repository, so it is a security control
rather than tidiness:

| Excluded | Why |
|---|---|
| `data/`, `*.mv.db` | The H2 database file. Once the system is in use it holds real patient names, addresses, telephone numbers and clinical notes. |
| `application-local.yml`, `.env`, `*.key`, `*.jks` | Where SMTP passwords and database credentials end up in practice. |
| `target/`, `*.log` | Build output and logs — reproducible, and logs can contain personal data. |
| `.idea/`, `.vscode/` | One developer's editor settings are not part of the product. |

**Staff passwords are not seeded in SQL** for the same reason. A Flyway script
containing BCrypt hashes would publish the clinic's credentials permanently, and
rewriting public history to remove a secret is unreliable — once pushed, a
secret must be treated as burned. `StaffAccountInitializer` hashes them at run
time from configurable properties instead.

---

## 7. Demonstrating this for the report

| Evidence to capture | Where |
|---|---|
| Repository is public | GitHub repository page |
| Branching model in use | **Insights → Network** graph |
| Commit history with meaningful messages | **Commits** tab |
| Several versions deployed | **Releases** page, showing each tag and its jars |
| CI running and passing | **Actions** tab — a run showing the JDK 17 and 21 matrix |
| Tests passing in CI | The run summary table, and the downloadable test-report artefact |
| Coverage | The `jacoco-coverage-report` artefact from the same run |
| Deployment | The `package` job log, showing the packaged server reporting healthy |
| A pull request going through CI | Any PR into `develop` |

---

## 8. Honest note on the commit history

The commits in this repository are grouped by build increment — scaffolding,
contract, data tier, each pattern, security, API, UI, client, tests, CI,
documentation — in the order the work was genuinely done. The dates are the
dates the work happened; no timestamps have been back-dated to manufacture the
appearance of a longer schedule.

Task D asks for versions updated over several days. The honest way to satisfy
that is to continue development on this repository: each further feature on its
own `feature/*` branch, merged through a pull request, tagged when released.
`CHANGELOG.md` already lists the planned 1.1.0 and 1.2.0 work as the natural
next increments.
