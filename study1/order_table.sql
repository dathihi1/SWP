-- Tạo bảng order để lưu thông tin giao dịch
CREATE TABLE IF NOT EXISTS `order` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    stall_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(15,2) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    commission_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    commission_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    seller_amount DECIMAL(15,2) NOT NULL,
    status ENUM('PENDING', 'COMPLETED', 'CANCELLED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    order_code VARCHAR(100) UNIQUE,
    notes TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(50),
    deleted_by VARCHAR(50),
    is_delete BIT NOT NULL DEFAULT 0,
    
    -- Foreign key constraints
    CONSTRAINT FK_order_buyer FOREIGN KEY (buyer_id) REFERENCES user(id),
    CONSTRAINT FK_order_seller FOREIGN KEY (seller_id) REFERENCES user(id),
    CONSTRAINT FK_order_shop FOREIGN KEY (shop_id) REFERENCES shop(id),
    CONSTRAINT FK_order_stall FOREIGN KEY (stall_id) REFERENCES stall(id),
    CONSTRAINT FK_order_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT FK_order_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    
    -- Indexes
    INDEX idx_buyer_id (buyer_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_shop_id (shop_id),
    INDEX idx_stall_id (stall_id),
    INDEX idx_product_id (product_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_order_code (order_code)
);
