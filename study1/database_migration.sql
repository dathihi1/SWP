-- Database Migration Script for Improved Payment System
-- Thời gian hold: 1 phút (để test)

-- 1. Tạo bảng wallet_hold
CREATE TABLE IF NOT EXISTS wallet_hold (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    order_id VARCHAR(100),
    status ENUM('PENDING', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_by VARCHAR(50) NULL,
    deleted_by VARCHAR(50) NULL,
    is_delete BIT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NULL,
    INDEX idx_user_status (user_id, status),
    INDEX idx_expires (expires_at),
    CONSTRAINT FK_wallet_hold_user FOREIGN KEY (user_id) REFERENCES user(id)
);

-- 2. Cập nhật bảng warehouse với lock fields
ALTER TABLE warehouse 
ADD COLUMN IF NOT EXISTS locked BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS locked_by BIGINT NULL,
ADD COLUMN IF NOT EXISTS locked_at DATETIME(6) NULL;

-- Tạo index cho warehouse locking
CREATE INDEX IF NOT EXISTS idx_warehouse_product_locked ON warehouse (product_id, locked, is_delete);

-- 3. Tạo bảng payment_queue
CREATE TABLE IF NOT EXISTS payment_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    cart_data JSON NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    error_message TEXT NULL,
    created_by VARCHAR(50) NULL,
    deleted_by VARCHAR(50) NULL,
    is_delete BIT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NULL,
    INDEX idx_status_created (status, created_at),
    INDEX idx_user_status (user_id, status),
    CONSTRAINT FK_payment_queue_user FOREIGN KEY (user_id) REFERENCES user(id)
);

-- 4. Cập nhật application.yaml để enable scheduling
-- Thêm vào application.yaml:
-- spring:
--   task:
--     scheduling:
--       enabled: true

-- 5. Test data cho warehouse (nếu cần)
-- INSERT INTO warehouse (item_type, item_data, product_id, shop_id, stall_id, user_id, created_at, is_delete, locked)
-- VALUES 
-- ('ACCOUNT', '{"username": "testuser1", "password": "testpass1", "email": "test1@example.com"}', 1, 1, 1, 1, NOW(), FALSE, FALSE),
-- ('ACCOUNT', '{"username": "testuser2", "password": "testpass2", "email": "test2@example.com"}', 1, 1, 1, 1, NOW(), FALSE, FALSE),
-- ('KEY', '{"key": "ABC123XYZ", "description": "Game key for premium content"}', 2, 1, 1, 1, NOW(), FALSE, FALSE);

-- 6. Cập nhật Redis configuration trong application.yaml
-- spring:
--   redis:
--     host: localhost
--     port: 6379
--     database: 0
--     timeout: 2000ms
--     lettuce:
--       pool:
--         max-active: 8
--         max-idle: 8
--         min-idle: 0

-- 7. Enable async processing trong application.yaml
-- spring:
--   task:
--     execution:
--       pool:
--         core-size: 2
--         max-size: 10
--         queue-capacity: 100
--       thread-name-prefix: "payment-"
--     scheduling:
--       pool:
--         size: 2
--       thread-name-prefix: "scheduler-"
