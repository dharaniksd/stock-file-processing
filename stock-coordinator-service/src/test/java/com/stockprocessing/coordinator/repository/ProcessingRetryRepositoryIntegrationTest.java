
package com.stockprocessing.coordinator.repository;

import com.stockprocessing.coordinator.entity.ProcessingJob;
import com.stockprocessing.coordinator.entity.ProcessingRetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ProcessingRetryRepositoryIntegrationTest {

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private ProcessingRetryRepository retryRepository;

    private Long createJob() {
        ProcessingJob job = new ProcessingJob(
                "retry-test.csv",
                "/data/retry-test.csv"
        );

        return jobRepository.saveAndFlush(job).getId();
    }

    private ProcessingRetry createRetry(Long jobId) {
        return new ProcessingRetry(
                jobId,
                "kafka-batch-001",
                "KAFKA_PUBLISH"
        );
    }

    @Test
    void shouldSaveAndRetrieveRetry() {
        Long jobId = createJob();

        ProcessingRetry retry =
                retryRepository.saveAndFlush(createRetry(jobId));

        assertThat(retry.getId()).isNotNull();
        assertThat(retry.getRetryStatus()).isEqualTo("PENDING");
        assertThat(retry.getAttemptCount()).isZero();
        assertThat(retry.getMaxAttempts()).isEqualTo(5);

        ProcessingRetry found = retryRepository
                .findById(retry.getId())
                .orElseThrow();

        assertThat(found.getRetryType())
                .isEqualTo("KAFKA_PUBLISH");
    }

    @Test
    void shouldFindRetryByJobIdAndRetryKey() {
        Long jobId = createJob();

        retryRepository.saveAndFlush(createRetry(jobId));

        var found = retryRepository.findByJobIdAndRetryKey(
                jobId,
                "kafka-batch-001"
        );

        assertThat(found).isPresent();

        assertThat(
                retryRepository.existsByJobIdAndRetryKey(
                        jobId,
                        "kafka-batch-001"
                )
        ).isTrue();
    }

    @Test
    void shouldFindDueRetries() {
        Long jobId = createJob();

        LocalDateTime now = LocalDateTime.now();

        ProcessingRetry retry = createRetry(jobId);
        retry.setNextRetryAt(now.minusMinutes(5));

        retryRepository.saveAndFlush(retry);

        var dueRetries =
                retryRepository
                        .findByRetryStatusAndNextRetryAtLessThanEqual(
                                "PENDING",
                                now
                        );

        assertThat(dueRetries)
                .extracting(ProcessingRetry::getJobId)
                .contains(jobId);
    }

    @Test
    void shouldUpdateRetryAttemptCount() {
        Long jobId = createJob();

        ProcessingRetry retry =
                retryRepository.saveAndFlush(createRetry(jobId));

        retry.setAttemptCount(1);
        retry.setRetryStatus("EXHAUSTED");

        retryRepository.saveAndFlush(retry);

        ProcessingRetry updated = retryRepository
                .findById(retry.getId())
                .orElseThrow();

        assertThat(updated.getAttemptCount()).isEqualTo(1);
        assertThat(updated.getRetryStatus())
                .isEqualTo("EXHAUSTED");
    }

    @Test
    void shouldFindExpiredClaims() {
        Long jobId = createJob();

        LocalDateTime now = LocalDateTime.now();

        ProcessingRetry retry = createRetry(jobId);
        retry.setRetryStatus("IN_PROGRESS");
        retry.setAttemptCount(1);
        retry.setClaimedBy("coordinator-1");
        retry.setClaimToken(UUID.randomUUID().toString());
        retry.setClaimExpiresAt(now.minusMinutes(1));

        retryRepository.saveAndFlush(retry);

        var expiredClaims =
                retryRepository
                        .findByRetryStatusAndClaimExpiresAtBefore(
                                "IN_PROGRESS",
                                now
                        );

        assertThat(expiredClaims)
                .extracting(ProcessingRetry::getJobId)
                .contains(jobId);
    }

    @Test
    void shouldRejectInvalidAttemptCount() {
        Long jobId = createJob();

        ProcessingRetry retry = createRetry(jobId);
        retry.setAttemptCount(6);
        retry.setMaxAttempts(5);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> retryRepository.saveAndFlush(retry)
        );
    }
}
