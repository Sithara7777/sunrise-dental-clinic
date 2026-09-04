package lk.icbt.cis6003.dental.server.repository;

import lk.icbt.cis6003.dental.common.enums.Role;
import lk.icbt.cis6003.dental.server.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for staff accounts.
 *
 * <p>Spring Data supplies the implementation of the Repository pattern at
 * runtime; declaring the interface is what keeps the business tier free of any
 * JDBC, {@code EntityManager} or SQL. Swapping H2 for MySQL therefore changes a
 * configuration property and nothing else in this file.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<User> findByRoleAndActiveTrue(Role role);

    List<User> findAllByOrderByFullNameAsc();

    long countByActiveTrue();
}
