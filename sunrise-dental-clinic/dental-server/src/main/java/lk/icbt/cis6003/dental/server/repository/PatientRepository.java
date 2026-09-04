package lk.icbt.cis6003.dental.server.repository;

import lk.icbt.cis6003.dental.server.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Data access for the patient master file. */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientCode(String patientCode);

    Optional<Patient> findByNicIgnoreCase(String nic);

    boolean existsByNicIgnoreCase(String nic);

    /**
     * Returning-patient lookup. Name plus phone number is the pragmatic
     * identity check a receptionist can actually perform at the desk - many
     * Sri Lankan patients do not carry their NIC to a dental appointment.
     */
    Optional<Patient> findFirstByFullNameIgnoreCaseAndContactNumber(String fullName, String contactNumber);

    /** Free-text search across the fields the front desk actually types. */
    @Query("""
           SELECT p FROM Patient p
           WHERE LOWER(p.fullName)     LIKE LOWER(CONCAT('%', :term, '%'))
              OR LOWER(p.patientCode)  LIKE LOWER(CONCAT('%', :term, '%'))
              OR p.contactNumber       LIKE CONCAT('%', :term, '%')
              OR LOWER(COALESCE(p.nic, '')) LIKE LOWER(CONCAT('%', :term, '%'))
           ORDER BY p.fullName ASC
           """)
    Page<Patient> search(@Param("term") String term, Pageable pageable);

    List<Patient> findAllByOrderByFullNameAsc();

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);
}
