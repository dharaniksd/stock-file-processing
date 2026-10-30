
package com.stockprocessing.coordinator.repository;

import com.stockprocessing.coordinator.entity.ProcessingJob;
import com.stockprocessing.coordinator.entity.StockRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class StockRecordRepositoryIntegrationTest {

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private StockRecordRepository stockRecordRepository;

    private Long createJob() {
        ProcessingJob job = new ProcessingJob(
                "stock-record-test.csv",
                "/data/stock-record-test.csv"
        );

        return jobRepository.saveAndFlush(job).getId();
    }

    private StockRecord createStockRecord(
            Long jobId,
            Long recordNumber
    ) {
        return new StockRecord(
                jobId,
                recordNumber,
                "AAPL",
                100L,
                new BigDecimal("150.2500")
        );
    }

    @Test
    void shouldSaveAndRetrieveStockRecord() {
        Long jobId = createJob();

        StockRecord saved = stockRecordRepository.saveAndFlush(
                createStockRecord(jobId, 1L)
        );

        assertThat(saved.getId()).isNotNull();

        StockRecord found = stockRecordRepository
                .findById(saved.getId())
                .orElseThrow();

        assertThat(found.getStockSymbol()).isEqualTo("AAPL");
        assertThat(found.getQuantity()).isEqualTo(100L);
        assertThat(found.getPrice())
                .isEqualByComparingTo("150.2500");
    }

    @Test
    void shouldFindRecordByJobIdAndRecordNumber() {
        Long jobId = createJob();

        stockRecordRepository.saveAndFlush(
                createStockRecord(jobId, 5001L)
        );

        var result = stockRecordRepository
                .findByJobIdAndRecordNumber(jobId, 5001L);

        assertThat(result).isPresent();

        assertThat(
                stockRecordRepository.existsByJobIdAndRecordNumber(
                        jobId,
                        5001L
                )
        ).isTrue();
    }

    @Test
    void shouldCountRecordsByJobId() {
        Long jobId = createJob();

        stockRecordRepository.saveAndFlush(
                createStockRecord(jobId, 1L)
        );

        stockRecordRepository.saveAndFlush(
                createStockRecord(jobId, 2L)
        );

        assertThat(stockRecordRepository.countByJobId(jobId))
                .isEqualTo(2L);
    }

    @Test
    void shouldSupportPagination() {
        Long jobId = createJob();

        for (long i = 1; i <= 5; i++) {
            stockRecordRepository.saveAndFlush(
                    createStockRecord(jobId, i)
            );
        }

        var page = stockRecordRepository.findByJobId(
                jobId,
                PageRequest.of(0, 2)
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void shouldRejectDuplicateJobAndRecordNumber() {
        Long jobId = createJob();

        stockRecordRepository.saveAndFlush(
                createStockRecord(jobId, 1L)
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> stockRecordRepository.saveAndFlush(
                        createStockRecord(jobId, 1L)
                )
        );
    }

    @Test
    void shouldRejectNegativeQuantity() {
        Long jobId = createJob();

        StockRecord record = createStockRecord(jobId, 1L);
        record.setQuantity(-10L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> stockRecordRepository.saveAndFlush(record)
        );
    }
}
