
package com.stockprocessing.coordinator.repository;

import com.stockprocessing.coordinator.entity.ProcessingJob;
import com.stockprocessing.coordinator.entity.WorkerLease;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class WorkerLeaseRepositoryIntegrationTest {

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private WorkerLeaseRepository leaseRepository;

    private Long createJob() {
        ProcessingJob job = new ProcessingJob(
                "lease-test.csv",
                "/data/lease-test.csv"
        );

        return jobRepository.saveAndFlush(job).getId();
    }

    @Test
    void shouldSaveAndFindLeaseByJobId() {
        Long jobId = createJob();

        LocalDateTime now = LocalDateTime.now();

        WorkerLease lease = new WorkerLease(
                jobId,
                "worker-1",
                now,
                now.plusMinutes(5)
        );

        leaseRepository.saveAndFlush(lease);

        WorkerLease found = leaseRepository
                .findByJobId(jobId)
                .orElseThrow();

        assertThat(found.getWorkerId())
                .isEqualTo("worker-1");

        assertThat(found.getLeaseStatus())
                .isEqualTo("ACTIVE");

        assertThat(found.getLeaseVersion())
                .isEqualTo(1L);
    }

    @Test
    void shouldFindExpiredLeases() {
        Long jobId = createJob();

        LocalDateTime now = LocalDateTime.now();

        WorkerLease lease = new WorkerLease(
                jobId,
                "worker-2",
                now.minusMinutes(10),
                now.minusMinutes(5)
        );

        leaseRepository.saveAndFlush(lease);

        var expiredLeases =
                leaseRepository.findByLeaseStatusAndExpiresAtBefore(
                        "ACTIVE",
                        now
                );

        assertThat(expiredLeases)
                .extracting(WorkerLease::getJobId)
                .contains(jobId);
    }

    @Test
    void shouldUpdateLeaseHeartbeat() {
        Long jobId = createJob();

        LocalDateTime now = LocalDateTime.now();

        WorkerLease lease = leaseRepository.saveAndFlush(
                new WorkerLease(
                        jobId,
                        "worker-3",
                        now,
                        now.plusMinutes(5)
                )
        );

        LocalDateTime newHeartbeat = now.plusMinutes(1);

        lease.setHeartbeatAt(newHeartbeat);
        lease.setExpiresAt(newHeartbeat.plusMinutes(5));

        leaseRepository.saveAndFlush(lease);

        WorkerLease updated = leaseRepository
                .findByJobId(jobId)
                .orElseThrow();

        assertThat(updated.getHeartbeatAt())
                .isEqualTo(newHeartbeat);

        assertThat(updated.getExpiresAt())
                .isEqualTo(newHeartbeat.plusMinutes(5));
    }

    @Test
    void shouldCheckLeaseExistenceByJobId() {
        Long jobId = createJob();

        LocalDateTime now = LocalDateTime.now();

        leaseRepository.saveAndFlush(
                new WorkerLease(
                        jobId,
                        "worker-4",
                        now,
                        now.plusMinutes(5)
                )
        );

        assertThat(leaseRepository.existsByJobId(jobId))
                .isTrue();
    }
}
