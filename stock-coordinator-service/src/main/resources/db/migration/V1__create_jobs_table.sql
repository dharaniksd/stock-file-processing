
CREATE TABLE processing_jobs (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 file_name VARCHAR(255) NOT NULL,
                                 file_path VARCHAR(1024) NOT NULL,
                                 status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
                                 total_records BIGINT NOT NULL DEFAULT 0,
                                 processed_records BIGINT NOT NULL DEFAULT 0,
                                 failed_records BIGINT NOT NULL DEFAULT 0,
                                 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                     ON UPDATE CURRENT_TIMESTAMP(6),

                                 PRIMARY KEY (id),
                                 INDEX idx_processing_jobs_status (status),
                                 INDEX idx_processing_jobs_created_at (created_at)
) ENGINE=InnoDB;
