# Implementation Phases: Automated Transaction Reconciliation Engine

This document details the step-by-step implementation plan for the Reconciliation Engine. Each phase builds upon the previous one to deliver a robust, production-ready system.

---

## Phase 1: Core Infrastructure & Reconciliation Engine
**Goal:** Establish the foundation, ingest data from multiple sources, and implement the core matching logic.

### 1.1 Project Setup & Architecture
- [ ] **Repository Initialization:**
    - Set up Monorepo or separate repos for Backend (Spring Boot) and Frontend (React).
    - Configure `.gitignore`, `README.md`, and initial folder structure.
- [ ] **Database Design:**
    - Design PostgreSQL schema for `transactions_raw`, `ledger_entries`, and `reconciliations`.
    - Create Flyway/Liquibase migration scripts for version control.
- [ ] **Spring Boot Skeleton:**
    - Initialize project with dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-amqp` (RabbitMQ), `lombok`.
    - Configure multi-module structure if needed (e.g., `core`, `api`, `worker`).

### 1.2 Transaction Ingestion Service
- [ ] **Generic Provider Interface:**
    - Define a `PaymentProvider` interface with methods like `fetchTransactions()`, `verifyTransaction()`.
    - Implement adapters for specific providers (e.g., `PaystackAdapter`, `FlutterwaveAdapter`).
- [ ] **CSV Parsing Module:**
    - Implement a flexible CSV parser (using OpenCSV or Apache Commons CSV).
    - Create strategy classes for different bank formats (GTBank, Access, Zenith).
    - Implement normalization logic to map bank-specific fields to the unified `Transaction` model.
- [ ] **Ingestion API:**
    - Create endpoints for manual CSV upload (`POST /api/ingest/csv`).
    - Implement scheduled jobs (Quartz or Spring Scheduler) to poll Provider APIs.

### 1.3 Reconciliation Rules Engine
- [ ] **Matching Logic:**
    - Implement **Exact Matching** (Reference + Amount + Time window).
    - Implement **Fuzzy Matching** logic for settlement delays.
- [ ] **Discrepancy Detection:**
    - Implement rules for:
        - Double Debits (Duplicate internal refs).
        - Missing Credits (Bank settlement exists, no ledger entry).
        - Amount Mismatches.
- [ ] **Confidence Scoring:**
    - Implement the scoring algorithm based on weighted rules.
    - Create a service to calculate and persist the score for each reconciliation run.

### 1.4 Output & Persistence
- [ ] **Persistence Layer:**
    - Save results to the `reconciliations` table.
    - Ensure database constraints (unique indexes) prevent duplicate processing.

---

## Phase 2: Webhook Reliability & Async Processing
**Goal:** Ensure data consistency by handling real-time updates and recovering from failures.

### 2.1 Message Queue Integration
- [ ] **RabbitMQ Setup:**
    - Provision RabbitMQ instance (local or cloud).
    - Define exchanges and queues: `transaction.ingested`, `webhook.received`, `reconciliation.start`.
- [ ] **Producer/Consumer Implementation:**
    - Update Ingestion Service to publish events to `transaction.ingested`.
    - Create a listener to trigger the Rules Engine asynchronously.

### 2.2 Webhook Handling
- [ ] **Webhook Controller:**
    - Create a generic endpoint to receive webhooks from providers.
    - Validate signatures (HMAC SHA512) to ensure security.
- [ ] **Webhook Logging:**
    - Persist every raw webhook payload to `webhook_log` table immediately upon receipt.

### 2.3 Recovery & Retry Mechanism
- [ ] **Missing Webhook Detector:**
    - Implement a scheduled job to compare API transaction lists against received webhooks.
- [ ] **Retry Logic:**
    - Implement exponential backoff for failed webhook processing.
    - Use RabbitMQ's delayed message exchange or a custom scheduler for retries.
- [ ] **Dead Letter Queue (DLQ):**
    - Configure DLQ for messages that fail after max retries.
    - Create a mechanism to replay messages from DLQ.

---

## Phase 3: Dispute Workflow & Dashboard
**Goal:** Provide a user interface for Operations teams to view discrepancies and take action.

### 3.1 Backend API for Dashboard
- [ ] **Dispute Management Endpoints:**
    - `GET /api/disputes`: List disputes with filtering (status, confidence score).
    - `GET /api/disputes/{id}`: Get detailed view including timeline and rule breakdown.
    - `POST /api/disputes/{id}/action`: Approve, Reject, or Escalate.
- [ ] **State Machine:**
    - Implement the state transitions (`PENDING` -> `AWAITING_REVIEW` -> `APPROVED`/`REJECTED`).
    - Ensure valid transitions only.

### 3.2 Frontend Dashboard (React)
- [ ] **Project Setup:**
    - Initialize React app (Vite or Create React App).
    - Set up UI component library (Chakra UI, Material UI, or Tailwind).
- [ ] **Authentication:**
    - Implement login page (integrate with backend auth).
- [ ] **Dispute List View:**
    - Table displaying disputes sorted by Confidence Score.
    - Color-coded badges for status and score.
- [ ] **Dispute Detail View:**
    - "Side-by-side" comparison view (Ledger vs. Provider vs. Bank).
    - Action buttons (Approve Refund, Reject, Escalate).

---

## Phase 4: Refund Execution & Security
**Goal:** Enable safe, auditable financial actions.

### 4.1 Refund Logic
- [ ] **Refund Interface:**
    - Add `initiateRefund()` to the `PaymentProvider` interface.
    - Implement refund logic in provider adapters.
- [ ] **Idempotency:**
    - Implement idempotency keys for all refund requests to prevent double refunds.
    - Store refund status in `refunds` table.

### 4.2 Security & Compliance
- [ ] **Role-Based Access Control (RBAC):**
    - Define roles: `VIEWER`, `OPERATOR`, `ADMIN`.
    - Restrict refund actions to `ADMIN` or specific `OPERATOR` roles.
- [ ] **Two-Factor Authentication (2FA) for Approvals:**
    - (Optional for MVP) Implement OTP verification before executing a refund.
- [ ] **Audit Logging:**
    - Implement an Aspect-Oriented Programming (AOP) aspect to intercept critical actions.
    - Log `who`, `what`, `when`, and `why` to the immutable `audit_log` table.
    - Ensure PII masking in logs.

---

## Phase 5: Production Hardening & Observability
**Goal:** Prepare the system for real-world traffic and ensure maintainability.

### 5.1 Observability
- [ ] **Metrics:**
    - Integrate Micrometer and Prometheus.
    - Expose custom metrics: `reconciliation.discrepancy.count`, `webhook.failure.rate`.
- [ ] **Logging:**
    - Configure structured logging (JSON format) for ELK stack ingestion.
    - Correlate logs using `TraceID` and `SpanID`.
- [ ] **Health Checks:**
    - Implement Spring Boot Actuator health endpoints.
    - Add checks for DB connectivity, RabbitMQ status, and Provider API reachability.

### 5.2 Testing & Documentation
- [ ] **Integration Testing:**
    - Write integration tests using Testcontainers (Postgres, RabbitMQ).
    - Test full reconciliation flows.
- [ ] **Load Testing:**
    - Use JMeter or k6 to simulate high transaction volumes.
    - Verify system stability under load.
- [ ] **Documentation:**
    - Generate API documentation (Swagger/OpenAPI).
    - Write "Runbooks" for common operational issues (e.g., "How to handle DLQ buildup").

---

## Summary of Technologies
- **Language:** Java 17+ (Spring Boot 3.x)
- **Database:** PostgreSQL 14+
- **Messaging:** RabbitMQ
- **Frontend:** React (TypeScript)
- **Containerization:** Docker
- **Testing:** JUnit 5, Mockito, Testcontainers
