
package com.stockprocessing.coordinator.repository;

import com.stockprocessing.coordinator.entity.ProcessingEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessingEventRepository
        extends JpaRepository<ProcessingEvent, Long> {

    Page<ProcessingEvent> findByJobId(
            Long jobId,
            Pageable pageable
    );

    List<ProcessingEvent> findByJobIdOrderByCreatedAtAsc(
            Long jobId
    );

    List<ProcessingEvent> findByCorrelationId(
            String correlationId
    );

    Page<ProcessingEvent> findByEventType(
            String eventType,
            Pageable pageable
    );

    long countByJobId(Long jobId);
}
