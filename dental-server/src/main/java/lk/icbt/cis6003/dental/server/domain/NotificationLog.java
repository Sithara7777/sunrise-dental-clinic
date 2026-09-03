package lk.icbt.cis6003.dental.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lk.icbt.cis6003.dental.common.enums.NotificationChannel;
import lk.icbt.cis6003.dental.common.enums.NotificationStatus;

import java.time.LocalDateTime;

/**
 * Evidence of every appointment alert the system attempted to send.
 *
 * <p>Persisting failures as well as successes matters: "we sent you a
 * reminder" is only defensible if there is a row proving it, and a run of
 * {@code FAILED} rows is how the clinic learns its SMS credit has run out.</p>
 */
@Entity
@Table(name = "notification_log",
       indexes = {
           @Index(name = "ix_notification_log_sent", columnList = "created_at"),
           @Index(name = "ix_notification_log_reference", columnList = "reference_key")
       })
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "recipient", nullable = false, length = 150)
    private String recipient;

    @Column(name = "subject", length = 200)
    private String subject;

    @Column(name = "body", length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.SENT;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Appointment or invoice number this alert relates to. */
    @Column(name = "reference_key", length = 30)
    private String referenceKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public NotificationLog() {
        // required by JPA
    }

    public NotificationLog(NotificationChannel channel, String recipient, String subject,
                           String body, String referenceKey) {
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.referenceKey = referenceKey;
        this.createdAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
    }

    public void markSuppressed(String reason) {
        this.status = NotificationStatus.SUPPRESSED;
        this.failureReason = reason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getReferenceKey() {
        return referenceKey;
    }

    public void setReferenceKey(String referenceKey) {
        this.referenceKey = referenceKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return channel + " -> " + recipient + " [" + status + "]";
    }
}
