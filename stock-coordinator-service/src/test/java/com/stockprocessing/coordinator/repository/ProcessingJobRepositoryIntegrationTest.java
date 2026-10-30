
package com.stockprocessing.coordinator.repository;

import com.stockprocessing.coordinator.entity.ProcessingJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProcessingJobRepositoryIntegrationTest {

    @Autowired
    private ProcessingJobRepository repository;

    @Test
    void shouldSaveAndRetrieveJob() {

        ProcessingJob job = new ProcessingJob(
                "test-stock.csv",
                "/data/test-stock.csv"
        );

        ProcessingJob saved = repository.saveAndFlush(job);

        assertThat(saved.getId()).isNotNull();

        ProcessingJob found = repository.findById(saved.getId())
                .orElseThrow();

        assertThat(found.getFileName())
                .isEqualTo("test-stock.csv");

        assertThat(found.getFilePath())
                .isEqualTo("/data/test-stock.csv");

        assertThat(found.getStatus())
                .isEqualTo("REGISTERED");

        assertThat(found.getTotalRecords())
                .isZero();
    }

    @Test
    void shouldUpdateJobStatus() {

        ProcessingJob job = repository.saveAndFlush(
                new ProcessingJob(
                        "update-test.csv",
                        "/data/update-test.csv"
                )
        );

        job.setStatus("PROCESSING");

        repository.saveAndFlush(job);

        ProcessingJob updated = repository.findById(job.getId())
                .orElseThrow();

        assertThat(updated.getStatus())
                .isEqualTo("PROCESSING");
    }

    @Test
    void shouldFindJobsByStatus() {

        repository.saveAndFlush(
                new ProcessingJob(
                        "status-test.csv",
                        "/data/status-test.csv"
                )
        );

        var jobs = repository.findByStatus("REGISTERED");

        assertThat(jobs)
                .extracting(ProcessingJob::getFileName)
                .contains("status-test.csv");
    }
}
