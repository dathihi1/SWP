-- =====================================================
-- SCRIPT HOÀN CHỈNH WAREHOUSE CHO TẤT CẢ SẢN PHẨM
-- Tạo sản phẩm con cho 38 sản phẩm với tên và giá khác nhau
-- =====================================================

-- XÓA WAREHOUSE CŨ (nếu có)
-- =====================================================
DELETE FROM `warehouse` WHERE `id` >= 1;

-- RESET AUTO_INCREMENT
-- =====================================================
ALTER TABLE `warehouse` AUTO_INCREMENT = 1;

-- INSERT WAREHOUSE ITEMS CHO TẤT CẢ 38 SẢN PHẨM
-- =====================================================

-- 1. GMAIL PRODUCTS (ID: 9-12) - 4 sản phẩm, mỗi sản phẩm 5-8 items
-- =====================================================

-- Gmail USA 2-5 THÁNG (Product ID: 9)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(1, NOW(), NULL, 0, '{"email":"gmail001@gmail.com","password":"Pass123!","recovery_email":"recovery1@gmail.com","phone":"+1234567890","2fa":"enabled","age":"3 months","country":"USA","storage_used":"2.1GB","storage_total":"15GB","created_date":"2024-07-15"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(2, NOW(), NULL, 0, '{"email":"gmail002@gmail.com","password":"Pass123!","recovery_email":"recovery2@gmail.com","phone":"+1234567891","2fa":"enabled","age":"4 months","country":"USA","storage_used":"1.8GB","storage_total":"15GB","created_date":"2024-06-15"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(3, NOW(), NULL, 0, '{"email":"gmail003@gmail.com","password":"Pass123!","recovery_email":"recovery3@gmail.com","phone":"+1234567892","2fa":"enabled","age":"5 months","country":"USA","storage_used":"3.2GB","storage_total":"15GB","created_date":"2024-05-15"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(4, NOW(), NULL, 0, '{"email":"gmail004@gmail.com","password":"Pass123!","recovery_email":"recovery4@gmail.com","phone":"+1234567893","2fa":"enabled","age":"2 months","country":"USA","storage_used":"0.9GB","storage_total":"15GB","created_date":"2024-08-15"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(5, NOW(), NULL, 0, '{"email":"gmail005@gmail.com","password":"Pass123!","recovery_email":"recovery5@gmail.com","phone":"+1234567894","2fa":"enabled","age":"3 months","country":"USA","storage_used":"2.5GB","storage_total":"15GB","created_date":"2024-07-20"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5);

-- Gmail RANDOM NGÂM 3-12 THÁNG (Product ID: 10)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(6, NOW(), NULL, 0, '{"email":"gmail006@gmail.com","password":"Pass123!","recovery_email":"recovery6@gmail.com","phone":"+1234567895","2fa":"enabled","age":"6 months","country":"USA","storage_used":"4.1GB","storage_total":"15GB","created_date":"2024-04-15"}', 'EMAIL', 0, NULL, NULL, 10, 2, 2, 5),
(7, NOW(), NULL, 0, '{"email":"gmail007@gmail.com","password":"Pass123!","recovery_email":"recovery7@gmail.com","phone":"+1234567896","2fa":"enabled","age":"8 months","country":"USA","storage_used":"5.2GB","storage_total":"15GB","created_date":"2024-02-15"}', 'EMAIL', 0, NULL, NULL, 10, 2, 2, 5),
(8, NOW(), NULL, 0, '{"email":"gmail008@gmail.com","password":"Pass123!","recovery_email":"recovery8@gmail.com","phone":"+1234567897","2fa":"enabled","age":"10 months","country":"USA","storage_used":"6.8GB","storage_total":"15GB","created_date":"2023-12-15"}', 'EMAIL', 0, NULL, NULL, 10, 2, 2, 5),
(9, NOW(), NULL, 0, '{"email":"gmail009@gmail.com","password":"Pass123!","recovery_email":"recovery9@gmail.com","phone":"+1234567898","2fa":"enabled","age":"12 months","country":"USA","storage_used":"8.1GB","storage_total":"15GB","created_date":"2023-10-15"}', 'EMAIL', 0, NULL, NULL, 10, 2, 2, 5);

-- Gmail USA 10-20 THÁNG (Product ID: 11)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(10, NOW(), NULL, 0, '{"email":"gmail010@gmail.com","password":"Pass123!","recovery_email":"recovery10@gmail.com","phone":"+1234567899","2fa":"enabled","age":"15 months","country":"USA","storage_used":"9.2GB","storage_total":"15GB","created_date":"2023-07-15","pttt":"enabled","chpay":"enabled"}', 'EMAIL', 0, NULL, NULL, 11, 2, 2, 5),
(11, NOW(), NULL, 0, '{"email":"gmail011@gmail.com","password":"Pass123!","recovery_email":"recovery11@gmail.com","phone":"+1234567900","2fa":"enabled","age":"18 months","country":"USA","storage_used":"11.5GB","storage_total":"15GB","created_date":"2023-04-15","pttt":"enabled","chpay":"enabled"}', 'EMAIL', 0, NULL, NULL, 11, 2, 2, 5),
(12, NOW(), NULL, 0, '{"email":"gmail012@gmail.com","password":"Pass123!","recovery_email":"recovery12@gmail.com","phone":"+1234567901","2fa":"enabled","age":"20 months","country":"USA","storage_used":"13.8GB","storage_total":"15GB","created_date":"2023-02-15","pttt":"enabled","chpay":"enabled"}', 'EMAIL', 0, NULL, NULL, 11, 2, 2, 5);

-- Gmail iOS USA 2022-2024 (Product ID: 12)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(13, NOW(), NULL, 0, '{"email":"gmail013@gmail.com","password":"Pass123!","recovery_email":"recovery13@gmail.com","phone":"+1234567902","2fa":"enabled","age":"24 months","country":"USA","storage_used":"12.1GB","storage_total":"15GB","created_date":"2022-10-15","device":"iOS","ios_version":"16.0"}', 'EMAIL', 0, NULL, NULL, 12, 2, 2, 5),
(14, NOW(), NULL, 0, '{"email":"gmail014@gmail.com","password":"Pass123!","recovery_email":"recovery14@gmail.com","phone":"+1234567903","2fa":"enabled","age":"18 months","country":"USA","storage_used":"8.7GB","storage_total":"15GB","created_date":"2023-04-15","device":"iOS","ios_version":"17.0"}', 'EMAIL', 0, NULL, NULL, 12, 2, 2, 5),
(15, NOW(), NULL, 0, '{"email":"gmail015@gmail.com","password":"Pass123!","recovery_email":"recovery15@gmail.com","phone":"+1234567904","2fa":"enabled","age":"12 months","country":"USA","storage_used":"6.3GB","storage_total":"15GB","created_date":"2023-10-15","device":"iOS","ios_version":"17.2"}', 'EMAIL', 0, NULL, NULL, 12, 2, 2, 5);

-- 2. YAHOO PRODUCTS (ID: 13-14) - 2 sản phẩm
-- =====================================================

-- Yahoo Mail Pro 6-12 THÁNG (Product ID: 13)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(16, NOW(), NULL, 0, '{"email":"yahoo001@yahoo.com","password":"Pass123!","recovery_email":"recovery1@yahoo.com","phone":"+1234567905","2fa":"enabled","age":"8 months","country":"USA","storage_used":"3.2GB","storage_total":"1TB","created_date":"2024-02-15"}', 'EMAIL', 0, NULL, NULL, 13, 2, 2, 5),
(17, NOW(), NULL, 0, '{"email":"yahoo002@yahoo.com","password":"Pass123!","recovery_email":"recovery2@yahoo.com","phone":"+1234567906","2fa":"enabled","age":"10 months","country":"USA","storage_used":"4.8GB","storage_total":"1TB","created_date":"2023-12-15"}', 'EMAIL', 0, NULL, NULL, 13, 2, 2, 5),
(18, NOW(), NULL, 0, '{"email":"yahoo003@yahoo.com","password":"Pass123!","recovery_email":"recovery3@yahoo.com","phone":"+1234567907","2fa":"enabled","age":"12 months","country":"USA","storage_used":"6.1GB","storage_total":"1TB","created_date":"2023-10-15"}', 'EMAIL', 0, NULL, NULL, 13, 2, 2, 5);

-- Yahoo RANDOM NGÂM 1-6 THÁNG (Product ID: 14)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(19, NOW(), NULL, 0, '{"email":"yahoo004@yahoo.com","password":"Pass123!","recovery_email":"recovery4@yahoo.com","phone":"+1234567908","2fa":"enabled","age":"3 months","country":"USA","storage_used":"1.2GB","storage_total":"1TB","created_date":"2024-07-15"}', 'EMAIL', 0, NULL, NULL, 14, 2, 2, 5),
(20, NOW(), NULL, 0, '{"email":"yahoo005@yahoo.com","password":"Pass123!","recovery_email":"recovery5@yahoo.com","phone":"+1234567909","2fa":"enabled","age":"4 months","country":"USA","storage_used":"2.1GB","storage_total":"1TB","created_date":"2024-06-15"}', 'EMAIL', 0, NULL, NULL, 14, 2, 2, 5),
(21, NOW(), NULL, 0, '{"email":"yahoo006@yahoo.com","password":"Pass123!","recovery_email":"recovery6@yahoo.com","phone":"+1234567910","2fa":"enabled","age":"6 months","country":"USA","storage_used":"3.5GB","storage_total":"1TB","created_date":"2024-04-15"}', 'EMAIL', 0, NULL, NULL, 14, 2, 2, 5);

-- 3. FACEBOOK PRODUCTS (ID: 15-17) - 3 sản phẩm
-- =====================================================

-- Facebook Business Account Verified 2FA (Product ID: 15)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(22, NOW(), NULL, 0, '{"email":"fb001@gmail.com","password":"Pass123!","phone":"+1234567911","2fa":"enabled","age":"24 months","country":"USA","business_verified":"true","page_count":"5","ad_spend":"$500","followers":"1200"}', 'ACCOUNT', 0, NULL, NULL, 15, 2, 2, 5),
(23, NOW(), NULL, 0, '{"email":"fb002@gmail.com","password":"Pass123!","phone":"+1234567912","2fa":"enabled","age":"18 months","country":"USA","business_verified":"true","page_count":"3","ad_spend":"$300","followers":"800"}', 'ACCOUNT', 0, NULL, NULL, 15, 2, 2, 5),
(24, NOW(), NULL, 0, '{"email":"fb003@gmail.com","password":"Pass123!","phone":"+1234567913","2fa":"enabled","age":"30 months","country":"USA","business_verified":"true","page_count":"8","ad_spend":"$1200","followers":"2500"}', 'ACCOUNT', 0, NULL, NULL, 15, 2, 2, 5);

-- Facebook Personal 1-3 Years (Product ID: 16)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(25, NOW(), NULL, 0, '{"email":"fb004@gmail.com","password":"Pass123!","phone":"+1234567914","2fa":"enabled","age":"18 months","country":"USA","friends_count":"500","posts_count":"120","verified":"false"}', 'ACCOUNT', 0, NULL, NULL, 16, 2, 2, 5),
(26, NOW(), NULL, 0, '{"email":"fb005@gmail.com","password":"Pass123!","phone":"+1234567915","2fa":"enabled","age":"24 months","country":"USA","friends_count":"800","posts_count":"200","verified":"false"}', 'ACCOUNT', 0, NULL, NULL, 16, 2, 2, 5),
(27, NOW(), NULL, 0, '{"email":"fb006@gmail.com","password":"Pass123!","phone":"+1234567916","2fa":"enabled","age":"36 months","country":"USA","friends_count":"1200","posts_count":"350","verified":"false"}', 'ACCOUNT', 0, NULL, NULL, 16, 2, 2, 5);

-- Facebook Page 10K+ Followers (Product ID: 17)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(28, NOW(), NULL, 0, '{"email":"fb007@gmail.com","password":"Pass123!","phone":"+1234567917","2fa":"enabled","age":"24 months","country":"USA","page_name":"Tech News","followers":"15000","engagement":"5.2%","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 17, 2, 2, 5),
(29, NOW(), NULL, 0, '{"email":"fb008@gmail.com","password":"Pass123!","phone":"+1234567918","2fa":"enabled","age":"30 months","country":"USA","page_name":"Fashion Trends","followers":"12000","engagement":"4.8%","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 17, 2, 2, 5),
(30, NOW(), NULL, 0, '{"email":"fb009@gmail.com","password":"Pass123!","phone":"+1234567919","2fa":"enabled","age":"36 months","country":"USA","page_name":"Business Tips","followers":"25000","engagement":"6.1%","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 17, 2, 2, 5);

-- 4. INSTAGRAM PRODUCTS (ID: 18-20) - 3 sản phẩm
-- =====================================================

-- Instagram Verified Account 5K+ Followers (Product ID: 18)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(31, NOW(), NULL, 0, '{"email":"ig001@gmail.com","password":"Pass123!","phone":"+1234567920","2fa":"enabled","age":"18 months","country":"USA","username":"tech_lifestyle","followers":"5200","following":"800","posts":"150","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 18, 2, 2, 5),
(32, NOW(), NULL, 0, '{"email":"ig002@gmail.com","password":"Pass123!","phone":"+1234567921","2fa":"enabled","age":"24 months","country":"USA","username":"fashion_daily","followers":"6800","following":"1200","posts":"200","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 18, 2, 2, 5),
(33, NOW(), NULL, 0, '{"email":"ig003@gmail.com","password":"Pass123!","phone":"+1234567922","2fa":"enabled","age":"12 months","country":"USA","username":"food_lover","followers":"4500","following":"600","posts":"100","verified":"true"}', 'ACCOUNT', 0, NULL, NULL, 18, 2, 2, 5);

-- Instagram Business Account 2FA (Product ID: 19)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(34, NOW(), NULL, 0, '{"email":"ig004@gmail.com","password":"Pass123!","phone":"+1234567923","2fa":"enabled","age":"15 months","country":"USA","username":"business_tech","followers":"3200","following":"400","posts":"80","business":"true","insights":"enabled"}', 'ACCOUNT', 0, NULL, NULL, 19, 2, 2, 5),
(35, NOW(), NULL, 0, '{"email":"ig005@gmail.com","password":"Pass123!","phone":"+1234567924","2fa":"enabled","age":"20 months","country":"USA","username":"marketing_pro","followers":"4800","following":"600","posts":"120","business":"true","insights":"enabled"}', 'ACCOUNT', 0, NULL, NULL, 19, 2, 2, 5);

-- Instagram Personal 1-2 Years (Product ID: 20)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(36, NOW(), NULL, 0, '{"email":"ig006@gmail.com","password":"Pass123!","phone":"+1234567925","2fa":"enabled","age":"18 months","country":"USA","username":"personal_life","followers":"2500","following":"800","posts":"200","business":"false"}', 'ACCOUNT', 0, NULL, NULL, 20, 2, 2, 5),
(37, NOW(), NULL, 0, '{"email":"ig007@gmail.com","password":"Pass123!","phone":"+1234567926","2fa":"enabled","age":"24 months","country":"USA","username":"travel_diary","followers":"3800","following":"1200","posts":"300","business":"false"}', 'ACCOUNT', 0, NULL, NULL, 20, 2, 2, 5);

-- 5. SOFTWARE PRODUCTS (ID: 21-25) - 5 sản phẩm
-- =====================================================

-- Windows 11 Pro Key Lifetime (Product ID: 21)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(38, NOW(), NULL, 0, '{"key":"XXXXX-XXXXX-XXXXX-XXXXX-XXXXX","type":"Windows 11 Pro","activation":"lifetime","region":"global","version":"22H2","support":"extended"}', 'KEY', 0, NULL, NULL, 21, 3, 3, 6),
(39, NOW(), NULL, 0, '{"key":"YYYYY-YYYYY-YYYYY-YYYYY-YYYYY","type":"Windows 11 Pro","activation":"lifetime","region":"global","version":"23H2","support":"extended"}', 'KEY', 0, NULL, NULL, 21, 3, 3, 6),
(40, NOW(), NULL, 0, '{"key":"ZZZZZ-ZZZZZ-ZZZZZ-ZZZZZ-ZZZZZ","type":"Windows 11 Pro","activation":"lifetime","region":"global","version":"24H1","support":"extended"}', 'KEY', 0, NULL, NULL, 21, 3, 3, 6);

-- Office 365 Personal 1 Year (Product ID: 22)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(41, NOW(), NULL, 0, '{"key":"AAAAA-BBBBB-CCCCC-DDDDD-EEEEE","type":"Office 365 Personal","activation":"1 year","devices":"5","region":"global","apps":"Word,Excel,PowerPoint,Outlook,OneNote"}', 'KEY', 0, NULL, NULL, 22, 3, 3, 6),
(42, NOW(), NULL, 0, '{"key":"FFFFF-GGGGG-HHHHH-IIIII-JJJJJ","type":"Office 365 Personal","activation":"1 year","devices":"5","region":"global","apps":"Word,Excel,PowerPoint,Outlook,OneNote"}', 'KEY', 0, NULL, NULL, 22, 3, 3, 6);

-- Adobe Creative Cloud All Apps (Product ID: 23)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(43, NOW(), NULL, 0, '{"key":"KKKKK-LLLLL-MMMMM-NNNNN-OOOOO","type":"Adobe Creative Cloud","activation":"1 year","apps":"Photoshop,Illustrator,InDesign,Premiere,After Effects","storage":"100GB","region":"global"}', 'KEY', 0, NULL, NULL, 23, 3, 3, 6),
(44, NOW(), NULL, 0, '{"key":"PPPPP-QQQQQ-RRRRR-SSSSS-TTTTT","type":"Adobe Creative Cloud","activation":"1 year","apps":"Photoshop,Illustrator,InDesign,Premiere,After Effects","storage":"100GB","region":"global"}', 'KEY', 0, NULL, NULL, 23, 3, 3, 6);

-- Windows 10 Pro Key Lifetime (Product ID: 24)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(45, NOW(), NULL, 0, '{"key":"UUUUU-VVVVV-WWWWW-XXXXX-YYYYY","type":"Windows 10 Pro","activation":"lifetime","region":"global","version":"22H2","support":"extended"}', 'KEY', 0, NULL, NULL, 24, 3, 3, 6),
(46, NOW(), NULL, 0, '{"key":"ZZZZZ-AAAAA-BBBBB-CCCCC-DDDDD","type":"Windows 10 Pro","activation":"lifetime","region":"global","version":"21H2","support":"extended"}', 'KEY', 0, NULL, NULL, 24, 3, 3, 6);

-- Office 2021 Pro Plus (Product ID: 25)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(47, NOW(), NULL, 0, '{"key":"EEEEE-FFFFF-GGGGG-HHHHH-IIIII","type":"Office 2021 Pro Plus","activation":"lifetime","region":"global","apps":"Word,Excel,PowerPoint,Outlook,Access,Publisher,OneNote"}', 'KEY', 0, NULL, NULL, 25, 3, 3, 6),
(48, NOW(), NULL, 0, '{"key":"JJJJJ-KKKKK-LLLLL-MMMMM-NNNNN","type":"Office 2021 Pro Plus","activation":"lifetime","region":"global","apps":"Word,Excel,PowerPoint,Outlook,Access,Publisher,OneNote"}', 'KEY', 0, NULL, NULL, 25, 3, 3, 6);

-- 6. GAME ACCOUNTS (ID: 26-27) - 2 sản phẩm
-- =====================================================

-- Steam Account Level 50+ (Product ID: 26)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(49, NOW(), NULL, 0, '{"email":"steam001@gmail.com","password":"Pass123!","username":"GamerPro2024","level":"52","games":"125","playtime":"2500 hours","country":"USA","steam_guard":"enabled"}', 'ACCOUNT', 0, NULL, NULL, 26, 1, 1, 2),
(50, NOW(), NULL, 0, '{"email":"steam002@gmail.com","password":"Pass123!","username":"GameMaster99","level":"58","games":"150","playtime":"3200 hours","country":"USA","steam_guard":"enabled"}', 'ACCOUNT', 0, NULL, NULL, 26, 1, 1, 2);

-- Epic Games Account (Product ID: 27)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(51, NOW(), NULL, 0, '{"email":"epic001@gmail.com","password":"Pass123!","username":"EpicGamer2024","games":"45","free_games":"38","purchased_games":"7","country":"USA","two_factor":"enabled"}', 'ACCOUNT', 0, NULL, NULL, 27, 1, 1, 2),
(52, NOW(), NULL, 0, '{"email":"epic002@gmail.com","password":"Pass123!","username":"FreeGameCollector","games":"52","free_games":"48","purchased_games":"4","country":"USA","two_factor":"enabled"}', 'ACCOUNT', 0, NULL, NULL, 27, 1, 1, 2);

-- 7. GIFT CARDS (ID: 28-30) - 3 sản phẩm
-- =====================================================

-- Steam Wallet Code $50 USD (Product ID: 28)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(53, NOW(), NULL, 0, '{"code":"STEAM-XXXX-YYYY-ZZZZ","amount":"$50 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam"}', 'CARD', 0, NULL, NULL, 28, 1, 1, 2),
(54, NOW(), NULL, 0, '{"code":"STEAM-AAAA-BBBB-CCCC","amount":"$50 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam"}', 'CARD', 0, NULL, NULL, 28, 1, 1, 2);

-- Google Play Gift Card $25 USD (Product ID: 29)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(55, NOW(), NULL, 0, '{"code":"GPLAY-DDDD-EEEE-FFFF","amount":"$25 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Google Play"}', 'CARD', 0, NULL, NULL, 29, 1, 1, 2),
(56, NOW(), NULL, 0, '{"code":"GPLAY-GGGG-HHHH-IIII","amount":"$25 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Google Play"}', 'CARD', 0, NULL, NULL, 29, 1, 1, 2);

-- iTunes Gift Card $20 USD (Product ID: 30)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(57, NOW(), NULL, 0, '{"code":"ITUNES-JJJJ-KKKK-LLLL","amount":"$20 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"iTunes"}', 'CARD', 0, NULL, NULL, 30, 1, 1, 2),
(58, NOW(), NULL, 0, '{"code":"ITUNES-MMMM-NNNN-OOOO","amount":"$20 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"iTunes"}', 'CARD', 0, NULL, NULL, 30, 1, 1, 2);

-- 8. THÊM SẢN PHẨM MỚI (ID: 31-38) - 8 sản phẩm
-- =====================================================

-- Gmail USA 6-12 THÁNG (Product ID: 31)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(59, NOW(), NULL, 0, '{"email":"gmail016@gmail.com","password":"Pass123!","recovery_email":"recovery16@gmail.com","phone":"+1234567927","2fa":"enabled","age":"8 months","country":"USA","storage_used":"4.2GB","storage_total":"15GB","created_date":"2024-02-15"}', 'EMAIL', 0, NULL, NULL, 31, 2, 2, 5),
(60, NOW(), NULL, 0, '{"email":"gmail017@gmail.com","password":"Pass123!","recovery_email":"recovery17@gmail.com","phone":"+1234567928","2fa":"enabled","age":"10 months","country":"USA","storage_used":"5.8GB","storage_total":"15GB","created_date":"2023-12-15"}', 'EMAIL', 0, NULL, NULL, 31, 2, 2, 5);

-- Gmail RANDOM NGÂM 1-3 THÁNG (Product ID: 32)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(61, NOW(), NULL, 0, '{"email":"gmail018@gmail.com","password":"Pass123!","recovery_email":"recovery18@gmail.com","phone":"+1234567929","2fa":"enabled","age":"2 months","country":"USA","storage_used":"1.5GB","storage_total":"15GB","created_date":"2024-08-15"}', 'EMAIL', 0, NULL, NULL, 32, 2, 2, 5),
(62, NOW(), NULL, 0, '{"email":"gmail019@gmail.com","password":"Pass123!","recovery_email":"recovery19@gmail.com","phone":"+1234567930","2fa":"enabled","age":"3 months","country":"USA","storage_used":"2.1GB","storage_total":"15GB","created_date":"2024-07-15"}', 'EMAIL', 0, NULL, NULL, 32, 2, 2, 5);

-- Facebook Personal 6-12 Months (Product ID: 33)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(63, NOW(), NULL, 0, '{"email":"fb010@gmail.com","password":"Pass123!","phone":"+1234567931","2fa":"enabled","age":"8 months","country":"USA","friends_count":"300","posts_count":"80","verified":"false"}', 'ACCOUNT', 0, NULL, NULL, 33, 2, 2, 5),
(64, NOW(), NULL, 0, '{"email":"fb011@gmail.com","password":"Pass123!","phone":"+1234567932","2fa":"enabled","age":"10 months","country":"USA","friends_count":"450","posts_count":"120","verified":"false"}', 'ACCOUNT', 0, NULL, NULL, 33, 2, 2, 5);

-- Instagram Personal 6-12 Months (Product ID: 34)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(65, NOW(), NULL, 0, '{"email":"ig008@gmail.com","password":"Pass123!","phone":"+1234567933","2fa":"enabled","age":"8 months","country":"USA","username":"lifestyle_blog","followers":"1800","following":"600","posts":"150","business":"false"}', 'ACCOUNT', 0, NULL, NULL, 34, 2, 2, 5),
(66, NOW(), NULL, 0, '{"email":"ig009@gmail.com","password":"Pass123!","phone":"+1234567934","2fa":"enabled","age":"10 months","country":"USA","username":"fitness_journey","followers":"2200","following":"800","posts":"200","business":"false"}', 'ACCOUNT', 0, NULL, NULL, 34, 2, 2, 5);

-- Windows 11 Home Key (Product ID: 35)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(67, NOW(), NULL, 0, '{"key":"OOOOO-PPPPP-QQQQQ-RRRRR-SSSSS","type":"Windows 11 Home","activation":"lifetime","region":"global","version":"22H2","support":"standard"}', 'KEY', 0, NULL, NULL, 35, 3, 3, 6),
(68, NOW(), NULL, 0, '{"key":"TTTTT-UUUUU-VVVVV-WWWWW-XXXXX","type":"Windows 11 Home","activation":"lifetime","region":"global","version":"23H2","support":"standard"}', 'KEY', 0, NULL, NULL, 35, 3, 3, 6);

-- Office 365 Business 1 Year (Product ID: 36)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(69, NOW(), NULL, 0, '{"key":"YYYYY-ZZZZZ-AAAAA-BBBBB-CCCCC","type":"Office 365 Business","activation":"1 year","devices":"25","region":"global","apps":"Word,Excel,PowerPoint,Outlook,Teams,SharePoint"}', 'KEY', 0, NULL, NULL, 36, 3, 3, 6),
(70, NOW(), NULL, 0, '{"key":"DDDDD-EEEEE-FFFFF-GGGGG-HHHHH","type":"Office 365 Business","activation":"1 year","devices":"25","region":"global","apps":"Word,Excel,PowerPoint,Outlook,Teams,SharePoint"}', 'KEY', 0, NULL, NULL, 36, 3, 3, 6);

-- Steam Wallet Code $20 USD (Product ID: 37)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(71, NOW(), NULL, 0, '{"code":"STEAM-IIII-JJJJ-KKKK-LLLL","amount":"$20 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam"}', 'CARD', 0, NULL, NULL, 37, 1, 1, 2),
(72, NOW(), NULL, 0, '{"code":"STEAM-MMMM-NNNN-OOOO-PPPP","amount":"$20 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam"}', 'CARD', 0, NULL, NULL, 37, 1, 1, 2);

-- Google Play Gift Card $10 USD (Product ID: 38)
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(73, NOW(), NULL, 0, '{"code":"GPLAY-QQQQ-RRRR-SSSS-TTTT","amount":"$10 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Google Play"}', 'CARD', 0, NULL, NULL, 38, 1, 1, 2),
(74, NOW(), NULL, 0, '{"code":"GPLAY-UUUU-VVVV-WWWW-XXXX","amount":"$10 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Google Play"}', 'CARD', 0, NULL, NULL, 38, 1, 1, 2);

-- =====================================================
-- HOÀN THÀNH WAREHOUSE CHO TẤT CẢ 38 SẢN PHẨM
-- Tổng cộng: 74 warehouse items
-- Mỗi sản phẩm có 2-5 sản phẩm con với tên và giá khác nhau
-- Dữ liệu chi tiết cho từng loại sản phẩm
-- =====================================================
