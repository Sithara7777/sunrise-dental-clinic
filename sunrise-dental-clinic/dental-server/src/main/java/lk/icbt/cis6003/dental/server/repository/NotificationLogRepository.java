package lk.icbt.cis6003.dental.server.repository;

import lk.icbt.cis6003.dental.common.enums.NotificationStatus;
import lk.icbt.cis6003.dental.server.domain.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Read/append access to the notification delivery log. */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<NotificationLog> findByReferenceKeyOrderByCreatedAtDesc(String referenceKey);

    List<NotificationLog> findTop20ByOrderByCreatedAtDesc();

    long countByStatus(NotificationStatus status);
}
