-- Sample Gmail products and variants for testing
-- This script creates sample Gmail products with variants similar to the image

-- Insert sample Gmail product
INSERT INTO product (shop_id, stall_id, category_id, type, name, description, price, quantity, unique_key, status, is_delete, created_at, updated_at) 
VALUES (1, 1, 1, 'ACCOUNT', 'Gmail USA Accounts', 'Tài khoản Gmail USA chất lượng cao, đảm bảo không trùng lặp', 50000, 100, 'gmail-usa-accounts-001', 'AVAILABLE', false, NOW(), NOW());

-- Get the product ID (assuming it's the last inserted)
SET @gmail_product_id = LAST_INSERT_ID();

-- Insert product variants for Gmail accounts
INSERT INTO product_variant (product_id, name, description, price, stock, status, unique_key, is_delete, created_at, updated_at) VALUES
(@gmail_product_id, 'Gmail USA 2-5 THÁNG. Bật 2FA. Not Used SỐNG TRÂU (ĐỌC MÔ TẢ)', 'Tài khoản Gmail USA từ 2-5 tháng tuổi, đã bật 2FA, chưa sử dụng, sống trâu', 25000, 50, 'AVAILABLE', 'gmail-usa-2-5m-2fa-001', false, NOW(), NOW()),
(@gmail_product_id, 'Gmail RANDOM NGÂM 3-12 THÁNG. Not Used. HÀNG TRÂU', 'Tài khoản Gmail random đã ngâm từ 3-12 tháng, chưa sử dụng, hàng trâu', 30000, 30, 'AVAILABLE', 'gmail-random-3-12m-001', false, NOW(), NOW()),
(@gmail_product_id, 'Gmail USA 1-5 THÁNG. Not Used. SỐNG TRÂU (ĐỌC MÔ TẢ)', 'Tài khoản Gmail USA từ 1-5 tháng tuổi, chưa sử dụng, sống trâu', 20000, 40, 'AVAILABLE', 'gmail-usa-1-5m-001', false, NOW(), NOW()),
(@gmail_product_id, 'GMAIL USA 10-20 THÁNG 2FA. NGÂM PTTT + Chpay+', 'Tài khoản Gmail USA từ 10-20 tháng tuổi, có 2FA, đã ngâm PTTT + Chpay+', 45000, 25, 'AVAILABLE', 'gmail-usa-10-20m-2fa-001', false, NOW(), NOW()),
(@gmail_product_id, 'Gmail IOS USA 2022-2024 Not Used. SỐNG TRÂU (ĐỌC MÔ TẢ)', 'Tài khoản Gmail iOS USA từ 2022-2024, chưa sử dụng, sống trâu', 35000, 35, 'AVAILABLE', 'gmail-ios-usa-2022-2024-001', false, NOW(), NOW());

-- Insert another Gmail product with different variants
INSERT INTO product (shop_id, stall_id, category_id, type, name, description, price, quantity, unique_key, status, is_delete, created_at, updated_at) 
VALUES (1, 1, 1, 'ACCOUNT', 'Gmail Premium Accounts', 'Tài khoản Gmail Premium với nhiều tính năng đặc biệt', 75000, 50, 'gmail-premium-accounts-001', 'AVAILABLE', false, NOW(), NOW());

SET @gmail_premium_id = LAST_INSERT_ID();

INSERT INTO product_variant (product_id, name, description, price, stock, status, unique_key, is_delete, created_at, updated_at) VALUES
(@gmail_premium_id, 'Gmail Premium 6-12 THÁNG. Full Info. SỐNG TRÂU', 'Tài khoản Gmail Premium từ 6-12 tháng, đầy đủ thông tin, sống trâu', 60000, 20, 'AVAILABLE', 'gmail-premium-6-12m-001', false, NOW(), NOW()),
(@gmail_premium_id, 'Gmail Premium 1-3 THÁNG. 2FA Enabled. HÀNG TRÂU', 'Tài khoản Gmail Premium từ 1-3 tháng, đã bật 2FA, hàng trâu', 40000, 30, 'AVAILABLE', 'gmail-premium-1-3m-2fa-001', false, NOW(), NOW());

-- Insert Facebook product variants
INSERT INTO product (shop_id, stall_id, category_id, type, name, description, price, quantity, unique_key, status, is_delete, created_at, updated_at) 
VALUES (1, 1, 1, 'ACCOUNT', 'Facebook Accounts', 'Tài khoản Facebook chất lượng cao', 30000, 80, 'facebook-accounts-001', 'AVAILABLE', false, NOW(), NOW());

SET @facebook_id = LAST_INSERT_ID();

INSERT INTO product_variant (product_id, name, description, price, stock, status, unique_key, is_delete, created_at, updated_at) VALUES
(@facebook_id, 'Facebook USA 3-6 THÁNG. Verified. SỐNG TRÂU', 'Tài khoản Facebook USA từ 3-6 tháng, đã xác thực, sống trâu', 25000, 25, 'AVAILABLE', 'facebook-usa-3-6m-verified-001', false, NOW(), NOW()),
(@facebook_id, 'Facebook Random 1-12 THÁNG. Not Used. HÀNG TRÂU', 'Tài khoản Facebook random từ 1-12 tháng, chưa sử dụng, hàng trâu', 20000, 35, 'AVAILABLE', 'facebook-random-1-12m-001', false, NOW(), NOW()),
(@facebook_id, 'Facebook IOS 2023-2024. Full Info. SỐNG TRÂU', 'Tài khoản Facebook iOS từ 2023-2024, đầy đủ thông tin, sống trâu', 30000, 20, 'AVAILABLE', 'facebook-ios-2023-2024-001', false, NOW(), NOW());
