-- =====================================================
-- SCRIPT INSERT DỮ LIỆU MMO MARKET CHÍNH THỐNG
-- Tạo hệ thống tạp hóa MMO giống sàn thương mại
-- =====================================================

-- 1. INSERT CATEGORIES (Danh mục sản phẩm MMO)
-- =====================================================
INSERT INTO `category` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `status`) VALUES
(1, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản email các loại: Gmail, Yahoo, Outlook', 'Email Accounts', 'ACTIVE'),
(2, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản mạng xã hội: Facebook, Instagram, Twitter', 'Social Media Accounts', 'ACTIVE'),
(3, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Phần mềm và bản quyền: Windows, Office, Adobe', 'Software & Licenses', 'ACTIVE'),
(4, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản game và ứng dụng', 'Game & App Accounts', 'ACTIVE'),
(5, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Dịch vụ số và chứng chỉ', 'Digital Services', 'ACTIVE'),
(6, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Thẻ cào và mã nạp', 'Gift Cards & Codes', 'ACTIVE');

-- 2. INSERT ADDITIONAL USERS (Thêm người dùng)
-- =====================================================
INSERT INTO `user` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `avatar_data`, `email`, `full_name`, `password`, `phone`, `provider`, `provider_id`, `role`, `status`, `username`) VALUES
(5, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'seller2@example.com', 'Nguyễn Văn Minh', '$2a$10$WerinLbk1Fly6CAsBuAUdOcdqkPcltoHBmt9htGtfB97ro1KJ0dq6', '0901234567', 'LOCAL', NULL, 'SELLER', 'ACTIVE', 'seller2'),
(6, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'seller3@example.com', 'Trần Thị Hoa', '$2a$10$WerinLbk1Fly6CAsBuAUdOcdqkPcltoHBmt9htGtfB97ro1KJ0dq6', '0901234568', 'LOCAL', NULL, 'SELLER', 'ACTIVE', 'seller3'),
(7, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'customer2@example.com', 'Lê Văn Nam', '$2a$10$gijWKSblsiG0GTkS/UgNIOW9e9OSPLr3DcwjLMxopRqlMVgDenbea', '0901234569', 'LOCAL', NULL, 'USER', 'ACTIVE', 'customer2'),
(8, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'customer3@example.com', 'Phạm Thị Lan', '$2a$10$gijWKSblsiG0GTkS/UgNIOW9e9OSPLr3DcwjLMxopRqlMVgDenbea', '0901234570', 'LOCAL', NULL, 'USER', 'ACTIVE', 'customer3');

-- 3. INSERT ADDITIONAL BANK ACCOUNTS
-- =====================================================
INSERT INTO `bankaccount` (`id`, `account_number`, `bank_account`, `bank_name`, `created_at`, `is_delete`, `user_id`) VALUES
(2, '9876543210', '9876543210 Techcombank', 'Techcombank', NOW(), 0, 5),
(3, '5555555555', '5555555555 BIDV', 'BIDV', NOW(), 0, 6);

-- 4. INSERT ADDITIONAL SHOPS
-- =====================================================
INSERT INTO `shop` (`id`, `bank_account_id`, `cccd`, `created_at`, `description`, `is_delete`, `shop_name`, `status`, `updated_at`, `user_id`) VALUES
(2, 2, '987654321', NOW(), 'Chuyên cung cấp tài khoản email và mạng xã hội chất lượng cao', 0, 'Email & Social Store', 'ACTIVE', NULL, 5),
(3, 3, '555555555', NOW(), 'Cửa hàng phần mềm và bản quyền chính hãng', 0, 'Software License Store', 'ACTIVE', NULL, 6);

-- 5. INSERT ADDITIONAL STALLS
-- =====================================================
INSERT INTO `stall` (`id`, `approval_reason`, `approved_at`, `approved_by`, `business_type`, `created_at`, `detailed_description`, `discount_percentage`, `is_active`, `is_delete`, `shop_id`, `short_description`, `stall_category`, `stall_image_data`, `stall_name`, `status`) VALUES
(2, 'Stall approved for email and social media accounts', NOW(), 1, 'Digital Services', NOW(), 'Chuyên cung cấp tài khoản Gmail, Yahoo, Facebook, Instagram chất lượng cao với giá cả hợp lý', 15, 1, 0, 2, 'Tài khoản email và mạng xã hội', 'Email & Social', NULL, 'Email & Social Accounts', 'OPEN'),
(3, 'Stall approved for software licenses', NOW(), 1, 'Digital Services', NOW(), 'Cung cấp các phần mềm và bản quyền chính hãng: Windows, Office, Adobe, và các phần mềm khác', 20, 1, 0, 3, 'Phần mềm và bản quyền', 'Software', NULL, 'Software & Licenses', 'OPEN');

-- 6. INSERT ADDITIONAL WALLETS
-- =====================================================
INSERT INTO `wallet` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `balance`, `user_id`) VALUES
(5, NOW(), 'SYSTEM', NULL, 0, NOW(), 0.00, 5),
(6, NOW(), 'SYSTEM', NULL, 0, NOW(), 0.00, 6),
(7, NOW(), 'SYSTEM', NULL, 0, NOW(), 0.00, 7),
(8, NOW(), 'SYSTEM', NULL, 0, NOW(), 0.00, 8);

-- 7. INSERT MMO PRODUCTS (Sản phẩm MMO chính thống)
-- =====================================================
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
-- Gmail Products (Tài khoản Gmail)
(9, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Gmail USA 2-5 tháng, bật 2FA, không sử dụng, sống trâu', 'Gmail USA 2-5 THÁNG. Bật 2FA. Not Used SỐNG TRÂU', 10500.00, 100, 2, 2, 'AVAILABLE', 'email', 'gmail-usa-2-5-months-2fa'),
(10, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Gmail Random ngâm 3-12 tháng, không sử dụng, hàng trâu', 'Gmail RANDOM NGÂM 3-12 THÁNG. Not Used. HÀNG TRÂU', 8500.00, 150, 2, 2, 'AVAILABLE', 'email', 'gmail-random-aged-3-12-months'),
(11, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Gmail USA 10-20 tháng 2FA, ngâm PTTT + Chpay+', 'GMAIL USA 10-20 THÁNG 2FA. NGÂM PTTT + Chpay+', 15000.00, 80, 2, 2, 'AVAILABLE', 'email', 'gmail-usa-10-20-months-2fa-pttt'),
(12, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Gmail iOS USA 2022-2024, sống trâu', 'Gmail iOS USA 2022-2024. SỐNG TRÂU', 12000.00, 60, 2, 2, 'AVAILABLE', 'email', 'gmail-ios-usa-2022-2024'),

-- Yahoo Products
(13, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Yahoo Mail Pro 6-12 tháng, bật 2FA, không sử dụng', 'Yahoo Mail Pro 6-12 THÁNG. Bật 2FA. Not Used', 8000.00, 120, 2, 2, 'AVAILABLE', 'email', 'yahoo-pro-6-12-months-2fa'),
(14, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Yahoo Random ngâm 1-6 tháng, hàng trâu', 'Yahoo RANDOM NGÂM 1-6 THÁNG. HÀNG TRÂU', 6000.00, 200, 2, 2, 'AVAILABLE', 'email', 'yahoo-random-aged-1-6-months'),

-- Facebook Products
(15, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Facebook Business Account đã xác thực, 2FA bật', 'Facebook Business Account Verified 2FA', 25000.00, 50, 2, 2, 'AVAILABLE', 'account', 'fb-business-verified-2fa'),
(16, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Facebook Personal Account 1-3 năm, không bị khóa', 'Facebook Personal 1-3 Years. Not Banned', 15000.00, 100, 2, 2, 'AVAILABLE', 'account', 'fb-personal-1-3-years'),
(17, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Facebook Page 10K+ followers, đã xác thực', 'Facebook Page 10K+ Followers Verified', 50000.00, 30, 2, 2, 'AVAILABLE', 'account', 'fb-page-10k-followers-verified'),

-- Instagram Products
(18, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Instagram Account đã xác thực, 5K+ followers', 'Instagram Verified Account 5K+ Followers', 30000.00, 40, 2, 2, 'AVAILABLE', 'account', 'ig-verified-5k-followers'),
(19, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Instagram Business Account, 2FA bật', 'Instagram Business Account 2FA', 20000.00, 60, 2, 2, 'AVAILABLE', 'account', 'ig-business-2fa'),
(20, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Instagram Personal 1-2 năm, không bị khóa', 'Instagram Personal 1-2 Years. Not Banned', 12000.00, 80, 2, 2, 'AVAILABLE', 'account', 'ig-personal-1-2-years'),

-- Software Products
(21, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Windows 11 Professional Key bản quyền vĩnh viễn', 'Windows 11 Pro Key Lifetime License', 180000.00, 25, 3, 3, 'AVAILABLE', 'software', 'win11-pro-lifetime-key'),
(22, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Office 365 Personal 1 năm, 5 thiết bị', 'Office 365 Personal 1 Year 5 Devices', 120000.00, 40, 3, 3, 'AVAILABLE', 'software', 'office365-personal-1year-5devices'),
(23, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Adobe Creative Cloud All Apps 1 năm', 'Adobe Creative Cloud All Apps 1 Year', 800000.00, 15, 3, 3, 'AVAILABLE', 'software', 'adobe-cc-all-apps-1year'),
(24, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Windows 10 Professional Key bản quyền vĩnh viễn', 'Windows 10 Pro Key Lifetime License', 150000.00, 35, 3, 3, 'AVAILABLE', 'software', 'win10-pro-lifetime-key'),
(25, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Microsoft Office 2021 Professional Plus', 'Microsoft Office 2021 Pro Plus', 200000.00, 20, 3, 3, 'AVAILABLE', 'software', 'office2021-pro-plus'),

-- Game Accounts
(26, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Steam Account Level 50+, 100+ games', 'Steam Account Level 50+ 100+ Games', 300000.00, 10, 1, 1, 'AVAILABLE', 'account', 'steam-account-level50-100games'),
(27, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Epic Games Account với nhiều game miễn phí', 'Epic Games Account Free Games', 50000.00, 25, 1, 1, 'AVAILABLE', 'account', 'epic-games-account-free-games'),

-- Gift Cards
(28, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Steam Wallet Code $50 USD', 'Steam Wallet Code $50 USD', 1200000.00, 5, 1, 1, 'AVAILABLE', 'giftcard', 'steam-wallet-50usd'),
(29, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Google Play Gift Card $25 USD', 'Google Play Gift Card $25 USD', 600000.00, 8, 1, 1, 'AVAILABLE', 'giftcard', 'google-play-25usd'),
(30, NOW(), 'SYSTEM', NULL, 0, NOW(), 'iTunes Gift Card $20 USD', 'iTunes Gift Card $20 USD', 480000.00, 12, 1, 1, 'AVAILABLE', 'giftcard', 'itunes-giftcard-20usd');

-- 8. INSERT WAREHOUSE ITEMS (Kho hàng)
-- =====================================================
-- Gmail warehouse items
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(1, NOW(), NULL, 0, '{"email":"gmail001@gmail.com","password":"Pass123!","recovery_email":"recovery1@gmail.com","phone":"+1234567890","2fa":"enabled","age":"3 months","country":"USA"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(2, NOW(), NULL, 0, '{"email":"gmail002@gmail.com","password":"Pass123!","recovery_email":"recovery2@gmail.com","phone":"+1234567891","2fa":"enabled","age":"4 months","country":"USA"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(3, NOW(), NULL, 0, '{"email":"gmail003@gmail.com","password":"Pass123!","recovery_email":"recovery3@gmail.com","phone":"+1234567892","2fa":"enabled","age":"5 months","country":"USA"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(4, NOW(), NULL, 0, '{"email":"gmail004@gmail.com","password":"Pass123!","recovery_email":"recovery4@gmail.com","phone":"+1234567893","2fa":"enabled","age":"2 months","country":"USA"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(5, NOW(), NULL, 0, '{"email":"gmail005@gmail.com","password":"Pass123!","recovery_email":"recovery5@gmail.com","phone":"+1234567894","2fa":"enabled","age":"3 months","country":"USA"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5);

-- Yahoo warehouse items
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(6, NOW(), NULL, 0, '{"email":"yahoo001@yahoo.com","password":"Pass123!","recovery_email":"recovery1@yahoo.com","phone":"+1234567895","2fa":"enabled","age":"6 months","country":"USA"}', 'EMAIL', 0, NULL, NULL, 13, 2, 2, 5),
(7, NOW(), NULL, 0, '{"email":"yahoo002@yahoo.com","password":"Pass123!","recovery_email":"recovery2@yahoo.com","phone":"+1234567896","2fa":"enabled","age":"8 months","country":"USA"}', 'EMAIL', 0, NULL, NULL, 13, 2, 2, 5),
(8, NOW(), NULL, 0, '{"email":"yahoo003@yahoo.com","password":"Pass123!","recovery_email":"recovery3@yahoo.com","phone":"+1234567897","2fa":"enabled","age":"10 months","country":"USA"}', 'EMAIL', 0, NULL, NULL, 13, 2, 2, 5);

-- Facebook warehouse items
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(9, NOW(), NULL, 0, '{"email":"fb001@gmail.com","password":"Pass123!","phone":"+1234567898","2fa":"enabled","age":"2 years","country":"USA","business_verified":"true","page_count":"5"}', 'ACCOUNT', 0, NULL, NULL, 15, 2, 2, 5),
(10, NOW(), NULL, 0, '{"email":"fb002@gmail.com","password":"Pass123!","phone":"+1234567899","2fa":"enabled","age":"1.5 years","country":"USA","business_verified":"true","page_count":"3"}', 'ACCOUNT', 0, NULL, NULL, 15, 2, 2, 5);

-- Instagram warehouse items
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(11, NOW(), NULL, 0, '{"email":"ig001@gmail.com","password":"Pass123!","phone":"+1234567900","2fa":"enabled","age":"1 year","country":"USA","followers":"5200","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 18, 2, 2, 5),
(12, NOW(), NULL, 0, '{"email":"ig002@gmail.com","password":"Pass123!","phone":"+1234567901","2fa":"enabled","age":"8 months","country":"USA","followers":"4800","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 18, 2, 2, 5);

-- Software warehouse items (Keys)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(13, NOW(), NULL, 0, '{"key":"XXXXX-XXXXX-XXXXX-XXXXX-XXXXX","type":"Windows 11 Pro","activation":"lifetime","region":"global"}', 'KEY', 0, NULL, NULL, 21, 3, 3, 6),
(14, NOW(), NULL, 0, '{"key":"YYYYY-YYYYY-YYYYY-YYYYY-YYYYY","type":"Windows 11 Pro","activation":"lifetime","region":"global"}', 'KEY', 0, NULL, NULL, 21, 3, 3, 6),
(15, NOW(), NULL, 0, '{"key":"ZZZZZ-ZZZZZ-ZZZZZ-ZZZZZ-ZZZZZ","type":"Windows 11 Pro","activation":"lifetime","region":"global"}', 'KEY', 0, NULL, NULL, 21, 3, 3, 6);

-- 9. INSERT SAMPLE ORDERS (Đơn hàng mẫu)
-- =====================================================
INSERT INTO `order` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `buyer_id`, `notes`, `order_code`, `payment_method`, `seller_id`, `shop_id`, `stall_id`, `status`, `total_amount`, `total_commission_amount`, `total_seller_amount`) VALUES
(1, NOW(), 'SYSTEM', NULL, 0, NOW(), 7, 'Giao hàng nhanh', 'ORD-2025-001', 'WALLET', 5, 2, 2, 'COMPLETED', 10500.00, 1050.00, 9450.00),
(2, NOW(), 'SYSTEM', NULL, 0, NOW(), 8, 'Cần giao hàng trong ngày', 'ORD-2025-002', 'WALLET', 5, 2, 2, 'COMPLETED', 15000.00, 1500.00, 13500.00);

-- 10. INSERT ORDER ITEMS
-- =====================================================
INSERT INTO `order_item` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `commission_amount`, `commission_rate`, `notes`, `order_id`, `product_id`, `quantity`, `seller_amount`, `seller_id`, `status`, `total_amount`, `unit_price`, `warehouse_id`) VALUES
(1, NOW(), 'SYSTEM', NULL, 0, NOW(), 1050.00, 10.00, 'Gmail USA 2-5 tháng', 1, 9, 1, 9450.00, 5, 'COMPLETED', 10500.00, 10500.00, 1),
(2, NOW(), 'SYSTEM', NULL, 0, NOW(), 1500.00, 10.00, 'Gmail USA 10-20 tháng 2FA', 2, 11, 1, 13500.00, 5, 'COMPLETED', 15000.00, 15000.00, 2);

-- 11. INSERT TRANSACTIONS
-- =====================================================
INSERT INTO `transaction` (`id`, `amount`, `buyer_id`, `completed_at`, `created_at`, `delivery_data`, `notes`, `payment_method`, `product_id`, `seller_id`, `shop_id`, `stall_id`, `status`, `transaction_code`, `updated_at`, `warehouse_item_id`) VALUES
(1, 10500.00, 7, NOW(), NOW(), '{"delivery_method":"instant","delivery_time":"immediate","item_delivered":"true"}', 'Gmail account delivered instantly', 'WALLET', 9, 5, 2, 2, 'SUCCESS', 'TXN-2025-001', NOW(), 1),
(2, 15000.00, 8, NOW(), NOW(), '{"delivery_method":"instant","delivery_time":"immediate","item_delivered":"true"}', 'Gmail account delivered instantly', 'WALLET', 11, 5, 2, 2, 'SUCCESS', 'TXN-2025-002', NOW(), 2);

-- 12. INSERT WALLET HISTORY
-- =====================================================
INSERT INTO `wallethistory` (`id`, `amount`, `created_at`, `created_by`, `deleted_by`, `description`, `is_delete`, `reference_id`, `status`, `type`, `updated_at`, `wallet_id`) VALUES
(1, -10500.00, NOW(), 'SYSTEM', NULL, 'Mua Gmail USA 2-5 THÁNG. Bật 2FA. Not Used SỐNG TRÂU', 0, 'TXN-2025-001', 'SUCCESS', 'PURCHASE', NOW(), 7),
(2, 9450.00, NOW(), 'SYSTEM', NULL, 'Bán Gmail USA 2-5 THÁNG. Bật 2FA. Not Used SỐNG TRÂU', 0, 'TXN-2025-001', 'SUCCESS', 'SALE', NOW(), 5),
(3, -15000.00, NOW(), 'SYSTEM', NULL, 'Mua Gmail USA 10-20 THÁNG 2FA. NGÂM PTTT + Chpay+', 0, 'TXN-2025-002', 'SUCCESS', 'PURCHASE', NOW(), 8),
(4, 13500.00, NOW(), 'SYSTEM', NULL, 'Bán Gmail USA 10-20 THÁNG 2FA. NGÂM PTTT + Chpay+', 0, 'TXN-2025-002', 'SUCCESS', 'SALE', NOW(), 5);

-- 13. INSERT REVIEWS
-- =====================================================
INSERT INTO `review` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `buyer_id`, `content`, `is_read`, `order_id`, `product_id`, `rating`, `reply_at`, `reply_content`, `seller_id`, `shop_id`, `stall_id`, `title`) VALUES
(1, NOW(), 'SYSTEM', NULL, 0, NOW(), 7, 'Tài khoản Gmail chất lượng tốt, giao hàng nhanh, seller hỗ trợ nhiệt tình', 0, 1, 9, 5, NULL, NULL, 5, 2, 2, 'Sản phẩm chất lượng cao'),
(2, NOW(), 'SYSTEM', NULL, 0, NOW(), 8, 'Gmail 10-20 tháng rất ổn định, 2FA hoạt động tốt, recommend', 0, 2, 11, 5, NULL, NULL, 5, 2, 2, 'Tài khoản ổn định');

-- 14. INSERT AUDIT LOGS
-- =====================================================
INSERT INTO `auditlog` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `action`, `category`, `details`, `device_info`, `failure_reason`, `ip_address`, `success`, `user_id`) VALUES
(5, NOW(), 'SYSTEM', NULL, 0, NOW(), 'PRODUCT_CREATED', 'ADMIN_ACTION', 'Created new MMO products for marketplace', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', NULL, '127.0.0.1', 1, 1),
(6, NOW(), 'SYSTEM', NULL, 0, NOW(), 'ORDER_COMPLETED', 'USER_ACTION', 'Order ORD-2025-001 completed successfully', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', NULL, '127.0.0.1', 1, 7),
(7, NOW(), 'SYSTEM', NULL, 0, NOW(), 'ORDER_COMPLETED', 'USER_ACTION', 'Order ORD-2025-002 completed successfully', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', NULL, '127.0.0.1', 1, 8);

-- 15. UPDATE WALLET BALANCES
-- =====================================================
UPDATE `wallet` SET `balance` = 9450.00 WHERE `user_id` = 5;
UPDATE `wallet` SET `balance` = 13500.00 WHERE `user_id` = 5;
UPDATE `wallet` SET `balance` = 22950.00 WHERE `user_id` = 5;

-- =====================================================
-- HOÀN THÀNH INSERT DỮ LIỆU MMO MARKET
-- Tổng cộng: 30 sản phẩm MMO đa dạng
-- 3 shop với 3 stall chuyên biệt
-- 8 người dùng (1 admin, 3 seller, 4 customer)
-- Kho hàng với 15 items sẵn sàng giao
-- 2 đơn hàng hoàn thành với review
-- =====================================================
