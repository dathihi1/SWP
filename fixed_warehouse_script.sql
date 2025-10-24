-- =====================================================
-- SCRIPT WAREHOUSE SỬA LỖI - TẤT CẢ SHOP CÓ SẢN PHẨM CON
-- Sửa lỗi trùng tên và thêm sản phẩm con cho Shop 1
-- =====================================================

-- XÓA WAREHOUSE CŨ (nếu có)
-- =====================================================
DELETE FROM `warehouse` WHERE `id` >= 1;

-- RESET AUTO_INCREMENT
-- =====================================================
ALTER TABLE `warehouse` AUTO_INCREMENT = 1;

-- 1. SHOP 1 - SẢN PHẨM CŨ (ID: 1-8) - 8 sản phẩm
-- =====================================================

-- Gmail Premium 2024 (Product ID: 1)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(1, NOW(), NULL, 0, '{"email":"premium_shop1_001@gmail.com","password":"Pass123!","recovery_email":"recovery_shop1_1@gmail.com","phone":"+1234567001","2fa":"enabled","age":"6 months","country":"USA","storage_used":"8.2GB","storage_total":"15GB","created_date":"2024-04-15","premium":"true","tier":"VIP","plan":"Standard","age_months":"6","unique_id":"GMAIL001"}', 'EMAIL', 0, NULL, NULL, 1, 1, 1, 2),
(2, NOW(), NULL, 0, '{"email":"premium_shop1_002@gmail.com","password":"Pass123!","recovery_email":"recovery_shop1_2@gmail.com","phone":"+1234567002","2fa":"enabled","age":"8 months","country":"USA","storage_used":"9.5GB","storage_total":"15GB","created_date":"2024-02-15","premium":"true","tier":"Premium","plan":"Advanced","age_months":"8","unique_id":"GMAIL002"}', 'EMAIL', 0, NULL, NULL, 1, 1, 1, 2),
(3, NOW(), NULL, 0, '{"email":"premium_shop1_003@gmail.com","password":"Pass123!","recovery_email":"recovery_shop1_3@gmail.com","phone":"+1234567003","2fa":"enabled","age":"10 months","country":"USA","storage_used":"11.2GB","storage_total":"15GB","created_date":"2023-12-15","premium":"true","tier":"Elite","plan":"Professional","age_months":"10","unique_id":"GMAIL003"}', 'EMAIL', 0, NULL, NULL, 1, 1, 1, 2);

-- Yahoo Mail Pro (Product ID: 2)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(4, NOW(), NULL, 0, '{"email":"yahoo_pro_shop1_001@yahoo.com","password":"Pass123!","recovery_email":"recovery_shop1_4@yahoo.com","phone":"+1234567004","2fa":"enabled","age":"12 months","country":"USA","storage_used":"15.8GB","storage_total":"1TB","created_date":"2023-10-15","pro":"true","tier":"Pro","plan":"Basic","age_months":"12","unique_id":"YAHOO001"}', 'EMAIL', 0, NULL, NULL, 2, 1, 1, 2),
(5, NOW(), NULL, 0, '{"email":"yahoo_pro_shop1_002@yahoo.com","password":"Pass123!","recovery_email":"recovery_shop1_5@yahoo.com","phone":"+1234567005","2fa":"enabled","age":"15 months","country":"USA","storage_used":"22.1GB","storage_total":"1TB","created_date":"2023-07-15","pro":"true","tier":"VIP","plan":"Plus","age_months":"15","unique_id":"YAHOO002"}', 'EMAIL', 0, NULL, NULL, 2, 1, 1, 2);

-- Windows 11 Pro Key (Product ID: 3)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(6, NOW(), NULL, 0, '{"key":"WIN11-SHOP1-XXXX-YYYY-ZZZZ-AAAA","type":"Windows 11 Pro","activation":"lifetime","region":"global","version":"22H2","support":"extended","edition":"Standard"}', 'KEY', 0, NULL, NULL, 3, 1, 1, 2),
(7, NOW(), NULL, 0, '{"key":"WIN11-SHOP1-BBBB-CCCC-DDDD-EEEE","type":"Windows 11 Pro","activation":"lifetime","region":"global","version":"23H2","support":"extended","edition":"Enterprise"}', 'KEY', 0, NULL, NULL, 3, 1, 1, 2);

-- Office 365 License (Product ID: 4)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(8, NOW(), NULL, 0, '{"key":"OFF365-SHOP1-FFFF-GGGG-HHHH-IIII","type":"Office 365","activation":"1 year","devices":"5","region":"global","apps":"Word,Excel,PowerPoint,Outlook","plan":"Personal"}', 'KEY', 0, NULL, NULL, 4, 1, 1, 2),
(9, NOW(), NULL, 0, '{"key":"OFF365-SHOP1-JJJJ-KKKK-LLLL-MMMM","type":"Office 365","activation":"1 year","devices":"5","region":"global","apps":"Word,Excel,PowerPoint,Outlook","plan":"Business"}', 'KEY', 0, NULL, NULL, 4, 1, 1, 2);

-- Facebook Business Account (Product ID: 5)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(10, NOW(), NULL, 0, '{"email":"fb_biz_shop1_001@gmail.com","password":"Pass123!","phone":"+1234567006","2fa":"enabled","age":"18 months","country":"USA","business_verified":"true","page_count":"3","ad_spend":"$200","tier":"Starter","plan":"Basic","followers":"500"}', 'ACCOUNT', 0, NULL, NULL, 5, 1, 1, 2),
(11, NOW(), NULL, 0, '{"email":"fb_biz_shop1_002@gmail.com","password":"Pass123!","phone":"+1234567007","2fa":"enabled","age":"24 months","country":"USA","business_verified":"true","page_count":"5","ad_spend":"$500","tier":"Professional","plan":"Advanced","followers":"1200"}', 'ACCOUNT', 0, NULL, NULL, 5, 1, 1, 2);

-- Instagram Verified Account (Product ID: 6)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(12, NOW(), NULL, 0, '{"email":"ig_ver_shop1_001@gmail.com","password":"Pass123!","phone":"+1234567008","2fa":"enabled","age":"12 months","country":"USA","username":"verified_tech_shop1","followers":"3500","following":"500","posts":"80","verified":"true","tier":"Verified","plan":"Standard"}', 'ACCOUNT', 0, NULL, NULL, 6, 1, 1, 2),
(13, NOW(), NULL, 0, '{"email":"ig_ver_shop1_002@gmail.com","password":"Pass123!","phone":"+1234567009","2fa":"enabled","age":"15 months","country":"USA","username":"verified_lifestyle_shop1","followers":"4200","following":"600","posts":"120","verified":"true","tier":"Elite","plan":"Premium"}', 'ACCOUNT', 0, NULL, NULL, 6, 1, 1, 2);

-- Domain .com Premium (Product ID: 7)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(14, NOW(), NULL, 0, '{"domain":"techpremium_shop1_2024.com","registrar":"GoDaddy","expiry":"2025-12-31","dns":"enabled","ssl":"included","privacy":"protected","tier":"Premium","plan":"Standard"}', 'KEY', 0, NULL, NULL, 7, 1, 1, 2),
(15, NOW(), NULL, 0, '{"domain":"businesspro_shop1_2024.com","registrar":"Namecheap","expiry":"2025-12-31","dns":"enabled","ssl":"included","privacy":"protected","tier":"Elite","plan":"Business"}', 'KEY', 0, NULL, NULL, 7, 1, 1, 2);

-- SSL Certificate (Product ID: 8)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(16, NOW(), NULL, 0, '{"certificate":"SSL-SHOP1-CERT-001-XXXX-YYYY-ZZZZ","type":"SSL Certificate","validity":"1 year","domain":"example-shop1.com","issuer":"Comodo","encryption":"256-bit","tier":"Standard","plan":"Basic"}', 'KEY', 0, NULL, NULL, 8, 1, 1, 2),
(17, NOW(), NULL, 0, '{"certificate":"SSL-SHOP1-CERT-002-AAAA-BBBB-CCCC","type":"SSL Certificate","validity":"1 year","domain":"example-shop1.org","issuer":"DigiCert","encryption":"256-bit","tier":"Elite","plan":"Advanced"}', 'KEY', 0, NULL, NULL, 8, 1, 1, 2);

-- 2. SHOP 2 - EMAIL & SOCIAL STORE (ID: 9-20) - 12 sản phẩm
-- =====================================================

-- Gmail USA 2-5 THÁNG (Product ID: 9) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(18, NOW(), NULL, 0, '{"email":"gmail_shop2_001@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_1@gmail.com","phone":"+1234567010","2fa":"enabled","age":"3 months","country":"USA","storage_used":"2.1GB","storage_total":"15GB","created_date":"2024-07-15"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(19, NOW(), NULL, 0, '{"email":"gmail_shop2_002@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_2@gmail.com","phone":"+1234567011","2fa":"enabled","age":"4 months","country":"USA","storage_used":"1.8GB","storage_total":"15GB","created_date":"2024-06-15"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(20, NOW(), NULL, 0, '{"email":"gmail_shop2_003@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_3@gmail.com","phone":"+1234567012","2fa":"enabled","age":"5 months","country":"USA","storage_used":"3.2GB","storage_total":"15GB","created_date":"2024-05-15"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5);

-- Gmail RANDOM NGÂM 3-12 THÁNG (Product ID: 10) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(21, NOW(), NULL, 0, '{"email":"gmail_shop2_004@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_4@gmail.com","phone":"+1234567013","2fa":"enabled","age":"6 months","country":"USA","storage_used":"4.1GB","storage_total":"15GB","created_date":"2024-04-15"}', 'EMAIL', 0, NULL, NULL, 10, 2, 2, 5),
(22, NOW(), NULL, 0, '{"email":"gmail_shop2_005@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_5@gmail.com","phone":"+1234567014","2fa":"enabled","age":"8 months","country":"USA","storage_used":"5.2GB","storage_total":"15GB","created_date":"2024-02-15"}', 'EMAIL', 0, NULL, NULL, 10, 2, 2, 5),
(23, NOW(), NULL, 0, '{"email":"gmail_shop2_006@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_6@gmail.com","phone":"+1234567015","2fa":"enabled","age":"10 months","country":"USA","storage_used":"6.8GB","storage_total":"15GB","created_date":"2023-12-15"}', 'EMAIL', 0, NULL, NULL, 10, 2, 2, 5);

-- Gmail USA 10-20 THÁNG (Product ID: 11) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(24, NOW(), NULL, 0, '{"email":"gmail_shop2_007@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_7@gmail.com","phone":"+1234567016","2fa":"enabled","age":"15 months","country":"USA","storage_used":"9.2GB","storage_total":"15GB","created_date":"2023-07-15","pttt":"enabled","chpay":"enabled"}', 'EMAIL', 0, NULL, NULL, 11, 2, 2, 5),
(25, NOW(), NULL, 0, '{"email":"gmail_shop2_008@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_8@gmail.com","phone":"+1234567017","2fa":"enabled","age":"18 months","country":"USA","storage_used":"11.5GB","storage_total":"15GB","created_date":"2023-04-15","pttt":"enabled","chpay":"enabled"}', 'EMAIL', 0, NULL, NULL, 11, 2, 2, 5);

-- Gmail iOS USA 2022-2024 (Product ID: 12) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(26, NOW(), NULL, 0, '{"email":"gmail_shop2_009@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_9@gmail.com","phone":"+1234567018","2fa":"enabled","age":"24 months","country":"USA","storage_used":"12.1GB","storage_total":"15GB","created_date":"2022-10-15","device":"iOS","ios_version":"16.0"}', 'EMAIL', 0, NULL, NULL, 12, 2, 2, 5),
(27, NOW(), NULL, 0, '{"email":"gmail_shop2_010@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_10@gmail.com","phone":"+1234567019","2fa":"enabled","age":"18 months","country":"USA","storage_used":"8.7GB","storage_total":"15GB","created_date":"2023-04-15","device":"iOS","ios_version":"17.0"}', 'EMAIL', 0, NULL, NULL, 12, 2, 2, 5);

-- Yahoo Mail Pro 6-12 THÁNG (Product ID: 13) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(28, NOW(), NULL, 0, '{"email":"yahoo_shop2_001@yahoo.com","password":"Pass123!","recovery_email":"recovery_shop2_11@yahoo.com","phone":"+1234567020","2fa":"enabled","age":"8 months","country":"USA","storage_used":"3.2GB","storage_total":"1TB","created_date":"2024-02-15"}', 'EMAIL', 0, NULL, NULL, 13, 2, 2, 5),
(29, NOW(), NULL, 0, '{"email":"yahoo_shop2_002@yahoo.com","password":"Pass123!","recovery_email":"recovery_shop2_12@yahoo.com","phone":"+1234567021","2fa":"enabled","age":"10 months","country":"USA","storage_used":"4.8GB","storage_total":"1TB","created_date":"2023-12-15"}', 'EMAIL', 0, NULL, NULL, 13, 2, 2, 5);

-- Yahoo RANDOM NGÂM 1-6 THÁNG (Product ID: 14) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(30, NOW(), NULL, 0, '{"email":"yahoo_shop2_003@yahoo.com","password":"Pass123!","recovery_email":"recovery_shop2_13@yahoo.com","phone":"+1234567022","2fa":"enabled","age":"3 months","country":"USA","storage_used":"1.2GB","storage_total":"1TB","created_date":"2024-07-15"}', 'EMAIL', 0, NULL, NULL, 14, 2, 2, 5),
(31, NOW(), NULL, 0, '{"email":"yahoo_shop2_004@yahoo.com","password":"Pass123!","recovery_email":"recovery_shop2_14@yahoo.com","phone":"+1234567023","2fa":"enabled","age":"4 months","country":"USA","storage_used":"2.1GB","storage_total":"1TB","created_date":"2024-06-15"}', 'EMAIL', 0, NULL, NULL, 14, 2, 2, 5);

-- Facebook Business Account Verified 2FA (Product ID: 15) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(32, NOW(), NULL, 0, '{"email":"fb_shop2_001@gmail.com","password":"Pass123!","phone":"+1234567024","2fa":"enabled","age":"24 months","country":"USA","business_verified":"true","page_count":"5","ad_spend":"$500","followers":"1200"}', 'ACCOUNT', 0, NULL, NULL, 15, 2, 2, 5),
(33, NOW(), NULL, 0, '{"email":"fb_shop2_002@gmail.com","password":"Pass123!","phone":"+1234567025","2fa":"enabled","age":"18 months","country":"USA","business_verified":"true","page_count":"3","ad_spend":"$300","followers":"800"}', 'ACCOUNT', 0, NULL, NULL, 15, 2, 2, 5);

-- Facebook Personal 1-3 Years (Product ID: 16) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(34, NOW(), NULL, 0, '{"email":"fb_shop2_003@gmail.com","password":"Pass123!","phone":"+1234567026","2fa":"enabled","age":"18 months","country":"USA","friends_count":"500","posts_count":"120","verified":"false"}', 'ACCOUNT', 0, NULL, NULL, 16, 2, 2, 5),
(35, NOW(), NULL, 0, '{"email":"fb_shop2_004@gmail.com","password":"Pass123!","phone":"+1234567027","2fa":"enabled","age":"24 months","country":"USA","friends_count":"800","posts_count":"200","verified":"false"}', 'ACCOUNT', 0, NULL, NULL, 16, 2, 2, 5);

-- Facebook Page 10K+ Followers (Product ID: 17) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(36, NOW(), NULL, 0, '{"email":"fb_shop2_005@gmail.com","password":"Pass123!","phone":"+1234567028","2fa":"enabled","age":"24 months","country":"USA","page_name":"Tech News Shop2","followers":"15000","engagement":"5.2%","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 17, 2, 2, 5),
(37, NOW(), NULL, 0, '{"email":"fb_shop2_006@gmail.com","password":"Pass123!","phone":"+1234567029","2fa":"enabled","age":"30 months","country":"USA","page_name":"Fashion Trends Shop2","followers":"12000","engagement":"4.8%","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 17, 2, 2, 5);

-- Instagram Verified Account 5K+ Followers (Product ID: 18) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(38, NOW(), NULL, 0, '{"email":"ig_shop2_001@gmail.com","password":"Pass123!","phone":"+1234567030","2fa":"enabled","age":"18 months","country":"USA","username":"tech_lifestyle_shop2","followers":"5200","following":"800","posts":"150","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 18, 2, 2, 5),
(39, NOW(), NULL, 0, '{"email":"ig_shop2_002@gmail.com","password":"Pass123!","phone":"+1234567031","2fa":"enabled","age":"24 months","country":"USA","username":"fashion_daily_shop2","followers":"6800","following":"1200","posts":"200","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 18, 2, 2, 5);

-- Instagram Business Account 2FA (Product ID: 19) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(40, NOW(), NULL, 0, '{"email":"ig_shop2_003@gmail.com","password":"Pass123!","phone":"+1234567032","2fa":"enabled","age":"15 months","country":"USA","username":"business_tech_shop2","followers":"3200","following":"400","posts":"80","business":"true","insights":"enabled"}', 'ACCOUNT', 0, NULL, NULL, 19, 2, 2, 5),
(41, NOW(), NULL, 0, '{"email":"ig_shop2_004@gmail.com","password":"Pass123!","phone":"+1234567033","2fa":"enabled","age":"20 months","country":"USA","username":"marketing_pro_shop2","followers":"4800","following":"600","posts":"120","business":"true","insights":"enabled"}', 'ACCOUNT', 0, NULL, NULL, 19, 2, 2, 5);

-- Instagram Personal 1-2 Years (Product ID: 20) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(42, NOW(), NULL, 0, '{"email":"ig_shop2_005@gmail.com","password":"Pass123!","phone":"+1234567034","2fa":"enabled","age":"18 months","country":"USA","username":"personal_life_shop2","followers":"2500","following":"800","posts":"200","business":"false"}', 'ACCOUNT', 0, NULL, NULL, 20, 2, 2, 5),
(43, NOW(), NULL, 0, '{"email":"ig_shop2_006@gmail.com","password":"Pass123!","phone":"+1234567035","2fa":"enabled","age":"24 months","country":"USA","username":"travel_diary_shop2","followers":"3800","following":"1200","posts":"300","business":"false"}', 'ACCOUNT', 0, NULL, NULL, 20, 2, 2, 5);

-- 3. SHOP 3 - SOFTWARE LICENSE STORE (ID: 21-25) - 5 sản phẩm
-- =====================================================

-- Windows 11 Pro Key Lifetime (Product ID: 21) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(44, NOW(), NULL, 0, '{"key":"WIN11-SHOP3-XXXX-YYYY-ZZZZ","type":"Windows 11 Pro","activation":"lifetime","region":"global","version":"22H2","support":"extended"}', 'KEY', 0, NULL, NULL, 21, 3, 3, 6),
(45, NOW(), NULL, 0, '{"key":"WIN11-SHOP3-AAAA-BBBB-CCCC","type":"Windows 11 Pro","activation":"lifetime","region":"global","version":"23H2","support":"extended"}', 'KEY', 0, NULL, NULL, 21, 3, 3, 6),
(46, NOW(), NULL, 0, '{"key":"WIN11-SHOP3-DDDD-EEEE-FFFF","type":"Windows 11 Pro","activation":"lifetime","region":"global","version":"24H1","support":"extended"}', 'KEY', 0, NULL, NULL, 21, 3, 3, 6);

-- Office 365 Personal 1 Year (Product ID: 22) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(47, NOW(), NULL, 0, '{"key":"OFF365-SHOP3-GGGG-HHHH-IIII","type":"Office 365 Personal","activation":"1 year","devices":"5","region":"global","apps":"Word,Excel,PowerPoint,Outlook,OneNote"}', 'KEY', 0, NULL, NULL, 22, 3, 3, 6),
(48, NOW(), NULL, 0, '{"key":"OFF365-SHOP3-JJJJ-KKKK-LLLL","type":"Office 365 Personal","activation":"1 year","devices":"5","region":"global","apps":"Word,Excel,PowerPoint,Outlook,OneNote"}', 'KEY', 0, NULL, NULL, 22, 3, 3, 6);

-- Adobe Creative Cloud All Apps (Product ID: 23) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(49, NOW(), NULL, 0, '{"key":"ADOBE-SHOP3-MMMM-NNNN-OOOO","type":"Adobe Creative Cloud","activation":"1 year","apps":"Photoshop,Illustrator,InDesign,Premiere,After Effects","storage":"100GB","region":"global"}', 'KEY', 0, NULL, NULL, 23, 3, 3, 6),
(50, NOW(), NULL, 0, '{"key":"ADOBE-SHOP3-PPPP-QQQQ-RRRR","type":"Adobe Creative Cloud","activation":"1 year","apps":"Photoshop,Illustrator,InDesign,Premiere,After Effects","storage":"100GB","region":"global"}', 'KEY', 0, NULL, NULL, 23, 3, 3, 6);

-- Windows 10 Pro Key Lifetime (Product ID: 24) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(51, NOW(), NULL, 0, '{"key":"WIN10-SHOP3-SSSS-TTTT-UUUU","type":"Windows 10 Pro","activation":"lifetime","region":"global","version":"22H2","support":"extended"}', 'KEY', 0, NULL, NULL, 24, 3, 3, 6),
(52, NOW(), NULL, 0, '{"key":"WIN10-SHOP3-VVVV-WWWW-XXXX","type":"Windows 10 Pro","activation":"lifetime","region":"global","version":"21H2","support":"extended"}', 'KEY', 0, NULL, NULL, 24, 3, 3, 6);

-- Office 2021 Pro Plus (Product ID: 25) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(53, NOW(), NULL, 0, '{"key":"OFF21-SHOP3-YYYY-ZZZZ-AAAA","type":"Office 2021 Pro Plus","activation":"lifetime","region":"global","apps":"Word,Excel,PowerPoint,Outlook,Access,Publisher,OneNote"}', 'KEY', 0, NULL, NULL, 25, 3, 3, 6),
(54, NOW(), NULL, 0, '{"key":"OFF21-SHOP3-BBBB-CCCC-DDDD","type":"Office 2021 Pro Plus","activation":"lifetime","region":"global","apps":"Word,Excel,PowerPoint,Outlook,Access,Publisher,OneNote"}', 'KEY', 0, NULL, NULL, 25, 3, 3, 6);

-- 4. SHOP 1 - GAME & GIFT CARDS (ID: 26-30) - 5 sản phẩm
-- =====================================================

-- Steam Account Level 50+ (Product ID: 26) - Shop 1
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(55, NOW(), NULL, 0, '{"email":"steam_shop1_001@gmail.com","password":"Pass123!","username":"GamerPro2024_Shop1","level":"52","games":"125","playtime":"2500 hours","country":"USA","steam_guard":"enabled","tier":"Pro","plan":"Standard"}', 'ACCOUNT', 0, NULL, NULL, 26, 1, 1, 2),
(56, NOW(), NULL, 0, '{"email":"steam_shop1_002@gmail.com","password":"Pass123!","username":"GameMaster99_Shop1","level":"58","games":"150","playtime":"3200 hours","country":"USA","steam_guard":"enabled","tier":"Elite","plan":"Premium"}', 'ACCOUNT', 0, NULL, NULL, 26, 1, 1, 2);

-- Epic Games Account (Product ID: 27) - Shop 1
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(57, NOW(), NULL, 0, '{"email":"epic_shop1_001@gmail.com","password":"Pass123!","username":"EpicGamer2024_Shop1","games":"45","free_games":"38","purchased_games":"7","country":"USA","two_factor":"enabled","tier":"Pro","plan":"Standard"}', 'ACCOUNT', 0, NULL, NULL, 27, 1, 1, 2),
(58, NOW(), NULL, 0, '{"email":"epic_shop1_002@gmail.com","password":"Pass123!","username":"FreeGameCollector_Shop1","games":"52","free_games":"48","purchased_games":"4","country":"USA","two_factor":"enabled","tier":"Elite","plan":"Premium"}', 'ACCOUNT', 0, NULL, NULL, 27, 1, 1, 2);

-- Steam Wallet Code $50 USD (Product ID: 28) - Shop 1
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(59, NOW(), NULL, 0, '{"code":"STEAM-SHOP1-XXXX-YYYY-ZZZZ","amount":"$50 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam","tier":"Premium","plan":"Standard","unique_id":"STEAM001","card_type":"Digital","brand":"Steam"}', 'CARD', 0, NULL, NULL, 28, 1, 1, 2),
(60, NOW(), NULL, 0, '{"code":"STEAM-SHOP1-AAAA-BBBB-CCCC","amount":"$45 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam","tier":"Elite","plan":"Premium","unique_id":"STEAM002","card_type":"Physical","brand":"Steam"}', 'CARD', 0, NULL, NULL, 28, 1, 1, 2);

-- Google Play Gift Card $25 USD (Product ID: 29) - Shop 1
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(61, NOW(), NULL, 0, '{"code":"GPLAY-SHOP1-DDDD-EEEE-FFFF","amount":"$25 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Google Play","tier":"Premium","plan":"Standard","unique_id":"GPLAY001","card_type":"Digital","brand":"Google"}', 'CARD', 0, NULL, NULL, 29, 1, 1, 2),
(62, NOW(), NULL, 0, '{"code":"GPLAY-SHOP1-GGGG-HHHH-IIII","amount":"$20 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Google Play","tier":"Elite","plan":"Premium","unique_id":"GPLAY002","card_type":"Physical","brand":"Google"}', 'CARD', 0, NULL, NULL, 29, 1, 1, 2);

-- iTunes Gift Card $20 USD (Product ID: 30) - Shop 1
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(63, NOW(), NULL, 0, '{"code":"ITUNES-SHOP1-JJJJ-KKKK-LLLL","amount":"$20 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"iTunes","tier":"Premium","plan":"Standard","unique_id":"ITUNES001","card_type":"Digital","brand":"Apple"}', 'CARD', 0, NULL, NULL, 30, 1, 1, 2),
(64, NOW(), NULL, 0, '{"code":"ITUNES-SHOP1-MMMM-NNNN-OOOO","amount":"$15 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"iTunes","tier":"Elite","plan":"Premium","unique_id":"ITUNES002","card_type":"Physical","brand":"Apple"}', 'CARD', 0, NULL, NULL, 30, 1, 1, 2);

-- 5. THÊM SẢN PHẨM MỚI (ID: 31-38) - 8 sản phẩm
-- =====================================================

-- Gmail USA 6-12 THÁNG (Product ID: 31) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(65, NOW(), NULL, 0, '{"email":"gmail_shop2_011@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_15@gmail.com","phone":"+1234567036","2fa":"enabled","age":"8 months","country":"USA","storage_used":"4.2GB","storage_total":"15GB","created_date":"2024-02-15"}', 'EMAIL', 0, NULL, NULL, 31, 2, 2, 5),
(66, NOW(), NULL, 0, '{"email":"gmail_shop2_012@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_16@gmail.com","phone":"+1234567037","2fa":"enabled","age":"10 months","country":"USA","storage_used":"5.8GB","storage_total":"15GB","created_date":"2023-12-15"}', 'EMAIL', 0, NULL, NULL, 31, 2, 2, 5);

-- Gmail RANDOM NGÂM 1-3 THÁNG (Product ID: 32) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(67, NOW(), NULL, 0, '{"email":"gmail_shop2_013@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_17@gmail.com","phone":"+1234567038","2fa":"enabled","age":"2 months","country":"USA","storage_used":"1.5GB","storage_total":"15GB","created_date":"2024-08-15"}', 'EMAIL', 0, NULL, NULL, 32, 2, 2, 5),
(68, NOW(), NULL, 0, '{"email":"gmail_shop2_014@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_18@gmail.com","phone":"+1234567039","2fa":"enabled","age":"3 months","country":"USA","storage_used":"2.1GB","storage_total":"15GB","created_date":"2024-07-15"}', 'EMAIL', 0, NULL, NULL, 32, 2, 2, 5);

-- Facebook Personal 6-12 Months (Product ID: 33) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(69, NOW(), NULL, 0, '{"email":"fb_shop2_007@gmail.com","password":"Pass123!","phone":"+1234567040","2fa":"enabled","age":"8 months","country":"USA","friends_count":"300","posts_count":"80","verified":"false"}', 'ACCOUNT', 0, NULL, NULL, 33, 2, 2, 5),
(70, NOW(), NULL, 0, '{"email":"fb_shop2_008@gmail.com","password":"Pass123!","phone":"+1234567041","2fa":"enabled","age":"10 months","country":"USA","friends_count":"450","posts_count":"120","verified":"false"}', 'ACCOUNT', 0, NULL, NULL, 33, 2, 2, 5);

-- Instagram Personal 6-12 Months (Product ID: 34) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(71, NOW(), NULL, 0, '{"email":"ig_shop2_007@gmail.com","password":"Pass123!","phone":"+1234567042","2fa":"enabled","age":"8 months","country":"USA","username":"lifestyle_blog_shop2","followers":"1800","following":"600","posts":"150","business":"false"}', 'ACCOUNT', 0, NULL, NULL, 34, 2, 2, 5),
(72, NOW(), NULL, 0, '{"email":"ig_shop2_008@gmail.com","password":"Pass123!","phone":"+1234567043","2fa":"enabled","age":"10 months","country":"USA","username":"fitness_journey_shop2","followers":"2200","following":"800","posts":"200","business":"false"}', 'ACCOUNT', 0, NULL, NULL, 34, 2, 2, 5);

-- Windows 11 Home Key (Product ID: 35) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(73, NOW(), NULL, 0, '{"key":"WIN11H-SHOP3-OOOO-PPPP-QQQQ","type":"Windows 11 Home","activation":"lifetime","region":"global","version":"22H2","support":"standard"}', 'KEY', 0, NULL, NULL, 35, 3, 3, 6),
(74, NOW(), NULL, 0, '{"key":"WIN11H-SHOP3-RRRR-SSSS-TTTT","type":"Windows 11 Home","activation":"lifetime","region":"global","version":"23H2","support":"standard"}', 'KEY', 0, NULL, NULL, 35, 3, 3, 6);

-- Office 365 Business 1 Year (Product ID: 36) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(75, NOW(), NULL, 0, '{"key":"OFF365B-SHOP3-UUUU-VVVV-WWWW","type":"Office 365 Business","activation":"1 year","devices":"25","region":"global","apps":"Word,Excel,PowerPoint,Outlook,Teams,SharePoint"}', 'KEY', 0, NULL, NULL, 36, 3, 3, 6),
(76, NOW(), NULL, 0, '{"key":"OFF365B-SHOP3-XXXX-YYYY-ZZZZ","type":"Office 365 Business","activation":"1 year","devices":"25","region":"global","apps":"Word,Excel,PowerPoint,Outlook,Teams,SharePoint"}', 'KEY', 0, NULL, NULL, 36, 3, 3, 6);

-- Steam Wallet Code $20 USD (Product ID: 37) - Shop 1
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(77, NOW(), NULL, 0, '{"code":"STEAM20-SHOP1-AAAA-BBBB-CCCC","amount":"$20 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam","tier":"Premium","plan":"Standard","unique_id":"STEAM20_001","card_type":"Digital","brand":"Steam"}', 'CARD', 0, NULL, NULL, 37, 1, 1, 2),
(78, NOW(), NULL, 0, '{"code":"STEAM20-SHOP1-DDDD-EEEE-FFFF","amount":"$15 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam","tier":"Elite","plan":"Premium","unique_id":"STEAM20_002","card_type":"Physical","brand":"Steam"}', 'CARD', 0, NULL, NULL, 37, 1, 1, 2);

-- Google Play Gift Card $10 USD (Product ID: 38) - Shop 1
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(79, NOW(), NULL, 0, '{"code":"GPLAY10-SHOP1-GGGG-HHHH-IIII","amount":"$10 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Google Play","tier":"Premium","plan":"Standard","unique_id":"GPLAY10_001","card_type":"Digital","brand":"Google"}', 'CARD', 0, NULL, NULL, 38, 1, 1, 2),
(80, NOW(), NULL, 0, '{"code":"GPLAY10-SHOP1-JJJJ-KKKK-LLLL","amount":"$8 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Google Play","tier":"Elite","plan":"Premium","unique_id":"GPLAY10_002","card_type":"Physical","brand":"Google"}', 'CARD', 0, NULL, NULL, 38, 1, 1, 2);

-- =====================================================
-- HOÀN THÀNH WAREHOUSE CHO TẤT CẢ 38 SẢN PHẨM
-- Tổng cộng: 80 warehouse items
-- Shop 1: 20 items (8 sản phẩm cũ + 5 sản phẩm mới)
-- Shop 2: 40 items (12 sản phẩm email & social)
-- Shop 3: 20 items (5 sản phẩm software)
-- Tên sản phẩm con không trùng giữa các shop
-- =====================================================
