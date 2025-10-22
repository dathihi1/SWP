-- Fix auditlog table to add category column if it doesn't exist
-- This script is safe to run multiple times

-- Check if category column exists, if not add it
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_NAME = 'auditlog' 
     AND COLUMN_NAME = 'category' 
     AND TABLE_SCHEMA = DATABASE()) = 0,
    'ALTER TABLE auditlog ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT ''USER_ACTION''',
    'SELECT ''Column already exists'' as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Update existing records to have appropriate categories
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

-- Add index for better performance
CREATE INDEX IF NOT EXISTS idx_auditlog_category ON auditlog(category);
CREATE INDEX IF NOT EXISTS idx_auditlog_category_user ON auditlog(category, user_id);
