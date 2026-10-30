
package com.stockprocessing.coordinator.repository;

import com.stockprocessing.coordinator.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessingJobRepository
        extends JpaRepository<ProcessingJob, Long> {

    List<ProcessingJob> findByStatus(String status);

    boolean existsByFilePath(String filePath);

    List<ProcessingJob> findByStatusOrderByCreatedAtAsc(String status);
}
