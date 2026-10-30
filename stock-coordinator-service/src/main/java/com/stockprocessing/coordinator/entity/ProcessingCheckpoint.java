
package com.stockprocessing.coordinator.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "processing_checkpoints")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessingCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true)
    private Long jobId;

    @Column(name = "last_record_number", nullable = false)
    private Long lastRecordNumber = 0L;

    @Column(name = "byte_offset", nullable = false)
    private Long byteOffset = 0L;

    @Column(name = "records_published", nullable = false)
    private Long recordsPublished = 0L;

    @Column(name = "checkpoint_version", nullable = false)
    private Long checkpointVersion = 0L;

    @Column(name = "lease_version", nullable = false)
    private Long leaseVersion;

    @Column(name = "last_worker_id", nullable = false, length = 128)
    private String lastWorkerId;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "file_last_modified_at", nullable = false)
    private LocalDateTime fileLastModifiedAt;

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

    public ProcessingCheckpoint(
            Long jobId,
            Long leaseVersion,
            String lastWorkerId,
            Long fileSizeBytes,
            LocalDateTime fileLastModifiedAt
    ) {
        this.jobId = jobId;
        this.leaseVersion = leaseVersion;
        this.lastWorkerId = lastWorkerId;
        this.fileSizeBytes = fileSizeBytes;
        this.fileLastModifiedAt = fileLastModifiedAt;
    }
}
