-- Migration script to add category column to auditlog table
-- This script adds a category field to distinguish between different types of audit logs

-- Add category column to auditlog table
ALTER TABLE auditlog
ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'USER_ACTION';

-- Add index for better query performance
CREATE INDEX idx_auditlog_category ON auditlog(category);

-- Add composite index for category and user_id for better filtering
CREATE INDEX idx_auditlog_category_user ON auditlog(category, user_id);

-- Update existing records to have appropriate categories based on their actions
UPDATE auditlog
SET category = 'SECURITY_EVENT'
WHERE action IN ('ACCOUNT_LOCKED', 'ACCOUNT_UNLOCKED', 'LOGIN_FAILED', 'FAILED_LOGIN_ATTEMPT');

UPDATE auditlog
SET category = 'USER_ACTION'
WHERE action IN ('LOGIN', 'LOGOUT', 'PROFILE_UPDATE', 'PASSWORD_CHANGE', 'REGISTER', 'OTP_VERIFY');

UPDATE auditlog
SET category = 'API_CALL'
WHERE action LIKE '%API%' OR action LIKE '%INTERNAL%';

UPDATE auditlog
SET category = 'SYSTEM_EVENT'
WHERE action LIKE '%SYSTEM%' OR action LIKE '%SCHEDULED%' OR action LIKE '%MAINTENANCE%';
