# Stock File Processing System — Flow Diagrams

**Target design:** Java 25, Spring Boot 4.1.2, Maven multi-module, MySQL 8, Kafka, Docker, Jenkins, Kubernetes/OpenShift. The diagrams describe planned processing behavior, not features already implemented. The initial service skeletons and health endpoints have been verified.

## 1. End-to-end processing

```mermaid
flowchart TD
    A["CSV file: 1–50 GB"] --> B["Dedicated ingestion server with persistent volume"]
    B --> C{"Registration method"}
    C -->|Scheduled scan| D["Discover stable files"]
    C -->|REST API| E["Register file path"]
    D --> F["Create job with file identity"]
    E --> F
    F --> G[("MySQL: jobs, leases, checkpoints")]
    G --> H["Coordinator claims job with lease"]
    H --> I["Ingestion streams CSV with bounded memory"]
    I --> J["Parse, validate, assign stable record IDs"]
    J --> K["Kafka producer with bounded batches"]
    K --> L[("Kafka: stock-records topic")]
    L --> M["Worker consumer group"]
    M --> N{"Valid record?"}
    N -->|Yes| O["Transactional idempotent write"]
    O --> P[("MySQL: stock records + processed IDs")]
    N -->|No| Q["Record error / DLQ policy"]
    P --> R["Commit consumer offset after DB commit"]
    Q --> S["Commit offset only after durable error handling"]
    R --> T["Monitoring: jobs, progress, failures"]
    S --> T
    G --> T
```

## 2. Safe producer checkpoint and recovery

```mermaid
flowchart TD
    A["Claim job and verify lease ownership"] --> B["Read last durable checkpoint"]
    B --> C["Resume from byte offset and record boundary"]
    C --> D["Read bounded batch"]
    D --> E["Publish with stable event IDs"]
    E --> F{"All events through checkpoint acknowledged?"}
    F -->|No| G["Retry with backoff; keep checkpoint unchanged"]
    G --> H{"Retries exhausted?"}
    H -->|No| E
    H -->|Yes| I["Stop / mark retryable; lease expires"]
    I --> A
    F -->|Yes| J["Persist next safe offset atomically with lease fencing"]
    J --> K{"EOF reached?"}
    K -->|No| D
    K -->|Yes| L["Mark publishing complete"]
```

**Invariant:** The durable producer checkpoint never advances beyond Kafka-acknowledged records. Replay after a crash is expected; consumer idempotency prevents duplicate business writes. CSV byte offsets must be recorded at valid record boundaries and the file identity must be checked on resume.

## 3. Consumer idempotency and retry

```mermaid
flowchart TD
    A["Poll Kafka event"] --> B["Validate envelope and record"]
    B --> C{"Valid?"}
    C -->|No| D["Durably record invalid event or publish to DLQ"]
    C -->|Yes| E["Begin MySQL transaction"]
    E --> F["Insert unique event ID / idempotency key"]
    F --> G{"Already processed?"}
    G -->|Yes| H["No-op; complete transaction"]
    G -->|No| I["Upsert / insert stock record"]
    I --> J["Commit MySQL transaction"]
    J --> K["Commit Kafka offset"]
    H --> K
    D --> K
    E -->|Transient database failure| L["Rollback and retry with backoff"]
    L --> A
```

**Invariant:** Commit Kafka offsets only after the corresponding database transaction or durable error handling succeeds. A crash between DB commit and Kafka offset commit may replay the event; the unique event ID makes that replay safe.

## 4. Deployment topology

```mermaid
flowchart LR
    U["REST clients"] --> ING["Ingestion service: 8081"]
    PV[("Local PersistentVolume: CSV files")] --> ING
    COORD["Coordinator service: 8082"] --> DB[("MySQL 8")]
    ING --> DB
    ING --> KAFKA[("Kafka cluster")]
    KAFKA --> W1["Worker pod A: 8083"]
    KAFKA --> W2["Worker pod B: 8083"]
    W1 --> DB
    W2 --> DB
    MON["Monitoring service: 8084"] --> DB
    J["Jenkins CI/CD"] --> REG["Container registry"]
    REG --> OCP["Kubernetes / OpenShift deployments"]
    OCP -. manages .-> ING
    OCP -. manages .-> COORD
    OCP -. manages .-> W1
    OCP -. manages .-> W2
    OCP -. manages .-> MON
```

**Deployment constraint:** Only the ingestion workload that has access to the dedicated local PV can read the CSV files. Keep ingestion single-active unless shared storage or an explicit leader/lease model is introduced. Worker pods can scale horizontally subject to Kafka partition count and database capacity.

## 5. Job lifecycle

```mermaid
stateDiagram-v2
    [*] --> REGISTERED
    REGISTERED --> CLAIMED
    CLAIMED --> PUBLISHING
    PUBLISHING --> PUBLISHED: Kafka acknowledgements complete
    PUBLISHING --> RETRYABLE: transient failure / lost lease
    RETRYABLE --> CLAIMED: retry eligible
    PUBLISHED --> PROCESSING
    PROCESSING --> COMPLETED: all records accounted for
    PROCESSING --> FAILED: unrecoverable error / policy limit
    PUBLISHING --> FAILED: unrecoverable error
    REGISTERED --> CANCELLED
    RETRYABLE --> CANCELLED
    COMPLETED --> [*]
    FAILED --> [*]
    CANCELLED --> [*]
```

**Important:** Publishing complete is not the same as processing complete. Completion requires durable accounting for successfully processed and rejected records, including in-flight Kafka messages.

## 6. Service boundaries

| Module | Responsibility | Local port |
|---|---|---:|
| stock-common | Shared contracts, DTOs, stable identifiers | — |
| stock-ingestion-service | Registration, scan, CSV streaming, Kafka publishing | 8081 |
| stock-coordinator-service | Job ownership, leases, retries, coordination | 8082 |
| stock-worker-service | Kafka consumption, validation, idempotent MySQL writes | 8083 |
| stock-monitoring-service | Progress, job status, failures and metrics API | 8084 |

## 7. Implementation notes

- Preserve `com.stockprocessing` as Maven groupId and Java package root.
- MySQL InnoDB transactions are used for durable job state, fencing tokens and unique idempotency keys.
- File registration should reject duplicates using an explicit file identity policy; changed files must not silently reuse checkpoints.
- Persist record-level failures durably, with a DLQ for events that cannot be processed according to policy.
- Use backpressure, bounded buffers and Kafka producer acknowledgements to avoid unbounded heap growth.
- Treat monitoring progress as eventually consistent and define counts precisely (read, published, processed, rejected).
- The exact SQL schema, API payloads, lease fencing mechanism, and Kafka topic configuration should be finalized during their respective development tasks.
