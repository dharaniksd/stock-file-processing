
package com.stockprocessing.coordinator.repository;

import com.stockprocessing.coordinator.entity.ProcessingCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessingCheckpointRepository
        extends JpaRepository<ProcessingCheckpoint, Long> {

    Optional<ProcessingCheckpoint> findByJobId(Long jobId);

    boolean existsByJobId(Long jobId);

    Optional<ProcessingCheckpoint> findByJobIdAndLeaseVersion(
            Long jobId,
            Long leaseVersion
    );
}
