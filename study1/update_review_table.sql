-- SQL migration script to update review table structure
-- This script adds missing columns and renames existing ones to match the new Review model

-- Add new columns
ALTER TABLE review ADD COLUMN order_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE review ADD COLUMN buyer_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE review ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE review ADD COLUMN stall_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE review ADD COLUMN title VARCHAR(255);
ALTER TABLE review ADD COLUMN content TEXT;
ALTER TABLE review ADD COLUMN reply_content TEXT;
ALTER TABLE review ADD COLUMN reply_at DATETIME(6);

-- Rename existing columns
ALTER TABLE review CHANGE COLUMN user_id buyer_id_temp BIGINT;
ALTER TABLE review CHANGE COLUMN comment content_temp TEXT;
ALTER TABLE review CHANGE COLUMN seller_reply reply_content_temp TEXT;

-- Update data (copy from old columns to new columns)
UPDATE review SET buyer_id = buyer_id_temp WHERE buyer_id_temp IS NOT NULL;
UPDATE review SET content = content_temp WHERE content_temp IS NOT NULL;
UPDATE review SET reply_content = reply_content_temp WHERE reply_content_temp IS NOT NULL;

-- Drop old columns
ALTER TABLE review DROP COLUMN buyer_id_temp;
ALTER TABLE review DROP COLUMN content_temp;
ALTER TABLE review DROP COLUMN reply_content_temp;

-- Add foreign key constraints
ALTER TABLE review ADD CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES `order`(id);
ALTER TABLE review ADD CONSTRAINT fk_review_buyer FOREIGN KEY (buyer_id) REFERENCES user(id);
ALTER TABLE review ADD CONSTRAINT fk_review_shop FOREIGN KEY (shop_id) REFERENCES shop(id);
ALTER TABLE review ADD CONSTRAINT fk_review_stall FOREIGN KEY (stall_id) REFERENCES stall(id);

-- Add indexes for better performance
CREATE INDEX idx_review_order_id ON review(order_id);
CREATE INDEX idx_review_buyer_id ON review(buyer_id);
CREATE INDEX idx_review_seller_id ON review(seller_id);
CREATE INDEX idx_review_shop_id ON review(shop_id);
CREATE INDEX idx_review_stall_id ON review(stall_id);
CREATE INDEX idx_review_product_id ON review(product_id);
CREATE INDEX idx_review_rating ON review(rating);

-- Add check constraint for rating (1-5)
ALTER TABLE review ADD CONSTRAINT chk_review_rating CHECK (rating >= 1 AND rating <= 5);

-- Optional: Add comments for documentation
ALTER TABLE review MODIFY COLUMN order_id BIGINT NOT NULL COMMENT 'ID of the order this review is for';
ALTER TABLE review MODIFY COLUMN buyer_id BIGINT NOT NULL COMMENT 'ID of the buyer who wrote the review';
ALTER TABLE review MODIFY COLUMN seller_id BIGINT NOT NULL COMMENT 'ID of the seller being reviewed';
ALTER TABLE review MODIFY COLUMN shop_id BIGINT NOT NULL COMMENT 'ID of the shop being reviewed';
ALTER TABLE review MODIFY COLUMN stall_id BIGINT NOT NULL COMMENT 'ID of the stall being reviewed';
ALTER TABLE review MODIFY COLUMN rating INT NOT NULL COMMENT 'Rating from 1 to 5 stars';
ALTER TABLE review MODIFY COLUMN title VARCHAR(255) COMMENT 'Short title of the review';
ALTER TABLE review MODIFY COLUMN content TEXT COMMENT 'Detailed review content';
ALTER TABLE review MODIFY COLUMN reply_content TEXT COMMENT 'Seller reply to the review';
ALTER TABLE review MODIFY COLUMN reply_at DATETIME(6) COMMENT 'When the seller replied to the review';
