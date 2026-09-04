package lk.icbt.cis6003.dental.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.icbt.cis6003.dental.common.ApiPaths;
import lk.icbt.cis6003.dental.common.dto.ApiResponse;
import lk.icbt.cis6003.dental.common.enums.Role;
import lk.icbt.cis6003.dental.server.security.ClinicUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Authentication, authorisation and session policy - requirement 1 of the
 * scenario, "only authorized staff can use the system".
 *
 * <p><b>Two filter chains, because there are two front doors.</b> The browser
 * needs an HTML login form and a redirect; the desktop client needs a JSON
 * 401 it can act on. Serving both from one chain would mean the Swing client
 * receiving an HTML login page as the "response" to a failed API call. The
 * {@code @Order(1)} chain therefore matches {@code /api/**} and speaks JSON;
 * the {@code @Order(2)} chain handles everything else and speaks HTML.</p>
 *
 * <p><b>Sessions and cookies.</b> Both chains are session-backed. After the
 * desktop client posts once to {@code /api/v1/auth/login} it holds a
 * {@code JSESSIONID} cookie and is authenticated for the rest of its run,
 * exactly as the browser is - so the distributed client and the web UI share
 * one session model rather than two. The browser additionally gets an opt-in
 * "remember this device" cookie. HTTP Basic remains enabled on the API chain
 * so the endpoints stay testable with {@code curl} and Swagger UI.</p>
 *
 * <p><b>CSRF.</b> Enabled for the browser chain, where a session cookie is sent
 * automatically by the browser and cross-site forgery is therefore possible.
 * Disabled for {@code /api/**}, whose clients are not browsers and do not
 * auto-attach credentials to third-party form posts.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize on service methods
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * BCrypt, with the default strength of 10.
     *
     * <p>BCrypt is deliberately slow and salts every hash, so two staff members
     * who choose the same password still store different values and a stolen
     * database cannot be attacked with a precomputed rainbow table. Storing
     * MD5, SHA-256 or - worse - plain text would satisfy the scenario's wording
     * and fail its intent.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Where the programmatic API login stores its authenticated context. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /*
     * No DaoAuthenticationProvider bean is declared here on purpose.
     *
     * Spring Security builds one automatically from the ClinicUserDetailsService
     * and PasswordEncoder beans above, and its default
     * hideUserNotFoundExceptions=true is exactly the behaviour this system
     * wants: a caller is never told whether it was the username or the password
     * that was wrong, so the login form cannot be used to discover valid
     * usernames. Declaring the provider explicitly would replace the whole
     * auto-configuration and, in Spring Security 6.5, also disables the
     * UserDetailsService wiring - a footgun for no benefit.
     */

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    /* ------------------------------------------------------------------ */
    /* Chain 1 - the REST web services consumed by the desktop client      */
    /* ------------------------------------------------------------------ */

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(ApiPaths.API_ROOT + "/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(ApiPaths.AUTH_LOGIN).permitAll()
                    .requestMatchers(ApiPaths.HELP + "/**").permitAll()
                    // Maintaining the catalogue and the roster is an admin job
                    .requestMatchers(org.springframework.http.HttpMethod.POST,
                                     ApiPaths.TREATMENTS, ApiPaths.DENTISTS).hasAuthority(Role.ADMIN.getAuthority())
                    .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                     ApiPaths.TREATMENTS + "/**", ApiPaths.DENTISTS + "/**")
                        .hasAuthority(Role.ADMIN.getAuthority())
                    .anyRequest().authenticated())
            // Basic auth kept alongside the session, so Swagger UI and curl work
            .httpBasic(basic -> basic.realmName("Sunrise Dental Clinic API"))
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(this::writeUnauthorised)
                    .accessDeniedHandler((request, response, denied) -> writeForbidden(response)));

        return http.build();
    }

    /* ------------------------------------------------------------------ */
    /* Chain 2 - the Thymeleaf web application used in the browser          */
    /* ------------------------------------------------------------------ */

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http,
                                              ClinicUserDetailsService userDetailsService,
                                              ClinicProperties properties) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/login", "/error", "/access-denied").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                    .requestMatchers("/help", "/help/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    // Reference data maintenance and the audit trail are admin only
                    .requestMatchers("/admin/**", "/treatments/new", "/treatments/*/edit",
                                     "/dentists/new", "/dentists/*/edit")
                        .hasAuthority(Role.ADMIN.getAuthority())
                    // Money is handled by the front desk and by management
                    .requestMatchers("/billing/**", "/invoices/**")
                        .hasAnyAuthority(Role.ADMIN.getAuthority(), Role.RECEPTIONIST.getAuthority())
                    .anyRequest().authenticated())

            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/dashboard", true)
                    .failureUrl("/login?error")
                    .permitAll())

            // POST-only logout (the Spring Security default). A GET logout can
            // be triggered by any image tag on a hostile page, which would let
            // a third party sign clinic staff out mid-booking.
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID", "clinic-remember-me")
                    .permitAll())

            // "Remember this device" - an explicit, opt-in persistent cookie
            .rememberMe(remember -> remember
                    .key("sunrise-dental-clinic-remember-me-key")
                    .rememberMeParameter("remember-me")
                    .rememberMeCookieName("clinic-remember-me")
                    .tokenValiditySeconds(properties.getSecurity().getRememberMeDays() * 24 * 60 * 60)
                    .userDetailsService(userDetailsService))

            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .invalidSessionUrl("/login?expired")
                    // One concurrent session per member of staff: shared front-desk
                    // machines otherwise make the audit trail ambiguous.
                    .maximumSessions(1)
                    .expiredUrl("/login?expired"))

            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))

            // H2's web console renders inside a frame; allow same-origin framing
            // so the development console works without weakening protection for
            // any other page.
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /* ------------------------------------------------------------------ */
    /* JSON error responses for the API chain                              */
    /* ------------------------------------------------------------------ */

    private void writeUnauthorised(jakarta.servlet.http.HttpServletRequest request,
                                   jakarta.servlet.http.HttpServletResponse response,
                                   org.springframework.security.core.AuthenticationException ex)
            throws java.io.IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.fail("Authentication is required. Please sign in.", "AUTHENTICATION_FAILED"));
    }

    private void writeForbidden(jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.fail("Your role does not permit this action.", "ACCESS_DENIED"));
    }
}
