
package com.stockprocessing.coordinator.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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

    protected ProcessingJob() {
        // Required by JPA
    }

    public ProcessingJob(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Long getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(Long processedRecords) {
        this.processedRecords = processedRecords;
    }

    public Long getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(Long failedRecords) {
        this.failedRecords = failedRecords;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
