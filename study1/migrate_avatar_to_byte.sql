-- Migration script to migrate avatar storage from file-based to byte array
-- This script helps transition from file storage to database byte storage

-- The avatar_data column already exists in the user table as LONGBLOB
-- This script is for reference and documentation purposes

-- Check current avatar storage status
SELECT 
    id, 
    username, 
    CASE 
        WHEN avatar_data IS NOT NULL AND LENGTH(avatar_data) > 0 THEN 'Byte Array'
        WHEN avatar_url IS NOT NULL AND avatar_url != '' THEN 'File URL'
        ELSE 'No Avatar'
    END as storage_type,
    CASE 
        WHEN avatar_data IS NOT NULL THEN LENGTH(avatar_data)
        ELSE 0
    END as data_size_bytes,
    avatar_url
FROM user 
WHERE is_delete = false
ORDER BY id;

-- Optional: Clear old file URLs if you want to force byte array storage only
-- UPDATE user 
-- SET avatar_url = NULL 
-- WHERE avatar_data IS NOT NULL AND LENGTH(avatar_data) > 0;

-- Add index for better performance when querying avatar data
-- CREATE INDEX idx_user_avatar_data ON user(avatar_data(100));

-- Add comment to document the change
-- ALTER TABLE user MODIFY COLUMN avatar_data LONGBLOB COMMENT 'Avatar image stored as byte array (preferred over file storage)';
