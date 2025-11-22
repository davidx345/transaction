-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 5.1 Transaction Ingestion Service
CREATE TABLE transactions_raw (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  source VARCHAR(50) NOT NULL, -- 'paystack' | 'bank' | 'ledger'
  external_reference VARCHAR(255),
  normalized_reference VARCHAR(255),
  amount DECIMAL(15,2),
  currency VARCHAR(3) DEFAULT 'NGN',
  status VARCHAR(50),
  customer_identifier VARCHAR(255),
  timestamp TIMESTAMP,
  raw_data JSONB,
  ingested_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_normalized_ref ON transactions_raw(normalized_reference);
CREATE INDEX idx_source ON transactions_raw(source);
CREATE INDEX idx_timestamp ON transactions_raw(timestamp);

-- 5.2.4 Idempotency Guarantees & Reconciliation Results
CREATE TABLE reconciliations (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  transaction_ref VARCHAR(255) UNIQUE,
  run_id VARCHAR(255),
  state VARCHAR(50), -- 'MATCHED', 'DISPUTED', etc.
  confidence_score INT,
  rules_fired JSONB,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_transaction_ref ON reconciliations(transaction_ref);

-- 5.3.1 Webhook Tracking
CREATE TABLE webhook_log (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  transaction_ref VARCHAR(255),
  provider VARCHAR(50),
  event_type VARCHAR(100),
  expected_at TIMESTAMP,
  received_at TIMESTAMP,
  status VARCHAR(50), -- 'received' | 'missing' | 'delayed' | 'recovered'
  retry_count INT DEFAULT 0,
  last_retry_at TIMESTAMP,
  payload JSONB
);

CREATE INDEX idx_missing_webhooks ON webhook_log(status) WHERE status = 'missing';

-- 5.5.1 Audit Log Schema
CREATE TABLE audit_log (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  event_id VARCHAR(100) UNIQUE,
  transaction_ref VARCHAR(255),
  event_type VARCHAR(100),
  actor VARCHAR(255),
  action VARCHAR(100),
  confidence_score INT,
  rules_fired JSONB,
  decision_rationale TEXT,
  before_state VARCHAR(50),
  after_state VARCHAR(50),
  metadata JSONB,
  ip_address INET,
  user_agent TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_audit_transaction_ref ON audit_log(transaction_ref);
CREATE INDEX idx_audit_event_type ON audit_log(event_type);
CREATE INDEX idx_audit_created_at ON audit_log(created_at DESC);
