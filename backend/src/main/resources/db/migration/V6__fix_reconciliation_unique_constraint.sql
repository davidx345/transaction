-- Fix unique constraint for multi-tenancy
-- The old constraint (transaction_ref unique globally) prevents different users from having the same transaction reference
-- The new constraint allows same transaction_ref for different users

-- Drop the old unique constraint on transaction_ref alone
ALTER TABLE reconciliations DROP CONSTRAINT IF EXISTS reconciliations_transaction_ref_key;

-- Create a new unique constraint on (transaction_ref, user_id) for multi-tenancy
-- This allows the same transaction reference for different users
CREATE UNIQUE INDEX IF NOT EXISTS idx_reconciliations_ref_user ON reconciliations(transaction_ref, user_id);
