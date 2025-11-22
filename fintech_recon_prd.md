# Product Requirements Document
## Automated Transaction Reconciliation & Dispute Triage Engine

**Version:** 1.0  
**Status:** MVP Specification  
**Target Audience:** Fintech Operations, Backend Engineering, Finance Teams  
**Author:** [Your Name]  
**Last Updated:** November 2025

---

## 1. Executive Summary

The **Automated Transaction Reconciliation & Dispute Triage Engine** is a backend financial operations system designed to detect, classify, and route transaction inconsistencies across payment provider records, internal ledgers, and bank settlement data.

**Core Value Proposition:**  
Automatically detects transaction inconsistencies across Payment Providers (Paystack, Flutterwave, etc.), internal ledger, and bank settlement records, assigns confidence scores based on industry-validated patterns, recovers webhook failures, and routes disputes through an auditable approval workflow—reducing manual reconciliation time by 70%+ while maintaining financial accuracy.

**Key Differentiator:**  
Unlike typical payment integration demos, this system addresses core fintech operational infrastructure: reconciliation automation, webhook reliability, dispute triage, and regulatory auditability.

---

## 2. Problem Statement

### 2.1 Current Pain Points

Nigerian fintech companies handling card payments, transfers, wallet transactions, and merchant settlements face significant operational friction:

**Operational Costs:**
- Manual reconciliation consumes 15-20 hours/week per finance officer
- Customer support teams spend 40% of time on transaction disputes
- Failed webhooks cause 2-3 hour resolution times per incident

**Financial Risk:**
- Undetected double debits erode customer trust
- Missing credits result in revenue leakage
- Settlement delays create cash flow uncertainty
- Manual refund processes introduce human error

**Technical Debt:**
- Operations teams manually compare:
  - Paystack/Flutterwave dashboard exports
  - Bank settlement CSVs (GTBank, Access, Zenith formats)
  - Internal PostgreSQL ledger tables
  - NIBSS settlement reports

**Impact Metrics:**
- ❌ Average dispute resolution time: 24-72 hours
- ❌ Manual reconciliation error rate: 5-8%
- ❌ Webhook failure rate: 3-7% (network issues, timeouts)
- ❌ Customer satisfaction impact: 23% of negative reviews mention payment issues

### 2.2 Why This Problem Matters

In Nigeria's fintech ecosystem:
- NIBSS processes 300M+ transactions monthly
- Settlement occurs in T+1/T+2 batches (not real-time)
- Network instability causes webhook delivery failures
- Bank CSV formats vary significantly across institutions
- Regulatory scrutiny (CBN) requires audit trails

**Business Case:**  
A fintech processing 10,000 transactions/day with 2% discrepancy rate = 200 manual investigations daily. At 15 minutes per investigation = 50 hours/day of ops work.

---

## 3. Goals & Objectives

### 3.1 Primary Goals

✅ **Automate reconciliation** between payment provider records, bank settlements, and internal ledger  
✅ **Detect mismatches in near-real-time** (within 5 minutes of data availability)  
✅ **Classify disputes** with explainable confidence scores  
✅ **Route disputes** through auditable approval workflows  
✅ **Recover failed webhooks** with intelligent retry logic  
✅ **Provide operational visibility** into financial data consistency  

### 3.2 Success Metrics (MVP)

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Automated Detection Rate** | 95%+ | % of discrepancies caught without manual review |
| **Confidence Score Accuracy** | 85%+ | % of high-confidence flags confirmed as valid |
| **Webhook Recovery Rate** | 80%+ | % of failed webhooks successfully recovered |
| **Manual Reconciliation Time Reduction** | 70%+ | Hours saved vs baseline |
| **False Positive Rate** | <10% | % of flagged disputes that were false alarms |
| **Audit Log Completeness** | 100% | % of actions with full audit trail |

### 3.3 Non-Goals (Out of Scope for MVP)

This product does **NOT**:
- ❌ Process payments or act as a payment gateway
- ❌ Perform KYC/identity verification
- ❌ Handle card scheme chargebacks (Visa/Mastercard disputes)
- ❌ Generate invoices or billing documents
- ❌ Provide customer-facing transaction history UI
- ❌ Execute automated refunds without human approval

---

## 4. User Personas

### Persona 1: Reconciliation Officer (Primary)
**Name:** Chiamaka, Finance Operations  
**Goals:**
- Complete daily reconciliation by 6 PM
- Identify discrepancies before month-end close
- Reduce manual CSV comparisons

**Pain Points:**
- Spends 3 hours/day on manual matching
- Bank CSVs arrive in inconsistent formats
- No visibility into webhook failures

**Success Criteria:**
- Dashboard shows all mismatches with confidence scores
- Can approve/reject refunds with one click
- Export reconciliation reports for audit

### Persona 2: Customer Support Agent
**Name:** Tunde, Support Team Lead  
**Goals:**
- Resolve transaction complaints within 2 hours
- Provide accurate status to customers
- Reduce ticket escalations to engineering

**Pain Points:**
- Can't determine if user should be refunded
- No visibility into settlement status
- Must manually check Paystack dashboard + internal DB

**Success Criteria:**
- Single source of truth for transaction status
- Clear refund eligibility indicators
- Audit trail to share with customers

### Persona 3: Backend Engineer (Secondary)
**Name:** Emeka, Payments Engineering  
**Goals:**
- Ensure idempotent transaction processing
- Monitor webhook delivery health
- Maintain ledger consistency

**Pain Points:**
- Webhook failures cause data inconsistencies
- No automated retry mechanism
- Manual investigation of failed transactions

**Success Criteria:**
- Webhook recovery runs automatically
- Detailed logs for debugging
- Metrics on reconciliation performance

---

## 5. Core Features (MVP)

### 5.1 Transaction Ingestion Service

**Responsibility:** Normalize data from multiple sources into unified format

**Data Sources:**
1. **Payment Provider APIs** (Paystack, Flutterwave, etc.)
   - Polling frequency: Every 5 minutes
   - Fields: reference, amount, status, customer_email, paid_at
   
2. **Bank Settlement CSVs** (Manual upload initially)
   - Supported formats: GTBank, Access Bank, Zenith, FCMB
   - Fields: transaction_ref, amount, settlement_date, narration
   
3. **Internal Ledger Table** (`transactions` table in PostgreSQL)
   - Fields: id, reference, user_id, amount, type (debit/credit), status, created_at

**Normalization Logic:**
```sql
CREATE TABLE transactions_raw (
  id UUID PRIMARY KEY,
  source VARCHAR(50), -- 'paystack' | 'bank' | 'ledger'
  external_reference VARCHAR(255),
  normalized_reference VARCHAR(255), -- Cleaned version
  amount DECIMAL(15,2),
  currency VARCHAR(3) DEFAULT 'NGN',
  status VARCHAR(50),
  customer_identifier VARCHAR(255),
  timestamp TIMESTAMP,
  raw_data JSONB, -- Original record
  ingested_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_normalized_ref ON transactions_raw(normalized_reference);
CREATE INDEX idx_source ON transactions_raw(source);
CREATE INDEX idx_timestamp ON transactions_raw(timestamp);
```

**Nigerian Bank CSV Quirks Handled:**
- **GTBank:** Uses `PAYMENT_REF` column with prefix `GTB-`
- **Access Bank:** Uses `NARRATION` field containing reference buried in text
- **Zenith:** Date format is `DD/MM/YYYY` vs ISO standard
- **FCMB:** Amount includes currency symbol `₦` that must be stripped

**Example Normalization:**
```python
def normalize_reference(source: str, raw_ref: str) -> str:
    if source == "gtbank":
        return raw_ref.replace("GTB-", "").strip()
    elif source == "access":
        # Extract reference from "Transfer|REF:PSK_ABC123|From:..."
        match = re.search(r"REF:([A-Z0-9_]+)", raw_ref)
        return match.group(1) if match else raw_ref
    elif source == "paystack":
        return raw_ref.upper().strip()
    return raw_ref
```

---

### 5.2 Reconciliation Rules Engine (CORE FEATURE)

**Responsibility:** Match transactions across sources and detect inconsistencies

#### 5.2.1 Matching Algorithm

**Phase 1: Exact Matching**
```
FOR each transaction T in ledger:
  FIND provider record P WHERE:
    P.reference = T.reference AND
    P.amount = T.amount AND
    ABS(P.timestamp - T.timestamp) < 30 minutes
  
  IF found:
    Mark as MATCHED
    Confidence: 100
```

**Phase 2: Fuzzy Matching (for settlement delays)**
```
FOR each unmatched bank settlement S:
  FIND ledger record L WHERE:
    SIMILARITY(S.reference, L.reference) > 0.8 AND
    ABS(S.amount - L.amount) < 0.01 AND
    S.settlement_date >= L.created_at AND
    S.settlement_date <= L.created_at + 48 hours
  
  IF found:
    Mark as MATCHED_DELAYED
    Confidence: 85-95 (based on time delta)
```

**Phase 3: Discrepancy Detection**

| Discrepancy Type | Detection Rule | Confidence Weight |
|------------------|----------------|-------------------|
| **Double Debit** | 2+ ledger debits, same reference, 1 Provider charge | +40 (duplicate ref) +30 (amount match) +20 (single settlement) |
| **Missing Credit** | Bank settlement exists, no ledger credit entry | +30 (settlement exists) +25 (reference valid) +15 (within T+2 window) |
| **Webhook Failure** | Provider shows success, ledger shows pending >1 hour | +35 (status mismatch) +25 (time threshold) +10 (known webhook issue) |
| **Amount Mismatch** | References match, amounts differ by >₦1 | +30 (same ref) -20 (different amount) |
| **Settlement Delay** | Transaction >48 hours old, no bank settlement | +20 (age) +15 (Provider success) -10 (within NIBSS window) |

#### 5.2.2 Confidence Scoring Model

**Philosophy:** Hybrid approach combining industry patterns + production tuning

**Weighting Justification:**
- Initial weights derived from patterns in Stripe's dispute resolution documentation, Paystack's reconciliation best practices, and weighted scoring techniques from fraud detection literature
- System designed to continuously refine weights based on false positive/negative rates in production
- Scores represent probability that manual investigation will confirm the discrepancy

**Scoring Formula:**
```
Confidence Score = Σ(rule_weight * rule_match) / max_possible_score * 100

Classification:
- 90-100: HIGH_CONFIDENCE (auto-flag for approval)
- 70-89:  MEDIUM_CONFIDENCE (needs investigation)
- 40-69:  LOW_CONFIDENCE (monitor, don't escalate)
- <40:    IGNORE (likely false positive)
```

**Example Calculation:**
```
Transaction: Double debit suspected
- Duplicate reference found: +40
- Amounts match exactly: +30  
- Single Paystack record: +20
- Customer complained: +10
- Timestamps consistent: -5 (reduces suspicion)

Total: 95/100 → HIGH_CONFIDENCE
```

**Tuning Mechanism:**
```sql
CREATE TABLE confidence_tuning (
  rule_name VARCHAR(100),
  current_weight INT,
  true_positive_count INT,
  false_positive_count INT,
  last_updated TIMESTAMP
);

-- Adjust weights monthly based on accuracy
UPDATE confidence_tuning
SET current_weight = current_weight + 5
WHERE rule_name = 'duplicate_reference'
  AND (true_positive_count / (true_positive_count + false_positive_count)) > 0.9;
```

#### 5.2.3 State Machine

```
┌─────────┐
│ PENDING │ (Transaction ingested)
└────┬────┘
     │
     ▼
┌────────────┐
│ PROCESSING │ (Rules engine evaluating)
└─────┬──────┘
      │
      ├──────────┬──────────┬──────────┐
      ▼          ▼          ▼          ▼
  ┌────────┐ ┌──────────┐ ┌──────┐ ┌─────────┐
  │MATCHED │ │ DISPUTED │ │FAILED│ │ESCALATED│
  └────────┘ └─────┬────┘ └──────┘ └─────────┘
                   │
                   ▼
            ┌──────────────┐
            │AWAITING_REVIEW│ (Human approval queue)
            └──────┬────────┘
                   │
                   ├────────────┬────────────┐
                   ▼            ▼            ▼
            ┌──────────┐ ┌──────────┐ ┌─────────┐
            │ APPROVED │ │ REJECTED │ │ESCALATED│
            └────┬─────┘ └──────────┘ └─────────┘
                 │
                 ▼
          ┌──────────┐
          │ RESOLVED │ (Refund executed/ticket closed)
          └──────────┘
```

**State Transitions:**
```python
class ReconciliationState(Enum):
    PENDING = "pending"
    PROCESSING = "processing"
    MATCHED = "matched"
    DISPUTED = "disputed"
    AWAITING_REVIEW = "awaiting_review"
    APPROVED = "approved"
    REJECTED = "rejected"
    ESCALATED = "escalated"
    RESOLVED = "resolved"
    FAILED = "failed"

ALLOWED_TRANSITIONS = {
    PENDING: [PROCESSING],
    PROCESSING: [MATCHED, DISPUTED, FAILED],
    DISPUTED: [AWAITING_REVIEW, ESCALATED],
    AWAITING_REVIEW: [APPROVED, REJECTED, ESCALATED],
    APPROVED: [RESOLVED],
    REJECTED: [RESOLVED],
}

def transition_state(dispute_id, new_state, actor):
    current = get_current_state(dispute_id)
    if new_state not in ALLOWED_TRANSITIONS[current]:
        raise InvalidTransitionError()
    
    # Atomic state update with audit log
    with transaction():
        update_state(dispute_id, new_state)
        log_audit_event(dispute_id, current, new_state, actor)
```

#### 5.2.4 Idempotency Guarantees

**Problem:** Reconciliation may run multiple times due to retries, crashes, or scheduled jobs overlapping.

**Solution: Reconciliation Run ID + Distributed Lock**

```python
import redis
from contextlib import contextmanager

@contextmanager
def reconciliation_lock(transaction_ref: str, timeout=300):
    """Distributed lock prevents duplicate processing"""
    redis_client = get_redis_client()
    lock_key = f"recon_lock:{transaction_ref}"
    
    acquired = redis_client.set(lock_key, "locked", nx=True, ex=timeout)
    if not acquired:
        raise AlreadyProcessingError()
    
    try:
        yield
    finally:
        redis_client.delete(lock_key)

def reconcile_transaction(transaction_ref: str):
    with reconciliation_lock(transaction_ref):
        # Check if already processed
        existing = get_reconciliation_result(transaction_ref)
        if existing and existing.state != ReconciliationState.FAILED:
            return existing  # Idempotent: return cached result
        
        # Generate unique run ID
        run_id = f"{transaction_ref}_{int(time.time())}"
        
        # Process with run ID
        result = run_rules_engine(transaction_ref, run_id)
        save_reconciliation_result(result)
        
        return result
```

**Database Guarantees:**
```sql
CREATE TABLE reconciliations (
  id UUID PRIMARY KEY,
  transaction_ref VARCHAR(255) UNIQUE, -- Prevents duplicate processing
  run_id VARCHAR(255),
  state VARCHAR(50),
  confidence_score INT,
  rules_fired JSONB,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Unique constraint ensures one reconciliation per transaction
CREATE UNIQUE INDEX idx_transaction_ref ON reconciliations(transaction_ref);
```

**Ledger Update Idempotency:**
```sql
-- Refunds use idempotency keys
CREATE TABLE refunds (
  id UUID PRIMARY KEY,
  dispute_id UUID REFERENCES reconciliations(id),
  idempotency_key VARCHAR(255) UNIQUE, -- Prevents duplicate refunds
  amount DECIMAL(15,2),
  status VARCHAR(50),
  executed_at TIMESTAMP
);

-- Example refund execution
INSERT INTO refunds (dispute_id, idempotency_key, amount, status)
VALUES (?, ?, ?, 'pending')
ON CONFLICT (idempotency_key) DO NOTHING; -- Idempotent
```

---

### 5.3 Webhook Reliability Layer

**Responsibility:** Detect, retry, and recover failed webhook deliveries

#### 5.3.1 Webhook Tracking

```sql
CREATE TABLE webhook_log (
  id UUID PRIMARY KEY,
  transaction_ref VARCHAR(255),
  provider VARCHAR(50), -- 'paystack' | 'flutterwave'
  event_type VARCHAR(100), -- 'charge.success' | 'transfer.success'
  expected_at TIMESTAMP, -- When webhook should have arrived
  received_at TIMESTAMP, -- Actual arrival time
  status VARCHAR(50), -- 'received' | 'missing' | 'delayed' | 'recovered'
  retry_count INT DEFAULT 0,
  last_retry_at TIMESTAMP,
  payload JSONB
);

CREATE INDEX idx_missing_webhooks ON webhook_log(status) WHERE status = 'missing';
```

#### 5.3.2 Detection Logic

```python
def detect_missing_webhooks():
    """
    Runs every 5 minutes
    Compares Provider API status with internal webhook receipts
    """
    recent_transactions = fetch_provider_transactions(last_30_minutes)
    
    for txn in recent_transactions:
        if txn.status == "success":
            webhook_received = check_webhook_log(txn.reference)
            
            if not webhook_received:
                time_since_txn = now() - txn.paid_at
                
                if time_since_txn > timedelta(minutes=15):
                    # Flag as missing
                    log_missing_webhook(txn.reference, expected_at=txn.paid_at)
                    enqueue_webhook_retry(txn.reference)
```

#### 5.3.3 Retry Strategy

**Exponential Backoff:**
```
Attempt 1: Immediate verification via Provider API
Attempt 2: 5 minutes later
Attempt 3: 15 minutes later (cumulative: 20 min)
Attempt 4: 30 minutes later (cumulative: 50 min)
Attempt 5: 1 hour later (cumulative: 1h 50min)
Final: Move to Dead Letter Queue after 5 attempts
```

**Implementation:**
```python
def retry_webhook_verification(transaction_ref: str, attempt: int):
    try:
        # Verify current status from Paystack
        paystack_status = verify_transaction_status(transaction_ref)
        internal_status = get_ledger_status(transaction_ref)
        
        if paystack_status == internal_status:
            mark_webhook_recovered(transaction_ref)
            return SUCCESS
        
        # Status mismatch: attempt reprocessing
        if attempt < 5:
            delay = calculate_backoff_delay(attempt)
            schedule_retry(transaction_ref, attempt + 1, delay)
        else:
            # Failed after 5 attempts
            move_to_dlq(transaction_ref)
            alert_ops_team(transaction_ref)
            
    except APIRateLimitError:
        # Exponential backoff for rate limits
        schedule_retry(transaction_ref, attempt, delay=3600)  # 1 hour
```

#### 5.3.4 Dead Letter Queue (DLQ)

```sql
CREATE TABLE webhook_dlq (
  id UUID PRIMARY KEY,
  transaction_ref VARCHAR(255),
  failure_reason TEXT,
  retry_attempts INT,
  last_error TEXT,
  moved_to_dlq_at TIMESTAMP DEFAULT NOW()
);

-- Alert when DLQ grows beyond threshold
SELECT COUNT(*) FROM webhook_dlq WHERE moved_to_dlq_at > NOW() - INTERVAL '24 hours';
-- If count > 50: Alert ops team
```

---

### 5.4 Dispute Workflow & Approval System

**Responsibility:** Route disputes to human reviewers with full context

#### 5.4.1 Dispute Routing Logic

```python
def route_dispute(reconciliation_result):
    confidence = reconciliation_result.confidence_score
    dispute_type = reconciliation_result.discrepancy_type
    
    if confidence >= 90:
        # High confidence: auto-flag for approval
        assign_to_queue("refund_approval_queue")
        notify_finance_team(reconciliation_result)
        
    elif 70 <= confidence < 90:
        # Medium confidence: investigation queue
        assign_to_queue("investigation_queue")
        
    elif 40 <= confidence < 70:
        # Low confidence: monitor only
        assign_to_queue("monitoring_queue")
        
    else:
        # Very low confidence: ignore
        mark_as_resolved(reconciliation_result, reason="low_confidence")
```

#### 5.4.2 Escalation Logic

**3-Attempt Escalation Flow:**

| Attempt | Action | Timing | Context Added |
|---------|--------|--------|---------------|
| **1** | Auto-verify via Provider API | Immediate | Current transaction status |
| **2** | Check bank settlement file | +24 hours | Wait for T+1 settlement |
| **3** | Query internal ledger audit log | +48 hours | Full transaction history |
| **Escalate** | Route to finance team lead | After attempt 3 | All previous data + customer complaint history |

**Implementation:**
```python
def escalate_dispute(dispute_id: str, attempt: int):
    dispute = get_dispute(dispute_id)
    
    if attempt == 1:
        # Attempt 1: Immediate API check
        updated_status = fetch_paystack_status(dispute.transaction_ref)
        if status_resolved(updated_status):
            resolve_dispute(dispute_id)
            return
    
    elif attempt == 2:
        # Attempt 2: Wait for settlement (T+1)
        if dispute.created_at < now() - timedelta(hours=24):
            settlement = check_bank_settlement(dispute.transaction_ref)
            if settlement:
                resolve_dispute(dispute_id, settlement_data=settlement)
                return
    
    elif attempt == 3:
        # Attempt 3: Deep audit log analysis
        audit_trail = get_full_audit_log(dispute.transaction_ref)
        if can_auto_resolve(audit_trail):
            resolve_dispute(dispute_id)
            return
    
    # Failed all attempts: escalate to human
    escalate_to_finance_lead(dispute_id, context={
        "attempts": attempt,
        "paystack_status": fetch_paystack_status(dispute.transaction_ref),
        "settlement_status": check_bank_settlement(dispute.transaction_ref),
        "audit_trail": get_full_audit_log(dispute.transaction_ref),
        "customer_complaints": get_related_tickets(dispute.transaction_ref)
    })
```

#### 5.4.3 Approval Dashboard Requirements

**Key Features:**
- List of disputes sorted by confidence score (high → low)
- One-click approve/reject with reason codes
- Full transaction timeline visualization
- Related customer support tickets
- Settlement status from bank CSV
- Audit log of all actions taken

**API Endpoints:**
```
GET  /api/disputes?status=awaiting_review&sort=confidence:desc
POST /api/disputes/{id}/approve
POST /api/disputes/{id}/reject
GET  /api/disputes/{id}/timeline
GET  /api/disputes/{id}/audit-log
```

---

### 5.5 Audit & Compliance Layer

**Responsibility:** Provide immutable audit trail for regulatory compliance

#### 5.5.1 Audit Log Schema

```sql
CREATE TABLE audit_log (
  id UUID PRIMARY KEY,
  event_id VARCHAR(100) UNIQUE, -- Globally unique event identifier
  transaction_ref VARCHAR(255),
  event_type VARCHAR(100), -- 'RECONCILIATION_RUN' | 'DISPUTE_FLAGGED' | 'REFUND_APPROVED'
  actor VARCHAR(255), -- 'system' | user_id
  action VARCHAR(100), -- 'MATCHED' | 'FLAGGED' | 'APPROVED' | 'REJECTED'
  confidence_score INT,
  rules_fired JSONB, -- Array of rule names + weights
  decision_rationale TEXT, -- Human-readable explanation
  before_state VARCHAR(50),
  after_state VARCHAR(50),
  metadata JSONB, -- Additional context
  ip_address INET, -- For human actions
  user_agent TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Immutability: No UPDATE or DELETE allowed
-- Append-only table with retention policy
CREATE POLICY audit_log_immutable ON audit_log
  FOR UPDATE USING (false);

CREATE INDEX idx_transaction_ref ON audit_log(transaction_ref);
CREATE INDEX idx_event_type ON audit_log(event_type);
CREATE INDEX idx_created_at ON audit_log(created_at DESC);
```

#### 5.5.2 What Gets Logged

**Every reconciliation run:**
```json
{
  "event_type": "RECONCILIATION_RUN",
  "transaction_ref": "PSK_abc123",
  "rules_fired": [
    {"rule": "duplicate_reference", "weight": 40, "matched": true},
    {"rule": "amount_match", "weight": 30, "matched": true},
    {"rule": "single_settlement", "weight": 20, "matched": true}
  ],
  "confidence_score": 90,
  "decision_rationale": "High confidence double debit detected: same reference found twice in ledger with single Paystack charge and bank settlement",
  "action": "FLAGGED_FOR_REVIEW"
}
```

**Every manual action:**
```json
{
  "event_type": "REFUND_APPROVED",
  "transaction_ref": "PSK_abc123",
  "actor": "user_chiamaka_123",
  "action": "APPROVED",
  "decision_rationale": "Confirmed duplicate debit after verifying bank settlement and customer complaint",
  "before_state": "AWAITING_REVIEW",
  "after_state": "APPROVED",
  "ip_address": "102.89.x.x",
  "metadata": {
    "refund_amount": 5000.00,
    "customer_email": "user@example.com"
  }
}
```

#### 5.5.3 Compliance Requirements

**CBN (Central Bank of Nigeria) Requirements:**
- Audit logs retained for 7 years
- PII (email, phone) masked in logs except for authorized personnel
- Immutable audit trail (append-only, no deletions)
- Transaction lineage traceable from initiation to settlement

**Implementation:**
```python
def mask_pii(data: dict) -> dict:
    """Mask personally identifiable information in logs"""
    if "customer_email" in data:
        email = data["customer_email"]
        username, domain = email.split("@")
        data["customer_email"] = f"{username[:2]}***@{domain}"
    
    if "phone" in data:
        phone = data["phone"]
        data["phone"] = f"{phone[:4]}****{phone[-2:]}"
    
    return data

def log_audit_event(event_data: dict):
    masked_data = mask_pii(event_data) if not is_authorized_user() else event_data
    insert_audit_log(masked_data)
```

**Two-Factor Approval for Refunds:**
```python
def approve_refund(dispute_id: str, approver_id: str, otp_code: str):
    # Verify OTP
    if not verify_otp(approver_id, otp_code):
        raise UnauthorizedError("Invalid OTP")
    
    # Check approver has permission
    if not has_refund_approval_permission(approver_id):
        raise PermissionDeniedError()
    
    # Execute refund with audit trail
    with transaction():
        update_dispute_state(dispute_id, "APPROVED")
        log_audit_event({
            "event_type": "REFUND_APPROVED",
            "actor": approver_id,
            "dispute_id": dispute_id,
            "two_factor_verified": True
        })
        execute_refund(dispute_id)
```

---

## 6. Nigerian Fintech Context

### 6.1 Settlement Timing (NIBSS Batch Windows)

**T+1 Settlement:** Most bank transfers settle next business day
**T+2 Settlement:** Some interbank transfers (especially cross-bank USSD)

**Reconciliation Implications:**
```python
def is_within_settlement_window(transaction_date: datetime) -> bool:
    """
    Don't flag as missing settlement if within expected window
    Accounts for weekends and Nigerian public holidays
    """
    business_days_elapsed = calculate_business_days(transaction_date, now())
    
    if business_days_elapsed < 1:
        return True  # T+1 not yet reached
    elif business_days_elapsed < 2:
        return True  # T+2 window
    else:
        return False  # Outside window: flag as delayed
```

### 6.2 Bank CSV Format Variations

**GTBank Settlement CSV:**
```csv
PAYMENT_REF,AMOUNT,SETTLEMENT_DATE,STATUS
GTB-PSK_abc123,5000.00,22/11/2024,SUCCESS
```

**Access Bank Settlement CSV:**
```csv
NARRATION,CREDIT_AMOUNT,VALUE_DATE
Transfer|REF:PSK_abc123|From:Customer Name,5000.00,2024-11-22
```

**Normalization Required:**
```python
class BankCSVParser:
    def parse_gtbank(self, row):
        return {
            "reference": row["PAYMENT_REF"].replace("GTB-", ""),
            "amount": float(row["AMOUNT"]),
            "date": datetime.strptime(row["SETTLEMENT_DATE"], "%d/%m/%Y")
        }
    
    def parse_access(self, row):
        # Extract reference from narration
        match = re.search(r"REF:([A-Z0-9_]+)", row["NARRATION"])
        reference = match.group(1) if match else None
        
        return {
            "reference": reference,
            "amount": float(row["CREDIT_AMOUNT"]),
            "date": datetime.fromisoformat(row["VALUE_DATE"])
        }
```

### 6.3 USSD Transaction Ambiguity

**Problem:** USSD transactions may show "PENDING" for 15+ minutes due to:
- Network handshake delays between NIBSS and banks
- USSD gateway timeouts
- Core banking system latency

**Solution: Grace Period Before Flagging**
```python
def should_flag_pending_transaction(txn):
    if txn.payment_method == "USSD":
        grace_period = timedelta(minutes=30)  # USSD needs longer
    else:
        grace_period = timedelta(minutes=15)  # Cards/transfers
    
    time_pending = now() - txn.created_at
    return time_pending > grace_period
```

---

## 7. Technical Architecture

### 7.1 System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     EXTERNAL DATA SOURCES                        │
├──────────────┬──────────────────┬─────────────────┬─────────────┤
│  Payment     │  Bank Settlement │ Other Providers │   NIBSS     │
│  Gateways    │      CSVs        │     (APIs)      │   Portal    │
└──────┬───────┴────────┬─────────┴────────┬────────┴──────┬──────┘
       │                │                  │               │
       └────────────────┼──────────────────┼───────────────┘
                        │                  │
                        ▼                  ▼
            ┌─────────────────────────────────────┐
            │   TRANSACTION INGESTOR SERVICE      │
            │  (Polling + CSV Upload + Webhooks)  │
            └──────────────┬──────────────────────┘
                           │ Normalized Data
                           ▼
            ┌─────────────────────────────────────┐
            │      POSTGRESQL (Primary Store)     │
            │  • transactions_raw                 │
            │  • ledger_entries                   │
            │  • webhook_log                      │
            └──────────────┬──────────────────────┘
                           │
                           ▼
            ┌─────────────────────────────────────┐
            │         RABBITMQ / KAFKA            │
            │  Queues:                            │
            │  • reconciliation_queue             │
            │  • dispute_queue                    │
            │  • webhook_retry_queue              │
            │  • dead_letter_queue                │
            └──────────────┬──────────────────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
┌──────────────────┐ ┌──────────────┐ ┌────────────────┐
│ RECONCILIATION   │ │   WEBHOOK    │ │    DISPUTE     │
│ RULES ENGINE     │ │   RECOVERY   │ │   WORKFLOW     │
│                  │ │   SERVICE    │ │    SERVICE     │
│ • Match logic    │ │ • Retry      │ │ • Escalation   │
│ • Confidence     │ │ • DLQ mgmt   │ │ • Approval     │
│ • State machine  │ │ • Verify     │ │ • Refund exec  │
└────────┬─────────┘ └──────┬───────┘ └────────┬───────┘
         │                  │                  │
         └──────────────────┼──────────────────┘
                            │
                            ▼
            ┌─────────────────────────────────────┐
            │      POSTGRESQL (Results Store)     │
            │  • reconciliations                  │
            │  • disputes                         │
            │  • audit_log (immutable)            │
            │  • refunds                          │
            └──────────────┬──────────────────────┘
                           │
                           ▼
            ┌─────────────────────────────────────┐
            │      DASHBOARD API (Spring Boot)    │
            │  REST endpoints for:                │
            │  • Dispute listing                  │
            │  • Approval actions                 │
            │  • Reporting                        │
            │  • Audit trail query                │
            └──────────────┬──────────────────────┘
                           │
                           ▼
            ┌─────────────────────────────────────┐
            │     FRONTEND (React Dashboard)      │
            │  • Dispute review UI                │
            │  • Reconciliation reports           │
            │  • Metrics & analytics              │
            └─────────────────────────────────────┘
```

### 7.2 Data Flow

**Happy Path: Transaction Matched Successfully**
```
1. Provider API → Ingestor → transactions_raw
2. Ingestor publishes to reconciliation_queue
3. Rules Engine picks up message
4. Finds exact match in ledger
5. Updates reconciliations table: state=MATCHED, confidence=100
6. Logs audit event
7. Done (no human intervention needed)
```

**Dispute Path: Double Debit Detected**
```
1. Ingestor ingests duplicate ledger debit
2. Rules Engine detects: 2 debits, 1 Provider charge
3. Calculates confidence: 95 (HIGH)
4. Creates dispute record: state=AWAITING_REVIEW
5. Publishes to dispute_queue
6. Dispute Workflow Service routes to refund_approval_queue
7. Notifies finance team via email/Slack
8. Human reviews in dashboard
9. Approves with 2FA
10. Refund executed with idempotency key
11. State → RESOLVED
12. Full audit trail logged
```

### 7.3 Technology Stack

| Component | Technology | Justification |
|-----------|-----------|---------------|
| **Backend Services** | Java Spring Boot | Industry standard for fintech, strong typing, mature ecosystem |
| **Database** | PostgreSQL 14+ | ACID compliance, JSONB support, mature tooling |
| **Message Queue** | RabbitMQ (MVP) / Kafka (scale) | Reliable message delivery, dead letter queues |
| **Caching** | Redis | Distributed locks, rate limiting |
| **API Documentation** | Swagger / OpenAPI | Auto-generated docs, contract testing |
| **Monitoring** | Prometheus + Grafana | Metrics, alerting, dashboards |
| **Logging** | ELK Stack (Elasticsearch, Logstash, Kibana) | Centralized log analysis |
| **CI/CD** | GitHub Actions / Jenkins | Automated testing, deployment |

---

## 8. Failure Scenarios & Mitigations

### 8.1 API Rate Limits

**Scenario:** Provider API rate limit hit during peak reconciliation

**Mitigation:**
```python
@retry(stop=stop_after_attempt(5), wait=wait_exponential(multiplier=2))
def fetch_provider_transactions(batch_size=100):
    try:
        response = provider_api.get("/transactions", params={"perPage": batch_size})
        return response.json()
    except RateLimitError as e:
        # Exponential backoff: 2s, 4s, 8s, 16s, 32s
        logging.warning(f"Rate limit hit: {e}. Retrying...")
        raise  # Trigger retry
```

### 8.2 Duplicate Bank CSV Uploads

**Scenario:** Operations team accidentally uploads same settlement file twice

**Mitigation:**
```python
def ingest_bank_csv(file_path: str, file_hash: str):
    # Check if file already processed
    existing = db.query(BankCSVUpload).filter_by(file_hash=file_hash).first()
    if existing:
        raise DuplicateFileError(f"File already processed on {existing.uploaded_at}")
    
    # Record file upload
    db.add(BankCSVUpload(file_hash=file_hash, uploaded_at=now()))
    
    # Process file
    parse_and_ingest(file_path)
```

### 8.3 Webhook Arrives During Reconciliation

**Scenario:** Webhook for transaction arrives while reconciliation is processing it

**Mitigation:**
- Distributed lock prevents concurrent processing
- State machine ensures only valid transitions
- Idempotency key prevents duplicate ledger updates

```python
with reconciliation_lock(transaction_ref):
    current_state = get_state(transaction_ref)
    if current_state == ReconciliationState.PROCESSING:
        # Already being reconciled, webhook queued for reprocessing
        enqueue_webhook_reprocess(transaction_ref, delay=60)
        return
    
    # Safe to process webhook
    process_webhook(transaction_ref)
```

### 8.4 Database Connection Pool Exhaustion

**Scenario:** High reconciliation load exhausts database connections

**Mitigation:**
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      
# Circuit breaker pattern
resilience4j:
  circuitbreaker:
    instances:
      databaseService:
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
        slidingWindowSize: 10
```

### 8.5 Incorrect Auto-Flag for Refund

**Scenario:** System flags legitimate transaction for refund due to rule bug

**Mitigation:**
- **No automatic refunds** — all require human approval
- Confidence score visible to approver
- Full audit trail shows why flagged
- Manual rejection with reason codes feeds back into tuning model

```python
# Approver can see full context
GET /api/disputes/abc-123
{
  "confidence_score": 92,
  "reason": "Duplicate reference detected",
  "rules_fired": [
    {"rule": "duplicate_reference", "weight": 40, "evidence": "..."},
    {"rule": "amount_match", "weight": 30, "evidence": "..."}
  ],
  "paystack_data": {...},
  "ledger_data": {...},
  "bank_settlement": {...},
  "customer_complaints": []
}
```

---

## 9. Observability & Metrics

### 9.1 Key Metrics to Instrument

**Reconciliation Performance:**
```
- reconciliation_run_duration_seconds (histogram: p50, p95, p99)
- reconciliations_total (counter: by status: matched, disputed, failed)
- confidence_score_distribution (histogram)
- rules_engine_errors_total (counter)
```

**Webhook Health:**
```
- webhooks_received_total (counter)
- webhooks_missing_total (counter)
- webhook_retry_attempts (histogram)
- webhook_recovery_rate (gauge)
- dlq_size (gauge)
```

**Dispute Workflow:**
```
- disputes_created_total (counter: by confidence tier)
- dispute_resolution_time_seconds (histogram)
- manual_override_rate (gauge)
- false_positive_rate (gauge)
- refunds_executed_total (counter)
```

**Business Metrics:**
```
- transaction_volume_total (counter: by source)
- discrepancy_rate (gauge: % of transactions with issues)
- settlement_delay_seconds (histogram)
- operational_time_saved_hours (gauge: vs manual baseline)
```

### 9.2 Alerting Rules

```yaml
# Prometheus alerting rules
groups:
  - name: reconciliation_alerts
    rules:
      - alert: HighDiscrepancyRate
        expr: rate(reconciliations_total{status="disputed"}[5m]) > 0.05
        for: 10m
        annotations:
          summary: "Discrepancy rate above 5% for 10 minutes"
      
      - alert: WebhookRecoveryFailing
        expr: webhook_recovery_rate < 0.7
        for: 15m
        annotations:
          summary: "Webhook recovery rate below 70%"
      
      - alert: DLQBacklogGrowing
        expr: dlq_size > 100
        annotations:
          summary: "Dead letter queue has 100+ unprocessed items"
      
      - alert: ReconciliationStalled
        expr: rate(reconciliation_run_duration_seconds[5m]) == 0
        for: 30m
        annotations:
          summary: "No reconciliation runs completed in 30 minutes"
```

---

## 10. MVP Roadmap & Phases

### Phase 1: Core Reconciliation Engine (Weeks 1-4)
**Goal:** Detect discrepancies with confidence scoring

**Deliverables:**
- ✅ Transaction ingestor (Paystack API + CSV upload)
- ✅ PostgreSQL schema (transactions_raw, reconciliations)
- ✅ Rules engine with 5 core rules
- ✅ Confidence scoring model
- ✅ State machine implementation
- ✅ Basic audit logging

**Success Criteria:**
- Can ingest 10,000 transactions/day
- Detects double debits with 85%+ accuracy
- Processes reconciliation run in <5 minutes

### Phase 2: Webhook Reliability (Weeks 5-6)
**Goal:** Recover failed webhooks automatically

**Deliverables:**
- ✅ Webhook tracking table
- ✅ Missing webhook detection job
- ✅ Retry logic with exponential backoff
- ✅ Dead letter queue
- ✅ Recovery metrics dashboard

**Success Criteria:**
- 80%+ webhook recovery rate
- DLQ remains <50 items/day

### Phase 3: Dispute Workflow (Weeks 7-9)
**Goal:** Enable human approval with full context

**Deliverables:**
- ✅ Dispute routing logic
- ✅ Escalation flow (3-attempt pattern)
- ✅ Approval API endpoints
- ✅ Dashboard UI (React)
- ✅ Email/Slack notifications

**Success Criteria:**
- Finance team can review/approve disputes in <2 minutes each
- Approval workflow handles 100+ disputes/day

### Phase 4: Refund Execution (Weeks 10-11)
**Goal:** Execute approved refunds safely

**Deliverables:**
- ✅ Idempotency-safe refund execution
- ✅ Two-factor approval
- ✅ Refund audit trail
- ✅ Customer notification system

**Success Criteria:**
- Zero duplicate refunds
- 100% audit trail coverage
- Refunds execute within 5 minutes of approval

### Phase 5: Production Hardening (Week 12)
**Goal:** Make system production-ready

**Deliverables:**
- ✅ Full observability (Prometheus + Grafana)
- ✅ Alerting rules
- ✅ Error handling & retries
- ✅ Load testing (10K transactions/hour)
- ✅ Documentation (API docs, runbooks)

**Success Criteria:**
- System handles 3x expected load
- All failure scenarios have runbooks
- 99.5% uptime over 1 week

---

## 11. Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| **Incorrect auto-refund** | High (financial loss) | Low | No auto-refunds in MVP; human approval required |
| **API rate limits** | Medium (delayed reconciliation) | Medium | Exponential backoff, batch requests, caching |
| **Database performance** | Medium (slow reconciliation) | Medium | Indexing, connection pooling, query optimization |
| **Webhook replay attacks** | High (duplicate processing) | Low | Idempotency keys, signature verification |
| **Bank CSV format changes** | High (ingestion failure) | Medium | Validation layer, alerts on parse errors, graceful degradation |
| **Regulatory non-compliance** | High (legal risk) | Low | Immutable audit logs, PII masking, 7-year retention |
| **False positive refunds** | Medium (customer confusion) | Medium | Confidence scores visible, manual review, reason codes |

---

## 12. Competitive Positioning

### 12.1 Why This Project Stands Out

**Most fintech portfolio projects:**
- ❌ "Payment gateway integration demo"
- ❌ "Wallet app with Paystack"
- ❌ "E-commerce checkout flow"

**This project demonstrates:**
- ✅ **Reconciliation** (real operational pain for fintechs)
- ✅ **Webhook recovery** (critical infrastructure problem)
- ✅ **Dispute triage** (decision support, not reckless automation)
- ✅ **Auditability** (regulatory awareness)
- ✅ **Nigerian context** (local domain expertise)
- ✅ **Production thinking** (failure scenarios, observability, idempotency)

### 12.2 What Real Fintechs Build Internally

This system mirrors internal tools at:
- **Paystack:** Reconciliation engine for merchant settlements
- **Flutterwave:** Dispute management system
- **Moniepoint:** Transaction monitoring & recovery
- **Banks:** NIBSS settlement reconciliation
- **Switching companies:** Inter-bank transaction matching

---

## 13. Success Metrics (12 Months Post-Launch)

| Metric | Baseline (Manual) | Target (Automated) | Impact |
|--------|-------------------|-------------------|--------|
| **Reconciliation Time** | 15 hours/week | 4 hours/week | 73% reduction |
| **Dispute Resolution Time** | 48 hours avg | 6 hours avg | 87% faster |
| **Webhook Recovery Rate** | 0% (manual) | 80%+ | New capability |
| **False Positive Rate** | N/A | <10% | High accuracy |
| **Operational Cost Savings** | Baseline | ₦2.5M/month saved | ROI: 8 months |
| **Customer Satisfaction** | 3.2/5 (payment issues) | 4.5/5 | +40% improvement |

---

## 14. Deliverables

### 14.1 MVP Scope

✅ **Backend Services (Java Spring Boot):**
- Transaction Ingestor Service
- Reconciliation Rules Engine
- Webhook Recovery Service
- Dispute Workflow Service
- Dashboard API

✅ **Database (PostgreSQL):**
- Normalized schema with audit trails
- Idempotency guarantees
- 7-year retention policy

✅ **Message Queue (RabbitMQ):**
- Reconciliation queue
- Dispute queue
- Webhook retry queue
- Dead letter queue

✅ **API Documentation (Swagger):**
- All endpoints documented
- Request/response examples
- Authentication flows

✅ **Dashboard UI (React):**
- Dispute review interface
- Approval workflow
- Reconciliation reports
- Audit log viewer

✅ **Observability:**
- Prometheus metrics
- Grafana dashboards
- Alerting rules
- Log aggregation (ELK)

### 14.2 Documentation

- System architecture diagram
- API reference (OpenAPI spec)
- Database schema documentation
- Runbooks for common scenarios
- Deployment guide
- Testing strategy

---

## 15. One-Sentence Summary (For Resume)

**Built automated reconciliation engine comparing Payment Providers, internal ledger, and bank settlement data with confidence-scored dispute triage, webhook recovery, and auditable approval workflows—reducing manual reconciliation time by 73% and enabling 80%+ webhook recovery rate.**

---

## Appendix A: Sample API Contracts

### Create Reconciliation Run
```http
POST /api/reconciliations/run
Content-Type: application/json

{
  "source": "paystack",
  "date_range": {
    "start": "2024-11-01T00:00:00Z",
    "end": "2024-11-22T23:59:59Z"
  }
}

Response 202 Accepted:
{
  "run_id": "run_abc123",
  "status": "processing",
  "estimated_completion": "2024-11-22T10:15:00Z"
}
```

### Get Disputes Awaiting Review
```http
GET /api/disputes?status=awaiting_review&sort=confidence:desc&limit=50

Response 200 OK:
{
  "disputes": [
    {
      "id": "disp_xyz789",
      "transaction_ref": "PSK_abc123",
      "confidence_score": 95,
      "discrepancy_type": "double_debit",
      "amount": 5000.00,
      "customer_email": "us***@example.com",
      "created_at": "2024-11-22T08:30:00Z",
      "rules_fired": [
        {"rule": "duplicate_reference", "weight": 40},
        {"rule": "amount_match", "weight": 30}
      ]
    }
  ],
  "total": 12,
  "page": 1
}
```

### Approve Refund
```http
POST /api/disputes/disp_xyz789/approve
Content-Type: application/json
Authorization: Bearer {token}

{
  "otp_code": "123456",
  "reason": "Confirmed duplicate debit after verifying bank settlement",
  "refund_amount": 5000.00
}

Response 200 OK:
{
  "dispute_id": "disp_xyz789",
  "status": "approved",
  "refund_id": "ref_def456",
  "estimated_completion": "2024-11-22T09:00:00Z"
}
```

---

## Appendix B: Confidence Scoring Examples

### Example 1: High Confidence (95)
```
Transaction: PSK_abc123
Ledger: 2 debits of ₦5,000 (same user, same timestamp)
Provider: 1 charge of ₦5,000 (success)
Bank: 1 settlement of ₦5,000

Rules Fired:
+ duplicate_reference: +40
+ amount_match: +30
+ single_settlement: +20
+ timestamp_proximity: +5

Score: 95 → HIGH_CONFIDENCE
Action: Flag for refund approval
```

### Example 2: Medium Confidence (78)
```
Transaction: PSK_def456
Ledger: Debit ₦3,000 (status: pending)
Provider: Charge ₦3,000 (status: success)
Bank: No settlement yet (T+1 not reached)

Rules Fired:
+ status_mismatch: +35
+ amount_match: +30
+ webhook_missing: +10
- settlement_pending: -10 (within window)

Score: 78 → MEDIUM_CONFIDENCE
Action: Wait 24h for settlement, then re-evaluate
```

### Example 3: Low Confidence (45)
```
Transaction: PSK_ghi789
Ledger: Debit ₦1,000
Provider: No matching record
Bank: Settlement ₦1,000

Rules Fired:
+ settlement_exists: +30
+ amount_match: +20
- provider_missing: -15 (unusual)
- small_amount: -10 (low priority)

Score: 45 → LOW_CONFIDENCE
Action: Monitor only, don't escalate
```

---

**END OF PRD**