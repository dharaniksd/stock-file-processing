# Stock File Processing System — Database Data Flow

## 1. Purpose

This document describes how the ingestion, coordination, worker, and monitoring services interact with MySQL 8.4 and Kafka when processing 1–50 GB CSV files containing hundreds of millions of independent stock records.

**Stack:** Java 25, Spring Boot 4.1.2, MySQL 8.4 (InnoDB), Kafka, Docker, Kubernetes/OpenShift.

> This is the **target design** for Phase 2, not a claim that all tables and services have already been implemented. Table names and state transitions should be finalized during schema implementation.

## 2. High-level database data flow

```mermaid
flowchart TD
    FILE[CSV files on ingestion PersistentVolume]
    SCAN[Scheduled scanner or REST registration]
    ING[Ingestion service]
    COORD[Coordinator service]
    KAFKA[(Kafka stock records topic)]
    WORK[Worker service instances]
    MON[Monitoring service]
    DB[(MySQL 8.4 InnoDB)]

    FILE --> SCAN
    SCAN --> ING
    ING -->|Register file and job| DB
    COORD -->|Claim jobs and maintain leases| DB
    COORD -->|Assign eligible job| ING
    ING -->|Read and parse CSV stream| KAFKA
    ING -->|Persist publish checkpoints| DB
    KAFKA -->|Consume records| WORK
    WORK -->|Idempotent stock writes and failure logs| DB
    MON -->|Read job status and progress| DB
```

## 3. Logical database entities

```mermaid
erDiagram
    INGESTION_JOBS ||--o{ WORKER_LEASES : has
    INGESTION_JOBS ||--o{ INGESTION_CHECKPOINTS : tracks
    INGESTION_JOBS ||--o{ PROCESSING_FAILURES : records
    INGESTION_JOBS ||--o{ JOB_RETRIES : schedules
    INGESTION_JOBS ||--o{ STOCK_RECORDS : produces

    INGESTION_JOBS {
        bigint id PK
        string file_path
        string file_fingerprint
        string status
        bigint file_size_bytes
        datetime created_at
        datetime updated_at
    }
    WORKER_LEASES {
        bigint id PK
        bigint job_id FK
        string owner_id
        bigint fencing_token
        datetime lease_expires_at
    }
    INGESTION_CHECKPOINTS {
        bigint id PK
        bigint job_id FK
        bigint next_byte_offset
        bigint next_record_number
        datetime updated_at
    }
    PROCESSING_FAILURES {
        bigint id PK
        bigint job_id FK
        bigint record_number
        string error_code
        datetime created_at
    }
    JOB_RETRIES {
        bigint id PK
        bigint job_id FK
        int attempt_number
        datetime next_attempt_at
    }
    STOCK_RECORDS {
        bigint id PK
        bigint job_id FK
        bigint record_number
        string stock_symbol
        decimal quantity
        decimal price
    }
```

These fields are illustrative. Final types, unique keys, foreign keys, and indexes belong in the versioned Flyway migration scripts. For idempotency, a candidate uniqueness constraint is `(job_id, record_number)` on `STOCK_RECORDS` (or a stable event ID if the record model requires it).

## 4. Job registration and claiming

```mermaid
sequenceDiagram
    participant S as Scanner or REST API
    participant I as Ingestion service
    participant D as MySQL
    participant C as Coordinator

    S->>I: Register discovered CSV path
    I->>D: Insert job if file identity is new
    D-->>I: Job ID and status
    C->>D: Begin transaction and select eligible job for update skip locked
    D-->>C: Claimable job
    C->>D: Update ownership, fencing token, and lease expiry
    C->>D: Commit transaction
    C-->>I: Job assignment
```

**Design rules:** Registration must deduplicate using a stable file identity (for example, canonical path plus verified fingerprint). Job claiming must be atomic. A lease protects coordination; the fencing token prevents stale owners from committing state after their lease has been replaced.

## 5. CSV streaming and Kafka checkpoint flow

```mermaid
sequenceDiagram
    participant I as Ingestion service
    participant F as CSV on PV
    participant K as Kafka
    participant D as MySQL

    I->>D: Read durable checkpoint and lease token
    D-->>I: Next safe byte offset and record number
    I->>F: Open file and resume at safe CSV boundary
    loop Bounded records in flight
        F-->>I: Next complete CSV record
        I->>K: Publish record with stable event identity
        K-->>I: Broker acknowledgment
        I->>D: Advance only contiguous acknowledged checkpoint
        D-->>I: Checkpoint committed
    end
    I->>D: Mark publishing complete when all records acknowledged
```

**Critical:** Kafka acknowledgments can arrive out of order. The checkpoint must advance only past a contiguous range of successfully acknowledged records, and only while the current owner holds a valid lease/fencing token. A crash between Kafka acknowledgment and MySQL checkpoint commit can cause replay; downstream writes must therefore be idempotent. Use CSV-aware record boundaries rather than assuming every newline is a complete record when quoted multiline fields are supported.

## 6. Kafka consumption and MySQL persistence

```mermaid
sequenceDiagram
    participant K as Kafka
    participant W as Worker consumer
    participant D as MySQL

    K-->>W: Stock event with job ID and record number
    W->>W: Validate and transform event
    W->>D: Begin transaction
    W->>D: Insert stock record with unique event identity
    alt New event
        D-->>W: Insert succeeds
    else Duplicate replay
        D-->>W: Unique conflict handled as already processed
    end
    W->>D: Commit transaction
    W->>K: Commit Kafka offset after DB commit
```

For invalid records, use an explicit policy: record the failure and either route to a dead-letter topic or mark the record as terminally rejected before advancing its Kafka offset. A MySQL transaction cannot atomically commit a Kafka consumer offset; replay is expected and must be safe. The consumer should not acknowledge an event before the durable database outcome is committed.

## 7. Crash recovery

```mermaid
flowchart TD
    A[Ingestion process or pod crashes] --> B[Coordinator detects expired lease]
    B --> C[Claim job with new fencing token]
    C --> D[Read last durable checkpoint]
    D --> E[Resume from safe CSV boundary]
    E --> F[Republish records not durably checkpointed]
    F --> G[Consumer applies unique event identity]
    G --> H[Insert new records or ignore duplicates]
    H --> I[Update progress and complete job]
```

**Important:** A local PersistentVolume must be accessible to whichever ingestion instance resumes the job. With a single dedicated ingestion server, recovery must respect its storage topology. Kafka retention must be sufficient for consumer recovery, and file contents must remain immutable while a job is processed.

## 8. Job lifecycle

```mermaid
stateDiagram-v2
    [*] --> REGISTERED
    REGISTERED --> CLAIMED
    CLAIMED --> PUBLISHING
    PUBLISHING --> PUBLISHED
    PUBLISHING --> RETRY_PENDING: Recoverable failure
    RETRY_PENDING --> CLAIMED: Lease acquired again
    PUBLISHED --> COMPLETED: All records reach terminal outcomes
    PUBLISHED --> FAILED: Unrecoverable processing failure
    PUBLISHING --> FAILED: Retry limit exceeded
    COMPLETED --> [*]
    FAILED --> [*]
```

`PUBLISHED` means Kafka publishing finished; it does **not** mean consumers finished writing all stock records. To declare `COMPLETED`, define an authoritative completion mechanism such as per-job terminal outcome counts, with safeguards against duplicate counting and missing messages.

## 9. Table ownership and access

| Service | Writes | Reads |
| --- | --- | --- |
| Ingestion | `ingestion_jobs`, `ingestion_checkpoints` | Job details and current lease token |
| Coordinator | `ingestion_jobs`, `worker_leases`, `job_retries` | Eligible jobs, leases, retries |
| Worker | `stock_records`, `processing_failures`, terminal outcome progress | Event identity / job metadata as needed |
| Monitoring | None by default | Job state, checkpoint progress, failures |

Service boundaries are logical; implement least-privilege database credentials and avoid multiple services independently changing the same state machine without a defined transaction contract.

## 10. Operational design decisions

1. Use **Flyway** for versioned schema changes, with a single migration owner during deployment.
2. Use **InnoDB** for transactional job claims, checkpoints, and idempotent inserts.
3. Keep **CSV file bytes outside MySQL**; store metadata and checkpoints in MySQL.
4. Use **Kafka** to decouple file reading from record processing.
5. Use **unique constraints** to make duplicate event replay harmless.
6. Use **bounded producer concurrency** and contiguous-ack checkpointing to avoid skipping records.
7. Keep **lease expiry and fencing** consistent to prevent stale worker writes.
8. Distinguish **publishing progress** from **consumer completion progress**.
9. Design indexes and retention/partitioning for very large `stock_records` and failure tables.
10. Use **UTC timestamps**, stable record identifiers, and auditable retry decisions.

## 11. Next implementation steps

- Task 2.2: Configure Flyway and create an initial migration.
- Task 2.3: Finalize `ingestion_jobs` schema and file identity constraints.
- Task 2.4: Finalize worker leases and fencing.
- Task 2.5: Finalize CSV checkpoints and replay semantics.
- Tasks 2.6–2.7: Finalize failures, retries, stock records, and unique keys.
- Tasks 2.8–2.12: Implement atomic queries, recovery, repositories, and integration tests.

All diagrams use GitHub-compatible Mermaid fenced blocks, with node labels kept simple to avoid parser errors.
