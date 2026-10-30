
package com.stockprocessing.coordinator.repository;

import tools.jackson.databind.ObjectMapper;

import com.stockprocessing.coordinator.entity.ProcessingEvent;
import com.stockprocessing.coordinator.entity.ProcessingJob;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ProcessingEventRepositoryIntegrationTest {

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private ProcessingEventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long createJob() {
        ProcessingJob job = new ProcessingJob(
                "event-test.csv",
                "/data/event-test.csv"
        );

        return jobRepository.saveAndFlush(job).getId();
    }

    private ProcessingEvent createEvent(
            Long jobId,
            String eventType
    ) {
        return new ProcessingEvent(
                jobId,
                eventType,
                "INFO",
                "Processing event recorded"
        );
    }

    @Test
    void shouldSaveAndRetrieveEvent() {
        Long jobId = createJob();

        ProcessingEvent event = eventRepository.saveAndFlush(
                createEvent(jobId, "FILE_REGISTERED")
        );

        assertThat(event.getId()).isNotNull();

        ProcessingEvent found = eventRepository
                .findById(event.getId())
                .orElseThrow();

        assertThat(found.getEventType())
                .isEqualTo("FILE_REGISTERED");

        assertThat(found.getEventStatus())
                .isEqualTo("INFO");
    }

    @Test
    void shouldPersistJsonEventDetails() throws Exception {
        Long jobId = createJob();

        ProcessingEvent event = createEvent(
                jobId,
                "CHECKPOINT_SAVED"
        );

        event.setEventDetails(
                objectMapper.readTree("""
                    {
                      "recordNumber": 5000000,
                      "byteOffset": 250000000,
                      "workerId": "worker-1"
                    }
                    """)
        );

        Long eventId = eventRepository
                .saveAndFlush(event)
                .getId();

        entityManager.clear();

        ProcessingEvent found = eventRepository
                .findById(eventId)
                .orElseThrow();

        assertThat(
                found.getEventDetails()
                        .get("recordNumber")
                        .asLong()
        ).isEqualTo(5_000_000L);

        assertThat(
                found.getEventDetails()
                        .get("workerId")
                        .asText()
        ).isEqualTo("worker-1");
    }

    @Test
    void shouldFindEventsByJobId() {
        Long jobId = createJob();

        eventRepository.saveAndFlush(
                createEvent(jobId, "FILE_REGISTERED")
        );

        eventRepository.saveAndFlush(
                createEvent(jobId, "PROCESSING_STARTED")
        );

        var events = eventRepository
                .findByJobIdOrderByCreatedAtAsc(jobId);

        assertThat(events).hasSize(2);
    }

    @Test
    void shouldFindEventsByCorrelationId() {
        Long jobId = createJob();

        ProcessingEvent event = createEvent(
                jobId,
                "KAFKA_PUBLISH_FAILED"
        );

        event.setCorrelationId("corr-123");

        eventRepository.saveAndFlush(event);

        var events = eventRepository
                .findByCorrelationId("corr-123");

        assertThat(events)
                .extracting(ProcessingEvent::getEventType)
                .contains("KAFKA_PUBLISH_FAILED");
    }

    @Test
    void shouldSupportPagination() {
        Long jobId = createJob();

        for (int i = 1; i <= 5; i++) {
            eventRepository.saveAndFlush(
                    createEvent(jobId, "CHECKPOINT_SAVED")
            );
        }

        var page = eventRepository.findByJobId(
                jobId,
                PageRequest.of(0, 2)
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void shouldRejectInvalidEventStatus() {
        Long jobId = createJob();

        ProcessingEvent event = new ProcessingEvent(
                jobId,
                "FILE_REGISTERED",
                "INVALID",
                "Invalid event status"
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> eventRepository.saveAndFlush(event)
        );
    }
}
