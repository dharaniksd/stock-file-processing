
package com.stockprocessing.coordinator.repository;

import com.stockprocessing.coordinator.entity.ProcessingRetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProcessingRetryRepository
        extends JpaRepository<ProcessingRetry, Long> {

    Optional<ProcessingRetry> findByJobIdAndRetryKey(
            Long jobId,
            String retryKey
    );

    boolean existsByJobIdAndRetryKey(
            Long jobId,
            String retryKey
    );

    List<ProcessingRetry> findByJobId(Long jobId);

    List<ProcessingRetry> findByRetryStatus(String retryStatus);

    List<ProcessingRetry>
    findByRetryStatusAndNextRetryAtLessThanEqual(
            String retryStatus,
            LocalDateTime nextRetryAt
    );

    List<ProcessingRetry>
    findByRetryStatusAndClaimExpiresAtBefore(
            String retryStatus,
            LocalDateTime claimExpiresAt
    );

    Optional<ProcessingRetry> findByIdAndClaimToken(
            Long id,
            String claimToken
    );
}
