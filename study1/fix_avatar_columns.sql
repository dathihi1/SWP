-- Fix avatar columns for user table
-- Run this manually if migration fails

USE mmo_market;

-- Check if columns exist first
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'mmo_market' 
AND TABLE_NAME = 'user' 
AND COLUMN_NAME IN ('avatar_data', 'avatar_url');

-- Add columns if they don't exist
ALTER TABLE user 
ADD COLUMN IF NOT EXISTS avatar_data LONGBLOB NULL COMMENT 'Avatar image data',
ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500) NULL COMMENT 'Avatar URL from OAuth provider';

-- Verify columns were added
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'mmo_market' 
AND TABLE_NAME = 'user' 
AND COLUMN_NAME IN ('avatar_data', 'avatar_url');










