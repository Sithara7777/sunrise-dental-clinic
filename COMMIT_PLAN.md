# Commit plan

**Sunrise Dental Clinic — CIS6003 WRIT1**

**176 commits**, grouped into 22 working sessions, covering all 325 files in the project.

---

## How to use this

Work down the list. For each commit, run the `git add` line exactly as written, then the `git commit` line:

```bash
git add <the files listed>
git commit -m "<the message>"
```

Check nothing was missed as you go:

```bash
git status --short        # should be empty after each commit
```

### The ordering rule

Each commit only uses files an earlier commit already added: contract before server, entities before repositories, repositories before services, services before controllers. So checking out any commit gives a tree that makes sense, and the history reads like the system was built rather than pasted in.

That claim is checked, not assumed. `docs/check_commit_order.py` reads every Java file, works out which commit adds each class, and fails if any commit imports a class that a later commit adds. It currently reports zero.

Two places could not be split any further, and the plan says so at the commit: `MainFrame` builds itself from the command list while `RefreshDashboardCommand` calls back into `MainFrame`, so they go in together.

Commits are grouped into sessions rather than dated. If you want the history to show work spread across days, make those commits on the days you actually do them — backdating with `GIT_AUTHOR_DATE` would put times in the log that are not true.

### Before you start

```bash
cd sunrise-dental-clinic
git init
git branch -M main
```

### Checkpoints

| After session | Run this | Expect |
|---|---|---|
| 3 | `./mvnw -pl dental-common test` | contract module compiles and its tests pass |
| 12 | `./mvnw -pl dental-server test` | unit tests pass |
| 17 | `./mvnw clean verify` | all 323 tests pass |
| 22 | `git log --oneline \| wc -l` | 176 |

---

## Day 1 - Repository setup

### 1. `chore: add gitignore`

Patient data and build output must never reach a public repo, so this goes in before anything else.

```bash
git add .gitignore
git commit -m "chore: add gitignore"
```

### 2. `chore: add gitattributes for line endings`

Stops Windows and Linux checkouts fighting over CRLF.

```bash
git add .gitattributes
git commit -m "chore: add gitattributes for line endings"
```

### 3. `chore: add maven wrapper`

So the project builds without installing Maven first.

```bash
git add \
  mvnw \
  mvnw.cmd \
  .mvn/wrapper/maven-wrapper.properties
git commit -m "chore: add maven wrapper"
```

### 4. `chore: add parent pom`

Java 17, Spring Boot 3.5.16 BOM, all plugin versions pinned.

```bash
git add pom.xml
git commit -m "chore: add parent pom"
```

### 5. `chore: add module poms`

Three modules so the build mirrors the three tiers.

```bash
git add \
  dental-common/pom.xml \
  dental-server/pom.xml \
  dental-client/pom.xml
git commit -m "chore: add module poms"
```

---

## Day 2 - Shared contract: enums

### 6. `feat: add Role enum`

The three staff roles.

```bash
git add dental-common/src/main/java/lk/icbt/cis6003/dental/common/enums/Role.java
git commit -m "feat: add Role enum"
```

### 7. `feat: add Gender enum`

```bash
git add dental-common/src/main/java/lk/icbt/cis6003/dental/common/enums/Gender.java
git commit -m "feat: add Gender enum"
```

### 8. `feat: add AppointmentStatus with lifecycle rules`

The enum owns which transitions are legal, so no service can bypass it.

```bash
git add dental-common/src/main/java/lk/icbt/cis6003/dental/common/enums/AppointmentStatus.java
git commit -m "feat: add AppointmentStatus with lifecycle rules"
```

### 9. `feat: add payment enums`

```bash
git add \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/enums/PaymentStatus.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/enums/PaymentMethod.java
git commit -m "feat: add payment enums"
```

### 10. `feat: add notification enums`

```bash
git add \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/enums/NotificationChannel.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/enums/NotificationStatus.java
git commit -m "feat: add notification enums"
```

### 11. `feat: add clinic constants and api paths`

VAT rate, slot length and the API base path in one place.

```bash
git add \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/ClinicConstants.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/ApiPaths.java
git commit -m "feat: add clinic constants and api paths"
```

---

## Day 3 - Shared contract: DTOs

### 12. `feat: add api response envelope`

One response shape for every endpoint, so the client handles failure in one place.

```bash
git add \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/ApiResponse.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/PageResponse.java
git commit -m "feat: add api response envelope"
```

### 13. `feat: add patient dto`

```bash
git add dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/PatientDto.java
git commit -m "feat: add patient dto"
```

### 14. `feat: add dentist and treatment dtos`

```bash
git add \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/DentistDto.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/TreatmentDto.java
git commit -m "feat: add dentist and treatment dtos"
```

### 15. `feat: add appointment dtos`

Validation annotations live on the DTO so both front ends enforce the same rules.

```bash
git add \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/AppointmentDto.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/AppointmentRequest.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/SlotDto.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/StatusUpdateRequest.java
git commit -m "feat: add appointment dtos"
```

### 16. `feat: add invoice dtos`

```bash
git add \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/InvoiceDto.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/InvoiceLineDto.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/BillingRequest.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/PaymentRequest.java
git commit -m "feat: add invoice dtos"
```

### 17. `feat: add auth and help dtos`

```bash
git add \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/LoginRequest.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/UserDto.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/HelpTopicDto.java
git commit -m "feat: add auth and help dtos"
```

### 18. `feat: add report dtos`

One envelope shared by all five reports.

```bash
git add \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/report/ReportDto.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/report/DailyScheduleRow.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/report/RevenueRow.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/report/DentistWorkloadRow.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/report/TreatmentPopularityRow.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/report/OutstandingInvoiceRow.java \
  dental-common/src/main/java/lk/icbt/cis6003/dental/common/dto/report/DashboardStatsDto.java
git commit -m "feat: add report dtos"
```

### 19. `test: add tests for the shared contract`

```bash
git add \
git commit -m "feat: add report dtos"
  dental-common/src/test/java/lk/icbt/cis6003/dental/common/dto/ValidationConstraintsTest.java \
  dental-common/src/test/java/lk/icbt/cis6003/dental/common/enums/AppointmentStatusTest.java
git commit -m "test: add tests for the shared contract"
```

---

## Day 4 - Server skeleton

### 20. `feat: add spring boot main class`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/DentalClinicApplication.java
git commit -m "feat: add spring boot main class"
```

### 21. `feat: add clinic configuration properties`

Every tunable bound to one class instead of scattered @Value.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/config/ClinicProperties.java
git commit -m "feat: add clinic configuration properties"
```

### 22. `feat: add application.yml`

H2 file mode by default, so it runs with nothing installed.

```bash
git add dental-server/src/main/resources/application.yml
git commit -m "feat: add application.yml"
```

### 23. `feat: add error codes`

A stable code per failure, so the client can react without parsing text.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/exception/ErrorCode.java
git commit -m "feat: add error codes"
```

### 24. `feat: add business exception`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/exception/BusinessException.java
git commit -m "feat: add business exception"
```

### 25. `feat: add the specific exceptions`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/exception/ResourceNotFoundException.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/exception/DuplicateResourceException.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/exception/SlotUnavailableException.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/exception/InvalidStateTransitionException.java
git commit -m "feat: add the specific exceptions"
```

### 26. `feat: add money helper`

Rounding in one place so no two totals disagree.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/util/MoneyUtils.java
git commit -m "feat: add money helper"
```

### 27. `test: add money helper tests`

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/util/MoneyUtilsTest.java
git commit -m "test: add money helper tests"
```

### 28. `feat: add security utils`

Who is signed in, in one place. Needed this early because the services stamp it on every record they write.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/security/SecurityUtils.java
git commit -m "feat: add security utils"
```

---

## Day 5 - Domain model

### 29. `feat: add base entity`

Id, timestamps and the @Version column every entity inherits.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/domain/BaseEntity.java
git commit -m "feat: add base entity"
```

### 30. `feat: add patient entity`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/domain/Patient.java
git commit -m "feat: add patient entity"
```

### 31. `feat: add dentist entity`

Each dentist carries their own working hours.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/domain/Dentist.java
git commit -m "feat: add dentist entity"
```

### 32. `feat: add treatment entity`

Holds the pricing strategy key the factory later resolves.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/domain/Treatment.java
git commit -m "feat: add treatment entity"
```

### 33. `feat: add appointment entity with slot lock`

slot_lock holds date|time while the chair is occupied and NULL once cancelled. This is what stops double booking.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/domain/Appointment.java
git commit -m "feat: add appointment entity with slot lock"
```

### 34. `feat: add invoice and invoice line entities`

Lines cascade with the invoice - they mean nothing without it.

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/domain/Invoice.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/domain/InvoiceLine.java
git commit -m "feat: add invoice and invoice line entities"
```

### 35. `feat: add user entity`

Tracks failed attempts and the lock-out window.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/domain/User.java
git commit -m "feat: add user entity"
```

### 36. `feat: add log and sequence entities`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/domain/AuditLog.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/domain/NotificationLog.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/domain/NumberSequence.java
git commit -m "feat: add log and sequence entities"
```

### 37. `test: add test data factory`

Boundary values in one place, so every test below can use them.

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/testsupport/TestDataFactory.java
git commit -m "test: add test data factory"
```

### 38. `test: add appointment lifecycle tests`

Written before the service, so the rules were settled first.

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/domain/AppointmentTest.java
git commit -m "test: add appointment lifecycle tests"
```

### 39. `test: add invoice tests`

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/domain/InvoiceTest.java
git commit -m "test: add invoice tests"
```

---

## Day 6 - Database schema

### 40. `feat: add h2 baseline schema`

Nine tables with the unique key on (dentist_id, slot_lock).

```bash
git add dental-server/src/main/resources/db/migration/h2/V1__baseline_schema.sql
git commit -m "feat: add h2 baseline schema"
```

### 41. `feat: add h2 reference data`

Dentists and the treatment catalogue.

```bash
git add dental-server/src/main/resources/db/migration/h2/V2__reference_data.sql
git commit -m "feat: add h2 reference data"
```

### 42. `feat: add h2 functions and views`

FN_INVOICE_TOTAL and FN_AGEING_BUCKET, plus the five reporting views.

```bash
git add dental-server/src/main/resources/db/migration/h2/V3__functions_and_views.sql
git commit -m "feat: add h2 functions and views"
```

### 43. `feat: add h2 audit triggers`

```bash
git add dental-server/src/main/resources/db/migration/h2/V4__triggers.sql
git commit -m "feat: add h2 audit triggers"
```

### 44. `feat: add h2 function bodies`

H2 aliases need the Java behind them.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/db/function/ClinicFunctions.java
git commit -m "feat: add h2 function bodies"
```

### 45. `feat: add h2 trigger classes`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/db/trigger/AppointmentAuditTrigger.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/db/trigger/InvoicePaymentAuditTrigger.java
git commit -m "feat: add h2 trigger classes"
```

### 46. `test: add stored function tests`

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/db/function/ClinicFunctionsTest.java
git commit -m "test: add stored function tests"
```

### 47. `feat: add mysql baseline schema and reference data`

Same model, second engine.

```bash
git add \
  dental-server/src/main/resources/db/migration/mysql/V1__baseline_schema.sql \
  dental-server/src/main/resources/db/migration/mysql/V2__reference_data.sql
git commit -m "feat: add mysql baseline schema and reference data"
```

### 48. `feat: add mysql functions, procedures and views`

```bash
git add dental-server/src/main/resources/db/migration/mysql/V3__functions_procedures_and_views.sql
git commit -m "feat: add mysql functions, procedures and views"
```

### 49. `feat: add mysql triggers that enforce working hours`

A booking outside the dentist's hours is refused even from a direct SQL insert.

```bash
git add dental-server/src/main/resources/db/migration/mysql/V4__triggers.sql
git commit -m "feat: add mysql triggers that enforce working hours"
```

---

## Day 7 - Repositories

### 50. `feat: add patient and appointment repositories`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/repository/PatientRepository.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/repository/AppointmentRepository.java
git commit -m "feat: add patient and appointment repositories"
```

### 51. `feat: add reference data repositories`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/repository/DentistRepository.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/repository/TreatmentRepository.java
git commit -m "feat: add reference data repositories"
```

### 52. `feat: add invoice repository`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/repository/InvoiceRepository.java
git commit -m "feat: add invoice repository"
```

### 53. `feat: add user repository`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/repository/UserRepository.java
git commit -m "feat: add user repository"
```

### 54. `feat: add log repositories`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/repository/AuditLogRepository.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/repository/NotificationLogRepository.java
git commit -m "feat: add log repositories"
```

### 55. `feat: add sequence repository with row lock`

SELECT ... FOR UPDATE, so two receptionists cannot be issued the same number.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/repository/NumberSequenceRepository.java
git commit -m "feat: add sequence repository with row lock"
```

### 56. `feat: add reporting dao`

Hand-written JDBC - reports aggregate thousands of rows and belong in SQL.

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/repository/dao/ReportingDao.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/repository/dao/JdbcReportingDao.java
git commit -m "feat: add reporting dao"
```

### 57. `feat: add entity to dto mappers`

No JPA entity ever crosses the network.

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/mapper/PatientMapper.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/mapper/DentistMapper.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/mapper/TreatmentMapper.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/mapper/AppointmentMapper.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/mapper/InvoiceMapper.java
git commit -m "feat: add entity to dto mappers"
```

---

## Day 8 - Billing rules (Strategy + Template Method + Factory)

### 58. `feat: add pricing context and result`

The input and output of a pricing rule.

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/pricing/PricingContext.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/pricing/PricingResult.java
git commit -m "feat: add pricing context and result"
```

### 59. `feat: add pricing strategy interface`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/pricing/PricingStrategy.java
git commit -m "feat: add pricing strategy interface"
```

### 60. `feat: add pricing template method`

calculate() is final - fees, surcharge, discount, cap, VAT, lines. A subclass can change how much but never the order.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/pricing/AbstractPricingStrategy.java
git commit -m "feat: add pricing template method"
```

### 61. `feat: add standard pricing rule`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/pricing/StandardPricingStrategy.java
git commit -m "feat: add standard pricing rule"
```

### 62. `feat: add surgical pricing rule`

Adds the theatre surcharge.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/pricing/SurgicalPricingStrategy.java
git commit -m "feat: add surgical pricing rule"
```

### 63. `feat: add cosmetic pricing rule`

No age concession on elective work.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/pricing/CosmeticPricingStrategy.java
git commit -m "feat: add cosmetic pricing rule"
```

### 64. `feat: add emergency pricing rule`

Out-of-hours loading.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/pricing/EmergencyPricingStrategy.java
git commit -m "feat: add emergency pricing rule"
```

### 65. `feat: add pricing strategy factory`

Registers from the Spring context, so adding a rule cannot mean forgetting to register it.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/pricing/PricingStrategyFactory.java
git commit -m "feat: add pricing strategy factory"
```

### 66. `test: add pricing rule tests`

Checked against hand-worked figures.

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/service/pricing/PricingStrategyTest.java
git commit -m "test: add pricing rule tests"
```

### 67. `test: add pricing factory tests`

Including the fallback when a treatment names an unknown rule.

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/service/pricing/PricingStrategyFactoryTest.java
git commit -m "test: add pricing factory tests"
```

---

## Day 9 - Booking rules (Chain of Responsibility)

### 68. `feat: add validation request and outcome`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/validation/BookingValidationRequest.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/validation/ValidationOutcome.java
git commit -m "feat: add validation request and outcome"
```

### 69. `feat: add validation handler interface`

Each handler carries its own order.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/validation/BookingValidationHandler.java
git commit -m "feat: add validation handler interface"
```

### 70. `feat: add booking window rule`

Not in the past, at most 90 days ahead. No database query.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/validation/BookingWindowHandler.java
git commit -m "feat: add booking window rule"
```

### 71. `feat: add clinic hours rule`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/validation/ClinicHoursHandler.java
git commit -m "feat: add clinic hours rule"
```

### 72. `feat: add slot alignment rule`

Must start on a 30-minute boundary.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/validation/SlotAlignmentHandler.java
git commit -m "feat: add slot alignment rule"
```

### 73. `feat: add dentist availability rule`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/validation/DentistAvailabilityHandler.java
git commit -m "feat: add dentist availability rule"
```

### 74. `feat: add double booking rules`

The two that cost a query, so they run last.

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/validation/DentistDoubleBookingHandler.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/validation/PatientDoubleBookingHandler.java
git commit -m "feat: add double booking rules"
```

### 75. `feat: assemble the validation chain`

Cheapest checks first; stops at the first failure.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/validation/BookingValidationChain.java
git commit -m "feat: assemble the validation chain"
```

### 76. `test: add validation chain tests`

Proves it short-circuits before touching the database.

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/service/validation/BookingValidationChainTest.java
git commit -m "test: add validation chain tests"
```

---

## Day 10 - Notifications (Observer + Adapter)

### 77. `feat: add gateway interface and message`

One clinic-shaped interface over transports with nothing in common.

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/gateway/MessageGateway.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/gateway/GatewayMessage.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/gateway/GatewayException.java
git commit -m "feat: add gateway interface and message"
```

### 78. `feat: add console email gateway`

Default, so the feature is demonstrable with no SMTP server.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/gateway/ConsoleEmailGateway.java
git commit -m "feat: add console email gateway"
```

### 79. `feat: add smtp email gateway`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/gateway/SmtpEmailGateway.java
git commit -m "feat: add smtp email gateway"
```

### 80. `feat: add sms gateway`

Normalises to E.164 and validates, but does not transmit.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/gateway/MockSmsGateway.java
git commit -m "feat: add sms gateway"
```

### 81. `test: add sms gateway tests`

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/service/notification/gateway/MockSmsGatewayTest.java
git commit -m "test: add sms gateway tests"
```

### 82. `feat: add appointment events`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/AppointmentEvent.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/AppointmentEventType.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/AppointmentObserver.java
git commit -m "feat: add appointment events"
```

### 83. `feat: add message composer`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/NotificationComposer.java
git commit -m "feat: add message composer"
```

### 84. `feat: add email and sms observers`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/EmailNotificationObserver.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/SmsNotificationObserver.java
git commit -m "feat: add email and sms observers"
```

### 85. `feat: add audit trail observer`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/AuditTrailObserver.java
git commit -m "feat: add audit trail observer"
```

### 86. `feat: add event publisher that isolates failures`

A dead SMS gateway must not lose the patient their appointment.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/notification/AppointmentEventPublisher.java
git commit -m "feat: add event publisher that isolates failures"
```

### 87. `test: add observer isolation tests`

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/service/notification/AppointmentEventPublisherTest.java
git commit -m "test: add observer isolation tests"
```

---

## Day 11 - Reports

### 88. `feat: add report request and template`

One fixed skeleton for all five reports.

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/report/ReportRequest.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/report/AbstractReportGenerator.java
git commit -m "feat: add report request and template"
```

### 89. `feat: add daily schedule report`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/report/DailyScheduleReportGenerator.java
git commit -m "feat: add daily schedule report"
```

### 90. `feat: add revenue report`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/report/RevenueReportGenerator.java
git commit -m "feat: add revenue report"
```

### 91. `feat: add outstanding invoices report`

Ageing bands come from the stored function, so the report and an ad-hoc query cannot disagree.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/report/OutstandingInvoiceReportGenerator.java
git commit -m "feat: add outstanding invoices report"
```

### 92. `feat: add dentist workload report`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/report/DentistWorkloadReportGenerator.java
git commit -m "feat: add dentist workload report"
```

### 93. `feat: add treatment popularity report`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/report/TreatmentPopularityReportGenerator.java
git commit -m "feat: add treatment popularity report"
```

### 94. `feat: add report generator factory`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/report/ReportGeneratorFactory.java
git commit -m "feat: add report generator factory"
```

---

## Day 12 - Services

### 95. `feat: add sequence allocator`

Its own transaction, so a clash surfaces where it can be caught.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/SequenceAllocator.java
git commit -m "feat: add sequence allocator"
```

### 96. `feat: add sequence generator service`

Issues APT- and INV- numbers under a row lock.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/SequenceGeneratorService.java
git commit -m "feat: add sequence generator service"
```

### 97. `test: add sequence generator tests`

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/service/SequenceGeneratorServiceTest.java
git commit -m "test: add sequence generator tests"
```

### 98. `feat: add patient service`

Matches an existing patient or registers exactly one - never a duplicate.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/PatientService.java
git commit -m "feat: add patient service"
```

### 99. `feat: add dentist and treatment services`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/DentistService.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/TreatmentService.java
git commit -m "feat: add dentist and treatment services"
```

### 100. `feat: add appointment service`

Validate, allocate a number, save, publish. The save catches a constraint violation and turns it into a sentence.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/AppointmentService.java
git commit -m "feat: add appointment service"
```

### 101. `test: add appointment service tests`

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/service/AppointmentServiceTest.java
git commit -m "test: add appointment service tests"
```

### 102. `feat: add billing service`

Cross-checks the Java total against the database function before issuing.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/BillingService.java
git commit -m "feat: add billing service"
```

### 103. `feat: add report service`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/ReportService.java
git commit -m "feat: add report service"
```

### 104. `feat: add help service`

Served from one place so both front ends always agree.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/HelpService.java
git commit -m "feat: add help service"
```

### 105. `feat: add receipt printer`

Rendered on the server, so browser and desktop print identically.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/util/ReceiptPrinter.java
git commit -m "feat: add receipt printer"
```

### 106. `feat: add clinic facade`

Collapses multi-call sequences into one round trip for the remote client.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/ClinicFacade.java
git commit -m "feat: add clinic facade"
```

### 107. `feat: add reminder scheduler`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/service/ReminderScheduler.java
git commit -m "feat: add reminder scheduler"
```

---

## Day 13 - Security

### 108. `feat: add user details for spring security`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/security/ClinicUserDetails.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/security/ClinicUserDetailsService.java
git commit -m "feat: add user details for spring security"
```

### 109. `feat: add login bookkeeping service`

Its own transaction so a clash between two simultaneous sign-ins can be caught.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/security/UserLoginBookkeepingService.java
git commit -m "feat: add login bookkeeping service"
```

### 110. `feat: add authentication audit listener`

Every attempt written to audit_log with the caller's IP.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/security/AuthenticationAuditListener.java
git commit -m "feat: add authentication audit listener"
```

### 111. `test: add audit listener tests`

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/security/AuthenticationAuditListenerTest.java
git commit -m "test: add audit listener tests"
```

### 112. `feat: add security config with two filter chains`

JSON 401 for the API, an HTML redirect for the browser. One chain would send login pages to the desktop client.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/config/SecurityConfig.java
git commit -m "feat: add security config with two filter chains"
```

### 113. `feat: add staff account bootstrap`

Creates the accounts on first start; never resets an existing one.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/bootstrap/StaffAccountInitializer.java
git commit -m "feat: add staff account bootstrap"
```

---

## Day 14 - REST web services

### 114. `feat: add rest exception handler`

One place that maps every failure to a code, and never leaks internals.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/api/RestExceptionHandler.java
git commit -m "feat: add rest exception handler"
```

### 115. `feat: add auth api`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/api/AuthApiController.java
git commit -m "feat: add auth api"
```

### 116. `feat: add appointment api`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/api/AppointmentApiController.java
git commit -m "feat: add appointment api"
```

### 117. `feat: add invoice api`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/api/InvoiceApiController.java
git commit -m "feat: add invoice api"
```

### 118. `feat: add reference data api`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/api/ReferenceDataApiController.java
git commit -m "feat: add reference data api"
```

### 119. `feat: add report api`

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/api/ReportApiController.java
git commit -m "feat: add report api"
```

### 120. `feat: add openapi config`

Publishes the contract at /swagger-ui.html.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/config/OpenApiConfig.java
git commit -m "feat: add openapi config"
```

---

## Day 15 - Web interface

### 121. `feat: add stylesheet`

Hand-written, no CDN, so the clinic works with no internet.

```bash
git add dental-server/src/main/resources/static/css/app.css
git commit -m "feat: add stylesheet"
```

### 122. `feat: add page layout fragments`

```bash
git add dental-server/src/main/resources/templates/fragments/layout.html
git commit -m "feat: add page layout fragments"
```

### 123. `feat: add web exception handler and error pages`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/WebExceptionHandler.java \
  dental-server/src/main/resources/templates/error/message.html \
  dental-server/src/main/resources/templates/access-denied.html
git commit -m "feat: add web exception handler and error pages"
```

### 124. `feat: add session and cookie state`

Recently viewed trail, and the table density preference.

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/session/RecentlyViewedTracker.java \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/session/UiPreferences.java
git commit -m "feat: add session and cookie state"
```

### 125. `feat: add global model advice`

Puts the current user on every page without each controller repeating it.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/GlobalModelAdvice.java
git commit -m "feat: add global model advice"
```

### 126. `feat: add login page`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/AuthWebController.java \
  dental-server/src/main/resources/templates/login.html
git commit -m "feat: add login page"
```

### 127. `feat: add dashboard`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/DashboardWebController.java \
  dental-server/src/main/resources/templates/dashboard.html
git commit -m "feat: add dashboard"
```

### 128. `feat: add appointment pages`

Book, list, view, day schedule and availability.

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/AppointmentWebController.java \
  dental-server/src/main/resources/templates/appointments/form.html \
  dental-server/src/main/resources/templates/appointments/list.html \
  dental-server/src/main/resources/templates/appointments/view.html \
  dental-server/src/main/resources/templates/appointments/day.html \
  dental-server/src/main/resources/templates/appointments/availability.html
git commit -m "feat: add appointment pages"
```

### 129. `feat: add patient pages`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/PatientWebController.java \
  dental-server/src/main/resources/templates/patients/form.html \
  dental-server/src/main/resources/templates/patients/list.html \
  dental-server/src/main/resources/templates/patients/view.html
git commit -m "feat: add patient pages"
```

### 130. `feat: add billing pages`

Calculate, then issue - the two steps are deliberately separate.

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/BillingWebController.java \
  dental-server/src/main/resources/templates/billing/calculate.html \
  dental-server/src/main/resources/templates/billing/list.html \
  dental-server/src/main/resources/templates/billing/view.html \
  dental-server/src/main/resources/templates/billing/receipt.html
git commit -m "feat: add billing pages"
```

### 131. `feat: add reference data pages`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/ReferenceWebController.java \
  dental-server/src/main/resources/templates/reference/dentists.html \
  dental-server/src/main/resources/templates/reference/dentist-form.html \
  dental-server/src/main/resources/templates/reference/treatments.html \
  dental-server/src/main/resources/templates/reference/treatment-form.html
git commit -m "feat: add reference data pages"
```

### 132. `feat: add report pages`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/ReportWebController.java \
  dental-server/src/main/resources/templates/reports/index.html \
  dental-server/src/main/resources/templates/reports/view.html
git commit -m "feat: add report pages"
```

### 133. `feat: add help pages`

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/HelpWebController.java \
  dental-server/src/main/resources/templates/help.html
git commit -m "feat: add help pages"
```

### 134. `feat: add admin pages`

Audit trail, notification log, users, and a diagnostics page generated from the running application.

```bash
git add \
  dental-server/src/main/java/lk/icbt/cis6003/dental/server/web/AdminWebController.java \
  dental-server/src/main/resources/templates/admin/audit.html \
  dental-server/src/main/resources/templates/admin/notifications.html \
  dental-server/src/main/resources/templates/admin/users.html \
  dental-server/src/main/resources/templates/admin/system.html
git commit -m "feat: add admin pages"
```

### 135. `feat: add demo data seeder`

Six weeks of history so the reports are not empty on first run.

```bash
git add dental-server/src/main/java/lk/icbt/cis6003/dental/server/bootstrap/DemoDataSeeder.java
git commit -m "feat: add demo data seeder"
```

---

## Day 16 - Desktop client

### 136. `feat: add client http layer`

HTTP and JSON only - no Spring, no JDBC, no server class on the classpath.

```bash
git add \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/api/ClinicApiClient.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/api/ApiException.java
git commit -m "feat: add client http layer"
```

### 137. `feat: add client session holder`

```bash
git add dental-client/src/main/java/lk/icbt/cis6003/dental/client/api/ClientSession.java
git commit -m "feat: add client session holder"
```

### 138. `feat: add ui helpers`

```bash
git add dental-client/src/main/java/lk/icbt/cis6003/dental/client/ui/UiUtils.java
git commit -m "feat: add ui helpers"
```

### 139. `feat: add login dialog`

```bash
git add dental-client/src/main/java/lk/icbt/cis6003/dental/client/ui/LoginDialog.java
git commit -m "feat: add login dialog"
```

### 140. `feat: add appointment windows`

```bash
git add \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/ui/RegisterAppointmentDialog.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/ui/SearchAppointmentWindow.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/ui/AppointmentDetailWindow.java
git commit -m "feat: add appointment windows"
```

### 141. `feat: add billing windows`

```bash
git add \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/ui/BillingWindow.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/ui/ReceiptWindow.java
git commit -m "feat: add billing windows"
```

### 142. `feat: add report and help windows`

```bash
git add \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/ui/ReportWindow.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/ui/HelpWindow.java
git commit -m "feat: add report and help windows"
```

### 143. `feat: add menu command base`

Each action is an object carrying its own label and role rule.

```bash
git add \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/MenuCommand.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/AbstractMenuCommand.java
git commit -m "feat: add menu command base"
```

### 144. `feat: add the menu commands and main window`

One commit because MainFrame builds itself from the command list and RefreshDashboardCommand calls back into MainFrame - splitting them would leave a commit that does not compile.

```bash
git add \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/RegisterAppointmentCommand.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/SearchAppointmentCommand.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/TodayScheduleCommand.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/GenerateBillCommand.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/FindBillCommand.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/ViewReportsCommand.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/ShowHelpCommand.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/RefreshDashboardCommand.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/AboutCommand.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/command/ExitCommand.java \
  dental-client/src/main/java/lk/icbt/cis6003/dental/client/ui/MainFrame.java
git commit -m "feat: add the menu commands and main window"
```

### 145. `feat: add client entry point`

Takes the server address as an argument.

```bash
git add dental-client/src/main/java/lk/icbt/cis6003/dental/client/DentalClientApplication.java
git commit -m "feat: add client entry point"
```

---

## Day 17 - Integration tests

### 146. `test: add integration test config`

In-memory H2 with the real Flyway schema.

```bash
git add dental-server/src/test/resources/application-test.yml
git commit -m "test: add integration test config"
```

### 147. `test: add security integration tests`

Roles, CSRF, lock-out, and JSON 401 rather than an HTML redirect.

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/integration/SecurityIT.java
git commit -m "test: add security integration tests"
```

### 148. `test: add appointment api tests`

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/integration/AppointmentApiIT.java
git commit -m "test: add appointment api tests"
```

### 149. `test: add billing api tests`

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/integration/BillingApiIT.java
git commit -m "test: add billing api tests"
```

### 150. `test: add database feature tests`

Writes straight through the repository to prove the constraint, not the service, is the guarantee.

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/integration/DatabaseFeaturesIT.java
git commit -m "test: add database feature tests"
```

### 151. `test: add report and web ui tests`

```bash
git add dental-server/src/test/java/lk/icbt/cis6003/dental/server/integration/ReportAndWebUiIT.java
git commit -m "test: add report and web ui tests"
```

---

## Day 18 - CI/CD

### 152. `ci: add build and test workflow`

JDK 17 and 21, then boots the packaged jar to prove it starts.

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add build and test workflow"
```

### 153. `ci: add release workflow`

Triggered by a v*.*.* tag, so a release always traces to one commit.

```bash
git add .github/workflows/release.yml
git commit -m "ci: add release workflow"
```

---

## Day 19 - Documentation

### 154. `docs: add readme`

```bash
git add README.md
git commit -m "docs: add readme"
```

### 155. `docs: add setup guide`

For running it on a machine that has never seen the project.

```bash
git add SETUP.md
git commit -m "docs: add setup guide"
```

### 156. `docs: add changelog`

```bash
git add CHANGELOG.md
git commit -m "docs: add changelog"
```

### 157. `docs: add version control notes`

```bash
git add docs/VERSION-CONTROL.md
git commit -m "docs: add version control notes"
```

---

## Day 20 - Diagrams

### 158. `docs: add diagram toolkit`

Shared hand-drawn style so all seven diagrams match.

```bash
git add docs/diagram_kit.py
git commit -m "docs: add diagram toolkit"
```

### 159. `docs: add use case diagram`

```bash
git add \
  docs/gen_usecase.py \
  docs/diagrams/fig01_use_case.png
git commit -m "docs: add use case diagram"
```

### 160. `docs: add class diagram`

```bash
git add \
  docs/gen_class.py \
  docs/diagrams/fig02_class.png
git commit -m "docs: add class diagram"
```

### 161. `docs: add sequence diagrams`

Sign in, register appointment, calculate bill.

```bash
git add \
  docs/gen_sequence.py \
  docs/diagrams/fig03_seq_signin.png \
  docs/diagrams/fig04_seq_appointment.png \
  docs/diagrams/fig05_seq_billing.png
git commit -m "docs: add sequence diagrams"
```

### 162. `docs: add architecture diagram`

```bash
git add \
  docs/gen_architecture.py \
  docs/diagrams/fig06_architecture.png
git commit -m "docs: add architecture diagram"
```

### 163. `docs: add er diagram`

```bash
git add \
  docs/gen_er.py \
  docs/diagrams/fig07_er_diagram.png
git commit -m "docs: add er diagram"
```

### 164. `docs: add editable drawio sources`

Same diagrams, editable in draw.io with the sketch style kept.

```bash
git add \
  docs/drawio_kit.py \
  docs/gen_drawio.py \
  docs/drawio/fig01_use_case.drawio \
  docs/drawio/fig02_class.drawio \
  docs/drawio/fig03_seq_signin.drawio \
  docs/drawio/fig04_seq_appointment.drawio \
  docs/drawio/fig05_seq_billing.drawio \
  docs/drawio/fig06_architecture.drawio \
  docs/drawio/fig07_er_diagram.drawio
git commit -m "docs: add editable drawio sources"
```

---

## Day 21 - Screenshots

### 165. `docs: add screenshots of the running system`

Captured against seeded data, not mock-ups.

```bash
git add \
  docs/screenshots/01_login.png \
  docs/screenshots/02_dashboard.png \
  docs/screenshots/03_appointments_list.png \
  docs/screenshots/04_appointment_new.png \
  docs/screenshots/05_appointment_view.png \
  docs/screenshots/06_availability.png \
  docs/screenshots/07_patients.png \
  docs/screenshots/08_billing_list.png \
  docs/screenshots/09_invoice_view.png \
  docs/screenshots/10_receipt.png \
  docs/screenshots/11_reports_index.png \
  docs/screenshots/12_report_revenue.png \
  docs/screenshots/13_report_outstanding.png \
  docs/screenshots/14_report_workload.png \
  docs/screenshots/15_daily_schedule.png \
  docs/screenshots/16_treatments.png \
  docs/screenshots/17_dentists.png \
  docs/screenshots/18_help.png \
  docs/screenshots/19_admin_audit.png \
  docs/screenshots/20_admin_notifications.png \
  docs/screenshots/21_admin_system.png \
  docs/screenshots/22_admin_users.png \
  docs/screenshots/23_swagger.png
git commit -m "docs: add screenshots of the running system"
```

### 166. `docs: crop screenshots for the report`

Full-page captures are unreadable when scaled to fit a page.

```bash
git add \
  docs/prep_screenshots.py \
  docs/screenshots/report/01_login.png \
  docs/screenshots/report/02_dashboard.png \
  docs/screenshots/report/04_appointment_new.png \
  docs/screenshots/report/05_appointment_view.png \
  docs/screenshots/report/09_invoice_view.png \
  docs/screenshots/report/10_receipt.png \
  docs/screenshots/report/12_report_revenue.png \
  docs/screenshots/report/19_admin_audit.png \
  docs/screenshots/report/20_admin_notifications.png \
  docs/screenshots/report/23_swagger.png
git commit -m "docs: crop screenshots for the report"
```

---

## Day 22 - The report

### 167. `docs: add report formatting toolkit`

A4, 1.5in left margin, Times New Roman, 1.5 spacing - enforced in one place.

```bash
git add docs/report_kit.py
git commit -m "docs: add report formatting toolkit"
```

### 168. `docs: add university logos`

```bash
git add \
  docs/assets/logo_icbt.png \
  docs/assets/logo_cardiff.png
git commit -m "docs: add university logos"
```

### 169. `docs: write task a - uml diagrams`

```bash
git add docs/report_content_a.py
git commit -m "docs: write task a - uml diagrams"
```

### 170. `docs: write task b - patterns and architecture`

```bash
git add docs/report_content_b.py
git commit -m "docs: write task b - patterns and architecture"
```

### 171. `docs: write tasks c and d - testing and git`

```bash
git add docs/report_content_cd.py
git commit -m "docs: write tasks c and d - testing and git"
```

### 172. `docs: assemble the report`

```bash
git add docs/build_report.py
git commit -m "docs: assemble the report"
```

### 173. `docs: add pdf export`

Goes through Word so the table of contents gets real page numbers.

```bash
git add docs/export_pdf.py
git commit -m "docs: add pdf export"
```

### 174. `docs: add format and word count checks`

Checks the exported PDF, not just the docx.

```bash
git add \
  docs/verify_report.py \
  docs/count_words.py \
  docs/trim_tables.py
git commit -m "docs: add format and word count checks"
```

### 175. `docs: add the built report`

```bash
git add \
  docs/CIS6003_WRIT1_Report.docx \
  docs/CIS6003_WRIT1_Report.pdf
git commit -m "docs: add the built report"
```

### 176. `docs: add the commit plan`

Generated from data so every file is provably covered once, and the ordering is checked rather than asserted.

```bash
git add \
  COMMIT_PLAN.md \
  docs/make_commit_plan.py \
  docs/check_commit_order.py
git commit -m "docs: add the commit plan"
```

---

## Tagging

Once everything is committed and `./mvnw clean verify` is green:

```bash
git tag -a v1.0.0 -m "First complete release"
git push origin main --tags
```

Pushing the tag triggers `release.yml`, which rebuilds from the tag and publishes a GitHub Release with both jars.
