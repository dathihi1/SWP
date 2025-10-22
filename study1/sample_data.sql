-- Insert sample data for testing
-- First, insert a user
INSERT INTO user (id, username, password, email, role, is_delete, created_at, created_by) 
VALUES (1, 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'test@example.com', 'USER', false, NOW(), 'system');

-- Insert a bank account
INSERT INTO bankaccount (id, account_number, bank_account, bank_name, created_at, is_delete, user_id)
VALUES (1, '1234567890', 'Test Bank Account', 'Vietcombank', NOW(), false, 1);

-- Insert a shop
INSERT INTO shop (id, bank_account_id, cccd, created_at, is_delete, shop_name, user_id)
VALUES (1, 1, '123456789', NOW(), false, 'Test Shop', 1);

-- Insert a stall
INSERT INTO stall (id, business_type, created_at, detailed_description, discount_percentage, is_delete, shop_id, short_description, stall_category, stall_name, status)
VALUES (1, 'Gaming', NOW(), 'Test stall for gaming products', 10.0, false, 1, 'Gaming stall', 'Gaming', 'Gaming Store', 'ACTIVE');

-- Insert a category
INSERT INTO category (id, name, description, created_at, is_delete, created_by)
VALUES (1, 'Gaming', 'Gaming related products', NOW(), false, 'system');

-- Insert sample products
INSERT INTO product (id, shop_id, stall_id, category_id, type, name, description, price, quantity, unique_key, status, is_delete, created_at, created_by)
VALUES 
(1, 1, 1, 1, 'GAME_ACCOUNT', 'Test Game Account 1', 'This is a test game account for demonstration purposes', 100000.00, 5, 'test-account-1', 'AVAILABLE', false, NOW(), 'system'),
(2, 1, 1, 1, 'GAME_ACCOUNT', 'Test Game Account 2', 'Another test game account for demonstration', 150000.00, 3, 'test-account-2', 'AVAILABLE', false, NOW(), 'system'),
(3, 1, 1, 1, 'GAME_ACCOUNT', 'Test Game Account 3', 'Third test game account', 200000.00, 2, 'test-account-3', 'AVAILABLE', false, NOW(), 'system');

-- Insert product variants for product 1
INSERT INTO product_variant (id, name, price, product_id, status, stock, unique_key, created_at, is_delete, created_by)
VALUES 
(1, 'Basic Package', 100000.00, 1, 'AVAILABLE', 5, 'variant-1-1', NOW(), false, 'system'),
(2, 'Premium Package', 150000.00, 1, 'AVAILABLE', 3, 'variant-1-2', NOW(), false, 'system'),
(3, 'VIP Package', 200000.00, 1, 'AVAILABLE', 2, 'variant-1-3', NOW(), false, 'system');

-- Insert a wallet for the user
INSERT INTO wallet (id, balance, created_at, is_delete, user_id)
VALUES (1, 1000000.00, NOW(), false, 1);
