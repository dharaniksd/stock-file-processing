
CREATE TABLE processing_checkpoints (

                                        id BIGINT NOT NULL AUTO_INCREMENT,

                                        job_id BIGINT NOT NULL,

                                        last_record_number BIGINT NOT NULL DEFAULT 0,

                                        byte_offset BIGINT NOT NULL DEFAULT 0,

                                        records_published BIGINT NOT NULL DEFAULT 0,

                                        checkpoint_version BIGINT NOT NULL DEFAULT 0,

                                        lease_version BIGINT NOT NULL,

                                        last_worker_id VARCHAR(128) NOT NULL,

                                        file_size_bytes BIGINT NOT NULL,

                                        file_last_modified_at TIMESTAMP(6) NOT NULL,

                                        created_at TIMESTAMP(6) NOT NULL
                                                                           DEFAULT CURRENT_TIMESTAMP(6),

                                        updated_at TIMESTAMP(6) NOT NULL
                                                                           DEFAULT CURRENT_TIMESTAMP(6)
                                            ON UPDATE CURRENT_TIMESTAMP(6),

                                        PRIMARY KEY (id),

                                        CONSTRAINT uk_processing_checkpoints_job
                                            UNIQUE (job_id),

                                        CONSTRAINT fk_processing_checkpoints_job
                                            FOREIGN KEY (job_id)
                                                REFERENCES processing_jobs(id)
                                                ON DELETE RESTRICT,

                                        CONSTRAINT chk_checkpoint_record_number
                                            CHECK (last_record_number >= 0),

                                        CONSTRAINT chk_checkpoint_byte_offset
                                            CHECK (byte_offset >= 0),

                                        CONSTRAINT chk_checkpoint_records_published
                                            CHECK (records_published >= 0),

                                        CONSTRAINT chk_checkpoint_version
                                            CHECK (checkpoint_version >= 0),

                                        CONSTRAINT chk_checkpoint_lease_version
                                            CHECK (lease_version > 0),

                                        CONSTRAINT chk_checkpoint_file_size
                                            CHECK (file_size_bytes >= 0),

                                        CONSTRAINT chk_checkpoint_offset_file_size
                                            CHECK (byte_offset <= file_size_bytes),

                                        INDEX idx_processing_checkpoints_updated_at
                                            (updated_at)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
