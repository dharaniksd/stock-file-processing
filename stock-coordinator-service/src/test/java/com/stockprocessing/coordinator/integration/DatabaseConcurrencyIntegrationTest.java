
package com.stockprocessing.coordinator.integration;

import com.stockprocessing.coordinator.entity.ProcessingJob;
import com.stockprocessing.coordinator.entity.StockRecord;
import com.stockprocessing.coordinator.repository.ProcessingJobRepository;
import com.stockprocessing.coordinator.repository.StockRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatabaseConcurrencyIntegrationTest {

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private StockRecordRepository stockRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void shouldPreventConcurrentDuplicateStockRecords()
            throws Exception {

        Long jobId = transactionTemplate.execute(status ->
                jobRepository.saveAndFlush(
                        new ProcessingJob(
                                "concurrent-test.csv",
                                "/data/concurrent-test.csv"
                        )
                ).getId()
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            CountDownLatch startLatch = new CountDownLatch(1);

            Callable<Boolean> insertTask = () -> {
                startLatch.await();

                try {
                    transactionTemplate.executeWithoutResult(status -> {
                        StockRecord record = new StockRecord(
                                jobId,
                                1001L,
                                "AAPL",
                                100L,
                                new BigDecimal("150.2500")
                        );

                        stockRepository.saveAndFlush(record);
                    });

                    return true;
                } catch (DataIntegrityViolationException ex) {
                    return false;
                }
            };

            List<Future<Boolean>> futures = new ArrayList<>();

            futures.add(executor.submit(insertTask));
            futures.add(executor.submit(insertTask));

            startLatch.countDown();

            int successfulInserts = 0;

            for (Future<Boolean> future : futures) {
                if (future.get(30, TimeUnit.SECONDS)) {
                    successfulInserts++;
                }
            }

            assertThat(successfulInserts).isEqualTo(1);
            assertThat(stockRepository.countByJobId(jobId))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();

            transactionTemplate.executeWithoutResult(status -> {
                stockRepository.deleteAll(
                        stockRepository.findByJobId(
                                jobId,
                                org.springframework.data.domain.Pageable.unpaged()
                        ).getContent()
                );

                jobRepository.deleteById(jobId);
            });
        }
    }
}
