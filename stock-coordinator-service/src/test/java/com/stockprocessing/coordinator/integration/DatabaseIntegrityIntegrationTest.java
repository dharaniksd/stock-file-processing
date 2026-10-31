
package com.stockprocessing.coordinator.integration;

import com.stockprocessing.coordinator.entity.*;
import com.stockprocessing.coordinator.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class DatabaseIntegrityIntegrationTest {

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private WorkerLeaseRepository leaseRepository;

    @Autowired
    private ProcessingCheckpointRepository checkpointRepository;

    @Autowired
    private ProcessingRetryRepository retryRepository;

    @Autowired
    private StockRecordRepository stockRepository;

    @Autowired
    private ProcessingEventRepository eventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long createJob() {
        return transactionTemplate.execute(status -> {
            ProcessingJob job = new ProcessingJob(
                    "integration-" + System.nanoTime() + ".csv",
                    "/data/integration-" + System.nanoTime() + ".csv"
            );
            return jobRepository.saveAndFlush(job).getId();
        });
    }

    private StockRecord stock(Long jobId, long number) {
        return new StockRecord(
                jobId,
                number,
                "AAPL",
                100L,
                new BigDecimal("150.2500")
        );
    }

    private void deleteJobAndChildren(Long jobId) {
        transactionTemplate.executeWithoutResult(status -> {
            eventRepository.deleteAll(eventRepository
                    .findByJobId(jobId,
                            org.springframework.data.domain.Pageable.unpaged())
                    .getContent());

            stockRepository.deleteAll(stockRepository
                    .findByJobId(jobId,
                            org.springframework.data.domain.Pageable.unpaged())
                    .getContent());

            retryRepository.deleteAll(
                    retryRepository.findByJobId(jobId)
            );

            checkpointRepository.findByJobId(jobId)
                    .ifPresent(checkpointRepository::delete);

            leaseRepository.findByJobId(jobId)
                    .ifPresent(leaseRepository::delete);

            jobRepository.deleteById(jobId);
        });
    }

    @Test
    void shouldPersistCompleteJobLifecycle() {
        Long jobId = createJob();

        try {
            transactionTemplate.executeWithoutResult(status -> {
                LocalDateTime now = LocalDateTime.now();

                leaseRepository.saveAndFlush(new WorkerLease(
                        jobId,
                        "worker-1",
                        now,
                        now.plusMinutes(5)
                ));

                checkpointRepository.saveAndFlush(
                        new ProcessingCheckpoint(
                                jobId,
                                1L,
                                "worker-1",
                                100000L,
                                now
                        )
                );

                retryRepository.saveAndFlush(
                        new ProcessingRetry(
                                jobId,
                                "kafka-batch-1",
                                "KAFKA_PUBLISH"
                        )
                );

                stockRepository.saveAndFlush(stock(jobId, 1L));

                eventRepository.saveAndFlush(
                        new ProcessingEvent(
                                jobId,
                                "PROCESSING_STARTED",
                                "INFO",
                                "Job processing started"
                        )
                );
            });

            assertThat(leaseRepository.findByJobId(jobId))
                    .isPresent();

            assertThat(checkpointRepository.findByJobId(jobId))
                    .isPresent();

            assertThat(retryRepository.findByJobId(jobId))
                    .hasSize(1);

            assertThat(stockRepository.countByJobId(jobId))
                    .isEqualTo(1);

            assertThat(eventRepository.countByJobId(jobId))
                    .isEqualTo(1);
        } finally {
            deleteJobAndChildren(jobId);
        }
    }

    @Test
    void shouldRejectInvalidForeignKey() {
        Long invalidJobId = Long.MAX_VALUE;

        assertThrows(
                DataIntegrityViolationException.class,
                () -> transactionTemplate.executeWithoutResult(status ->
                        stockRepository.saveAndFlush(
                                stock(invalidJobId, 1L)
                        )
                )
        );
    }

    @Test
    void shouldRejectDuplicateWorkerLease() {
        Long jobId = createJob();

        try {
            LocalDateTime now = LocalDateTime.now();

            transactionTemplate.executeWithoutResult(status ->
                    leaseRepository.saveAndFlush(
                            new WorkerLease(
                                    jobId,
                                    "worker-1",
                                    now,
                                    now.plusMinutes(5)
                            )
                    )
            );

            assertThrows(
                    DataIntegrityViolationException.class,
                    () -> transactionTemplate.executeWithoutResult(status ->
                            leaseRepository.saveAndFlush(
                                    new WorkerLease(
                                            jobId,
                                            "worker-2",
                                            now,
                                            now.plusMinutes(5)
                                    )
                            )
                    )
            );
        } finally {
            deleteJobAndChildren(jobId);
        }
    }

    @Test
    void shouldRejectDuplicateCheckpoint() {
        Long jobId = createJob();

        try {
            LocalDateTime now = LocalDateTime.now();

            transactionTemplate.executeWithoutResult(status ->
                    checkpointRepository.saveAndFlush(
                            new ProcessingCheckpoint(
                                    jobId, 1L, "worker-1", 1000L, now
                            )
                    )
            );

            assertThrows(
                    DataIntegrityViolationException.class,
                    () -> transactionTemplate.executeWithoutResult(status ->
                            checkpointRepository.saveAndFlush(
                                    new ProcessingCheckpoint(
                                            jobId, 1L, "worker-2", 1000L, now
                                    )
                            )
                    )
            );
        } finally {
            deleteJobAndChildren(jobId);
        }
    }

    @Test
    void shouldRejectDuplicateRetryKey() {
        Long jobId = createJob();

        try {
            transactionTemplate.executeWithoutResult(status ->
                    retryRepository.saveAndFlush(
                            new ProcessingRetry(
                                    jobId, "retry-001", "JOB"
                            )
                    )
            );

            assertThrows(
                    DataIntegrityViolationException.class,
                    () -> transactionTemplate.executeWithoutResult(status ->
                            retryRepository.saveAndFlush(
                                    new ProcessingRetry(
                                            jobId, "retry-001", "JOB"
                                    )
                            )
                    )
            );
        } finally {
            deleteJobAndChildren(jobId);
        }
    }

    @Test
    void shouldRejectDuplicateStockRecord() {
        Long jobId = createJob();

        try {
            transactionTemplate.executeWithoutResult(status ->
                    stockRepository.saveAndFlush(stock(jobId, 1L))
            );

            assertThrows(
                    DataIntegrityViolationException.class,
                    () -> transactionTemplate.executeWithoutResult(status ->
                            stockRepository.saveAndFlush(stock(jobId, 1L))
                    )
            );
        } finally {
            deleteJobAndChildren(jobId);
        }
    }

    @Test
    void shouldRollbackMultipleRepositoryOperations() {
        Long jobId = createJob();

        try {
            assertThrows(
                    IllegalStateException.class,
                    () -> transactionTemplate.executeWithoutResult(status -> {
                        stockRepository.saveAndFlush(stock(jobId, 1L));

                        eventRepository.saveAndFlush(
                                new ProcessingEvent(
                                        jobId,
                                        "PROCESSING_STARTED",
                                        "INFO",
                                        "Temporary event"
                                )
                        );

                        throw new IllegalStateException(
                                "Simulated processing failure"
                        );
                    })
            );

            assertThat(stockRepository.countByJobId(jobId))
                    .isZero();

            assertThat(eventRepository.countByJobId(jobId))
                    .isZero();
        } finally {
            deleteJobAndChildren(jobId);
        }
    }

    @Test
    void shouldPreventDeletingJobWithChildren() {
        Long jobId = createJob();

        try {
            transactionTemplate.executeWithoutResult(status ->
                    stockRepository.saveAndFlush(stock(jobId, 1L))
            );

            assertThrows(
                    DataIntegrityViolationException.class,
                    () -> transactionTemplate.executeWithoutResult(status -> {
                        jobRepository.deleteById(jobId);
                        jobRepository.flush();
                    })
            );

            assertThat(jobRepository.existsById(jobId)).isTrue();
        } finally {
            deleteJobAndChildren(jobId);
        }
    }

    @Test
    void shouldValidateFlywayAndJsonSchema() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = 1
                """,
                Integer.class
        );

        assertThat(migrationCount).isGreaterThanOrEqualTo(6);

        String jsonType = jdbcTemplate.queryForObject(
                """
                SELECT DATA_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'processing_events'
                  AND COLUMN_NAME = 'event_details'
                """,
                String.class
        );

        assertThat(jsonType).isEqualToIgnoringCase("json");
    }
}
