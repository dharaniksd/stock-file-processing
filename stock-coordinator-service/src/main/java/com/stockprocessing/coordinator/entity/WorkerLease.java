
package com.stockprocessing.coordinator.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "worker_leases")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkerLease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true)
    private Long jobId;

    @Column(name = "worker_id", nullable = false, length = 128)
    private String workerId;

    @Column(name = "lease_status", nullable = false, length = 20)
    private String leaseStatus = "ACTIVE";

    @Column(name = "lease_version", nullable = false)
    private Long leaseVersion = 1L;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Column(name = "heartbeat_at", nullable = false)
    private LocalDateTime heartbeatAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

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

    public WorkerLease(
            Long jobId,
            String workerId,
            LocalDateTime acquiredAt,
            LocalDateTime expiresAt
    ) {
        this.jobId = jobId;
        this.workerId = workerId;
        this.acquiredAt = acquiredAt;
        this.heartbeatAt = acquiredAt;
        this.expiresAt = expiresAt;
    }
}
