package lk.icbt.cis6003.dental.server.repository;

import lk.icbt.cis6003.dental.common.enums.PaymentStatus;
import lk.icbt.cis6003.dental.server.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Data access for issued bills. */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @EntityGraph(attributePaths = {"lines", "appointment"})
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @EntityGraph(attributePaths = {"lines", "appointment"})
    Optional<Invoice> findByAppointmentAppointmentNumber(String appointmentNumber);

    boolean existsByAppointmentAppointmentNumber(String appointmentNumber);

    @EntityGraph(attributePaths = {"appointment"})
    @Query("""
           SELECT i FROM Invoice i
           WHERE (:term IS NULL OR :term = ''
                  OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :term, '%'))
                  OR LOWER(i.patientName)   LIKE LOWER(CONCAT('%', :term, '%')))
             AND (:status   IS NULL OR i.paymentStatus = :status)
             AND (:fromDate IS NULL OR i.issuedDate >= :fromDate)
             AND (:toDate   IS NULL OR i.issuedDate <= :toDate)
           """)
    Page<Invoice> search(@Param("term") String term,
                         @Param("status") PaymentStatus status,
                         @Param("fromDate") LocalDate fromDate,
                         @Param("toDate") LocalDate toDate,
                         Pageable pageable);

    List<Invoice> findByIssuedDateOrderByInvoiceNumberAsc(LocalDate issuedDate);

    List<Invoice> findByIssuedDateBetweenOrderByIssuedDateAscInvoiceNumberAsc(LocalDate from, LocalDate to);

    @Query("""
           SELECT i FROM Invoice i
           WHERE i.paymentStatus IN :statuses
           ORDER BY i.issuedDate ASC
           """)
    List<Invoice> findOutstanding(@Param("statuses") Collection<PaymentStatus> statuses);

    @Query("""
           SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i
           WHERE i.issuedDate = :date AND i.paymentStatus <> lk.icbt.cis6003.dental.common.enums.PaymentStatus.CANCELLED
           """)
    BigDecimal sumTotalForDate(@Param("date") LocalDate date);

    @Query("""
           SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i
           WHERE i.issuedDate BETWEEN :from AND :to
             AND i.paymentStatus <> lk.icbt.cis6003.dental.common.enums.PaymentStatus.CANCELLED
           """)
    BigDecimal sumTotalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
           SELECT COALESCE(SUM(i.totalAmount - i.amountPaid), 0) FROM Invoice i
           WHERE i.paymentStatus IN :statuses
           """)
    BigDecimal sumOutstanding(@Param("statuses") Collection<PaymentStatus> statuses);

    long countByPaymentStatusIn(Collection<PaymentStatus> statuses);
}
