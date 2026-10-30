
package com.stockprocessing.coordinator.repository;

import com.stockprocessing.coordinator.entity.WorkerLease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkerLeaseRepository
        extends JpaRepository<WorkerLease, Long> {

    Optional<WorkerLease> findByJobId(Long jobId);

    boolean existsByJobId(Long jobId);

    List<WorkerLease> findByWorkerId(String workerId);

    List<WorkerLease> findByLeaseStatus(String leaseStatus);

    List<WorkerLease> findByLeaseStatusAndExpiresAtBefore(
            String leaseStatus,
            LocalDateTime expiresAt
    );
}
