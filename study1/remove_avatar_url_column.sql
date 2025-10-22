-- Migration script to remove avatar_url column and use only avatar_data (byte[])
-- This script removes the avatar_url column since we're now using only byte[] storage

-- Step 1: Check if avatar_url column exists
-- SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_NAME = 'user' AND COLUMN_NAME = 'avatar_url';

-- Step 2: Remove avatar_url column (if it exists)
-- Note: This will permanently delete any avatar URLs stored in the database
-- Make sure to backup any important data before running this script

ALTER TABLE user DROP COLUMN IF EXISTS avatar_url;

-- Step 3: Verify the change
-- SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_NAME = 'user' AND COLUMN_NAME IN ('avatar_data', 'avatar_url');

-- Step 4: Add index for avatar_data if needed (optional)
-- CREATE INDEX idx_user_avatar_data ON user (avatar_data(1)) WHERE avatar_data IS NOT NULL;

-- Notes:
-- - avatar_data column should already exist as LONGBLOB
-- - This migration removes the file-based avatar storage approach
-- - All avatars are now stored as byte arrays directly in the database
-- - No more file system dependencies for avatar storage
