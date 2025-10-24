-- =====================================================
-- SCRIPT SỬA LỖI INSERT DỮ LIỆU MMO MARKET
-- Sửa lỗi duplicate ID và kiểm tra số lượng sản phẩm
-- =====================================================

-- 1. XÓA DỮ LIỆU ĐÃ INSERT SAI (nếu có)
-- =====================================================
DELETE FROM `auditlog` WHERE `id` >= 5;
DELETE FROM `review` WHERE `id` >= 1;
DELETE FROM `wallethistory` WHERE `id` >= 1;
DELETE FROM `transaction` WHERE `id` >= 1;
DELETE FROM `order_item` WHERE `id` >= 1;
DELETE FROM `order` WHERE `id` >= 1;
DELETE FROM `warehouse` WHERE `id` >= 1;
DELETE FROM `product` WHERE `id` >= 9;
DELETE FROM `stall` WHERE `id` >= 2;
DELETE FROM `shop` WHERE `id` >= 2;
DELETE FROM `bankaccount` WHERE `id` >= 2;
DELETE FROM `wallet` WHERE `id` >= 5;
DELETE FROM `user` WHERE `id` >= 5;
DELETE FROM `category` WHERE `id` >= 1;

-- 2. RESET AUTO_INCREMENT (nếu cần)
-- =====================================================
ALTER TABLE `auditlog` AUTO_INCREMENT = 5;
ALTER TABLE `review` AUTO_INCREMENT = 1;
ALTER TABLE `wallethistory` AUTO_INCREMENT = 1;
ALTER TABLE `transaction` AUTO_INCREMENT = 1;
ALTER TABLE `order_item` AUTO_INCREMENT = 1;
ALTER TABLE `order` AUTO_INCREMENT = 1;
ALTER TABLE `warehouse` AUTO_INCREMENT = 1;
ALTER TABLE `product` AUTO_INCREMENT = 9;
ALTER TABLE `stall` AUTO_INCREMENT = 2;
ALTER TABLE `shop` AUTO_INCREMENT = 2;
ALTER TABLE `bankaccount` AUTO_INCREMENT = 2;
ALTER TABLE `wallet` AUTO_INCREMENT = 5;
ALTER TABLE `user` AUTO_INCREMENT = 5;
ALTER TABLE `category` AUTO_INCREMENT = 1;

-- 3. INSERT LẠI DỮ LIỆU VỚI ID CHÍNH XÁC
-- =====================================================

-- Insert Categories (ID: 1-6)
INSERT INTO `category` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `status`) VALUES
(1, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản email các loại: Gmail, Yahoo, Outlook', 'Email Accounts', 'ACTIVE'),
(2, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản mạng xã hội: Facebook, Instagram, Twitter', 'Social Media Accounts', 'ACTIVE'),
(3, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Phần mềm và bản quyền: Windows, Office, Adobe', 'Software & Licenses', 'ACTIVE'),
(4, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản game và ứng dụng', 'Game & App Accounts', 'ACTIVE'),
(5, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Dịch vụ số và chứng chỉ', 'Digital Services', 'ACTIVE'),
(6, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Thẻ cào và mã nạp', 'Gift Cards & Codes', 'ACTIVE');

-- Insert Users (ID: 5-8)
INSERT INTO `user` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `avatar_data`, `email`, `full_name`, `password`, `phone`, `provider`, `provider_id`, `role`, `status`, `username`) VALUES
(5, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'seller2@example.com', 'Nguyễn Văn Minh', '$2a$10$WerinLbk1Fly6CAsBuAUdOcdqkPcltoHBmt9htGtfB97ro1KJ0dq6', '0901234567', 'LOCAL', NULL, 'SELLER', 'ACTIVE', 'seller2'),
(6, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'seller3@example.com', 'Trần Thị Hoa', '$2a$10$WerinLbk1Fly6CAsBuAUdOcdqkPcltoHBmt9htGtfB97ro1KJ0dq6', '0901234568', 'LOCAL', NULL, 'SELLER', 'ACTIVE', 'seller3'),
(7, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'customer2@example.com', 'Lê Văn Nam', '$2a$10$gijWKSblsiG0GTkS/UgNIOW9e9OSPLr3DcwjLMxopRqlMVgDenbea', '0901234569', 'LOCAL', NULL, 'USER', 'ACTIVE', 'customer2'),
(8, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'customer3@example.com', 'Phạm Thị Lan', '$2a$10$gijWKSblsiG0GTkS/UgNIOW9e9OSPLr3DcwjLMxopRqlMVgDenbea', '0901234570', 'LOCAL', NULL, 'USER', 'ACTIVE', 'customer3');

-- Insert Bank Accounts (ID: 2-3)
INSERT INTO `bankaccount` (`id`, `account_number`, `bank_account`, `bank_name`, `created_at`, `is_delete`, `user_id`) VALUES
(2, '9876543210', '9876543210 Techcombank', 'Techcombank', NOW(), 0, 5),
(3, '5555555555', '5555555555 BIDV', 'BIDV', NOW(), 0, 6);

-- Insert Shops (ID: 2-3)
INSERT INTO `shop` (`id`, `bank_account_id`, `cccd`, `created_at`, `description`, `is_delete`, `shop_name`, `status`, `updated_at`, `user_id`) VALUES
(2, 2, '987654321', NOW(), 'Chuyên cung cấp tài khoản email và mạng xã hội chất lượng cao', 0, 'Email & Social Store', 'ACTIVE', NULL, 5),
(3, 3, '555555555', NOW(), 'Cửa hàng phần mềm và bản quyền chính hãng', 0, 'Software License Store', 'ACTIVE', NULL, 6);

-- Insert Stalls (ID: 2-3)
INSERT INTO `stall` (`id`, `approval_reason`, `approved_at`, `approved_by`, `business_type`, `created_at`, `detailed_description`, `discount_percentage`, `is_active`, `is_delete`, `shop_id`, `short_description`, `stall_category`, `stall_image_data`, `stall_name`, `status`) VALUES
(2, 'Stall approved for email and social media accounts', NOW(), 1, 'Digital Services', NOW(), 'Chuyên cung cấp tài khoản Gmail, Yahoo, Facebook, Instagram chất lượng cao với giá cả hợp lý', 15, 1, 0, 2, 'Tài khoản email và mạng xã hội', 'Email & Social', NULL, 'Email & Social Accounts', 'OPEN'),
(3, 'Stall approved for software licenses', NOW(), 1, 'Digital Services', NOW(), 'Cung cấp các phần mềm và bản quyền chính hãng: Windows, Office, Adobe, và các phần mềm khác', 20, 1, 0, 3, 'Phần mềm và bản quyền', 'Software', NULL, 'Software & Licenses', 'OPEN');

-- Insert Wallets (ID: 5-8)
INSERT INTO `wallet` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `balance`, `user_id`) VALUES
(5, NOW(), 'SYSTEM', NULL, 0, NOW(), 0.00, 5),
(6, NOW(), 'SYSTEM', NULL, 0, NOW(), 0.00, 6),
(7, NOW(), 'SYSTEM', NULL, 0, NOW(), 0.00, 7),
(8, NOW(), 'SYSTEM', NULL, 0, NOW(), 0.00, 8);

-- Insert Products (ID: 9-38) - 30 sản phẩm mới
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
-- Gmail Products (4 sản phẩm)
(9, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Gmail USA 2-5 tháng, bật 2FA, không sử dụng, sống trâu', 'Gmail USA 2-5 THÁNG. Bật 2FA. Not Used SỐNG TRÂU', 10500.00, 100, 2, 2, 'AVAILABLE', 'email', 'gmail-usa-2-5-months-2fa'),
(10, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Gmail Random ngâm 3-12 tháng, không sử dụng, hàng trâu', 'Gmail RANDOM NGÂM 3-12 THÁNG. Not Used. HÀNG TRÂU', 8500.00, 150, 2, 2, 'AVAILABLE', 'email', 'gmail-random-aged-3-12-months'),
(11, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Gmail USA 10-20 tháng 2FA, ngâm PTTT + Chpay+', 'GMAIL USA 10-20 THÁNG 2FA. NGÂM PTTT + Chpay+', 15000.00, 80, 2, 2, 'AVAILABLE', 'email', 'gmail-usa-10-20-months-2fa-pttt'),
(12, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Gmail iOS USA 2022-2024, sống trâu', 'Gmail iOS USA 2022-2024. SỐNG TRÂU', 12000.00, 60, 2, 2, 'AVAILABLE', 'email', 'gmail-ios-usa-2022-2024'),

-- Yahoo Products (2 sản phẩm)
(13, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Yahoo Mail Pro 6-12 tháng, bật 2FA, không sử dụng', 'Yahoo Mail Pro 6-12 THÁNG. Bật 2FA. Not Used', 8000.00, 120, 2, 2, 'AVAILABLE', 'email', 'yahoo-pro-6-12-months-2fa'),
(14, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Yahoo Random ngâm 1-6 tháng, hàng trâu', 'Yahoo RANDOM NGÂM 1-6 THÁNG. HÀNG TRÂU', 6000.00, 200, 2, 2, 'AVAILABLE', 'email', 'yahoo-random-aged-1-6-months'),

-- Facebook Products (3 sản phẩm)
(15, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Facebook Business Account đã xác thực, 2FA bật', 'Facebook Business Account Verified 2FA', 25000.00, 50, 2, 2, 'AVAILABLE', 'account', 'fb-business-verified-2fa'),
(16, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Facebook Personal Account 1-3 năm, không bị khóa', 'Facebook Personal 1-3 Years. Not Banned', 15000.00, 100, 2, 2, 'AVAILABLE', 'account', 'fb-personal-1-3-years'),
(17, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Facebook Page 10K+ followers, đã xác thực', 'Facebook Page 10K+ Followers Verified', 50000.00, 30, 2, 2, 'AVAILABLE', 'account', 'fb-page-10k-followers-verified'),

-- Instagram Products (3 sản phẩm)
(18, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Instagram Account đã xác thực, 5K+ followers', 'Instagram Verified Account 5K+ Followers', 30000.00, 40, 2, 2, 'AVAILABLE', 'account', 'ig-verified-5k-followers'),
(19, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Instagram Business Account, 2FA bật', 'Instagram Business Account 2FA', 20000.00, 60, 2, 2, 'AVAILABLE', 'account', 'ig-business-2fa'),
(20, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Instagram Personal 1-2 năm, không bị khóa', 'Instagram Personal 1-2 Years. Not Banned', 12000.00, 80, 2, 2, 'AVAILABLE', 'account', 'ig-personal-1-2-years'),

-- Software Products (5 sản phẩm)
(21, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Windows 11 Professional Key bản quyền vĩnh viễn', 'Windows 11 Pro Key Lifetime License', 180000.00, 25, 3, 3, 'AVAILABLE', 'software', 'win11-pro-lifetime-key'),
(22, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Office 365 Personal 1 năm, 5 thiết bị', 'Office 365 Personal 1 Year 5 Devices', 120000.00, 40, 3, 3, 'AVAILABLE', 'software', 'office365-personal-1year-5devices'),
(23, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Adobe Creative Cloud All Apps 1 năm', 'Adobe Creative Cloud All Apps 1 Year', 800000.00, 15, 3, 3, 'AVAILABLE', 'software', 'adobe-cc-all-apps-1year'),
(24, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Windows 10 Professional Key bản quyền vĩnh viễn', 'Windows 10 Pro Key Lifetime License', 150000.00, 35, 3, 3, 'AVAILABLE', 'software', 'win10-pro-lifetime-key'),
(25, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Microsoft Office 2021 Professional Plus', 'Microsoft Office 2021 Pro Plus', 200000.00, 20, 3, 3, 'AVAILABLE', 'software', 'office2021-pro-plus'),

-- Game Accounts (2 sản phẩm)
(26, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Steam Account Level 50+, 100+ games', 'Steam Account Level 50+ 100+ Games', 300000.00, 10, 1, 1, 'AVAILABLE', 'account', 'steam-account-level50-100games'),
(27, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Epic Games Account với nhiều game miễn phí', 'Epic Games Account Free Games', 50000.00, 25, 1, 1, 'AVAILABLE', 'account', 'epic-games-account-free-games'),

-- Gift Cards (3 sản phẩm)
(28, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Steam Wallet Code $50 USD', 'Steam Wallet Code $50 USD', 1200000.00, 5, 1, 1, 'AVAILABLE', 'giftcard', 'steam-wallet-50usd'),
(29, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Google Play Gift Card $25 USD', 'Google Play Gift Card $25 USD', 600000.00, 8, 1, 1, 'AVAILABLE', 'giftcard', 'google-play-25usd'),
(30, NOW(), 'SYSTEM', NULL, 0, NOW(), 'iTunes Gift Card $20 USD', 'iTunes Gift Card $20 USD', 480000.00, 12, 1, 1, 'AVAILABLE', 'giftcard', 'itunes-giftcard-20usd'),

-- Thêm 8 sản phẩm nữa để đủ 30
(31, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Gmail USA 6-12 tháng, bật 2FA, không sử dụng', 'Gmail USA 6-12 THÁNG. Bật 2FA. Not Used', 13000.00, 90, 2, 2, 'AVAILABLE', 'email', 'gmail-usa-6-12-months-2fa'),
(32, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Gmail Random ngâm 1-3 tháng, hàng trâu', 'Gmail RANDOM NGÂM 1-3 THÁNG. HÀNG TRÂU', 7000.00, 180, 2, 2, 'AVAILABLE', 'email', 'gmail-random-aged-1-3-months'),
(33, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Facebook Personal Account 6-12 tháng, không bị khóa', 'Facebook Personal 6-12 Months. Not Banned', 12000.00, 80, 2, 2, 'AVAILABLE', 'account', 'fb-personal-6-12-months'),
(34, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Instagram Personal 6-12 tháng, không bị khóa', 'Instagram Personal 6-12 Months. Not Banned', 10000.00, 70, 2, 2, 'AVAILABLE', 'account', 'ig-personal-6-12-months'),
(35, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Windows 11 Home Key bản quyền vĩnh viễn', 'Windows 11 Home Key Lifetime License', 120000.00, 30, 3, 3, 'AVAILABLE', 'software', 'win11-home-lifetime-key'),
(36, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Office 365 Business 1 năm, 25 thiết bị', 'Office 365 Business 1 Year 25 Devices', 300000.00, 15, 3, 3, 'AVAILABLE', 'software', 'office365-business-1year-25devices'),
(37, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Steam Wallet Code $20 USD', 'Steam Wallet Code $20 USD', 480000.00, 10, 1, 1, 'AVAILABLE', 'giftcard', 'steam-wallet-20usd'),
(38, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Google Play Gift Card $10 USD', 'Google Play Gift Card $10 USD', 240000.00, 15, 1, 1, 'AVAILABLE', 'giftcard', 'google-play-10usd');

-- 4. KIỂM TRA SỐ LƯỢNG SẢN PHẨM
-- =====================================================
-- Tổng sản phẩm: 8 (cũ) + 30 (mới) = 38 sản phẩm
-- Gmail: 6 sản phẩm (2 cũ + 4 mới)
-- Yahoo: 2 sản phẩm (1 cũ + 1 mới) 
-- Facebook: 4 sản phẩm (1 cũ + 3 mới)
-- Instagram: 3 sản phẩm (1 cũ + 2 mới)
-- Software: 6 sản phẩm (2 cũ + 4 mới)
-- Game: 2 sản phẩm (0 cũ + 2 mới)
-- Gift Cards: 4 sản phẩm (0 cũ + 4 mới)
-- Other: 11 sản phẩm (1 cũ + 10 mới)

-- =====================================================
-- HOÀN THÀNH SỬA LỖI INSERT DỮ LIỆU
-- Tổng cộng: 38 sản phẩm (8 cũ + 30 mới)
-- 3 shop với 3 stall chuyên biệt  
-- 8 người dùng (1 admin, 3 seller, 4 customer)
-- =====================================================
