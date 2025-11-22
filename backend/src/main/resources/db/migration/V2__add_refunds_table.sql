-- 5.2.4 Idempotency Guarantees & Refund Tracking
CREATE TABLE refunds (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  dispute_id UUID REFERENCES reconciliations(id),
  transaction_ref VARCHAR(255),
  idempotency_key VARCHAR(255) UNIQUE,
  amount DECIMAL(15,2),
  currency VARCHAR(3) DEFAULT 'NGN',
  status VARCHAR(50), -- 'PENDING', 'SUCCESS', 'FAILED'
  provider_reference VARCHAR(255),
  executed_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_refund_idempotency ON refunds(idempotency_key);
