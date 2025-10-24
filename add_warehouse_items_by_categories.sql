-- =====================================================
-- SCRIPT THÊM SẢN PHẨM CON VÀO WAREHOUSE THEO LOẠI GIAN HÀNG
-- Thêm warehouse items cho các sản phẩm mới theo bảng fix
-- =====================================================

-- 1. EMAIL & SOCIAL SHOP - WAREHOUSE ITEMS
-- =====================================================

-- Gmail Premium 6-12 tháng (Product ID: 9) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(81, NOW(), NULL, 0, '{"email":"gmail_premium_shop2_001@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_001@gmail.com","phone":"+1234568001","2fa":"enabled","age":"8 months","country":"USA","storage_used":"6.2GB","storage_total":"15GB","created_date":"2024-02-15","premium":"true","tier":"Premium","plan":"Standard","age_months":"8","unique_id":"GMAIL_PREM_001"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(82, NOW(), NULL, 0, '{"email":"gmail_premium_shop2_002@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_002@gmail.com","phone":"+1234568002","2fa":"enabled","age":"10 months","country":"USA","storage_used":"7.8GB","storage_total":"15GB","created_date":"2023-12-15","premium":"true","tier":"Elite","plan":"Advanced","age_months":"10","unique_id":"GMAIL_PREM_002"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5),
(83, NOW(), NULL, 0, '{"email":"gmail_premium_shop2_003@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_003@gmail.com","phone":"+1234568003","2fa":"enabled","age":"12 months","country":"USA","storage_used":"9.1GB","storage_total":"15GB","created_date":"2023-10-15","premium":"true","tier":"VIP","plan":"Professional","age_months":"12","unique_id":"GMAIL_PREM_003"}', 'EMAIL', 0, NULL, NULL, 9, 2, 2, 5);

-- Gmail Random 3-6 tháng (Product ID: 10) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(84, NOW(), NULL, 0, '{"email":"gmail_random_shop2_001@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_004@gmail.com","phone":"+1234568004","2fa":"enabled","age":"4 months","country":"USA","storage_used":"2.1GB","storage_total":"15GB","created_date":"2024-06-15","random":"true","tier":"Standard","plan":"Basic","age_months":"4","unique_id":"GMAIL_RAND_001"}', 'EMAIL', 0, NULL, NULL, 10, 2, 2, 5),
(85, NOW(), NULL, 0, '{"email":"gmail_random_shop2_002@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_005@gmail.com","phone":"+1234568005","2fa":"enabled","age":"5 months","country":"USA","storage_used":"3.2GB","storage_total":"15GB","created_date":"2024-05-15","random":"true","tier":"Premium","plan":"Standard","age_months":"5","unique_id":"GMAIL_RAND_002"}', 'EMAIL', 0, NULL, NULL, 10, 2, 2, 5),
(86, NOW(), NULL, 0, '{"email":"gmail_random_shop2_003@gmail.com","password":"Pass123!","recovery_email":"recovery_shop2_006@gmail.com","phone":"+1234568006","2fa":"enabled","age":"6 months","country":"USA","storage_used":"4.5GB","storage_total":"15GB","created_date":"2024-04-15","random":"true","tier":"Elite","plan":"Advanced","age_months":"6","unique_id":"GMAIL_RAND_003"}', 'EMAIL', 0, NULL, NULL, 10, 2, 2, 5);

-- Yahoo Mail Pro 6-12 tháng (Product ID: 11) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(87, NOW(), NULL, 0, '{"email":"yahoo_pro_shop2_001@yahoo.com","password":"Pass123!","recovery_email":"recovery_shop2_007@yahoo.com","phone":"+1234568007","2fa":"enabled","age":"8 months","country":"USA","storage_used":"12.5GB","storage_total":"1TB","created_date":"2024-02-15","pro":"true","tier":"Pro","plan":"Standard","age_months":"8","unique_id":"YAHOO_PRO_001"}', 'EMAIL', 0, NULL, NULL, 11, 2, 2, 5),
(88, NOW(), NULL, 0, '{"email":"yahoo_pro_shop2_002@yahoo.com","password":"Pass123!","recovery_email":"recovery_shop2_008@yahoo.com","phone":"+1234568008","2fa":"enabled","age":"10 months","country":"USA","storage_used":"18.2GB","storage_total":"1TB","created_date":"2023-12-15","pro":"true","tier":"VIP","plan":"Advanced","age_months":"10","unique_id":"YAHOO_PRO_002"}', 'EMAIL', 0, NULL, NULL, 11, 2, 2, 5);

-- Facebook Business Account (Product ID: 12) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(89, NOW(), NULL, 0, '{"email":"fb_biz_shop2_001@gmail.com","password":"Pass123!","phone":"+1234568009","2fa":"enabled","age":"18 months","country":"USA","business_verified":"true","page_count":"4","ad_spend":"$300","tier":"Professional","plan":"Advanced","followers":"800"}', 'ACCOUNT', 0, NULL, NULL, 12, 2, 2, 5),
(90, NOW(), NULL, 0, '{"email":"fb_biz_shop2_002@gmail.com","password":"Pass123!","phone":"+1234568010","2fa":"enabled","age":"24 months","country":"USA","business_verified":"true","page_count":"6","ad_spend":"$600","tier":"Elite","plan":"Premium","followers":"1500"}', 'ACCOUNT', 0, NULL, NULL, 12, 2, 2, 5);

-- Instagram Business Account (Product ID: 13) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(91, NOW(), NULL, 0, '{"email":"ig_biz_shop2_001@gmail.com","password":"Pass123!","phone":"+1234568011","2fa":"enabled","age":"15 months","country":"USA","username":"business_tech_shop2","followers":"4200","following":"600","posts":"120","business":"true","insights":"enabled","tier":"Professional","plan":"Advanced"}', 'ACCOUNT', 0, NULL, NULL, 13, 2, 2, 5),
(92, NOW(), NULL, 0, '{"email":"ig_biz_shop2_002@gmail.com","password":"Pass123!","phone":"+1234568012","2fa":"enabled","age":"20 months","country":"USA","username":"marketing_pro_shop2","followers":"5800","following":"800","posts":"180","business":"true","insights":"enabled","tier":"Elite","plan":"Premium"}', 'ACCOUNT', 0, NULL, NULL, 13, 2, 2, 5);

-- Twitter Verified Account (Product ID: 14) - Shop 2
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(93, NOW(), NULL, 0, '{"email":"twitter_shop2_001@gmail.com","password":"Pass123!","phone":"+1234568013","2fa":"enabled","age":"12 months","country":"USA","username":"tech_news_shop2","followers":"3200","following":"500","posts":"200","verified":"true","tier":"Verified","plan":"Standard"}', 'ACCOUNT', 0, NULL, NULL, 14, 2, 2, 5),
(94, NOW(), NULL, 0, '{"email":"twitter_shop2_002@gmail.com","password":"Pass123!","phone":"+1234568014","2fa":"enabled","age":"18 months","country":"USA","username":"business_insights_shop2","followers":"4500","following":"700","posts":"300","verified":"true","tier":"Elite","plan":"Premium"}', 'ACCOUNT', 0, NULL, NULL, 14, 2, 2, 5);

-- 2. SOFTWARE SHOP - WAREHOUSE ITEMS
-- =====================================================

-- Windows 11 Pro Key Lifetime (Product ID: 15) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(95, NOW(), NULL, 0, '{"key":"WIN11-SHOP3-XXXX-YYYY-ZZZZ-AAAA","type":"Windows 11 Pro","activation":"lifetime","region":"global","version":"22H2","support":"extended","edition":"Standard","tier":"Premium","plan":"Standard"}', 'KEY', 0, NULL, NULL, 15, 3, 3, 6),
(96, NOW(), NULL, 0, '{"key":"WIN11-SHOP3-BBBB-CCCC-DDDD-EEEE","type":"Windows 11 Pro","activation":"lifetime","region":"global","version":"23H2","support":"extended","edition":"Enterprise","tier":"Elite","plan":"Premium"}', 'KEY', 0, NULL, NULL, 15, 3, 3, 6),
(97, NOW(), NULL, 0, '{"key":"WIN11-SHOP3-FFFF-GGGG-HHHH-IIII","type":"Windows 11 Pro","activation":"lifetime","region":"global","version":"24H1","support":"extended","edition":"Professional","tier":"VIP","plan":"Advanced"}', 'KEY', 0, NULL, NULL, 15, 3, 3, 6);

-- Office 365 Personal 1 năm (Product ID: 16) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(98, NOW(), NULL, 0, '{"key":"OFF365-SHOP3-JJJJ-KKKK-LLLL-MMMM","type":"Office 365 Personal","activation":"1 year","devices":"5","region":"global","apps":"Word,Excel,PowerPoint,Outlook,OneNote","plan":"Personal","tier":"Premium","plan_type":"Standard"}', 'KEY', 0, NULL, NULL, 16, 3, 3, 6),
(99, NOW(), NULL, 0, '{"key":"OFF365-SHOP3-NNNN-OOOO-PPPP-QQQQ","type":"Office 365 Personal","activation":"1 year","devices":"5","region":"global","apps":"Word,Excel,PowerPoint,Outlook,OneNote","plan":"Personal","tier":"Elite","plan_type":"Premium"}', 'KEY', 0, NULL, NULL, 16, 3, 3, 6);

-- Adobe Creative Cloud All Apps (Product ID: 17) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(100, NOW(), NULL, 0, '{"key":"ADOBE-SHOP3-RRRR-SSSS-TTTT-UUUU","type":"Adobe Creative Cloud","activation":"1 year","apps":"Photoshop,Illustrator,InDesign,Premiere,After Effects","storage":"100GB","region":"global","tier":"Premium","plan":"Standard"}', 'KEY', 0, NULL, NULL, 17, 3, 3, 6),
(101, NOW(), NULL, 0, '{"key":"ADOBE-SHOP3-VVVV-WWWW-XXXX-YYYY","type":"Adobe Creative Cloud","activation":"1 year","apps":"Photoshop,Illustrator,InDesign,Premiere,After Effects","storage":"100GB","region":"global","tier":"Elite","plan":"Premium"}', 'KEY', 0, NULL, NULL, 17, 3, 3, 6);

-- Windows 10 Pro Key Lifetime (Product ID: 18) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(102, NOW(), NULL, 0, '{"key":"WIN10-SHOP3-ZZZZ-AAAA-BBBB-CCCC","type":"Windows 10 Pro","activation":"lifetime","region":"global","version":"22H2","support":"extended","edition":"Standard","tier":"Premium","plan":"Standard"}', 'KEY', 0, NULL, NULL, 18, 3, 3, 6),
(103, NOW(), NULL, 0, '{"key":"WIN10-SHOP3-DDDD-EEEE-FFFF-GGGG","type":"Windows 10 Pro","activation":"lifetime","region":"global","version":"21H2","support":"extended","edition":"Enterprise","tier":"Elite","plan":"Premium"}', 'KEY', 0, NULL, NULL, 18, 3, 3, 6);

-- Office 2021 Pro Plus (Product ID: 19) - Shop 3
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(104, NOW(), NULL, 0, '{"key":"OFF21-SHOP3-HHHH-IIII-JJJJ-KKKK","type":"Office 2021 Pro Plus","activation":"lifetime","region":"global","apps":"Word,Excel,PowerPoint,Outlook,Access,Publisher,OneNote","edition":"Pro Plus","tier":"Premium","plan":"Standard"}', 'KEY', 0, NULL, NULL, 19, 3, 3, 6),
(105, NOW(), NULL, 0, '{"key":"OFF21-SHOP3-LLLL-MMMM-NNNN-OOOO","type":"Office 2021 Pro Plus","activation":"lifetime","region":"global","apps":"Word,Excel,PowerPoint,Outlook,Access,Publisher,OneNote","edition":"Pro Plus","tier":"Elite","plan":"Premium"}', 'KEY', 0, NULL, NULL, 19, 3, 3, 6);

-- 3. GAME SHOP - WAREHOUSE ITEMS
-- =====================================================

-- Steam Account Level 50+ (Product ID: 20) - Shop 4
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(106, NOW(), NULL, 0, '{"email":"steam_shop4_001@gmail.com","password":"Pass123!","username":"GamerPro2024_Shop4","level":"52","games":"125","playtime":"2500 hours","country":"USA","steam_guard":"enabled","tier":"Pro","plan":"Standard","unique_id":"STEAM_SHOP4_001"}', 'ACCOUNT', 0, NULL, NULL, 20, 4, 4, 7),
(107, NOW(), NULL, 0, '{"email":"steam_shop4_002@gmail.com","password":"Pass123!","username":"GameMaster99_Shop4","level":"58","games":"150","playtime":"3200 hours","country":"USA","steam_guard":"enabled","tier":"Elite","plan":"Premium","unique_id":"STEAM_SHOP4_002"}', 'ACCOUNT', 0, NULL, NULL, 20, 4, 4, 7);

-- Epic Games Account (Product ID: 21) - Shop 4
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(108, NOW(), NULL, 0, '{"email":"epic_shop4_001@gmail.com","password":"Pass123!","username":"EpicGamer2024_Shop4","games":"45","free_games":"38","purchased_games":"7","country":"USA","two_factor":"enabled","tier":"Pro","plan":"Standard","unique_id":"EPIC_SHOP4_001"}', 'ACCOUNT', 0, NULL, NULL, 21, 4, 4, 7),
(109, NOW(), NULL, 0, '{"email":"epic_shop4_002@gmail.com","password":"Pass123!","username":"FreeGameCollector_Shop4","games":"52","free_games":"48","purchased_games":"4","country":"USA","two_factor":"enabled","tier":"Elite","plan":"Premium","unique_id":"EPIC_SHOP4_002"}', 'ACCOUNT', 0, NULL, NULL, 21, 4, 4, 7);

-- Steam Wallet Code $50 (Product ID: 22) - Shop 4
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(110, NOW(), NULL, 0, '{"code":"STEAM50-SHOP4-XXXX-YYYY-ZZZZ","amount":"$50 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam","tier":"Premium","plan":"Standard","unique_id":"STEAM50_001","card_type":"Digital","brand":"Steam"}', 'CARD', 0, NULL, NULL, 22, 4, 4, 7),
(111, NOW(), NULL, 0, '{"code":"STEAM50-SHOP4-AAAA-BBBB-CCCC","amount":"$50 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam","tier":"Elite","plan":"Premium","unique_id":"STEAM50_002","card_type":"Physical","brand":"Steam"}', 'CARD', 0, NULL, NULL, 22, 4, 4, 7);

-- Steam Wallet Code $20 (Product ID: 23) - Shop 4
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(112, NOW(), NULL, 0, '{"code":"STEAM20-SHOP4-DDDD-EEEE-FFFF","amount":"$20 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam","tier":"Premium","plan":"Standard","unique_id":"STEAM20_001","card_type":"Digital","brand":"Steam"}', 'CARD', 0, NULL, NULL, 23, 4, 4, 7),
(113, NOW(), NULL, 0, '{"code":"STEAM20-SHOP4-GGGG-HHHH-IIII","amount":"$20 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Steam","tier":"Elite","plan":"Premium","unique_id":"STEAM20_002","card_type":"Physical","brand":"Steam"}', 'CARD', 0, NULL, NULL, 23, 4, 4, 7);

-- Google Play Gift Card $25 (Product ID: 24) - Shop 4
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(114, NOW(), NULL, 0, '{"code":"GPLAY25-SHOP4-JJJJ-KKKK-LLLL","amount":"$25 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Google Play","tier":"Premium","plan":"Standard","unique_id":"GPLAY25_001","card_type":"Digital","brand":"Google"}', 'CARD', 0, NULL, NULL, 24, 4, 4, 7),
(115, NOW(), NULL, 0, '{"code":"GPLAY25-SHOP4-MMMM-NNNN-OOOO","amount":"$25 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"Google Play","tier":"Elite","plan":"Premium","unique_id":"GPLAY25_002","card_type":"Physical","brand":"Google"}', 'CARD', 0, NULL, NULL, 24, 4, 4, 7);

-- iTunes Gift Card $20 (Product ID: 25) - Shop 4
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(116, NOW(), NULL, 0, '{"code":"ITUNES20-SHOP4-PPPP-QQQQ-RRRR","amount":"$20 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"iTunes","tier":"Premium","plan":"Standard","unique_id":"ITUNES20_001","card_type":"Digital","brand":"Apple"}', 'CARD', 0, NULL, NULL, 25, 4, 4, 7),
(117, NOW(), NULL, 0, '{"code":"ITUNES20-SHOP4-SSSS-TTTT-UUUU","amount":"$20 USD","currency":"USD","region":"Global","expiry":"2025-12-31","platform":"iTunes","tier":"Elite","plan":"Premium","unique_id":"ITUNES20_002","card_type":"Physical","brand":"Apple"}', 'CARD', 0, NULL, NULL, 25, 4, 4, 7);

-- 4. OTHER SHOP - WAREHOUSE ITEMS
-- =====================================================

-- Domain .com Premium (Product ID: 26) - Shop 5
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(118, NOW(), NULL, 0, '{"domain":"techpremium_shop5_2024.com","registrar":"GoDaddy","expiry":"2025-12-31","dns":"enabled","ssl":"included","privacy":"protected","tier":"Premium","plan":"Standard","unique_id":"DOMAIN_001"}', 'KEY', 0, NULL, NULL, 26, 5, 5, 8),
(119, NOW(), NULL, 0, '{"domain":"businesspro_shop5_2024.com","registrar":"Namecheap","expiry":"2025-12-31","dns":"enabled","ssl":"included","privacy":"protected","tier":"Elite","plan":"Business","unique_id":"DOMAIN_002"}', 'KEY', 0, NULL, NULL, 26, 5, 5, 8);

-- SSL Certificate (Product ID: 27) - Shop 5
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(120, NOW(), NULL, 0, '{"certificate":"SSL-SHOP5-CERT-001-XXXX-YYYY-ZZZZ","type":"SSL Certificate","validity":"1 year","domain":"example-shop5.com","issuer":"Comodo","encryption":"256-bit","tier":"Standard","plan":"Basic","unique_id":"SSL_001"}', 'KEY', 0, NULL, NULL, 27, 5, 5, 8),
(121, NOW(), NULL, 0, '{"certificate":"SSL-SHOP5-CERT-002-AAAA-BBBB-CCCC","type":"SSL Certificate","validity":"1 year","domain":"example-shop5.org","issuer":"DigiCert","encryption":"256-bit","tier":"Elite","plan":"Advanced","unique_id":"SSL_002"}', 'KEY', 0, NULL, NULL, 27, 5, 5, 8);

-- VPS Hosting 1 năm (Product ID: 28) - Shop 5
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(122, NOW(), NULL, 0, '{"vps_id":"VPS-SHOP5-001-XXXX-YYYY","type":"VPS Hosting","duration":"1 year","cpu":"4 cores","ram":"8GB","storage":"100GB SSD","bandwidth":"1TB","os":"Ubuntu 20.04","tier":"Premium","plan":"Standard","unique_id":"VPS_001"}', 'KEY', 0, NULL, NULL, 28, 5, 5, 8),
(123, NOW(), NULL, 0, '{"vps_id":"VPS-SHOP5-002-AAAA-BBBB","type":"VPS Hosting","duration":"1 year","cpu":"8 cores","ram":"16GB","storage":"200GB SSD","bandwidth":"2TB","os":"CentOS 8","tier":"Elite","plan":"Premium","unique_id":"VPS_002"}', 'KEY', 0, NULL, NULL, 28, 5, 5, 8);

-- Cloud Storage 1TB (Product ID: 29) - Shop 5
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(124, NOW(), NULL, 0, '{"storage_id":"CLOUD-SHOP5-001-XXXX-YYYY","type":"Cloud Storage","capacity":"1TB","duration":"1 year","provider":"AWS","encryption":"AES-256","backup":"enabled","tier":"Premium","plan":"Standard","unique_id":"CLOUD_001"}', 'KEY', 0, NULL, NULL, 29, 5, 5, 8),
(125, NOW(), NULL, 0, '{"storage_id":"CLOUD-SHOP5-002-AAAA-BBBB","type":"Cloud Storage","capacity":"1TB","duration":"1 year","provider":"Google Cloud","encryption":"AES-256","backup":"enabled","tier":"Elite","plan":"Premium","unique_id":"CLOUD_002"}', 'KEY', 0, NULL, NULL, 29, 5, 5, 8);

-- VPN Premium 1 năm (Product ID: 30) - Shop 5
INSERT INTO `warehouse` (`id`, `created_at`, `deleted_by`, `is_delete`, `item_data`, `item_type`, `locked`, `locked_at`, `locked_by`, `product_id`, `shop_id`, `stall_id`, `user_id`) VALUES
(126, NOW(), NULL, 0, '{"vpn_id":"VPN-SHOP5-001-XXXX-YYYY","type":"VPN Premium","duration":"1 year","servers":"50+","countries":"30+","protocols":"OpenVPN,WireGuard","speed":"unlimited","tier":"Premium","plan":"Standard","unique_id":"VPN_001"}', 'KEY', 0, NULL, NULL, 30, 5, 5, 8),
(127, NOW(), NULL, 0, '{"vpn_id":"VPN-SHOP5-002-AAAA-BBBB","type":"VPN Premium","duration":"1 year","servers":"100+","countries":"50+","protocols":"OpenVPN,WireGuard,IKEv2","speed":"unlimited","tier":"Elite","plan":"Premium","unique_id":"VPN_002"}', 'KEY', 0, NULL, NULL, 30, 5, 5, 8);

-- =====================================================
-- HOÀN THÀNH THÊM WAREHOUSE ITEMS CHO TẤT CẢ SẢN PHẨM MỚI
-- Tổng cộng: 47 warehouse items mới
-- Email & Social Shop: 14 items
-- Software Shop: 11 items
-- Game Shop: 12 items
-- Other Shop: 10 items
-- Tất cả sản phẩm con đều có unique_id riêng biệt
-- =====================================================
