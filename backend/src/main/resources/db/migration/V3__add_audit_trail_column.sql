-- Add audit_trail column to reconciliations table
ALTER TABLE reconciliations ADD COLUMN IF NOT EXISTS audit_trail JSONB;
