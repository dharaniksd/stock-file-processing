
CREATE TABLE worker_leases (
                               id BIGINT NOT NULL AUTO_INCREMENT,

                               job_id BIGINT NOT NULL,

                               worker_id VARCHAR(128) NOT NULL,

                               lease_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                               lease_version BIGINT NOT NULL DEFAULT 1,

                               acquired_at TIMESTAMP(6) NOT NULL
                                                                 DEFAULT CURRENT_TIMESTAMP(6),

                               heartbeat_at TIMESTAMP(6) NOT NULL
                                                                 DEFAULT CURRENT_TIMESTAMP(6),

                               expires_at TIMESTAMP(6) NOT NULL,

                               created_at TIMESTAMP(6) NOT NULL
                                                                 DEFAULT CURRENT_TIMESTAMP(6),

                               updated_at TIMESTAMP(6) NOT NULL
                                                                 DEFAULT CURRENT_TIMESTAMP(6)
                                   ON UPDATE CURRENT_TIMESTAMP(6),

                               PRIMARY KEY (id),

                               CONSTRAINT uk_worker_leases_job
                                   UNIQUE (job_id),

                               CONSTRAINT fk_worker_leases_job
                                   FOREIGN KEY (job_id)
                                       REFERENCES processing_jobs(id)
                                       ON DELETE RESTRICT,

                               CONSTRAINT chk_worker_leases_status
                                   CHECK (lease_status IN (
                                                           'ACTIVE', 'RELEASED', 'EXPIRED'
                                       )),

                               CONSTRAINT chk_worker_leases_version
                                   CHECK (lease_version > 0),

                               INDEX idx_worker_leases_expiration
                                   (lease_status, expires_at),

                               INDEX idx_worker_leases_worker
                                   (worker_id)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
