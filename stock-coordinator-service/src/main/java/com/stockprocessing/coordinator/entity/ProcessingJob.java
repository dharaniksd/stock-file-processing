
package com.stockprocessing.coordinator.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "processing_jobs")
public class ProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "REGISTERED";

    @Column(name = "total_records", nullable = false)
    private Long totalRecords = 0L;

    @Column(name = "processed_records", nullable = false)
    private Long processedRecords = 0L;

    @Column(name = "failed_records", nullable = false)
    private Long failedRecords = 0L;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public ProcessingJob(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }
}
