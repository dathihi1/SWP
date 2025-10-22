-- Test script to verify seller payment logic with new seller_id column

-- 1. Check if seller_id column exists and has data
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

-- 2. Check for orders with missing seller_id
SELECT 
    oi.id,
    oi.order_id,
    oi.seller_id,
    oi.seller_amount,
    w.user_id as warehouse_user_id
FROM order_item oi
LEFT JOIN warehouse w ON w.id = oi.warehouse_id
WHERE oi.seller_id IS NULL OR oi.seller_id = 0
ORDER BY oi.created_at DESC
LIMIT 10;

-- 3. Check wallet history for seller payments
SELECT 
    wh.id,
    wh.wallet_id,
    wh.type,
    wh.amount,
    wh.status,
    wh.description,
    wh.created_at
FROM wallethistory wh
WHERE wh.type = 'SALE_SUCCESS'
ORDER BY wh.created_at DESC
LIMIT 10;

-- 4. Check total seller amounts vs wallet history
SELECT 
    SUM(oi.seller_amount) as total_seller_amount,
    COUNT(*) as order_items_count
FROM order_item oi
WHERE oi.seller_id IS NOT NULL AND oi.seller_id > 0;

-- 5. Check if seller wallets have received payments
SELECT 
    u.id as user_id,
    u.username,
    w.balance,
    COUNT(wh.id) as sale_success_count,
    SUM(CASE WHEN wh.type = 'SALE_SUCCESS' THEN wh.amount ELSE 0 END) as total_received
FROM users u
JOIN wallet w ON w.user_id = u.id
LEFT JOIN wallethistory wh ON wh.wallet_id = w.id AND wh.type = 'SALE_SUCCESS'
WHERE u.role = 'SELLER'
GROUP BY u.id, u.username, w.balance
ORDER BY total_received DESC;
