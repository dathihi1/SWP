-- =====================================================
-- SCRIPT TỔNG HỢP - THÊM SHOP VÀ SẢN PHẨM THEO LOẠI GIAN HÀNG
-- Chạy script này để thêm đầy đủ các shop và sản phẩm theo dropdown
-- =====================================================

-- Chạy script thêm shop và sản phẩm
SOURCE add_shops_by_categories.sql;

-- Chạy script thêm warehouse items
SOURCE add_warehouse_items_by_categories.sql;

-- =====================================================
-- KIỂM TRA KẾT QUẢ
-- =====================================================

-- Kiểm tra số lượng shop
SELECT 'SHOPS' as table_name, COUNT(*) as count FROM shop WHERE is_delete = 0;

-- Kiểm tra số lượng stall
SELECT 'STALLS' as table_name, COUNT(*) as count FROM stall WHERE is_delete = 0;

-- Kiểm tra số lượng sản phẩm
SELECT 'PRODUCTS' as table_name, COUNT(*) as count FROM product WHERE is_delete = 0;

-- Kiểm tra số lượng warehouse items
SELECT 'WAREHOUSE' as table_name, COUNT(*) as count FROM warehouse WHERE is_delete = 0;

-- Kiểm tra shop theo loại
SELECT 
    s.shop_name,
    st.stall_category,
    COUNT(p.id) as product_count
FROM shop s
LEFT JOIN stall st ON s.id = st.shop_id
LEFT JOIN product p ON s.id = p.shop_id
WHERE s.is_delete = 0 AND st.is_delete = 0 AND p.is_delete = 0
GROUP BY s.id, s.shop_name, st.stall_category
ORDER BY s.id;

-- =====================================================
-- HOÀN THÀNH SETUP THEO LOẠI GIAN HÀNG
-- =====================================================
