package lk.icbt.cis6003.dental.server.repository;

import lk.icbt.cis6003.dental.server.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Read/append access to the audit trail. Rows are never updated or deleted. */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);

    List<AuditLog> findTop20ByOrderByOccurredAtDesc();

    List<AuditLog> findByEntityTypeAndEntityKeyOrderByOccurredAtDesc(String entityType, String entityKey);

    @Query("""
           SELECT a FROM AuditLog a
           WHERE (:username IS NULL OR :username = '' OR LOWER(a.username) = LOWER(:username))
             AND (:action   IS NULL OR :action   = '' OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%')))
           ORDER BY a.occurredAt DESC
           """)
    Page<AuditLog> search(@Param("username") String username,
                          @Param("action") String action,
                          Pageable pageable);
}
