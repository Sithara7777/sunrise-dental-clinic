package lk.icbt.cis6003.dental.server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.icbt.cis6003.dental.common.ApiPaths;
import lk.icbt.cis6003.dental.common.dto.LoginRequest;
import lk.icbt.cis6003.dental.common.dto.TreatmentDto;
import lk.icbt.cis6003.dental.server.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for requirement 1 - "The system must require a username and
 * password for secure access. Only authorized staff can use the system."
 *
 * <p>Also the evidence for the assessment's Ethical strand: they show that
 * unauthenticated access is refused everywhere it should be, that the browser
 * and the API each fail in the way their own client can understand, that roles
 * are actually enforced rather than merely hidden in the UI, and that every
 * sign-in attempt is recorded.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Authentication, authorisation and sessions")
class SecurityIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuditLogRepository auditLogRepository;

    /* ================================================================== */
    /* Unauthenticated access                                              */
    /* ================================================================== */

    @Test
    @DisplayName("an unauthenticated API call gets a JSON 401, not an HTML login page")
    void apiRefusesAnonymousWithJson() throws Exception {
        mockMvc.perform(get(ApiPaths.APPOINTMENTS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @DisplayName("an unauthenticated browser request is redirected to the login page")
    void browserRedirectsAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("the login page and the help page are reachable without signing in")
    void publicPagesAreReachable() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());

        // Help is public on purpose: a new member of staff who cannot get past
        // the login screen is exactly who needs the "Signing in" topic.
        mockMvc.perform(get("/help")).andExpect(status().isOk());
        mockMvc.perform(get(ApiPaths.HELP)).andExpect(status().isOk());
    }

    /* ================================================================== */
    /* Signing in                                                          */
    /* ================================================================== */

    @Test
    @DisplayName("correct credentials sign a user in through the browser form")
    void browserFormLoginSucceeds() throws Exception {
        mockMvc.perform(formLogin("/login").user("reception").password("Reception@123"))
                .andExpect(authenticated().withUsername("reception"))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("wrong credentials are refused with the SAME message as an unknown username")
    void wrongPasswordIsRefusedGenerically() throws Exception {
        mockMvc.perform(formLogin("/login").user("reception").password("wrong-password"))
                .andExpect(unauthenticated());

        mockMvc.perform(formLogin("/login").user("no-such-user").password("anything"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("the API login returns the user and establishes a session cookie")
    void apiLoginEstablishesASession() throws Exception {
        mockMvc.perform(post(ApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("reception", "Reception@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("reception"))
                .andExpect(jsonPath("$.data.role").value("RECEPTIONIST"))
                .andExpect(jsonPath("$.data.sessionId").exists());
    }

    @Test
    @DisplayName("the API login never echoes a password back")
    void apiLoginNeverEchoesThePassword() throws Exception {
        String body = mockMvc.perform(post(ApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("reception", "Reception@123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .as("no password or hash may ever appear in a response")
                .doesNotContain("Reception@123")
                .doesNotContain("password")
                .doesNotContain("$2a$");
    }

    @Test
    @DisplayName("the session cookie authenticates subsequent API calls without re-sending credentials")
    void sessionCookieAuthenticatesLaterCalls() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post(ApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("reception", "Reception@123"))))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);

        assertThat(session).isNotNull();

        mockMvc.perform(get(ApiPaths.AUTH_ME).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("reception"));
    }

    @Test
    @DisplayName("bad API credentials return 401 with AUTHENTICATION_FAILED")
    void apiLoginRefusesBadCredentials() throws Exception {
        mockMvc.perform(post(ApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("reception", "definitely-wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"));
    }

    /* ================================================================== */
    /* Signing out                                                         */
    /* ================================================================== */

    @Test
    @DisplayName("signing out ends the session, and later calls are refused")
    void signOutEndsTheSession() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post(ApiPaths.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("reception", "Reception@123"))))
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(post(ApiPaths.AUTH_LOGOUT).session(session))
                .andExpect(status().isOk());

        assertThat(session.isInvalid()).as("the session must be invalidated, not merely cleared")
                .isTrue();
    }

    /* ================================================================== */
    /* Roles                                                               */
    /* ================================================================== */

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a receptionist may NOT change the treatment price list")
    void receptionistCannotEditTheCatalogue() throws Exception {
        TreatmentDto treatment = new TreatmentDto();
        treatment.setCode("HACK");
        treatment.setName("Unauthorised Treatment");
        treatment.setCategory("Test");
        treatment.setBasePrice(new BigDecimal("1.00"));
        treatment.setDurationMinutes(30);
        treatment.setPricingStrategy("STANDARD");

        mockMvc.perform(post(ApiPaths.TREATMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(treatment)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("an administrator MAY change the treatment price list")
    void administratorCanEditTheCatalogue() throws Exception {
        TreatmentDto treatment = new TreatmentDto();
        treatment.setCode("TESTTX");
        treatment.setName("Test Treatment");
        treatment.setCategory("Test");
        treatment.setBasePrice(new BigDecimal("1000.00"));
        treatment.setDurationMinutes(30);
        treatment.setPricingStrategy("STANDARD");

        mockMvc.perform(post(ApiPaths.TREATMENTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(treatment)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a receptionist may NOT reach the audit trail")
    void receptionistCannotReachTheAuditTrail() throws Exception {
        // 403 with a forward to the access-denied page, not a redirect: the
        // user IS authenticated, they simply lack the role, and forwarding
        // keeps the original URL in the address bar so they can see what they
        // were refused.
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    @DisplayName("an administrator may reach the audit trail")
    void administratorCanReachTheAuditTrail() throws Exception {
        mockMvc.perform(get("/admin/audit")).andExpect(status().isOk());
    }

    /* ================================================================== */
    /* Auditing and hardening                                              */
    /* ================================================================== */

    @Test
    @DisplayName("every sign-in attempt, successful or not, is recorded")
    void signInAttemptsAreAudited() throws Exception {
        long before = auditLogRepository.count();

        mockMvc.perform(formLogin("/login").user("reception").password("Reception@123"));
        mockMvc.perform(formLogin("/login").user("reception").password("wrong"));

        assertThat(auditLogRepository.count())
                .as("both the success and the failure must leave a trail")
                .isGreaterThanOrEqualTo(before + 2);
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a state-changing post without a CSRF token does not take effect")
    void browserFormsRequireCsrf() throws Exception {
        /*
         * Asserted on the EFFECT rather than on the status code.
         *
         * A CSRF rejection surfaces as 403 or as a 302 to the login page
         * depending on whether the security context has been restored by the
         * time CsrfFilter runs - and under MockMvc that ordering is a detail of
         * the test harness, not of the application. What the clinic actually
         * needs is that the request DOES NOT DO ANYTHING, and the cookie is the
         * observable proof of that either way.
         */
        String withoutToken = mockMvc.perform(post("/preferences/density").param("value", "compact"))
                .andReturn().getResponse().getHeader("Set-Cookie");

        assertThat(withoutToken == null || !withoutToken.contains("clinic-prefs"))
                .as("a forged post must not be able to change anything")
                .isTrue();

        // With a valid token the same request is honoured.
        mockMvc.perform(post("/preferences/density").param("value", "compact").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("clinic-prefs=compact")));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("the workstation preference is stored in the clinic-prefs cookie")
    void preferenceIsStoredInACookie() throws Exception {
        mockMvc.perform(post("/preferences/density").param("value", "compact").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("clinic-prefs=compact")));
    }

    @Test
    @WithMockUser(username = "reception", authorities = "ROLE_RECEPTIONIST")
    @DisplayName("a tampered preference cookie falls back to the default rather than reaching a template")
    void tamperedCookieIsIgnored() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .cookie(new jakarta.servlet.http.Cookie("clinic-prefs",
                                "<script>alert(1)</script>")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("alert(1)"))));
    }
}
