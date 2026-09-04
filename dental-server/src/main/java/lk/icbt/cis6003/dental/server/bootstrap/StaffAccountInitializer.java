package lk.icbt.cis6003.dental.server.bootstrap;

import lk.icbt.cis6003.dental.common.enums.Role;
import lk.icbt.cis6003.dental.server.config.ClinicProperties;
import lk.icbt.cis6003.dental.server.domain.User;
import lk.icbt.cis6003.dental.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the initial staff accounts the first time the application starts.
 *
 * <p><b>Why not seed them in SQL?</b> Task D requires the Git repository to be
 * <em>public</em>. A migration script containing BCrypt hashes would publish
 * the clinic's credentials to the internet permanently, and rewriting Git
 * history to remove them afterwards is notoriously unreliable. Hashing at
 * run time from configurable properties keeps secrets out of version control
 * entirely.</p>
 *
 * <p>The runner is idempotent - it creates only accounts that are missing - so
 * restarting the application never resets a password an administrator has
 * changed.</p>
 */
@Component
@Order(1)
public class StaffAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaffAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClinicProperties properties;

    public StaffAccountInitializer(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   ClinicProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ClinicProperties.Security security = properties.getSecurity();

        boolean created = false;
        created |= createIfMissing("admin", security.getDefaultAdminPassword(),
                "Samanthi Rajapaksa", "admin@sunrisedental.lk", Role.ADMIN, null);
        created |= createIfMissing("reception", security.getDefaultReceptionPassword(),
                "Chamari Gunasekara", "reception@sunrisedental.lk", Role.RECEPTIONIST, null);
        created |= createIfMissing("reception2", security.getDefaultReceptionPassword(),
                "Ishara Bandara", "reception2@sunrisedental.lk", Role.RECEPTIONIST, null);
        created |= createIfMissing("nperera", security.getDefaultDentistPassword(),
                "Nimal Perera", "nimal.perera@sunrisedental.lk", Role.DENTIST, "DEN-001");

        if (created) {
            log.warn("""

                    ******************************************************************
                     Initial staff accounts have been created with their DEFAULT
                     passwords. Change them before this system is used with real
                     patient data.

                       admin      / {}   (Administrator)
                       reception  / {}   (Receptionist)
                       reception2 / {}   (Receptionist)
                       nperera    / {}   (Dentist - Dr Nimal Perera)
                    ******************************************************************
                    """,
                    security.getDefaultAdminPassword(),
                    security.getDefaultReceptionPassword(),
                    security.getDefaultReceptionPassword(),
                    security.getDefaultDentistPassword());
        }
    }

    /**
     * @return {@code true} if the account did not exist and was created
     */
    private boolean createIfMissing(String username, String rawPassword, String fullName,
                                    String email, Role role, String linkedDentistCode) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return false;
        }

        User user = new User(username, passwordEncoder.encode(rawPassword), fullName, role);
        user.setEmail(email);
        user.setLinkedDentistCode(linkedDentistCode);
        user.setActive(true);
        userRepository.save(user);

        log.info("Created staff account '{}' with role {}", username, role);
        return true;
    }
}
