
package com.stockprocessing.coordinator.entity;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "processing_events")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_status", nullable = false, length = 20)
    private String eventStatus = "INFO";

    @Column(name = "worker_id", length = 128)
    private String workerId;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "event_message", length = 1000)
    private String eventMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_details", columnDefinition = "json")
    private JsonNode eventDetails;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    public ProcessingEvent(
            Long jobId,
            String eventType,
            String eventStatus,
            String eventMessage
    ) {
        this.jobId = jobId;
        this.eventType = eventType;
        this.eventStatus = eventStatus;
        this.eventMessage = eventMessage;
    }
}
