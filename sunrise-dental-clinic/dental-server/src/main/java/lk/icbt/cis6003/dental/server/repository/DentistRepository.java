package lk.icbt.cis6003.dental.server.repository;

import lk.icbt.cis6003.dental.server.domain.Dentist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Data access for the clinic's dentists. */
@Repository
public interface DentistRepository extends JpaRepository<Dentist, Long> {

    Optional<Dentist> findByDentistCode(String dentistCode);

    boolean existsByDentistCode(String dentistCode);

    List<Dentist> findByActiveTrueOrderByFullNameAsc();

    List<Dentist> findAllByOrderByFullNameAsc();

    long countByActiveTrue();
}
