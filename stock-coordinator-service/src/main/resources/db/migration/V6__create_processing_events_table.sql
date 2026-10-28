
-- V6: Create processing events table
-- MySQL 8.4 / InnoDB

CREATE TABLE processing_events (

                                   id BIGINT NOT NULL AUTO_INCREMENT,

                                   job_id BIGINT NOT NULL,

                                   event_type VARCHAR(50) NOT NULL,

                                   event_status VARCHAR(20) NOT NULL
                                       DEFAULT 'INFO',

                                   worker_id VARCHAR(128) NULL,

                                   correlation_id VARCHAR(128) NULL,

                                   event_message VARCHAR(1000) NULL,

                                   event_details JSON NULL,

                                   created_at TIMESTAMP(6) NOT NULL
                                       DEFAULT CURRENT_TIMESTAMP(6),

                                   PRIMARY KEY (id),

                                   CONSTRAINT fk_processing_events_job
                                       FOREIGN KEY (job_id)
                                           REFERENCES processing_jobs(id)
                                           ON DELETE RESTRICT,

                                   CONSTRAINT chk_processing_events_status
                                       CHECK (
                                           event_status IN (
                                                            'INFO',
                                                            'SUCCESS',
                                                            'WARNING',
                                                            'ERROR'
                                               )
                                           ),

                                   CONSTRAINT chk_processing_events_type
                                       CHECK (
                                           CHAR_LENGTH(TRIM(event_type)) > 0
                                           ),

                                   INDEX idx_processing_events_job_time
                                       (job_id, created_at),

                                   INDEX idx_processing_events_type_time
                                       (event_type, created_at),

                                   INDEX idx_processing_events_correlation
                                       (correlation_id)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
