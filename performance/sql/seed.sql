-- Repeatable P1 dataset. Requires V1–V4. Uses fictional users and SKUs only.
-- Password for all p1_* accounts is test123456 (BCrypt, same hash as E1 seed).

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM inventory_count_item;
DELETE FROM inventory_count;
DELETE FROM purchase_receipt_item;
DELETE FROM purchase_receipt;
DELETE FROM inventory_ledger;
DELETE FROM inventory_reservation;
DELETE FROM prescription_item;
DELETE FROM prescription;
DELETE FROM refund_record;
DELETE FROM payment_attempt;
DELETE FROM inbox_event;
DELETE FROM business_audit_log;
DELETE FROM order_status_log;
DELETE FROM pharmacy_order_item WHERE order_id IN (SELECT id FROM pharmacy_order WHERE order_no LIKE 'P1%');
DELETE FROM pharmacy_order WHERE order_no LIKE 'P1%';
DELETE FROM shopping_cart WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'p1\\_%');
DELETE FROM user_address WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'p1\\_%');
DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'p1\\_%');
DELETE FROM refresh_token WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'p1\\_%');
DELETE FROM sys_user WHERE username LIKE 'p1\\_%';
DELETE FROM medicine_batch WHERE batch_no LIKE 'P1-%';
DELETE FROM medicine WHERE medicine_name LIKE 'P1 %';
DELETE FROM medicine_category WHERE category_name LIKE 'P1-CAT-%';

SET FOREIGN_KEY_CHECKS = 1;

DROP TEMPORARY TABLE IF EXISTS p1_digits;
CREATE TEMPORARY TABLE p1_digits (n INT NOT NULL PRIMARY KEY);
INSERT INTO p1_digits (n) VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

INSERT INTO medicine_category (category_name, sort_no, status, is_deleted)
SELECT CONCAT('P1-CAT-', LPAD(seq, 2, '0')), seq, 1, 0
FROM (
    SELECT d0.n + d1.n * 10 AS seq
    FROM p1_digits d0
    CROSS JOIN p1_digits d1
) s
WHERE seq BETWEEN 1 AND 20;

INSERT INTO medicine (
    category_id, medicine_name, description, price, stock, warning_stock,
    prescription_required, status, version, is_deleted
)
SELECT
    c.id,
    CONCAT('P1 Catalog ', LPAD(seq, 4, '0')),
    'P1 fictional catalog SKU',
    8.50 + (seq % 40),
    30,
    5,
    0,
    1,
    0,
    0
FROM (
    SELECT d0.n + d1.n * 10 + d2.n * 100 + d3.n * 1000 AS seq
    FROM p1_digits d0
    CROSS JOIN p1_digits d1
    CROSS JOIN p1_digits d2
    CROSS JOIN p1_digits d3
) s
JOIN medicine_category c
    ON c.category_name = CONCAT('P1-CAT-', LPAD(((seq - 1) % 20) + 1, 2, '0'))
WHERE seq BETWEEN 1 AND 1200;

INSERT INTO medicine (
    category_id, medicine_name, description, price, stock, warning_stock,
    prescription_required, status, version, is_deleted
)
SELECT id, 'P1 HOT 压测装量', 'P1 hot path OTC', 9.90, 500000, 50, 0, 1, 0, 0
FROM medicine_category
WHERE category_name = 'P1-CAT-01'
LIMIT 1;

INSERT INTO medicine_batch (
    medicine_id, location_id, batch_no, production_date, expiry_date, purchase_price,
    available_qty, reserved_qty, quality_status, sellable, version
)
SELECT
    m.id,
    loc.id,
    CONCAT('P1-CAT-', m.id, '-A'),
    DATE_SUB(CURDATE(), INTERVAL 60 DAY),
    DATE_ADD(CURDATE(), INTERVAL 120 DAY),
    4.00,
    10,
    0,
    'QUALIFIED',
    1,
    0
FROM medicine m
JOIN inventory_location loc ON loc.location_code = 'MAIN'
WHERE m.medicine_name LIKE 'P1 Catalog %';

INSERT INTO medicine_batch (
    medicine_id, location_id, batch_no, production_date, expiry_date, purchase_price,
    available_qty, reserved_qty, quality_status, sellable, version
)
SELECT
    m.id,
    loc.id,
    CONCAT('P1-CAT-', m.id, '-B'),
    DATE_SUB(CURDATE(), INTERVAL 30 DAY),
    DATE_ADD(CURDATE(), INTERVAL 400 DAY),
    4.00,
    20,
    0,
    'QUALIFIED',
    1,
    0
FROM medicine m
JOIN inventory_location loc ON loc.location_code = 'MAIN'
WHERE m.medicine_name LIKE 'P1 Catalog %';

INSERT INTO medicine_batch (
    medicine_id, location_id, batch_no, production_date, expiry_date, purchase_price,
    available_qty, reserved_qty, quality_status, sellable, version
)
SELECT
    m.id,
    loc.id,
    'P1-HOT-EARLY',
    DATE_SUB(CURDATE(), INTERVAL 90 DAY),
    DATE_ADD(CURDATE(), INTERVAL 80 DAY),
    3.50,
    100000,
    0,
    'QUALIFIED',
    1,
    0
FROM medicine m
JOIN inventory_location loc ON loc.location_code = 'MAIN'
WHERE m.medicine_name = 'P1 HOT 压测装量';

INSERT INTO medicine_batch (
    medicine_id, location_id, batch_no, production_date, expiry_date, purchase_price,
    available_qty, reserved_qty, quality_status, sellable, version
)
SELECT
    m.id,
    loc.id,
    'P1-HOT-LATE',
    DATE_SUB(CURDATE(), INTERVAL 20 DAY),
    DATE_ADD(CURDATE(), INTERVAL 500 DAY),
    3.50,
    400000,
    0,
    'QUALIFIED',
    1,
    0
FROM medicine m
JOIN inventory_location loc ON loc.location_code = 'MAIN'
WHERE m.medicine_name = 'P1 HOT 压测装量';

-- Near-expiry filler for EXPLAIN of warehouse-style filters.
INSERT INTO medicine_batch (
    medicine_id, location_id, batch_no, production_date, expiry_date, purchase_price,
    available_qty, reserved_qty, quality_status, sellable, version
)
SELECT
    m.id,
    loc.id,
    CONCAT('P1-NEAR-', m.id),
    DATE_SUB(CURDATE(), INTERVAL 200 DAY),
    DATE_ADD(CURDATE(), INTERVAL 25 DAY),
    4.00,
    2,
    0,
    'QUALIFIED',
    1,
    0
FROM medicine m
JOIN inventory_location loc ON loc.location_code = 'MAIN'
WHERE m.medicine_name LIKE 'P1 Catalog %'
  AND m.id % 15 = 0;

UPDATE medicine m
JOIN (
    SELECT medicine_id, SUM(available_qty) AS qty
    FROM medicine_batch
    WHERE sellable = 1 AND quality_status = 'QUALIFIED' AND expiry_date > CURDATE()
    GROUP BY medicine_id
) b ON b.medicine_id = m.id
SET m.stock = b.qty
WHERE m.medicine_name LIKE 'P1 %';

INSERT INTO sys_user (username, password, nickname, phone, role, status)
SELECT
    CONCAT('p1_user_', LPAD(seq, 3, '0')),
    '$2b$12$Ccf2Ffq7NOIzz8Q3k/OyuOS.a8JTyToH0/qgE5tyyeqASMTUSMX8i',
    CONCAT('P1用户', LPAD(seq, 3, '0')),
    CONCAT('13900000', LPAD(seq, 3, '0')),
    'USER',
    1
FROM (
    SELECT d0.n + d1.n * 10 + d2.n * 100 AS seq
    FROM p1_digits d0
    CROSS JOIN p1_digits d1
    CROSS JOIN p1_digits d2
) s
WHERE seq BETWEEN 1 AND 100;

INSERT INTO sys_user (username, password, nickname, phone, role, status)
VALUES (
    'p1_warehouse',
    '$2b$12$Ccf2Ffq7NOIzz8Q3k/OyuOS.a8JTyToH0/qgE5tyyeqASMTUSMX8i',
    'P1仓库',
    '13900000999',
    'WAREHOUSE',
    1
);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = u.role
WHERE u.username LIKE 'p1\\_%';

INSERT INTO user_address (
    user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default, is_deleted
)
SELECT
    u.id,
    u.nickname,
    u.phone,
    '测试省',
    '测试市',
    '测试区',
    CONCAT('P1 路 ', u.id, ' 号'),
    1,
    0
FROM sys_user u
WHERE u.username LIKE 'p1_user_%';

-- Pad order list queries. These rows do not reserve stock.
INSERT INTO pharmacy_order (
    order_no, user_id, address_id, receiver_name, receiver_phone, receiver_address,
    product_amount, delivery_fee, order_amount, order_status, user_remark, create_time, update_time
)
SELECT
    CONCAT('P1H', LPAD(seq, 6, '0')),
    u.id,
    a.id,
    a.receiver_name,
    a.receiver_phone,
    CONCAT(a.province, a.city, a.district, a.detail_address),
    18.50,
    5.00,
    23.50,
    'COMPLETED',
    'P1 historical pad',
    DATE_SUB(NOW(), INTERVAL (seq % 4000) MINUTE),
    NOW()
FROM (
    SELECT d0.n + d1.n * 10 + d2.n * 100 + d3.n * 1000 + d4.n * 10000 AS seq
    FROM p1_digits d0
    CROSS JOIN p1_digits d1
    CROSS JOIN p1_digits d2
    CROSS JOIN p1_digits d3
    CROSS JOIN p1_digits d4
) s
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM sys_user
    WHERE username LIKE 'p1_user_%'
) u ON u.rn = (seq % 100) + 1
JOIN user_address a ON a.user_id = u.id AND a.is_deleted = 0
WHERE seq BETWEEN 1 AND 20000;

INSERT INTO pharmacy_order_item (order_id, medicine_id, medicine_name, medicine_price, quantity, subtotal_amount)
SELECT o.id, m.id, m.medicine_name, 8.50, 1, 8.50
FROM pharmacy_order o
JOIN medicine m ON m.medicine_name = 'P1 Catalog 0001'
WHERE o.order_no LIKE 'P1H%';

ANALYZE TABLE medicine, medicine_batch, pharmacy_order, pharmacy_order_item, inventory_reservation, inventory_ledger, shopping_cart;
