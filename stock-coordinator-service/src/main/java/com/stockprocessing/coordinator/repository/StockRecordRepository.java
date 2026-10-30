
package com.stockprocessing.coordinator.repository;

import com.stockprocessing.coordinator.entity.StockRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRecordRepository
        extends JpaRepository<StockRecord, Long> {

    Optional<StockRecord> findByJobIdAndRecordNumber(
            Long jobId,
            Long recordNumber
    );

    boolean existsByJobIdAndRecordNumber(
            Long jobId,
            Long recordNumber
    );

    long countByJobId(Long jobId);

    Page<StockRecord> findByJobId(
            Long jobId,
            Pageable pageable
    );
}
