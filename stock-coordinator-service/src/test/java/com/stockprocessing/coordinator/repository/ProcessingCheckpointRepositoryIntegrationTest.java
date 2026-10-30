
package com.stockprocessing.coordinator.repository;

import com.stockprocessing.coordinator.entity.ProcessingCheckpoint;
import com.stockprocessing.coordinator.entity.ProcessingJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ProcessingCheckpointRepositoryIntegrationTest {

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private ProcessingCheckpointRepository checkpointRepository;

    private Long createJob() {
        ProcessingJob job = new ProcessingJob(
                "checkpoint-test.csv",
                "/data/checkpoint-test.csv"
        );

        return jobRepository.saveAndFlush(job).getId();
    }

    private ProcessingCheckpoint createCheckpoint(Long jobId) {
        return new ProcessingCheckpoint(
                jobId,
                1L,
                "worker-1",
                100_000_000L,
                LocalDateTime.now().withNano(0)
        );
    }

    @Test
    void shouldSaveAndFindCheckpointByJobId() {
        Long jobId = createJob();

        ProcessingCheckpoint checkpoint =
                checkpointRepository.saveAndFlush(
                        createCheckpoint(jobId)
                );

        assertThat(checkpoint.getId()).isNotNull();

        ProcessingCheckpoint found = checkpointRepository
                .findByJobId(jobId)
                .orElseThrow();

        assertThat(found.getJobId()).isEqualTo(jobId);
        assertThat(found.getLastRecordNumber()).isZero();
        assertThat(found.getByteOffset()).isZero();
        assertThat(found.getCheckpointVersion()).isZero();
        assertThat(found.getLeaseVersion()).isEqualTo(1L);
    }

    @Test
    void shouldUpdateCheckpointProgress() {
        Long jobId = createJob();

        ProcessingCheckpoint checkpoint =
                checkpointRepository.saveAndFlush(
                        createCheckpoint(jobId)
                );

        checkpoint.setLastRecordNumber(10_000L);
        checkpoint.setByteOffset(5_000_000L);
        checkpoint.setRecordsPublished(10_000L);
        checkpoint.setCheckpointVersion(1L);

        checkpointRepository.saveAndFlush(checkpoint);

        ProcessingCheckpoint updated = checkpointRepository
                .findByJobId(jobId)
                .orElseThrow();

        assertThat(updated.getLastRecordNumber())
                .isEqualTo(10_000L);

        assertThat(updated.getByteOffset())
                .isEqualTo(5_000_000L);

        assertThat(updated.getRecordsPublished())
                .isEqualTo(10_000L);

        assertThat(updated.getCheckpointVersion())
                .isEqualTo(1L);
    }

    @Test
    void shouldFindCheckpointByLeaseVersion() {
        Long jobId = createJob();

        checkpointRepository.saveAndFlush(
                createCheckpoint(jobId)
        );

        var result = checkpointRepository
                .findByJobIdAndLeaseVersion(jobId, 1L);

        assertThat(result).isPresent();

        var wrongVersion = checkpointRepository
                .findByJobIdAndLeaseVersion(jobId, 2L);

        assertThat(wrongVersion).isEmpty();
    }

    @Test
    void shouldCheckCheckpointExistence() {
        Long jobId = createJob();

        assertThat(checkpointRepository.existsByJobId(jobId))
                .isFalse();

        checkpointRepository.saveAndFlush(
                createCheckpoint(jobId)
        );

        assertThat(checkpointRepository.existsByJobId(jobId))
                .isTrue();
    }

    @Test
    void shouldRejectOffsetGreaterThanFileSize() {
        Long jobId = createJob();

        ProcessingCheckpoint checkpoint =
                createCheckpoint(jobId);

        checkpoint.setByteOffset(200_000_000L);

        assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> checkpointRepository.saveAndFlush(checkpoint)
        );
    }
}
