-- =====================================================
-- SCRIPT THÊM SHOP VÀ SẢN PHẨM THEO LOẠI GIAN HÀNG
-- Dựa trên dropdown: Công nghệ, Email & Social, Phần mềm, Game, Khác
-- =====================================================

-- 1. THÊM USERS CHO CÁC SHOP MỚI
-- =====================================================

-- User cho Email & Social Shop
INSERT INTO `user` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `avatar_data`, `email`, `full_name`, `password`, `phone`, `provider`, `provider_id`, `role`, `status`, `username`) VALUES
(5, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'email_social_seller@example.com', 'Email & Social Seller', '$2a$10$WerinLbk1Fly6CAsBuAUdOcdqkPcltoHBmt9htGtfB97ro1KJ0dq6', '+1234567890', 'LOCAL', NULL, 'SELLER', 'ACTIVE', 'email_social_seller');

-- User cho Software Shop  
INSERT INTO `user` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `avatar_data`, `email`, `full_name`, `password`, `phone`, `provider`, `provider_id`, `role`, `status`, `username`) VALUES
(6, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'software_seller@example.com', 'Software Seller', '$2a$10$WerinLbk1Fly6CAsBuAUdOcdqkPcltoHBmt9htGtfB97ro1KJ0dq6', '+1234567891', 'LOCAL', NULL, 'SELLER', 'ACTIVE', 'software_seller');

-- User cho Game Shop
INSERT INTO `user` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `avatar_data`, `email`, `full_name`, `password`, `phone`, `provider`, `provider_id`, `role`, `status`, `username`) VALUES
(7, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'game_seller@example.com', 'Game Seller', '$2a$10$WerinLbk1Fly6CAsBuAUdOcdqkPcltoHBmt9htGtfB97ro1KJ0dq6', '+1234567892', 'LOCAL', NULL, 'SELLER', 'ACTIVE', 'game_seller');

-- User cho Other Shop
INSERT INTO `user` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `avatar_data`, `email`, `full_name`, `password`, `phone`, `provider`, `provider_id`, `role`, `status`, `username`) VALUES
(8, NOW(), 'SYSTEM', NULL, 0, NOW(), NULL, 'other_seller@example.com', 'Other Seller', '$2a$10$WerinLbk1Fly6CAsBuAUdOcdqkPcltoHBmt9htGtfB97ro1KJ0dq6', '+1234567893', 'LOCAL', NULL, 'SELLER', 'ACTIVE', 'other_seller');

-- 2. THÊM BANK ACCOUNTS CHO CÁC SHOP MỚI
-- =====================================================

-- Bank account cho Email & Social Shop
INSERT INTO `bank_account` (`id`, `account_number`, `bank_name`, `created_at`, `is_delete`, `updated_at`, `user_id`) VALUES
(2, '1234567890123456', 'Vietcombank', NOW(), 0, NOW(), 5);

-- Bank account cho Software Shop
INSERT INTO `bank_account` (`id`, `account_number`, `bank_name`, `created_at`, `is_delete`, `updated_at`, `user_id`) VALUES
(3, '1234567890123457', 'BIDV', NOW(), 0, NOW(), 6);

-- Bank account cho Game Shop
INSERT INTO `bank_account` (`id`, `account_number`, `bank_name`, `created_at`, `is_delete`, `updated_at`, `user_id`) VALUES
(4, '1234567890123458', 'Techcombank', NOW(), 0, NOW(), 7);

-- Bank account cho Other Shop
INSERT INTO `bank_account` (`id`, `account_number`, `bank_name`, `created_at`, `is_delete`, `updated_at`, `user_id`) VALUES
(5, '1234567890123459', 'Agribank', NOW(), 0, NOW(), 8);

-- 3. THÊM CÁC SHOP MỚI
-- =====================================================

-- Email & Social Shop
INSERT INTO `shop` (`id`, `bank_account_id`, `cccd`, `created_at`, `description`, `is_delete`, `shop_name`, `status`, `updated_at`, `user_id`) VALUES
(2, 2, '234567890', NOW(), 'Chuyên cung cấp tài khoản email và mạng xã hội chất lượng cao', 0, 'Email & Social Store', 'ACTIVE', NOW(), 5);

-- Software Shop
INSERT INTO `shop` (`id`, `bank_account_id`, `cccd`, `created_at`, `description`, `is_delete`, `shop_name`, `status`, `updated_at`, `user_id`) VALUES
(3, 3, '345678901', NOW(), 'Cửa hàng phần mềm và bản quyền chính hãng', 0, 'Software License Store', 'ACTIVE', NOW(), 6);

-- Game Shop
INSERT INTO `shop` (`id`, `bank_account_id`, `cccd`, `created_at`, `description`, `is_delete`, `shop_name`, `status`, `updated_at`, `user_id`) VALUES
(4, 4, '456789012', NOW(), 'Gian hàng game và gift card uy tín', 0, 'Game & Gift Store', 'ACTIVE', NOW(), 7);

-- Other Shop
INSERT INTO `shop` (`id`, `bank_account_id`, `cccd`, `created_at`, `description`, `is_delete`, `shop_name`, `status`, `updated_at`, `user_id`) VALUES
(5, 5, '567890123', NOW(), 'Các sản phẩm và dịch vụ khác', 0, 'Other Services Store', 'ACTIVE', NOW(), 8);

-- 4. THÊM CÁC STALL CHO CÁC SHOP MỚI
-- =====================================================

-- Stall cho Email & Social Shop
INSERT INTO `stall` (`id`, `approval_reason`, `approved_at`, `approved_by`, `business_type`, `created_at`, `detailed_description`, `discount_percentage`, `is_active`, `is_delete`, `shop_id`, `short_description`, `stall_category`, `stall_image_data`, `stall_name`, `status`) VALUES
(2, 'Approved for email and social media accounts', NOW(), 1, 'Digital Services', NOW(), 'Chuyên cung cấp tài khoản email và mạng xã hội với chất lượng cao, đảm bảo an toàn và bảo mật', 15, 1, 0, 2, 'Tài khoản email và mạng xã hội chất lượng', 'Email & Social', NULL, 'Email & Social Stall', 'OPEN');

-- Stall cho Software Shop
INSERT INTO `stall` (`id`, `approval_reason`, `approved_at`, `approved_by`, `business_type`, `created_at`, `detailed_description`, `discount_percentage`, `is_active`, `is_delete`, `shop_id`, `short_description`, `stall_category`, `stall_image_data`, `stall_name`, `status`) VALUES
(3, 'Approved for software licenses', NOW(), 1, 'Software', NOW(), 'Cung cấp các phần mềm và bản quyền chính hãng với giá cả hợp lý', 20, 1, 0, 3, 'Phần mềm và bản quyền chính hãng', 'Software', NULL, 'Software License Stall', 'OPEN');

-- Stall cho Game Shop
INSERT INTO `stall` (`id`, `approval_reason`, `approved_at`, `approved_by`, `business_type`, `created_at`, `detailed_description`, `discount_percentage`, `is_active`, `is_delete`, `shop_id`, `short_description`, `stall_category`, `stall_image_data`, `stall_name`, `status`) VALUES
(4, 'Approved for gaming products', NOW(), 1, 'Gaming', NOW(), 'Gian hàng chuyên về game và gift card với nhiều lựa chọn đa dạng', 10, 1, 0, 4, 'Game và gift card đa dạng', 'Game', NULL, 'Game & Gift Stall', 'OPEN');

-- Stall cho Other Shop
INSERT INTO `stall` (`id`, `approval_reason`, `approved_at`, `approved_by`, `business_type`, `created_at`, `detailed_description`, `discount_percentage`, `is_active`, `is_delete`, `shop_id`, `short_description`, `stall_category`, `stall_image_data`, `stall_name`, `status`) VALUES
(5, 'Approved for other services', NOW(), 1, 'Other Services', NOW(), 'Cung cấp các dịch vụ và sản phẩm khác với chất lượng đảm bảo', 5, 1, 0, 5, 'Dịch vụ và sản phẩm khác', 'Other', NULL, 'Other Services Stall', 'OPEN');

-- 5. THÊM SẢN PHẨM CHO EMAIL & SOCIAL SHOP
-- =====================================================

-- Gmail Premium 6-12 tháng
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(9, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản Gmail Premium 6-12 tháng với dung lượng 15GB', 'Gmail Premium 6-12 tháng', 45000.00, 10, 2, 2, 'AVAILABLE', 'email', 'gmail-premium-6-12m');

-- Gmail Random 3-6 tháng
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(10, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản Gmail Random 3-6 tháng', 'Gmail Random 3-6 tháng', 25000.00, 15, 2, 2, 'AVAILABLE', 'email', 'gmail-random-3-6m');

-- Yahoo Mail Pro 6-12 tháng
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(11, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản Yahoo Mail Pro 6-12 tháng', 'Yahoo Mail Pro 6-12 tháng', 35000.00, 8, 2, 2, 'AVAILABLE', 'email', 'yahoo-pro-6-12m');

-- Facebook Business Account
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(12, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản Facebook Business đã xác thực', 'Facebook Business Account', 120000.00, 5, 2, 2, 'AVAILABLE', 'account', 'fb-business-verified');

-- Instagram Business Account
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(13, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản Instagram Business với 2FA', 'Instagram Business Account', 90000.00, 7, 2, 2, 'AVAILABLE', 'account', 'ig-business-2fa');

-- Twitter Verified Account
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(14, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản Twitter đã xác thực', 'Twitter Verified Account', 80000.00, 6, 2, 2, 'AVAILABLE', 'account', 'twitter-verified');

-- 6. THÊM SẢN PHẨM CHO SOFTWARE SHOP
-- =====================================================

-- Windows 11 Pro Key Lifetime
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(15, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Key Windows 11 Pro bản quyền vĩnh viễn', 'Windows 11 Pro Key Lifetime', 250000.00, 20, 3, 3, 'AVAILABLE', 'software', 'win11-pro-lifetime');

-- Office 365 Personal 1 năm
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(16, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Giấy phép Office 365 Personal 1 năm', 'Office 365 Personal 1 Year', 180000.00, 15, 3, 3, 'AVAILABLE', 'software', 'office365-personal-1y');

-- Adobe Creative Cloud All Apps
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(17, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Adobe Creative Cloud All Apps 1 năm', 'Adobe Creative Cloud All Apps', 300000.00, 10, 3, 3, 'AVAILABLE', 'software', 'adobe-cc-all-apps');

-- Windows 10 Pro Key Lifetime
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(18, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Key Windows 10 Pro bản quyền vĩnh viễn', 'Windows 10 Pro Key Lifetime', 200000.00, 25, 3, 3, 'AVAILABLE', 'software', 'win10-pro-lifetime');

-- Office 2021 Pro Plus
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(19, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Office 2021 Pro Plus bản quyền vĩnh viễn', 'Office 2021 Pro Plus', 220000.00, 18, 3, 3, 'AVAILABLE', 'software', 'office2021-pro-plus');

-- 7. THÊM SẢN PHẨM CHO GAME SHOP
-- =====================================================

-- Steam Account Level 50+
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(20, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản Steam Level 50+ với nhiều game', 'Steam Account Level 50+', 150000.00, 8, 4, 4, 'AVAILABLE', 'account', 'steam-account-lvl50');

-- Epic Games Account
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(21, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tài khoản Epic Games với nhiều game miễn phí', 'Epic Games Account', 80000.00, 12, 4, 4, 'AVAILABLE', 'account', 'epic-games-account');

-- Steam Wallet Code $50
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(22, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Steam Wallet Code $50 USD', 'Steam Wallet Code $50', 120000.00, 20, 4, 4, 'AVAILABLE', 'card', 'steam-wallet-50');

-- Steam Wallet Code $20
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(23, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Steam Wallet Code $20 USD', 'Steam Wallet Code $20', 50000.00, 30, 4, 4, 'AVAILABLE', 'card', 'steam-wallet-20');

-- Google Play Gift Card $25
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(24, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Google Play Gift Card $25 USD', 'Google Play Gift Card $25', 60000.00, 25, 4, 4, 'AVAILABLE', 'card', 'google-play-25');

-- iTunes Gift Card $20
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(25, NOW(), 'SYSTEM', NULL, 0, NOW(), 'iTunes Gift Card $20 USD', 'iTunes Gift Card $20', 50000.00, 20, 4, 4, 'AVAILABLE', 'card', 'itunes-gift-20');

-- 8. THÊM SẢN PHẨM CHO OTHER SHOP
-- =====================================================

-- Domain .com Premium
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(26, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Tên miền .com cao cấp với SSL', 'Domain .com Premium', 600000.00, 5, 5, 5, 'AVAILABLE', 'other', 'domain-com-premium');

-- SSL Certificate
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(27, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Chứng chỉ SSL bảo mật 1 năm', 'SSL Certificate', 250000.00, 10, 5, 5, 'AVAILABLE', 'other', 'ssl-certificate');

-- VPS Hosting 1 năm
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(28, NOW(), 'SYSTEM', NULL, 0, NOW(), 'VPS Hosting 1 năm với cấu hình cao', 'VPS Hosting 1 Year', 800000.00, 8, 5, 5, 'AVAILABLE', 'other', 'vps-hosting-1y');

-- Cloud Storage 1TB
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(29, NOW(), 'SYSTEM', NULL, 0, NOW(), 'Cloud Storage 1TB với bảo mật cao', 'Cloud Storage 1TB', 300000.00, 15, 5, 5, 'AVAILABLE', 'other', 'cloud-storage-1tb');

-- VPN Premium 1 năm
INSERT INTO `product` (`id`, `created_at`, `created_by`, `deleted_by`, `is_delete`, `updated_at`, `description`, `name`, `price`, `quantity`, `shop_id`, `stall_id`, `status`, `type`, `unique_key`) VALUES
(30, NOW(), 'SYSTEM', NULL, 0, NOW(), 'VPN Premium 1 năm với tốc độ cao', 'VPN Premium 1 Year', 200000.00, 20, 5, 5, 'AVAILABLE', 'other', 'vpn-premium-1y');

-- =====================================================
-- HOÀN THÀNH THÊM SHOP VÀ SẢN PHẨM THEO LOẠI GIAN HÀNG
-- Tổng cộng: 4 shop mới, 4 stall mới, 22 sản phẩm mới
-- Email & Social: 6 sản phẩm
-- Software: 5 sản phẩm  
-- Game: 6 sản phẩm
-- Other: 5 sản phẩm
-- =====================================================
