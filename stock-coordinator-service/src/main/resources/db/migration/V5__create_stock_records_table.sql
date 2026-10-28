
-- V5: Create stock records table
-- MySQL 8.4 / InnoDB
-- Each source CSV record is uniquely identified
-- by (job_id, record_number).

CREATE TABLE stock_records (

                               id BIGINT NOT NULL AUTO_INCREMENT,

                               job_id BIGINT NOT NULL,

                               record_number BIGINT NOT NULL,

                               stock_symbol VARCHAR(32) NOT NULL,

                               quantity BIGINT NOT NULL,

                               price DECIMAL(19,4) NOT NULL,

                               created_at TIMESTAMP(6) NOT NULL
                                   DEFAULT CURRENT_TIMESTAMP(6),

                               PRIMARY KEY (id),

                               CONSTRAINT uk_stock_records_job_record
                                   UNIQUE (job_id, record_number),

                               CONSTRAINT fk_stock_records_job
                                   FOREIGN KEY (job_id)
                                       REFERENCES processing_jobs(id)
                                       ON DELETE RESTRICT,

                               CONSTRAINT chk_stock_records_record_number
                                   CHECK (record_number > 0),

                               CONSTRAINT chk_stock_records_symbol
                                   CHECK (CHAR_LENGTH(TRIM(stock_symbol)) > 0),

                               CONSTRAINT chk_stock_records_quantity
                                   CHECK (quantity >= 0),

                               CONSTRAINT chk_stock_records_price
                                   CHECK (price >= 0),

                               INDEX idx_stock_records_symbol
                                   (stock_symbol),

                               INDEX idx_stock_records_created_at
                                   (created_at)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
