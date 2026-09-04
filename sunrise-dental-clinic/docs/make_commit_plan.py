"""
Generate COMMIT_PLAN.md.

The plan is written here as data rather than typed into Markdown by hand, so
the script can prove two things before the file is written:

  1. every tracked file appears in exactly one commit, and
  2. no commit references a file that is not tracked.

Ordering rule: a commit may only depend on files an earlier commit already
added. That keeps the history sensible if anyone checks out a middle commit.
"""
import os
import subprocess
import sys
from collections import Counter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "COMMIT_PLAN.md")

S = "dental-server/src/main/java/lk/icbt/cis6003/dental/server"
T = "dental-server/src/test/java/lk/icbt/cis6003/dental/server"
R = "dental-server/src/main/resources"
C = "dental-common/src/main/java/lk/icbt/cis6003/dental/common"
CT = "dental-common/src/test/java/lk/icbt/cis6003/dental/common"
K = "dental-client/src/main/java/lk/icbt/cis6003/dental/client"


def g(prefix, *names):
    return [f"{prefix}/{n}" for n in names]


# ---------------------------------------------------------------------------
#  (phase title, [(message, note, [files...]), ...])
# ---------------------------------------------------------------------------
PLAN = [
("Day 1 - Repository setup", [
 ("chore: add gitignore",
  "Patient data and build output must never reach a public repo, so this "
  "goes in before anything else.",
  [".gitignore"]),
 ("chore: add gitattributes for line endings",
  "Stops Windows and Linux checkouts fighting over CRLF.",
  [".gitattributes"]),
 ("chore: add maven wrapper",
  "So the project builds without installing Maven first.",
  ["mvnw", "mvnw.cmd", ".mvn/wrapper/maven-wrapper.properties"]),
 ("chore: add parent pom",
  "Java 17, Spring Boot 3.5.16 BOM, all plugin versions pinned.",
  ["pom.xml"]),
 ("chore: add module poms",
  "Three modules so the build mirrors the three tiers.",
  ["dental-common/pom.xml", "dental-server/pom.xml", "dental-client/pom.xml"]),
]),

("Day 2 - Shared contract: enums", [
 ("feat: add Role enum",
  "The three staff roles.",
  g(C, "enums/Role.java")),
 ("feat: add Gender enum",
  "", g(C, "enums/Gender.java")),
 ("feat: add AppointmentStatus with lifecycle rules",
  "The enum owns which transitions are legal, so no service can bypass it.",
  g(C, "enums/AppointmentStatus.java")),
 ("feat: add payment enums",
  "", g(C, "enums/PaymentStatus.java", "enums/PaymentMethod.java")),
 ("feat: add notification enums",
  "", g(C, "enums/NotificationChannel.java", "enums/NotificationStatus.java")),
 ("feat: add clinic constants and api paths",
  "VAT rate, slot length and the API base path in one place.",
  g(C, "ClinicConstants.java", "ApiPaths.java")),
]),

("Day 3 - Shared contract: DTOs", [
 ("feat: add api response envelope",
  "One response shape for every endpoint, so the client handles failure in "
  "one place.",
  g(C, "dto/ApiResponse.java", "dto/PageResponse.java")),
 ("feat: add patient dto",
  "", g(C, "dto/PatientDto.java")),
 ("feat: add dentist and treatment dtos",
  "", g(C, "dto/DentistDto.java", "dto/TreatmentDto.java")),
 ("feat: add appointment dtos",
  "Validation annotations live on the DTO so both front ends enforce the "
  "same rules.",
  g(C, "dto/AppointmentDto.java", "dto/AppointmentRequest.java",
       "dto/SlotDto.java", "dto/StatusUpdateRequest.java")),
 ("feat: add invoice dtos",
  "", g(C, "dto/InvoiceDto.java", "dto/InvoiceLineDto.java",
          "dto/BillingRequest.java", "dto/PaymentRequest.java")),
 ("feat: add auth and help dtos",
  "", g(C, "dto/LoginRequest.java", "dto/UserDto.java",
          "dto/HelpTopicDto.java")),
 ("feat: add report dtos",
  "One envelope shared by all five reports.",
  g(C, "dto/report/ReportDto.java", "dto/report/DailyScheduleRow.java",
       "dto/report/RevenueRow.java", "dto/report/DentistWorkloadRow.java",
       "dto/report/TreatmentPopularityRow.java",
       "dto/report/OutstandingInvoiceRow.java",
       "dto/report/DashboardStatsDto.java")),
 ("test: add tests for the shared contract",
  "", g(CT, "dto/ApiResponseTest.java", "dto/ValidationConstraintsTest.java",
          "enums/AppointmentStatusTest.java")),
]),

("Day 4 - Server skeleton", [
 ("feat: add spring boot main class",
  "", g(S, "DentalClinicApplication.java")),
 ("feat: add clinic configuration properties",
  "Every tunable bound to one class instead of scattered @Value.",
  g(S, "config/ClinicProperties.java")),
 ("feat: add application.yml",
  "H2 file mode by default, so it runs with nothing installed.",
  [f"{R}/application.yml"]),
 ("feat: add error codes",
  "A stable code per failure, so the client can react without parsing text.",
  g(S, "exception/ErrorCode.java")),
 ("feat: add business exception",
  "", g(S, "exception/BusinessException.java")),
 ("feat: add the specific exceptions",
  "", g(S, "exception/ResourceNotFoundException.java",
          "exception/DuplicateResourceException.java",
          "exception/SlotUnavailableException.java",
          "exception/InvalidStateTransitionException.java")),
 ("feat: add money helper",
  "Rounding in one place so no two totals disagree.",
  g(S, "util/MoneyUtils.java")),
 ("test: add money helper tests",
  "", g(T, "util/MoneyUtilsTest.java")),
 ("feat: add security utils",
  "Who is signed in, in one place. Needed this early because the services "
  "stamp it on every record they write.",
  g(S, "security/SecurityUtils.java")),
]),

("Day 5 - Domain model", [
 ("feat: add base entity",
  "Id, timestamps and the @Version column every entity inherits.",
  g(S, "domain/BaseEntity.java")),
 ("feat: add patient entity",
  "", g(S, "domain/Patient.java")),
 ("feat: add dentist entity",
  "Each dentist carries their own working hours.",
  g(S, "domain/Dentist.java")),
 ("feat: add treatment entity",
  "Holds the pricing strategy key the factory later resolves.",
  g(S, "domain/Treatment.java")),
 ("feat: add appointment entity with slot lock",
  "slot_lock holds date|time while the chair is occupied and NULL once "
  "cancelled. This is what stops double booking.",
  g(S, "domain/Appointment.java")),
 ("feat: add invoice and invoice line entities",
  "Lines cascade with the invoice - they mean nothing without it.",
  g(S, "domain/Invoice.java", "domain/InvoiceLine.java")),
 ("feat: add user entity",
  "Tracks failed attempts and the lock-out window.",
  g(S, "domain/User.java")),
 ("feat: add log and sequence entities",
  "", g(S, "domain/AuditLog.java", "domain/NotificationLog.java",
          "domain/NumberSequence.java")),
 ("test: add test data factory",
  "Boundary values in one place, so every test below can use them.",
  g(T, "testsupport/TestDataFactory.java")),
 ("test: add appointment lifecycle tests",
  "Written before the service, so the rules were settled first.",
  g(T, "domain/AppointmentTest.java")),
 ("test: add invoice tests",
  "", g(T, "domain/InvoiceTest.java")),
]),

("Day 6 - Database schema", [
 ("feat: add h2 baseline schema",
  "Nine tables with the unique key on (dentist_id, slot_lock).",
  [f"{R}/db/migration/h2/V1__baseline_schema.sql"]),
 ("feat: add h2 reference data",
  "Dentists and the treatment catalogue.",
  [f"{R}/db/migration/h2/V2__reference_data.sql"]),
 ("feat: add h2 functions and views",
  "FN_INVOICE_TOTAL and FN_AGEING_BUCKET, plus the five reporting views.",
  [f"{R}/db/migration/h2/V3__functions_and_views.sql"]),
 ("feat: add h2 audit triggers",
  "", [f"{R}/db/migration/h2/V4__triggers.sql"]),
 ("feat: add h2 function bodies",
  "H2 aliases need the Java behind them.",
  g(S, "db/function/ClinicFunctions.java")),
 ("feat: add h2 trigger classes",
  "", g(S, "db/trigger/AppointmentAuditTrigger.java",
          "db/trigger/InvoicePaymentAuditTrigger.java")),
 ("test: add stored function tests",
  "", g(T, "db/function/ClinicFunctionsTest.java")),
 ("feat: add mysql baseline schema and reference data",
  "Same model, second engine.",
  [f"{R}/db/migration/mysql/V1__baseline_schema.sql",
   f"{R}/db/migration/mysql/V2__reference_data.sql"]),
 ("feat: add mysql functions, procedures and views",
  "", [f"{R}/db/migration/mysql/V3__functions_procedures_and_views.sql"]),
 ("feat: add mysql triggers that enforce working hours",
  "A booking outside the dentist's hours is refused even from a direct "
  "SQL insert.",
  [f"{R}/db/migration/mysql/V4__triggers.sql"]),
]),

("Day 7 - Repositories", [
 ("feat: add patient and appointment repositories",
  "", g(S, "repository/PatientRepository.java",
          "repository/AppointmentRepository.java")),
 ("feat: add reference data repositories",
  "", g(S, "repository/DentistRepository.java",
          "repository/TreatmentRepository.java")),
 ("feat: add invoice repository",
  "", g(S, "repository/InvoiceRepository.java")),
 ("feat: add user repository",
  "", g(S, "repository/UserRepository.java")),
 ("feat: add log repositories",
  "", g(S, "repository/AuditLogRepository.java",
          "repository/NotificationLogRepository.java")),
 ("feat: add sequence repository with row lock",
  "SELECT ... FOR UPDATE, so two receptionists cannot be issued the same "
  "number.",
  g(S, "repository/NumberSequenceRepository.java")),
 ("feat: add reporting dao",
  "Hand-written JDBC - reports aggregate thousands of rows and belong "
  "in SQL.",
  g(S, "repository/dao/ReportingDao.java",
       "repository/dao/JdbcReportingDao.java")),
 ("feat: add entity to dto mappers",
  "No JPA entity ever crosses the network.",
  g(S, "mapper/PatientMapper.java", "mapper/DentistMapper.java",
       "mapper/TreatmentMapper.java", "mapper/AppointmentMapper.java",
       "mapper/InvoiceMapper.java")),
]),

("Day 8 - Billing rules (Strategy + Template Method + Factory)", [
 ("feat: add pricing context and result",
  "The input and output of a pricing rule.",
  g(S, "service/pricing/PricingContext.java",
       "service/pricing/PricingResult.java")),
 ("feat: add pricing strategy interface",
  "", g(S, "service/pricing/PricingStrategy.java")),
 ("feat: add pricing template method",
  "calculate() is final - fees, surcharge, discount, cap, VAT, lines. "
  "A subclass can change how much but never the order.",
  g(S, "service/pricing/AbstractPricingStrategy.java")),
 ("feat: add standard pricing rule",
  "", g(S, "service/pricing/StandardPricingStrategy.java")),
 ("feat: add surgical pricing rule",
  "Adds the theatre surcharge.",
  g(S, "service/pricing/SurgicalPricingStrategy.java")),
 ("feat: add cosmetic pricing rule",
  "No age concession on elective work.",
  g(S, "service/pricing/CosmeticPricingStrategy.java")),
 ("feat: add emergency pricing rule",
  "Out-of-hours loading.",
  g(S, "service/pricing/EmergencyPricingStrategy.java")),
 ("feat: add pricing strategy factory",
  "Registers from the Spring context, so adding a rule cannot mean "
  "forgetting to register it.",
  g(S, "service/pricing/PricingStrategyFactory.java")),
 ("test: add pricing rule tests",
  "Checked against hand-worked figures.",
  g(T, "service/pricing/PricingStrategyTest.java")),
 ("test: add pricing factory tests",
  "Including the fallback when a treatment names an unknown rule.",
  g(T, "service/pricing/PricingStrategyFactoryTest.java")),
]),

("Day 9 - Booking rules (Chain of Responsibility)", [
 ("feat: add validation request and outcome",
  "", g(S, "service/validation/BookingValidationRequest.java",
          "service/validation/ValidationOutcome.java")),
 ("feat: add validation handler interface",
  "Each handler carries its own order.",
  g(S, "service/validation/BookingValidationHandler.java")),
 ("feat: add booking window rule",
  "Not in the past, at most 90 days ahead. No database query.",
  g(S, "service/validation/BookingWindowHandler.java")),
 ("feat: add clinic hours rule",
  "", g(S, "service/validation/ClinicHoursHandler.java")),
 ("feat: add slot alignment rule",
  "Must start on a 30-minute boundary.",
  g(S, "service/validation/SlotAlignmentHandler.java")),
 ("feat: add dentist availability rule",
  "", g(S, "service/validation/DentistAvailabilityHandler.java")),
 ("feat: add double booking rules",
  "The two that cost a query, so they run last.",
  g(S, "service/validation/DentistDoubleBookingHandler.java",
       "service/validation/PatientDoubleBookingHandler.java")),
 ("feat: assemble the validation chain",
  "Cheapest checks first; stops at the first failure.",
  g(S, "service/validation/BookingValidationChain.java")),
 ("test: add validation chain tests",
  "Proves it short-circuits before touching the database.",
  g(T, "service/validation/BookingValidationChainTest.java")),
]),

("Day 10 - Notifications (Observer + Adapter)", [
 ("feat: add gateway interface and message",
  "One clinic-shaped interface over transports with nothing in common.",
  g(S, "service/notification/gateway/MessageGateway.java",
       "service/notification/gateway/GatewayMessage.java",
       "service/notification/gateway/GatewayException.java")),
 ("feat: add console email gateway",
  "Default, so the feature is demonstrable with no SMTP server.",
  g(S, "service/notification/gateway/ConsoleEmailGateway.java")),
 ("feat: add smtp email gateway",
  "", g(S, "service/notification/gateway/SmtpEmailGateway.java")),
 ("feat: add sms gateway",
  "Normalises to E.164 and validates, but does not transmit.",
  g(S, "service/notification/gateway/MockSmsGateway.java")),
 ("test: add sms gateway tests",
  "", g(T, "service/notification/gateway/MockSmsGatewayTest.java")),
 ("feat: add appointment events",
  "", g(S, "service/notification/AppointmentEvent.java",
          "service/notification/AppointmentEventType.java",
          "service/notification/AppointmentObserver.java")),
 ("feat: add message composer",
  "", g(S, "service/notification/NotificationComposer.java")),
 ("feat: add email and sms observers",
  "", g(S, "service/notification/EmailNotificationObserver.java",
          "service/notification/SmsNotificationObserver.java")),
 ("feat: add audit trail observer",
  "", g(S, "service/notification/AuditTrailObserver.java")),
 ("feat: add event publisher that isolates failures",
  "A dead SMS gateway must not lose the patient their appointment.",
  g(S, "service/notification/AppointmentEventPublisher.java")),
 ("test: add observer isolation tests",
  "", g(T, "service/notification/AppointmentEventPublisherTest.java")),
]),

("Day 11 - Reports", [
 ("feat: add report request and template",
  "One fixed skeleton for all five reports.",
  g(S, "service/report/ReportRequest.java",
       "service/report/AbstractReportGenerator.java")),
 ("feat: add daily schedule report",
  "", g(S, "service/report/DailyScheduleReportGenerator.java")),
 ("feat: add revenue report",
  "", g(S, "service/report/RevenueReportGenerator.java")),
 ("feat: add outstanding invoices report",
  "Ageing bands come from the stored function, so the report and an "
  "ad-hoc query cannot disagree.",
  g(S, "service/report/OutstandingInvoiceReportGenerator.java")),
 ("feat: add dentist workload report",
  "", g(S, "service/report/DentistWorkloadReportGenerator.java")),
 ("feat: add treatment popularity report",
  "", g(S, "service/report/TreatmentPopularityReportGenerator.java")),
 ("feat: add report generator factory",
  "", g(S, "service/report/ReportGeneratorFactory.java")),
]),

("Day 12 - Services", [
 ("feat: add sequence allocator",
  "Its own transaction, so a clash surfaces where it can be caught.",
  g(S, "service/SequenceAllocator.java")),
 ("feat: add sequence generator service",
  "Issues APT- and INV- numbers under a row lock.",
  g(S, "service/SequenceGeneratorService.java")),
 ("test: add sequence generator tests",
  "", g(T, "service/SequenceGeneratorServiceTest.java")),
 ("feat: add patient service",
  "Matches an existing patient or registers exactly one - never a "
  "duplicate.",
  g(S, "service/PatientService.java")),
 ("feat: add dentist and treatment services",
  "", g(S, "service/DentistService.java", "service/TreatmentService.java")),
 ("feat: add appointment service",
  "Validate, allocate a number, save, publish. The save catches a "
  "constraint violation and turns it into a sentence.",
  g(S, "service/AppointmentService.java")),
 ("test: add appointment service tests",
  "", g(T, "service/AppointmentServiceTest.java"),),
 ("feat: add billing service",
  "Cross-checks the Java total against the database function before "
  "issuing.",
  g(S, "service/BillingService.java")),
 ("feat: add report service",
  "", g(S, "service/ReportService.java")),
 ("feat: add help service",
  "Served from one place so both front ends always agree.",
  g(S, "service/HelpService.java")),
 ("feat: add receipt printer",
  "Rendered on the server, so browser and desktop print identically.",
  g(S, "util/ReceiptPrinter.java")),
 ("feat: add clinic facade",
  "Collapses multi-call sequences into one round trip for the remote "
  "client.",
  g(S, "service/ClinicFacade.java")),
 ("feat: add reminder scheduler",
  "", g(S, "service/ReminderScheduler.java")),
]),

("Day 13 - Security", [
 ("feat: add user details for spring security",
  "", g(S, "security/ClinicUserDetails.java",
          "security/ClinicUserDetailsService.java")),
 ("feat: add login bookkeeping service",
  "Its own transaction so a clash between two simultaneous sign-ins can "
  "be caught.",
  g(S, "security/UserLoginBookkeepingService.java")),
 ("feat: add authentication audit listener",
  "Every attempt written to audit_log with the caller's IP.",
  g(S, "security/AuthenticationAuditListener.java")),
 ("test: add audit listener tests",
  "", g(T, "security/AuthenticationAuditListenerTest.java")),
 ("feat: add security config with two filter chains",
  "JSON 401 for the API, an HTML redirect for the browser. One chain "
  "would send login pages to the desktop client.",
  g(S, "config/SecurityConfig.java")),
 ("feat: add staff account bootstrap",
  "Creates the accounts on first start; never resets an existing one.",
  g(S, "bootstrap/StaffAccountInitializer.java")),
]),

("Day 14 - REST web services", [
 ("feat: add rest exception handler",
  "One place that maps every failure to a code, and never leaks internals.",
  g(S, "api/RestExceptionHandler.java")),
 ("feat: add auth api",
  "", g(S, "api/AuthApiController.java")),
 ("feat: add appointment api",
  "", g(S, "api/AppointmentApiController.java")),
 ("feat: add invoice api",
  "", g(S, "api/InvoiceApiController.java")),
 ("feat: add reference data api",
  "", g(S, "api/ReferenceDataApiController.java")),
 ("feat: add report api",
  "", g(S, "api/ReportApiController.java")),
 ("feat: add openapi config",
  "Publishes the contract at /swagger-ui.html.",
  g(S, "config/OpenApiConfig.java")),
]),

("Day 15 - Web interface", [
 ("feat: add stylesheet",
  "Hand-written, no CDN, so the clinic works with no internet.",
  [f"{R}/static/css/app.css"]),
 ("feat: add page layout fragments",
  "", [f"{R}/templates/fragments/layout.html"]),
 ("feat: add web exception handler and error pages",
  "", g(S, "web/WebExceptionHandler.java") +
     [f"{R}/templates/error/message.html",
      f"{R}/templates/access-denied.html"]),
 ("feat: add session and cookie state",
  "Recently viewed trail, and the table density preference.",
  g(S, "web/session/RecentlyViewedTracker.java",
       "web/session/UiPreferences.java")),
 ("feat: add global model advice",
  "Puts the current user on every page without each controller repeating "
  "it.",
  g(S, "web/GlobalModelAdvice.java")),
 ("feat: add login page",
  "", g(S, "web/AuthWebController.java") + [f"{R}/templates/login.html"]),
 ("feat: add dashboard",
  "", g(S, "web/DashboardWebController.java") +
     [f"{R}/templates/dashboard.html"]),
 ("feat: add appointment pages",
  "Book, list, view, day schedule and availability.",
  g(S, "web/AppointmentWebController.java") +
     [f"{R}/templates/appointments/form.html",
      f"{R}/templates/appointments/list.html",
      f"{R}/templates/appointments/view.html",
      f"{R}/templates/appointments/day.html",
      f"{R}/templates/appointments/availability.html"]),
 ("feat: add patient pages",
  "", g(S, "web/PatientWebController.java") +
     [f"{R}/templates/patients/form.html",
      f"{R}/templates/patients/list.html",
      f"{R}/templates/patients/view.html"]),
 ("feat: add billing pages",
  "Calculate, then issue - the two steps are deliberately separate.",
  g(S, "web/BillingWebController.java") +
     [f"{R}/templates/billing/calculate.html",
      f"{R}/templates/billing/list.html",
      f"{R}/templates/billing/view.html",
      f"{R}/templates/billing/receipt.html"]),
 ("feat: add reference data pages",
  "", g(S, "web/ReferenceWebController.java") +
     [f"{R}/templates/reference/dentists.html",
      f"{R}/templates/reference/dentist-form.html",
      f"{R}/templates/reference/treatments.html",
      f"{R}/templates/reference/treatment-form.html"]),
 ("feat: add report pages",
  "", g(S, "web/ReportWebController.java") +
     [f"{R}/templates/reports/index.html",
      f"{R}/templates/reports/view.html"]),
 ("feat: add help pages",
  "", g(S, "web/HelpWebController.java") + [f"{R}/templates/help.html"]),
 ("feat: add admin pages",
  "Audit trail, notification log, users, and a diagnostics page generated "
  "from the running application.",
  g(S, "web/AdminWebController.java") +
     [f"{R}/templates/admin/audit.html",
      f"{R}/templates/admin/notifications.html",
      f"{R}/templates/admin/users.html",
      f"{R}/templates/admin/system.html"]),
 ("feat: add demo data seeder",
  "Six weeks of history so the reports are not empty on first run.",
  g(S, "bootstrap/DemoDataSeeder.java")),
]),

("Day 16 - Desktop client", [
 ("feat: add client http layer",
  "HTTP and JSON only - no Spring, no JDBC, no server class on the "
  "classpath.",
  g(K, "api/ClinicApiClient.java", "api/ApiException.java")),
 ("feat: add client session holder",
  "", g(K, "api/ClientSession.java")),
 ("feat: add ui helpers",
  "", g(K, "ui/UiUtils.java")),
 ("feat: add login dialog",
  "", g(K, "ui/LoginDialog.java")),
 ("feat: add appointment windows",
  "", g(K, "ui/RegisterAppointmentDialog.java",
          "ui/SearchAppointmentWindow.java",
          "ui/AppointmentDetailWindow.java")),
 ("feat: add billing windows",
  "", g(K, "ui/BillingWindow.java", "ui/ReceiptWindow.java")),
 ("feat: add report and help windows",
  "", g(K, "ui/ReportWindow.java", "ui/HelpWindow.java")),
 ("feat: add menu command base",
  "Each action is an object carrying its own label and role rule.",
  g(K, "command/MenuCommand.java", "command/AbstractMenuCommand.java")),
 ("feat: add the menu commands and main window",
  "One commit because MainFrame builds itself from the command list and "
  "RefreshDashboardCommand calls back into MainFrame - splitting them "
  "would leave a commit that does not compile.",
  g(K, "command/RegisterAppointmentCommand.java",
       "command/SearchAppointmentCommand.java",
       "command/TodayScheduleCommand.java",
       "command/GenerateBillCommand.java",
       "command/FindBillCommand.java",
       "command/ViewReportsCommand.java",
       "command/ShowHelpCommand.java",
       "command/RefreshDashboardCommand.java",
       "command/AboutCommand.java",
       "command/ExitCommand.java",
       "ui/MainFrame.java")),
 ("feat: add client entry point",
  "Takes the server address as an argument.",
  g(K, "DentalClientApplication.java")),
]),

("Day 17 - Integration tests", [
 ("test: add integration test config",
  "In-memory H2 with the real Flyway schema.",
  ["dental-server/src/test/resources/application-test.yml"]),
 ("test: add security integration tests",
  "Roles, CSRF, lock-out, and JSON 401 rather than an HTML redirect.",
  g(T, "integration/SecurityIT.java")),
 ("test: add appointment api tests",
  "", g(T, "integration/AppointmentApiIT.java")),
 ("test: add billing api tests",
  "", g(T, "integration/BillingApiIT.java")),
 ("test: add database feature tests",
  "Writes straight through the repository to prove the constraint, not "
  "the service, is the guarantee.",
  g(T, "integration/DatabaseFeaturesIT.java")),
 ("test: add report and web ui tests",
  "", g(T, "integration/ReportAndWebUiIT.java")),
]),

("Day 18 - CI/CD", [
 ("ci: add build and test workflow",
  "JDK 17 and 21, then boots the packaged jar to prove it starts.",
  [".github/workflows/ci.yml"]),
 ("ci: add release workflow",
  "Triggered by a v*.*.* tag, so a release always traces to one commit.",
  [".github/workflows/release.yml"]),
]),

("Day 19 - Documentation", [
 ("docs: add readme",
  "", ["README.md"]),
 ("docs: add setup guide",
  "For running it on a machine that has never seen the project.",
  ["SETUP.md"]),
 ("docs: add changelog",
  "", ["CHANGELOG.md"]),
 ("docs: add version control notes",
  "", ["docs/VERSION-CONTROL.md"]),
]),

("Day 20 - Diagrams", [
 ("docs: add diagram toolkit",
  "Shared hand-drawn style so all seven diagrams match.",
  ["docs/diagram_kit.py"]),
 ("docs: add use case diagram",
  "", ["docs/gen_usecase.py", "docs/diagrams/fig01_use_case.png"]),
 ("docs: add class diagram",
  "", ["docs/gen_class.py", "docs/diagrams/fig02_class.png"]),
 ("docs: add sequence diagrams",
  "Sign in, register appointment, calculate bill.",
  ["docs/gen_sequence.py", "docs/diagrams/fig03_seq_signin.png",
   "docs/diagrams/fig04_seq_appointment.png",
   "docs/diagrams/fig05_seq_billing.png"]),
 ("docs: add architecture diagram",
  "", ["docs/gen_architecture.py", "docs/diagrams/fig06_architecture.png"]),
 ("docs: add er diagram",
  "", ["docs/gen_er.py", "docs/diagrams/fig07_er_diagram.png"]),
 ("docs: add editable drawio sources",
  "Same diagrams, editable in draw.io with the sketch style kept.",
  ["docs/drawio_kit.py", "docs/gen_drawio.py"] +
  [f"docs/drawio/fig0{i}_{n}.drawio" for i, n in
   [(1, "use_case"), (2, "class"), (3, "seq_signin"),
    (4, "seq_appointment"), (5, "seq_billing"), (6, "architecture"),
    (7, "er_diagram")]]),
]),

("Day 21 - Screenshots", [
 ("docs: add screenshots of the running system",
  "Captured against seeded data, not mock-ups.",
  [f"docs/screenshots/{n}" for n in [
   "01_login.png", "02_dashboard.png", "03_appointments_list.png",
   "04_appointment_new.png", "05_appointment_view.png",
   "06_availability.png", "07_patients.png", "08_billing_list.png",
   "09_invoice_view.png", "10_receipt.png", "11_reports_index.png",
   "12_report_revenue.png", "13_report_outstanding.png",
   "14_report_workload.png", "15_daily_schedule.png", "16_treatments.png",
   "17_dentists.png", "18_help.png", "19_admin_audit.png",
   "20_admin_notifications.png", "21_admin_system.png",
   "22_admin_users.png", "23_swagger.png"]]),
 ("docs: crop screenshots for the report",
  "Full-page captures are unreadable when scaled to fit a page.",
  ["docs/prep_screenshots.py"] +
  [f"docs/screenshots/report/{n}" for n in [
   "01_login.png", "02_dashboard.png", "04_appointment_new.png",
   "05_appointment_view.png", "09_invoice_view.png", "10_receipt.png",
   "12_report_revenue.png", "19_admin_audit.png",
   "20_admin_notifications.png", "23_swagger.png"]]),
]),

("Day 22 - The report", [
 ("docs: add report formatting toolkit",
  "A4, 1.5in left margin, Times New Roman, 1.5 spacing - enforced in one "
  "place.",
  ["docs/report_kit.py"]),
 ("docs: add university logos",
  "", ["docs/assets/logo_icbt.png", "docs/assets/logo_cardiff.png"]),
 ("docs: write task a - uml diagrams",
  "", ["docs/report_content_a.py"]),
 ("docs: write task b - patterns and architecture",
  "", ["docs/report_content_b.py"]),
 ("docs: write tasks c and d - testing and git",
  "", ["docs/report_content_cd.py"]),
 ("docs: assemble the report",
  "", ["docs/build_report.py"]),
 ("docs: add pdf export",
  "Goes through Word so the table of contents gets real page numbers.",
  ["docs/export_pdf.py"]),
 ("docs: add format and word count checks",
  "Checks the exported PDF, not just the docx.",
  ["docs/verify_report.py", "docs/count_words.py", "docs/trim_tables.py"]),
 ("docs: add the built report",
  "", ["docs/CIS6003_WRIT1_Report.docx", "docs/CIS6003_WRIT1_Report.pdf"]),
 ("docs: add the commit plan",
  "Generated from data so every file is provably covered once, and the "
  "ordering is checked rather than asserted.",
  ["COMMIT_PLAN.md", "docs/make_commit_plan.py",
   "docs/check_commit_order.py"]),
]),
]


def tracked():
    out = subprocess.run(["git", "ls-files"], cwd=ROOT,
                         capture_output=True, text=True).stdout
    return [l.strip() for l in out.split("\n") if l.strip()]


def validate(planned, on_disk):
    ok = True
    dupes = [f for f, n in Counter(planned).items() if n > 1]
    if dupes:
        ok = False
        print(f"FAIL  {len(dupes)} file(s) in more than one commit:")
        for f in dupes[:20]:
            print("        ", f)

    # planned but not yet tracked - they are added by the last commit
    pending = {"COMMIT_PLAN.md", "docs/make_commit_plan.py",
               "docs/check_commit_order.py"}
    missing = sorted(set(on_disk) - set(planned))
    if missing:
        ok = False
        print(f"FAIL  {len(missing)} tracked file(s) in no commit:")
        for f in missing[:20]:
            print("        ", f)

    ghosts = sorted(set(planned) - set(on_disk) - pending)
    if ghosts:
        ok = False
        print(f"FAIL  {len(ghosts)} planned file(s) that do not exist:")
        for f in ghosts[:20]:
            print("        ", f)
    return ok


def render(total_commits, total_files):
    L = []
    add = L.append
    add("# Commit plan")
    add("")
    add("**Sunrise Dental Clinic — CIS6003 WRIT1**")
    add("")
    add(f"**{total_commits} commits**, grouped into {len(PLAN)} working "
        f"sessions, covering all {total_files} files in the project.")
    add("")
    add("---")
    add("")
    add("## How to use this")
    add("")
    add("Work down the list. For each commit, run the `git add` line exactly "
        "as written, then the `git commit` line:")
    add("")
    add("```bash")
    add("git add <the files listed>")
    add('git commit -m "<the message>"')
    add("```")
    add("")
    add("Check nothing was missed as you go:")
    add("")
    add("```bash")
    add("git status --short        # should be empty after each commit")
    add("```")
    add("")
    add("### The ordering rule")
    add("")
    add("Each commit only uses files an earlier commit already added: "
        "contract before server, entities before repositories, repositories "
        "before services, services before controllers. So checking out any "
        "commit gives a tree that makes sense, and the history reads like the "
        "system was built rather than pasted in.")
    add("")
    add("That claim is checked, not assumed. `docs/check_commit_order.py` "
        "reads every Java file, works out which commit adds each class, and "
        "fails if any commit imports a class that a later commit adds. It "
        "currently reports zero.")
    add("")
    add("Two places could not be split any further, and the plan says so at "
        "the commit: `MainFrame` builds itself from the command list while "
        "`RefreshDashboardCommand` calls back into `MainFrame`, so they go in "
        "together.")
    add("")
    add("Commits are grouped into sessions rather than dated. If you want the "
        "history to show work spread across days, make those commits on the "
        "days you actually do them — backdating with `GIT_AUTHOR_DATE` would "
        "put times in the log that are not true.")
    add("")
    add("### Before you start")
    add("")
    add("```bash")
    add("cd sunrise-dental-clinic")
    add("git init")
    add("git branch -M main")
    add("```")
    add("")
    add("### Checkpoints")
    add("")
    add("| After session | Run this | Expect |")
    add("|---|---|---|")
    add("| 3 | `./mvnw -pl dental-common test` | contract module compiles and "
        "its tests pass |")
    add("| 12 | `./mvnw -pl dental-server test` | unit tests pass |")
    add("| 17 | `./mvnw clean verify` | all 323 tests pass |")
    add("| 22 | `git log --oneline \\| wc -l` | "
        f"{total_commits} |")
    add("")
    add("---")
    add("")

    n = 0
    for title, commits in PLAN:
        add(f"## {title}")
        add("")
        for msg, note, files in commits:
            n += 1
            add(f"### {n}. `{msg}`")
            add("")
            if note:
                add(note)
                add("")
            add("```bash")
            if len(files) == 1:
                add(f"git add {files[0]}")
            else:
                add("git add \\")
                for i, f in enumerate(files):
                    add(f"  {f}" + (" \\" if i < len(files) - 1 else ""))
            add(f'git commit -m "{msg}"')
            add("```")
            add("")
        add("---")
        add("")

    add("## Tagging")
    add("")
    add("Once everything is committed and `./mvnw clean verify` is green:")
    add("")
    add("```bash")
    add('git tag -a v1.0.0 -m "First complete release"')
    add("git push origin main --tags")
    add("```")
    add("")
    add("Pushing the tag triggers `release.yml`, which rebuilds from the tag "
        "and publishes a GitHub Release with both jars.")
    add("")
    return "\n".join(L)


def main():
    planned = [f for _, commits in PLAN for _, _, files in commits
               for f in files]
    on_disk = tracked()
    total_commits = sum(len(c) for _, c in PLAN)

    print(f"sessions      : {len(PLAN)}")
    print(f"commits       : {total_commits}")
    print(f"files planned : {len(planned)}")
    print(f"files tracked : {len(on_disk)}")
    print()

    if not validate(planned, on_disk):
        print("\nplan is inconsistent - not writing COMMIT_PLAN.md")
        return 1

    print("OK    every tracked file appears in exactly one commit")
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(render(total_commits, len(planned)))
    print("wrote", OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main())
