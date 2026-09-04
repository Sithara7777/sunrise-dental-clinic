package lk.icbt.cis6003.dental.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * An immutable record of who did what, and when.
 *
 * <p>Rows arrive from two independent sources, on purpose:</p>
 * <ol>
 *   <li>the application, via the audit observer on the notification pipeline
 *       and the login event listener; and</li>
 *   <li>the database itself, via the {@code trg_appointment_audit} trigger,
 *       which fires even for changes made outside the application.</li>
 * </ol>
 *
 * <p>It does not extend {@link BaseEntity} because an audit row is append-only:
 * it has no {@code updated_at} and no optimistic-lock version, and giving it
 * one would imply it could legitimately be edited.</p>
 */
@Entity
@Table(name = "audit_log",
       indexes = {
           @Index(name = "ix_audit_log_occurred", columnList = "occurred_at"),
           @Index(name = "ix_audit_log_username", columnList = "username"),
           @Index(name = "ix_audit_log_entity", columnList = "entity_type,entity_key")
       })
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "action", nullable = false, length = 60)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_key", length = 50)
    private String entityKey;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "APPLICATION";

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    public AuditLog() {
        // required by JPA
    }

    public AuditLog(String username, String action, String entityType, String entityKey, String details) {
        this.username = username;
        this.action = action;
        this.entityType = entityType;
        this.entityKey = entityKey;
        this.details = details;
        this.occurredAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityKey() {
        return entityKey;
    }

    public void setEntityKey(String entityKey) {
        this.entityKey = entityKey;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    @Override
    public String toString() {
        return occurredAt + " " + username + " " + action + " " + entityType + ":" + entityKey;
    }
}
