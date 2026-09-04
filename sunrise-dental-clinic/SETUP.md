# SETUP — Running this project on another laptop

**Sunrise Dental Clinic — Appointment & Patient Management System**
CIS6003 Advanced Programming, WRIT1

This guide assumes the other laptop has **never seen this project**. Follow it
top to bottom and it will run.

There are two ways to do it. Read section 1, pick one, then follow only that
section.

| | Option A — Just run it | Option B — Build from source |
|---|---|---|
| **Best for** | Showing the system working | Marking, editing, or running the tests |
| **Needs** | Java 17+ only | Java 17+ only (Maven ships with the project) |
| **You hand over** | 2 files (~71 MB) | The whole project folder (~1 MB zipped) |
| **Time on their laptop** | About 2 minutes | About 5 minutes (first build downloads dependencies) |
| **Needs internet on their laptop?** | **No** | **Yes**, for the first build only |

> **Recommendation:** if your friend only wants to *see* it working, use
> **Option A**. If they need to open the code, run the tests, or you are handing
> this to a marker, use **Option B**.

---

## 1. What the other laptop must have

### 1.1 Java 17 or newer — the only requirement

Ask them to open a terminal and run:

```bash
java -version
```

**What good looks like** — the first number must be **17 or higher**:

```
openjdk version "21.0.6" 2025-01-21 LTS
```

**If the number is lower than 17, or the command is not recognised**, they must
install a JDK first:

1. Go to <https://adoptium.net/temurin/releases/>
2. Choose **Version: 21 (LTS)**, **Package Type: JDK**, and their operating
   system.
3. Download the **`.msi`** (Windows), **`.pkg`** (macOS) or **`.tar.gz`** (Linux)
   and run it.
4. On Windows, tick **"Set JAVA_HOME variable"** and **"Add to PATH"** during
   installation. These are not ticked by default and the project will not run
   without them.
5. **Close the terminal and open a new one**, then run `java -version` again.
   The old terminal will not see the new installation.

### 1.2 That is the whole list

They do **not** need to install Maven, MySQL, XAMPP, Tomcat, an IDE, or anything
else. The database is created automatically as a file inside the project folder.

---

## 2. Option A — Just run it (2 files, no building)

### 2.1 Files to give them

Copy **these two files** onto a USB stick, Google Drive or WhatsApp:

| File | Where to find it on your laptop | Size |
|---|---|---|
| `sunrise-dental-server.jar` | `sunrise-dental-clinic/dental-server/target/` | ~68 MB |
| `sunrise-dental-client.jar` | `sunrise-dental-clinic/dental-client/target/` | ~2.6 MB |

> **If those files do not exist on your laptop yet**, build them once:
> ```bash
> cd sunrise-dental-clinic
> ./mvnw clean package          # Windows: mvnw.cmd clean package
> ```

Optionally also send `docs/CIS6003_WRIT1_Report.pdf` so they can read what the
system does.

### 2.2 Steps on their laptop

**Step 1 — Make a folder.** Anywhere, for example `Desktop\dental`. Put both
`.jar` files in it. Nothing else is needed.

**Step 2 — Open a terminal in that folder.**

- **Windows:** open the folder in File Explorer, click the address bar, type
  `cmd` and press Enter.
- **macOS:** right-click the folder → *Services* → *New Terminal at Folder*.
- **Linux:** right-click inside the folder → *Open in Terminal*.

**Step 3 — Start the server.**

```bash
java -jar sunrise-dental-server.jar
```

Wait about 15 seconds. It is ready when this appears:

```
==========================================================================
 Sunrise Dental Clinic Management System is running
 CIS6003 Advanced Programming - WRIT1
--------------------------------------------------------------------------
 Web application  : http://localhost:8080/login
 ...
==========================================================================
```

**Leave this terminal window open.** Closing it stops the server.

**Step 4 — Open the web application.** In a browser go to:

**<http://localhost:8080/login>**

Sign in with:

| Username | Password |
|---|---|
| `admin` | `Admin@123` |

The dashboard will already be full of data — about 40 patients and six weeks of
appointments and bills are created automatically on the first start, so the
reports are not empty.

**Step 5 — Start the desktop client (optional but worth showing).**

Open a **second** terminal in the same folder and run:

```bash
java -jar sunrise-dental-client.jar http://localhost:8080
```

A desktop window opens. Sign in with the same username and password. This is a
completely separate program talking to the server over HTTP — which is the point
of the "distributed application" requirement.

**Step 6 — Stop everything.** Press `Ctrl + C` in the server terminal, or just
close the window.

---

## 3. Option B — Build from source

### 3.1 Files to give them

Give them the **entire `sunrise-dental-clinic` folder**.

**Before zipping it, delete these** — they are rebuilt automatically and make the
zip enormous:

| Delete | Why |
|---|---|
| `dental-common/target/` | Build output |
| `dental-server/target/` | Build output (~77 MB) |
| `dental-client/target/` | Build output |
| `data/` | The local database — it holds test patient data and is recreated on start |

One command does it:

```bash
cd sunrise-dental-clinic
./mvnw clean                  # Windows: mvnw.cmd clean
rm -rf data                   # Windows: rmdir /s /q data
```

Then zip the folder. It should be about **1.3 MB without `docs/`** (under 1 MB zipped), or about
**37 MB with `docs/`** (the report, diagrams and screenshots).

> **Better still:** if the project is on GitHub, just send them the link and they
> can run `git clone <url>`. That guarantees they get exactly the right files.

**Make sure these are included** — a zip made by dragging files can silently skip
hidden ones:

| Must be included | Why it matters |
|---|---|
| `mvnw` and `mvnw.cmd` | The Maven wrapper. Without it they must install Maven themselves. |
| `.mvn/` folder | Tells the wrapper which Maven version to fetch. Hidden on macOS/Linux. |
| `pom.xml` (in the root) | The build file. Without it nothing builds. |
| `dental-common/`, `dental-server/`, `dental-client/` | The three modules, each with its own `pom.xml` and `src/`. |
| `.github/` | The CI/CD workflows (needed for the Task D marks). Hidden. |
| `README.md`, `SETUP.md`, `CHANGELOG.md` | Documentation. |
| `docs/` | The report, diagrams and screenshots. Optional if size is a problem. |

### 3.2 Steps on their laptop

**Step 1 — Unzip** the folder somewhere with no spaces or accents in the path.
`C:\projects\sunrise-dental-clinic` is fine; `C:\My Documents\新しい\...` can
cause problems.

**Step 2 — Open a terminal in the project root.** This is the folder that
contains `pom.xml` and `mvnw`. Check with:

```bash
# Windows
dir pom.xml

# macOS / Linux
ls pom.xml
```

If that says "not found", they are in the wrong folder.

**Step 3 — On macOS or Linux only**, make the wrapper executable:

```bash
chmod +x mvnw
```

Skip this on Windows.

**Step 4 — Build.**

```bash
# Windows
mvnw.cmd clean package

# macOS / Linux
./mvnw clean package
```

The **first** build downloads Maven and about 200 MB of dependencies, so it needs
internet and takes 3–5 minutes. Later builds take about 30 seconds and work
offline.

It worked when the last lines read:

```
[INFO] BUILD SUCCESS
[INFO] Total time:  02:14 min
```

**Step 5 — Run the tests (optional, but this is what a marker will want).**

```bash
./mvnw clean verify           # Windows: mvnw.cmd clean verify
```

This runs all **323** tests and takes about two minutes. Expect:

```
[INFO] Tests run: 226, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 97, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

A coverage report is written to
`dental-server/target/site/jacoco/index.html` — open it in a browser.

**Step 6 — Start the server.**

```bash
java -jar dental-server/target/sunrise-dental-server.jar
```

**Step 7 — Open <http://localhost:8080/login>** and sign in as `admin` /
`Admin@123`.

**Step 8 — Start the desktop client** in a second terminal:

```bash
java -jar dental-client/target/sunrise-dental-client.jar http://localhost:8080
```

---

## 4. All the sign-in accounts

| Username | Password | Role | What they can do |
|---|---|---|---|
| `admin` | `Admin@123` | Administrator | Everything: treatments, staff accounts, audit trail, system diagnostics |
| `reception` | `Reception@123` | Receptionist | Book appointments, search, bill, take payments |
| `reception2` | `Reception@123` | Receptionist | A second receptionist — sign in as both at once to demonstrate that the system refuses double bookings |
| `nperera` | `Dentist@123` | Dentist | View the diary and patient history only |

These accounts are created automatically the first time the server starts.

---

## 5. Useful addresses once it is running

| What | Address |
|---|---|
| Web application | <http://localhost:8080/login> |
| REST API documentation (Swagger UI) | <http://localhost:8080/swagger-ui.html> |
| Health check | <http://localhost:8080/actuator/health> |
| Database console | <http://localhost:8080/h2-console> |
| System diagnostics | <http://localhost:8080/admin/system> |

For the **database console**, sign in to the web application first, then use:

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/sunrisedental` |
| User Name | `sa` |
| Password | *(leave empty)* |

---

## 6. Running the client and server on two different laptops

This is the best way to show that the application is genuinely distributed.

**On the laptop running the server**, find its IP address:

```bash
# Windows
ipconfig            # look for "IPv4 Address", e.g. 192.168.1.42

# macOS / Linux
ip addr show        # or: ifconfig
```

Start the server as normal. Both laptops must be on the **same Wi-Fi network**.

**On the second laptop**, start the client with the first laptop's address:

```bash
java -jar sunrise-dental-client.jar http://192.168.1.42:8080
```

If it cannot connect, the server laptop's firewall is blocking port 8080. On
Windows, the first time the server starts, a Windows Defender prompt appears —
click **Allow access** on **Private networks**.

---

## 7. If something goes wrong

| What they see | What it means | Fix |
|---|---|---|
| `'java' is not recognized as an internal or external command` | Java is not installed, or not on the PATH | Install a JDK (section 1.1). Close and reopen the terminal afterwards. |
| `UnsupportedClassVersionError ... class file version 61.0` | Their Java is older than 17 | Install JDK 17 or newer. |
| `Web server failed to start. Port 8080 was already in use` | Something else is using port 8080 | Start with a different port: `java -jar sunrise-dental-server.jar --server.port=8081` and use `http://localhost:8081` everywhere, including in the client command. |
| `Unable to access jarfile ...` | The terminal is not in the folder containing the jar | `cd` into the right folder, or type the full path to the jar. |
| Browser says "This site can't be reached" | The server is not running, or it is still starting | Check the server terminal for the `is running` banner. Wait 15 seconds and refresh. |
| The client says it cannot reach the server | Wrong address, or the server is not running | Open <http://localhost:8080/actuator/health> in a browser first. It must show `{"status":"UP"}`. |
| Sign-in is refused with the passwords above | An old `data/` folder exists with changed passwords | Stop the server, delete the `data/` folder, start again. |
| The dashboard and reports are empty | The database was created with seeding switched off | Stop the server, delete the `data/` folder, start again. |
| `mvnw: Permission denied` (macOS/Linux) | The wrapper is not executable | `chmod +x mvnw` |
| `mvnw.cmd is not recognized` (Windows) | Wrong folder, or the wrapper was not included in the zip | Check `pom.xml` and `mvnw.cmd` are both in the current folder. |
| The build stops with `Could not resolve dependencies` | No internet on the first build | Connect to the internet and run the build again. |
| Build fails with `Could not create local repository` | Non-ASCII characters or spaces in the folder path | Move the project to a simple path such as `C:\projects\dental`. |

**The catch-all fix.** Almost every runtime problem is a stale database. Stop the
server, delete the `data/` folder next to the jar, and start again. Nothing
important is lost — it is regenerated with fresh demonstration data.

---

## 8. Quick reference card

Everything needed, on one screen:

```bash
# Check Java (must be 17 or higher)
java -version

# --- Option A: run the two jars ---
java -jar sunrise-dental-server.jar
java -jar sunrise-dental-client.jar http://localhost:8080

# --- Option B: build from source ---
./mvnw clean package                                    # build
./mvnw clean verify                                     # build + 323 tests
java -jar dental-server/target/sunrise-dental-server.jar
java -jar dental-client/target/sunrise-dental-client.jar http://localhost:8080

# Web app  : http://localhost:8080/login
# Sign in  : admin / Admin@123

# Different port
java -jar sunrise-dental-server.jar --server.port=8081

# Start with a clean, empty database
#   stop the server, delete the data/ folder, start again
```

---

*For what the system does and how it is built, see [README.md](README.md).*
