
package com.stockprocessing.coordinator.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "processing_retries")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessingRetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "retry_key", nullable = false, length = 255)
    private String retryKey;

    @Column(name = "retry_type", nullable = false, length = 30)
    private String retryType;

    @Column(name = "retry_status", nullable = false, length = 20)
    private String retryStatus = "PENDING";

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 5;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "claimed_by", length = 128)
    private String claimedBy;

    @Column(
            name = "claim_token",
            columnDefinition = "char(36) character set ascii collate ascii_bin"
    )
    private String claimToken;

    @Column(name = "claim_expires_at")
    private LocalDateTime claimExpiresAt;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    @Setter(AccessLevel.NONE)
    private LocalDateTime updatedAt;

    public ProcessingRetry(
            Long jobId,
            String retryKey,
            String retryType
    ) {
        this.jobId = jobId;
        this.retryKey = retryKey;
        this.retryType = retryType;
    }
}
