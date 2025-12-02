-- Add user_id column to transactions_raw table for multi-tenancy
ALTER TABLE transactions_raw ADD COLUMN IF NOT EXISTS user_id UUID;

-- Add user_id column to reconciliations table for multi-tenancy
ALTER TABLE reconciliations ADD COLUMN IF NOT EXISTS user_id UUID;

-- Add foreign key constraints (optional, commented out to avoid issues if users table doesn't have matching IDs)
-- ALTER TABLE transactions_raw ADD CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id);
-- ALTER TABLE reconciliations ADD CONSTRAINT fk_reconciliations_user FOREIGN KEY (user_id) REFERENCES users(id);

-- Create indexes for efficient querying by user_id
CREATE INDEX IF NOT EXISTS idx_transactions_raw_user_id ON transactions_raw(user_id);
CREATE INDEX IF NOT EXISTS idx_reconciliations_user_id ON reconciliations(user_id);
