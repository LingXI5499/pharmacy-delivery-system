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
DELETE FROM pharmacy_order_item;
DELETE FROM pharmacy_order;
DELETE FROM shopping_cart;
DELETE FROM user_address WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'e1\_%');
DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'e1\_%');
DELETE FROM refresh_token WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'e1\_%');
DELETE FROM sys_user WHERE username LIKE 'e1\_%';
DELETE FROM purchase_order_item WHERE purchase_order_id IN (SELECT id FROM purchase_order WHERE purchase_no LIKE 'E1PO%');
DELETE FROM purchase_order WHERE purchase_no LIKE 'E1PO%';
DELETE FROM supplier WHERE supplier_code LIKE 'E1%';
DELETE FROM inventory_ledger WHERE business_id LIKE 'E1%';
DELETE FROM medicine_batch WHERE batch_no LIKE 'E1-%';
DELETE FROM medicine WHERE medicine_name LIKE 'E1 %';
DELETE FROM medicine_category WHERE category_name LIKE 'E1 %';

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO sys_user (username, password, nickname, phone, role, status) VALUES
('e1_admin', 'test123456', 'E1 管理员', '13810000001', 'ADMIN', 1),
('e1_user', 'test123456', 'E1 普通用户', '13810000002', 'USER', 1),
('e1_pharmacist', 'test123456', 'E1 药师', '13810000003', 'PHARMACIST', 1),
('e1_purchaser', 'test123456', 'E1 采购员', '13810000004', 'PURCHASER', 1),
('e1_warehouse', 'test123456', 'E1 仓库员', '13810000005', 'WAREHOUSE', 1);

INSERT INTO sys_user_role(user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = u.role
WHERE u.username LIKE 'e1\_%';

INSERT INTO user_address (user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default) VALUES
((SELECT id FROM sys_user WHERE username = 'e1_user'), 'E1 普通用户', '13810000002', '广东省', '惠州市', '惠城区', 'E1 演示路 1 号', 1),
((SELECT id FROM sys_user WHERE username = 'e1_admin'), 'E1 管理员', '13810000001', '广东省', '惠州市', '惠城区', 'E1 管理中心 9 号', 1);

INSERT INTO medicine_category (category_name, description, sort_no, status, is_deleted) VALUES
('E1 常规用药', 'Playwright E2E 常规药品分类', 901, 1, 0),
('E1 处方用药', 'Playwright E2E 处方药分类', 902, 1, 0);

INSERT INTO medicine (
  category_id, medicine_name, description, usage_instruction, precautions, price, stock, warning_stock,
  prescription_required, status, version, is_deleted
) VALUES
(
  (SELECT id FROM medicine_category WHERE category_name = 'E1 常规用药'),
  'E1 OTC 感冒灵颗粒',
  'E1 端到端测试用常规药品',
  '按说明书使用',
  '仅供虚构测试数据使用',
  19.80, 6, 2, 0, 1, 0, 0
),
(
  (SELECT id FROM medicine_category WHERE category_name = 'E1 处方用药'),
  'E1 RX 阿莫西林胶囊',
  'E1 端到端测试用处方药',
  '遵医嘱使用',
  '仅供虚构测试数据使用',
  29.90, 3, 1, 1, 1, 0, 0
);

INSERT INTO medicine_batch (
  medicine_id, location_id, batch_no, production_date, expiry_date, purchase_price,
  available_qty, reserved_qty, quality_status, sellable, version
) VALUES
(
  (SELECT id FROM medicine WHERE medicine_name = 'E1 OTC 感冒灵颗粒'),
  (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
  'E1-OTC-001',
  DATE_SUB(CURRENT_DATE, INTERVAL 180 DAY),
  DATE_ADD(CURRENT_DATE, INTERVAL 45 DAY),
  8.50,
  2, 0, 'QUALIFIED', 1, 0
),
(
  (SELECT id FROM medicine WHERE medicine_name = 'E1 OTC 感冒灵颗粒'),
  (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
  'E1-OTC-002',
  DATE_SUB(CURRENT_DATE, INTERVAL 120 DAY),
  DATE_ADD(CURRENT_DATE, INTERVAL 180 DAY),
  8.70,
  4, 0, 'QUALIFIED', 1, 0
),
(
  (SELECT id FROM medicine WHERE medicine_name = 'E1 RX 阿莫西林胶囊'),
  (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
  'E1-RX-001',
  DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY),
  DATE_ADD(CURRENT_DATE, INTERVAL 240 DAY),
  12.50,
  3, 0, 'QUALIFIED', 1, 0
);

INSERT INTO supplier (supplier_code, supplier_name, contact_name, phone, status) VALUES
('E1-SUP-001', 'E1 演示供应商', 'E1 联系人', '13810000100', 1);
