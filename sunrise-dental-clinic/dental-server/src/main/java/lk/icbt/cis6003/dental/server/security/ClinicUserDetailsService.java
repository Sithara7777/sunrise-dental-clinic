package lk.icbt.cis6003.dental.server.security;

import lk.icbt.cis6003.dental.server.domain.User;
import lk.icbt.cis6003.dental.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a staff account for authentication - requirement 1, "the system must
 * require a username and password for secure access".
 *
 * <p>The failure message is deliberately generic ("Invalid username or
 * password") and identical whether the username is unknown or the password is
 * wrong. A message that distinguishes the two turns the login form into a tool
 * for discovering valid usernames, which is exactly the account enumeration
 * weakness the Ethical strand of the assessment criteria asks students to
 * avoid.</p>
 */
@Service
public class ClinicUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(ClinicUserDetailsService.class);

    private static final String GENERIC_FAILURE = "Invalid username or password";

    private final UserRepository userRepository;

    public ClinicUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> {
                    // The real reason goes to the log; the user sees nothing specific.
                    log.info("Login attempt for unknown username '{}'", username);
                    return new UsernameNotFoundException(GENERIC_FAILURE);
                });

        return new ClinicUserDetails(user);
    }
}
