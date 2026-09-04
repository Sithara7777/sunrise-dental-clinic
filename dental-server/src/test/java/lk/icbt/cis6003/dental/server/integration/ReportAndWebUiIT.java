package lk.icbt.cis6003.dental.server.integration;

import lk.icbt.cis6003.dental.common.ApiPaths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Integration tests for the reports (Task B, "a suitable set of reports"), the
 * Help section (requirement 5), and every page of the web user interface.
 *
 * <p>The report tests are parameterised over the report codes rather than
 * written five times. That is deliberate: the same generic envelope serves
 * every report, so the same test should serve every report - and a sixth report
 * would be covered by adding one string.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Reports, help and the web user interface")
class ReportAndWebUiIT {

    @Autowired private MockMvc mockMvc;

    /* ================================================================== */
    /* Reports                                                             */
    /* ================================================================== */

    @ParameterizedTest(name = "{0} returns a well-formed report envelope")
    @ValueSource(strings = {
        "DAILY_SCHEDULE", "REVENUE", "DENTIST_WORKLOAD",
        "TREATMENT_POPULARITY", "OUTSTANDING_INVOICES"
    })
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("every report returns headings, a matching cell grid and summary figures")
    void everyReportIsWellFormed(String reportCode) throws Exception {
        mockMvc.perform(get(ApiPaths.REPORTS + "/" + reportCode)
                        .param("fromDate", LocalDate.now().minusDays(30).toString())
                        .param("toDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportCode").value(reportCode))
                .andExpect(jsonPath("$.data.title").isNotEmpty())
                .andExpect(jsonPath("$.data.description").isNotEmpty())
                .andExpect(jsonPath("$.data.columnHeaders").isArray())
                .andExpect(jsonPath("$.data.cells").isArray())
                .andExpect(jsonPath("$.data.summary").exists())
                .andExpect(jsonPath("$.data.generatedAt").exists());
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("the report list advertises all five reports")
    void reportListAdvertisesEveryReport() throws Exception {
        mockMvc.perform(get(ApiPaths.REPORTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5));
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("an unknown report code returns 404 and lists what IS available")
    void unknownReportCodeIsRejected() throws Exception {
        mockMvc.perform(get(ApiPaths.REPORTS + "/NO_SUCH_REPORT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Available reports")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("a reversed date range is refused with a helpful message")
    void reversedDateRangeIsRefused() throws Exception {
        mockMvc.perform(get(ApiPaths.REPORTS + "/REVENUE")
                        .param("fromDate", LocalDate.now().toString())
                        .param("toDate", LocalDate.now().minusDays(10).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("is after")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("a range longer than a year is refused rather than allowed to time out")
    void excessiveDateRangeIsRefused() throws Exception {
        mockMvc.perform(get(ApiPaths.REPORTS + "/REVENUE")
                        .param("fromDate", LocalDate.now().minusYears(3).toString())
                        .param("toDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("at most")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("the dashboard returns every operational figure it advertises")
    void dashboardReturnsItsFigures() throws Exception {
        mockMvc.perform(get(ApiPaths.REPORT_DASHBOARD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayAppointments").exists())
                .andExpect(jsonPath("$.data.totalPatients").exists())
                .andExpect(jsonPath("$.data.outstandingBalance").exists())
                .andExpect(jsonPath("$.data.noShowRate").exists())
                .andExpect(jsonPath("$.data.chairUtilisationToday").exists())
                .andExpect(jsonPath("$.data.weeklyTrend.length()").value(7));
    }

    /* ================================================================== */
    /* Requirement 5 - Help                                                */
    /* ================================================================== */

    @Test
    @DisplayName("the help service returns numbered steps for every topic")
    void helpTopicsHaveSteps() throws Exception {
        mockMvc.perform(get(ApiPaths.HELP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data[0].steps").isArray())
                .andExpect(jsonPath("$.data[0].steps[0]").isNotEmpty());
    }

    @Test
    @DisplayName("help covers all six functions the scenario asks for")
    void helpCoversEveryScenarioFunction() throws Exception {
        String body = mockMvc.perform(get(ApiPaths.HELP))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .contains("login")                  // 1 authentication
                .contains("register-appointment")   // 2 register
                .contains("find-appointment")       // 3 display
                .contains("billing")                // 4 bill
                .contains("reports")                // management reports
                .contains("exit");                  // 6 exit
    }

    @Test
    @DisplayName("an unknown help topic returns 404")
    void unknownHelpTopicIsRejected() throws Exception {
        mockMvc.perform(get(ApiPaths.HELP + "/no-such-topic"))
                .andExpect(status().isNotFound());
    }

    /* ================================================================== */
    /* The web user interface                                              */
    /* ================================================================== */

    @ParameterizedTest(name = "{0} renders")
    @ValueSource(strings = {
        "/dashboard", "/appointments", "/appointments/new", "/appointments/day",
        "/patients", "/patients/new", "/invoices", "/reports",
        "/dentists", "/treatments", "/help",
        "/admin/audit", "/admin/notifications", "/admin/users", "/admin/system"
    })
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("every page of the web application renders without error")
    void everyPageRenders(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @ParameterizedTest(name = "{0} renders")
    @ValueSource(strings = {
        "DAILY_SCHEDULE", "REVENUE", "DENTIST_WORKLOAD",
        "TREATMENT_POPULARITY", "OUTSTANDING_INVOICES"
    })
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("one generic template renders every report page")
    void everyReportPageRenders(String reportCode) throws Exception {
        mockMvc.perform(get("/reports/" + reportCode))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/view"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("the dashboard shows the clinic name and today's figures")
    void dashboardPageShowsFigures() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sunrise Dental Clinic")))
                .andExpect(content().string(containsString("Appointments today")))
                .andExpect(content().string(containsString("Outstanding")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("the booking form offers no field for the appointment number")
    void bookingFormHasNoAppointmentNumberField() throws Exception {
        String html = mockMvc.perform(get("/appointments/new"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Typing the number by hand is exactly how the paper system produced
        // duplicates, so the field must not exist to be typed into.
        org.assertj.core.api.Assertions.assertThat(html)
                .doesNotContain("name=\"appointmentNumber\"")
                .contains("issued automatically");
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("the diagnostics page reports the patterns actually registered at run time")
    void diagnosticsPageReportsTheLiveRegistries() throws Exception {
        mockMvc.perform(get("/admin/system"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("STANDARD")))
                .andExpect(content().string(containsString("SURGICAL")))
                .andExpect(content().string(containsString("BOOKING_WINDOW")))
                .andExpect(content().string(containsString("DENTIST_DOUBLE_BOOKING")))
                .andExpect(content().string(containsString("EmailNotificationObserver")))
                .andExpect(content().string(containsString("DAILY_SCHEDULE")));
    }

    @Test
    @DisplayName("the OpenAPI contract is published for the web services")
    void openApiContractIsPublished() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['" + ApiPaths.APPOINTMENTS + "']").exists())
                .andExpect(jsonPath("$.paths['" + ApiPaths.INVOICES + "']").exists());
    }
}
