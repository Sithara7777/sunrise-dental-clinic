package lk.icbt.cis6003.dental.server.repository;

import lk.icbt.cis6003.dental.server.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Data access for the treatment catalogue. */
@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    Optional<Treatment> findByCode(String code);

    boolean existsByCode(String code);

    List<Treatment> findByActiveTrueOrderByNameAsc();

    List<Treatment> findAllByOrderByCategoryAscNameAsc();

    List<Treatment> findByCategoryAndActiveTrueOrderByNameAsc(String category);

    long countByActiveTrue();
}
