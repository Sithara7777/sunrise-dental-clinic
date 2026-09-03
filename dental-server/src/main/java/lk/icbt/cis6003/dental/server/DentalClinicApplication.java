package lk.icbt.cis6003.dental.server;

import lk.icbt.cis6003.dental.common.ClinicConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Sunrise Dental Clinic server.
 *
 * <p>This single process hosts all three tiers of the architecture:</p>
 * <ul>
 *   <li><b>Presentation</b> - Thymeleaf controllers for the browser and REST
 *       controllers publishing the web services the desktop client consumes;</li>
 *   <li><b>Business</b> - services, pricing strategies, the booking validation
 *       chain, the notification observers and the report generators;</li>
 *   <li><b>Data</b> - JPA entities, Spring Data repositories, a JDBC reporting
 *       DAO and a Flyway-managed schema.</li>
 * </ul>
 *
 * <p>Deploying the tiers in one process is a deployment decision, not an
 * architectural one: the boundaries between them are package and interface
 * boundaries, so the data tier could move to its own service without the
 * presentation tier changing. The desktop client is already a separate
 * process, which is what makes the system distributed today.</p>
 */
@SpringBootApplication
@EnableScheduling
public class DentalClinicApplication {

    private static final Logger log = LoggerFactory.getLogger(DentalClinicApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DentalClinicApplication.class, args);
    }

    /**
     * Prints the URLs a marker or a new developer needs, so nobody has to read
     * the configuration to find the login page.
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> startupBanner(Environment environment) {
        return event -> {
            String port = environment.getProperty("server.port", "8080");
            String context = environment.getProperty("server.servlet.context-path", "");
            String base = "http://localhost:" + port + context;
            String profiles = String.join(", ", environment.getActiveProfiles());

            log.info("""

                    ==========================================================================
                     {} Management System is running
                     CIS6003 Advanced Programming - WRIT1
                    --------------------------------------------------------------------------
                     Web application  : {}/login
                     REST web service : {}/api/v1
                     API documentation: {}/swagger-ui.html
                     H2 database view : {}/h2-console
                     Health check     : {}/actuator/health
                     Active profile(s): {}
                    --------------------------------------------------------------------------
                     Start the desktop client with:
                       java -jar dental-client/target/sunrise-dental-client.jar {}
                    ==========================================================================
                    """,
                    ClinicConstants.CLINIC_NAME, base, base, base, base, base,
                    profiles.isBlank() ? "default (H2 file database)" : profiles, base);
        };
    }
}
