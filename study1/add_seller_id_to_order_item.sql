-- Migration script to add seller_id column to order_item table
-- This will help with easier seller payment processing

-- Add seller_id column to order_item table
ALTER TABLE order_item ADD COLUMN seller_id BIGINT NOT NULL DEFAULT 0;

-- Update existing order_items with seller_id from warehouse
UPDATE order_item oi 
SET seller_id = (
    SELECT w.user_id 
    FROM warehouse w 
    WHERE w.id = oi.warehouse_id
)
WHERE oi.warehouse_id IS NOT NULL;

-- Add foreign key constraint (optional, can be added later if needed)
-- ALTER TABLE order_item ADD CONSTRAINT fk_order_item_seller 
-- FOREIGN KEY (seller_id) REFERENCES users(id);

-- Add index for better performance
CREATE INDEX idx_order_item_seller_id ON order_item(seller_id);

-- Verify the changes
SELECT 
    oi.id,
    oi.order_id,
    oi.product_id,
    oi.warehouse_id,
    oi.seller_id,
    oi.seller_amount,
    w.user_id as warehouse_user_id
FROM order_item oi
LEFT JOIN warehouse w ON w.id = oi.warehouse_id
LIMIT 10;
