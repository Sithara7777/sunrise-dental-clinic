package lk.icbt.cis6003.dental.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lk.icbt.cis6003.dental.common.ClinicConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Publishes the web-service contract as OpenAPI 3, browsable at
 * {@code /swagger-ui.html}.
 *
 * <p>Task B requires a distributed application with web services. A service
 * nobody can discover is not much of a service: this makes the contract
 * self-describing, lets the endpoints be exercised from a browser without
 * writing any client code, and gives the assessment a concrete artefact
 * demonstrating what "web services" means here rather than only asserting
 * it.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI clinicOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title(ClinicConstants.CLINIC_NAME + " - Web Service API")
                        .version("v1")
                        .description("""
                                REST web services for the Sunrise Dental Clinic appointment and
                                patient management system.

                                These endpoints are the contract between the server and the
                                menu-driven desktop client, which runs as a separate process and
                                reaches the system only over HTTP.

                                **Signing in.** POST your credentials to `/api/v1/auth/login`; the
                                response sets a `JSESSIONID` cookie that authenticates every
                                subsequent call. HTTP Basic authentication is also accepted, which
                                is what the *Authorize* button below uses.

                                **Response shape.** Every endpoint returns the same envelope:
                                `{ success, message, data, errorCode, fieldErrors, timestamp }`.

                                Module: CIS6003 Advanced Programming - WRIT1.
                                """)
                        .contact(new Contact()
                                .name(ClinicConstants.CLINIC_NAME)
                                .email(ClinicConstants.CLINIC_EMAIL))
                        .license(new License()
                                .name("Academic use - ICBT Campus / Cardiff Metropolitan University")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development server")))
                .components(new Components()
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Staff username and password"))
                        .addSecuritySchemes("sessionCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Session cookie issued by /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
