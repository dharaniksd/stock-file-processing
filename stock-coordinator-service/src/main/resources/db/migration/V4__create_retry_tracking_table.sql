
CREATE TABLE processing_retries (

                                    id BIGINT NOT NULL AUTO_INCREMENT,

                                    job_id BIGINT NOT NULL,

                                    retry_key VARCHAR(255) NOT NULL,

                                    retry_type VARCHAR(30) NOT NULL,

                                    retry_status VARCHAR(20) NOT NULL
                                                               DEFAULT 'PENDING',

                                    attempt_count INT NOT NULL DEFAULT 0,

                                    max_attempts INT NOT NULL DEFAULT 5,

                                    next_retry_at TIMESTAMP(6) NULL,

                                    last_error_code VARCHAR(100) NULL,

                                    last_error_message TEXT NULL,

                                    claimed_by VARCHAR(128) NULL,

                                    claim_token CHAR(36)
                                           CHARACTER SET ascii
                                           COLLATE ascii_bin NULL,

                                    claim_expires_at TIMESTAMP(6) NULL,

                                    created_at TIMESTAMP(6) NOT NULL
                                                               DEFAULT CURRENT_TIMESTAMP(6),

                                    updated_at TIMESTAMP(6) NOT NULL
                                                               DEFAULT CURRENT_TIMESTAMP(6)
                                        ON UPDATE CURRENT_TIMESTAMP(6),

                                    PRIMARY KEY (id),

                                    CONSTRAINT uk_processing_retries_job_key
                                        UNIQUE (job_id, retry_key),

                                    CONSTRAINT fk_processing_retries_job
                                        FOREIGN KEY (job_id)
                                            REFERENCES processing_jobs(id)
                                            ON DELETE RESTRICT,

                                    CONSTRAINT chk_processing_retries_type
                                        CHECK (
                                            retry_type IN (
                                                           'JOB',
                                                           'KAFKA_PUBLISH',
                                                           'DATABASE_WRITE'
                                                )
                                            ),

                                    CONSTRAINT chk_processing_retries_status
                                        CHECK (
                                            retry_status IN (
                                                             'PENDING',
                                                             'IN_PROGRESS',
                                                             'SUCCEEDED',
                                                             'EXHAUSTED',
                                                             'CANCELLED'
                                                )
                                            ),

                                    CONSTRAINT chk_processing_retries_attempts
                                        CHECK (
                                            attempt_count >= 0
                                                AND max_attempts > 0
                                                AND attempt_count <= max_attempts
                                            ),

                                    CONSTRAINT chk_processing_retries_claim
                                        CHECK (
                                            (
                                                retry_status = 'IN_PROGRESS'
                                                    AND claimed_by IS NOT NULL
                                                    AND claim_token IS NOT NULL
                                                    AND claim_expires_at IS NOT NULL
                                                )
                                                OR
                                            (
                                                retry_status <> 'IN_PROGRESS'
                                                    AND claimed_by IS NULL
                                                    AND claim_token IS NULL
                                                    AND claim_expires_at IS NULL
                                                )
                                            ),

                                    INDEX idx_processing_retries_due
                                        (retry_status, next_retry_at),

                                    INDEX idx_processing_retries_claim_expiry
                                        (retry_status, claim_expires_at),

                                    INDEX idx_processing_retries_job_status
                                        (job_id, retry_status)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
