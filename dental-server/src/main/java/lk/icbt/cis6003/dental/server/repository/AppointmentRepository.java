package lk.icbt.cis6003.dental.server.repository;

import lk.icbt.cis6003.dental.common.enums.AppointmentStatus;
import lk.icbt.cis6003.dental.server.domain.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Data access for appointments.
 *
 * <p>{@code @EntityGraph} appears on the read methods deliberately. An
 * appointment's patient, dentist and treatment are LAZY, and every screen that
 * lists appointments displays all three; without the graph, listing 50
 * appointments would issue 151 queries. Naming the graph on the query is the
 * targeted fix - it keeps the associations lazy everywhere else.</p>
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"patient", "dentist", "treatment"})
    Optional<Appointment> findByAppointmentNumber(String appointmentNumber);

    boolean existsByAppointmentNumber(String appointmentNumber);

    /* ------------------------------------------------------------------ */
    /* Double-booking checks                                               */
    /* ------------------------------------------------------------------ */

    /**
     * The pre-flight availability check. The unique index on
     * {@code (dentist_id, slot_lock)} is the real guarantee; this query exists
     * so the receptionist gets a clear message instead of a constraint
     * violation.
     */
    @Query("""
           SELECT COUNT(a) > 0 FROM Appointment a
           WHERE a.dentist.dentistCode = :dentistCode
             AND a.appointmentDate     = :date
             AND a.appointmentTime     = :time
             AND a.status IN :occupyingStatuses
             AND (:excludeId IS NULL OR a.id <> :excludeId)
           """)
    boolean isSlotTaken(@Param("dentistCode") String dentistCode,
                        @Param("date") LocalDate date,
                        @Param("time") java.time.LocalTime time,
                        @Param("occupyingStatuses") Collection<AppointmentStatus> occupyingStatuses,
                        @Param("excludeId") Long excludeId);

    /** Everything already in a dentist's diary for one day. */
    @Query("""
           SELECT a FROM Appointment a
           WHERE a.dentist.dentistCode = :dentistCode
             AND a.appointmentDate     = :date
             AND a.status IN :occupyingStatuses
           ORDER BY a.appointmentTime ASC
           """)
    List<Appointment> findDiary(@Param("dentistCode") String dentistCode,
                                @Param("date") LocalDate date,
                                @Param("occupyingStatuses") Collection<AppointmentStatus> occupyingStatuses);

    /** Guards against booking the same patient with two dentists at one time. */
    @Query("""
           SELECT COUNT(a) > 0 FROM Appointment a
           WHERE a.patient.patientCode = :patientCode
             AND a.appointmentDate     = :date
             AND a.appointmentTime     = :time
             AND a.status IN :occupyingStatuses
           """)
    boolean patientAlreadyBooked(@Param("patientCode") String patientCode,
                                 @Param("date") LocalDate date,
                                 @Param("time") java.time.LocalTime time,
                                 @Param("occupyingStatuses") Collection<AppointmentStatus> occupyingStatuses);

    /* ------------------------------------------------------------------ */
    /* Listing and searching                                               */
    /* ------------------------------------------------------------------ */

    @EntityGraph(attributePaths = {"patient", "dentist", "treatment"})
    List<Appointment> findByAppointmentDateOrderByAppointmentTimeAsc(LocalDate date);

    @EntityGraph(attributePaths = {"patient", "dentist", "treatment"})
    @Query("""
           SELECT a FROM Appointment a
           WHERE (:term IS NULL OR :term = ''
                  OR LOWER(a.appointmentNumber)  LIKE LOWER(CONCAT('%', :term, '%'))
                  OR LOWER(a.patient.fullName)   LIKE LOWER(CONCAT('%', :term, '%'))
                  OR a.patient.contactNumber     LIKE CONCAT('%', :term, '%'))
             AND (:status   IS NULL OR a.status = :status)
             AND (:dentist  IS NULL OR :dentist = '' OR a.dentist.dentistCode = :dentist)
             AND (:fromDate IS NULL OR a.appointmentDate >= :fromDate)
             AND (:toDate   IS NULL OR a.appointmentDate <= :toDate)
           """)
    Page<Appointment> search(@Param("term") String term,
                             @Param("status") AppointmentStatus status,
                             @Param("dentist") String dentistCode,
                             @Param("fromDate") LocalDate fromDate,
                             @Param("toDate") LocalDate toDate,
                             Pageable pageable);

    @EntityGraph(attributePaths = {"patient", "dentist", "treatment"})
    List<Appointment> findByPatientPatientCodeOrderByAppointmentDateDescAppointmentTimeDesc(String patientCode);

    @EntityGraph(attributePaths = {"patient", "dentist", "treatment"})
    @Query("""
           SELECT a FROM Appointment a
           WHERE a.appointmentDate BETWEEN :from AND :to
           ORDER BY a.appointmentDate ASC, a.appointmentTime ASC
           """)
    List<Appointment> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Feeds the automatic reminder job. */
    @EntityGraph(attributePaths = {"patient", "dentist", "treatment"})
    @Query("""
           SELECT a FROM Appointment a
           WHERE a.appointmentDate = :date
             AND a.status IN :statuses
           ORDER BY a.appointmentTime ASC
           """)
    List<Appointment> findForReminder(@Param("date") LocalDate date,
                                      @Param("statuses") Collection<AppointmentStatus> statuses);

    /* ------------------------------------------------------------------ */
    /* Counters used by the dashboard                                      */
    /* ------------------------------------------------------------------ */

    long countByAppointmentDate(LocalDate date);

    long countByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status);

    long countByAppointmentDateBetween(LocalDate from, LocalDate to);

    long countByStatus(AppointmentStatus status);

    long countByPatientPatientCode(String patientCode);

    @Query("""
           SELECT COALESCE(SUM(a.durationMinutes), 0) FROM Appointment a
           WHERE a.appointmentDate = :date AND a.status IN :statuses
           """)
    long sumBookedMinutes(@Param("date") LocalDate date,
                          @Param("statuses") Collection<AppointmentStatus> statuses);
}
