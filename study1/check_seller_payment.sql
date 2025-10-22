-- Script để kiểm tra seller payment sau khi thêm seller_id

-- 1. Kiểm tra cột seller_id đã được thêm chưa
DESCRIBE order_item;

-- 2. Kiểm tra dữ liệu seller_id trong order_item
SELECT 
    oi.id,
    oi.order_id,
    oi.product_id,
    oi.warehouse_id,
    oi.seller_id,
    oi.seller_amount,
    w.user_id as warehouse_user_id,
    CASE 
        WHEN oi.seller_id = w.user_id THEN 'MATCH' 
        ELSE 'MISMATCH' 
    END as seller_id_match
FROM order_item oi
LEFT JOIN warehouse w ON w.id = oi.warehouse_id
ORDER BY oi.created_at DESC
LIMIT 10;

-- 3. Kiểm tra wallet history cho seller
SELECT 
    wh.id,
    wh.wallet_id,
    wh.type,
    wh.amount,
    wh.status,
    wh.description,
    wh.created_at,
    u.username
FROM wallethistory wh
JOIN wallet w ON w.id = wh.wallet_id
JOIN users u ON u.id = w.user_id
WHERE wh.type = 'SALE_SUCCESS'
ORDER BY wh.created_at DESC
LIMIT 10;

-- 4. Kiểm tra tổng số tiền seller đã nhận
SELECT 
    u.id as user_id,
    u.username,
    w.balance as current_balance,
    COUNT(wh.id) as sale_success_count,
    SUM(CASE WHEN wh.type = 'SALE_SUCCESS' THEN wh.amount ELSE 0 END) as total_received,
    SUM(CASE WHEN wh.type = 'PURCHASE' THEN ABS(wh.amount) ELSE 0 END) as total_spent
FROM users u
JOIN wallet w ON w.user_id = u.id
LEFT JOIN wallethistory wh ON wh.wallet_id = w.id
WHERE u.role = 'SELLER'
GROUP BY u.id, u.username, w.balance
ORDER BY total_received DESC;

-- 5. Kiểm tra order mới nhất
SELECT 
    o.id,
    o.order_code,
    o.buyer_id,
    o.total_amount,
    o.total_seller_amount,
    o.total_commission_amount,
    o.status,
    o.created_at
FROM `order` o
ORDER BY o.created_at DESC
LIMIT 5;

-- 6. Kiểm tra order items mới nhất
SELECT 
    oi.id,
    oi.order_id,
    oi.product_id,
    oi.warehouse_id,
    oi.seller_id,
    oi.seller_amount,
    oi.status,
    oi.created_at
FROM order_item oi
ORDER BY oi.created_at DESC
LIMIT 10;
