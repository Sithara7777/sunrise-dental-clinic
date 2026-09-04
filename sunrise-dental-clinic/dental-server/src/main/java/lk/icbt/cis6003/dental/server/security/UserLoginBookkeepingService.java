package lk.icbt.cis6003.dental.server.security;

import lk.icbt.cis6003.dental.server.domain.User;
import lk.icbt.cis6003.dental.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Updates a staff account's login bookkeeping - the failed-attempt counter,
 * the lock-out timestamp, the last-signed-in timestamp - in its own short
 * transaction.
 *
 * <p><b>Why this is a separate service rather than inline code in
 * {@link AuthenticationAuditListener}.</b> {@code User} carries a
 * {@code @Version} column, and the front desk is exactly the place where the
 * same account authenticates from more than one place at once - two browser
 * tabs, a Basic-auth client that re-authenticates on every call, a receptionist
 * and the desktop client both signed in as {@code reception}. When two such
 * requests race, the SECOND to commit hits
 * {@link org.springframework.dao.OptimisticLockingFailureException} - and that
 * failure surfaces at the COMMIT of the enclosing transaction, which happens
 * inside Spring's transactional proxy, after the calling method's body has
 * already run. A try/catch written inside the same method that calls
 * {@code save()} would not be in the right place to catch it.</p>
 *
 * <p>Putting this update in its own {@code REQUIRES_NEW} transaction, in its
 * own Spring-proxied bean, makes that commit happen synchronously as part of
 * calling this method - so the caller's try/catch around the call genuinely
 * does catch a conflict, rather than missing it because the failure occurred
 * after the caller's own method had returned.</p>
 */
@Service
public class UserLoginBookkeepingService {

    private final UserRepository userRepository;

    public UserLoginBookkeepingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Clears the failed-attempt counter and stamps the sign-in time.
     *
     * @return the updated user, or empty if the username does not exist
     * @throws org.springframework.dao.OptimisticLockingFailureException if
     *         another request updated the same account in the same instant;
     *         the caller decides how to degrade
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<User> recordSuccessfulLogin(String username) {
        Optional<User> user = userRepository.findByUsernameIgnoreCase(username);
        user.ifPresent(u -> {
            u.registerSuccessfulLogin();
            userRepository.save(u);
        });
        return user;
    }

    /**
     * Increments the failed-attempt counter, locking the account once the
     * threshold set on {@link User} is reached.
     *
     * @return the updated user, or empty if the username does not exist
     * @throws org.springframework.dao.OptimisticLockingFailureException if
     *         another request updated the same account in the same instant
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<User> recordFailedLogin(String username) {
        Optional<User> user = userRepository.findByUsernameIgnoreCase(username);
        user.ifPresent(u -> {
            u.registerFailedLogin();
            userRepository.save(u);
        });
        return user;
    }
}
