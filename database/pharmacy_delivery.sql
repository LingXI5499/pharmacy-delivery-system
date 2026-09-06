
-- 单店药店即时配送管理系统：MySQL 8.x 初始化脚本
-- 注意：本脚本会删除并重建 pharmacy_delivery 数据库。仅用于本地课程设计演示。

DROP DATABASE IF EXISTS pharmacy_delivery;
CREATE DATABASE pharmacy_delivery DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE pharmacy_delivery;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(32) NOT NULL COMMENT '登录账号',
  password VARCHAR(64) NOT NULL COMMENT '明文密码，仅课程演示',
  nickname VARCHAR(32) NOT NULL COMMENT '昵称',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  role VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0禁用',
  last_login_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_role_status (role, status),
  CONSTRAINT ck_sys_user_role CHECK (role IN ('USER','ADMIN')),
  CONSTRAINT ck_sys_user_status CHECK (status IN (0,1))
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE medicine_category (
  id BIGINT NOT NULL AUTO_INCREMENT,
  category_name VARCHAR(50) NOT NULL,
  category_image VARCHAR(255) DEFAULT NULL,
  description VARCHAR(255) DEFAULT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_medicine_category_name (category_name),
  KEY idx_medicine_category_display (status, is_deleted, sort_no),
  CONSTRAINT ck_medicine_category_status CHECK (status IN (0,1)),
  CONSTRAINT ck_medicine_category_deleted CHECK (is_deleted IN (0,1))
) ENGINE=InnoDB COMMENT='药品分类表';

CREATE TABLE medicine (
  id BIGINT NOT NULL AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  medicine_name VARCHAR(100) NOT NULL,
  image_url VARCHAR(255) DEFAULT NULL,
  description VARCHAR(500) DEFAULT NULL,
  usage_instruction VARCHAR(1000) DEFAULT NULL,
  precautions VARCHAR(1000) DEFAULT NULL,
  price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  warning_stock INT NOT NULL DEFAULT 5,
  status TINYINT NOT NULL DEFAULT 1,
  version INT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_medicine_category_status (category_id, status, is_deleted),
  KEY idx_medicine_name (medicine_name),
  KEY idx_medicine_stock (stock, warning_stock),
  KEY idx_medicine_display (status, is_deleted, create_time),
  CONSTRAINT fk_medicine_category FOREIGN KEY (category_id) REFERENCES medicine_category(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT ck_medicine_price CHECK (price > 0),
  CONSTRAINT ck_medicine_stock CHECK (stock >= 0),
  CONSTRAINT ck_medicine_warning_stock CHECK (warning_stock >= 0),
  CONSTRAINT ck_medicine_status CHECK (status IN (0,1)),
  CONSTRAINT ck_medicine_deleted CHECK (is_deleted IN (0,1))
) ENGINE=InnoDB COMMENT='药品表';

CREATE TABLE user_address (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  receiver_name VARCHAR(32) NOT NULL,
  receiver_phone VARCHAR(20) NOT NULL,
  province VARCHAR(32) DEFAULT NULL,
  city VARCHAR(32) DEFAULT NULL,
  district VARCHAR(32) DEFAULT NULL,
  detail_address VARCHAR(255) NOT NULL,
  is_default TINYINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_address_user_default (user_id, is_default, is_deleted),
  CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT ck_address_default CHECK (is_default IN (0,1)),
  CONSTRAINT ck_address_deleted CHECK (is_deleted IN (0,1))
) ENGINE=InnoDB COMMENT='用户收货地址表';

CREATE TABLE delivery_rider (
  id BIGINT NOT NULL AUTO_INCREMENT,
  rider_name VARCHAR(32) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT NULL,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_delivery_rider_phone (phone),
  KEY idx_delivery_rider_status (status, is_deleted),
  CONSTRAINT ck_delivery_rider_status CHECK (status IN (0,1)),
  CONSTRAINT ck_delivery_rider_deleted CHECK (is_deleted IN (0,1))
) ENGINE=InnoDB COMMENT='骑手信息表';

CREATE TABLE shopping_cart (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  medicine_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  selected TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cart_user_medicine (user_id, medicine_id),
  KEY idx_cart_user_selected (user_id, selected),
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_cart_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT ck_cart_quantity CHECK (quantity > 0),
  CONSTRAINT ck_cart_selected CHECK (selected IN (0,1))
) ENGINE=InnoDB COMMENT='购物车表';

CREATE TABLE pharmacy_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  address_id BIGINT DEFAULT NULL,
  receiver_name VARCHAR(32) NOT NULL,
  receiver_phone VARCHAR(20) NOT NULL,
  receiver_address VARCHAR(400) NOT NULL,
  product_amount DECIMAL(10,2) NOT NULL,
  delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  order_amount DECIMAL(10,2) NOT NULL,
  order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_ACCEPT',
  rider_id BIGINT DEFAULT NULL,
  rider_name VARCHAR(32) DEFAULT NULL,
  rider_phone VARCHAR(20) DEFAULT NULL,
  user_remark VARCHAR(255) DEFAULT NULL,
  admin_remark VARCHAR(255) DEFAULT NULL,
  accepted_time DATETIME DEFAULT NULL,
  packed_time DATETIME DEFAULT NULL,
  dispatched_time DATETIME DEFAULT NULL,
  completed_time DATETIME DEFAULT NULL,
  canceled_time DATETIME DEFAULT NULL,
  cancel_reason VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pharmacy_order_no (order_no),
  KEY idx_order_user_time (user_id, create_time),
  KEY idx_order_status_time (order_status, create_time),
  KEY idx_order_rider_status (rider_id, order_status),
  KEY idx_order_create_time (create_time),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_order_address FOREIGN KEY (address_id) REFERENCES user_address(id) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_order_rider FOREIGN KEY (rider_id) REFERENCES delivery_rider(id) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT ck_order_product_amount CHECK (product_amount >= 0),
  CONSTRAINT ck_order_delivery_fee CHECK (delivery_fee >= 0),
  CONSTRAINT ck_order_amount CHECK (order_amount >= 0),
  CONSTRAINT ck_order_status CHECK (order_status IN ('PENDING_ACCEPT','TO_PACK','TO_DISPATCH','DELIVERING','COMPLETED','CANCELED'))
) ENGINE=InnoDB COMMENT='订单主表';

CREATE TABLE pharmacy_order_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  medicine_id BIGINT DEFAULT NULL,
  medicine_name VARCHAR(100) NOT NULL,
  medicine_image VARCHAR(255) DEFAULT NULL,
  medicine_price DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL,
  subtotal_amount DECIMAL(10,2) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order_item_order (order_id),
  KEY idx_order_item_medicine (medicine_id),
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES pharmacy_order(id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT ck_order_item_price CHECK (medicine_price >= 0),
  CONSTRAINT ck_order_item_quantity CHECK (quantity > 0),
  CONSTRAINT ck_order_item_subtotal CHECK (subtotal_amount >= 0)
) ENGINE=InnoDB COMMENT='订单明细表';

CREATE TABLE order_status_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  before_status VARCHAR(20) DEFAULT NULL,
  after_status VARCHAR(20) NOT NULL,
  operator_type VARCHAR(16) NOT NULL,
  operator_id BIGINT DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order_status_log_order_time (order_id, create_time),
  CONSTRAINT fk_order_status_log_order FOREIGN KEY (order_id) REFERENCES pharmacy_order(id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT ck_order_status_log_operator CHECK (operator_type IN ('USER','ADMIN','SYSTEM'))
) ENGINE=InnoDB COMMENT='订单状态日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- 演示账号。密码根据课程设计要求明文保存，仅用于本地教学演示。
INSERT INTO sys_user (username, password, nickname, phone, role, status) VALUES
('admin', '123456', '药品管理员', '13800000000', 'ADMIN', 1),
('user01', '123456', '演示用户', '13900000000', 'USER', 1),
('user02', '123456', '张小安', '13600000000', 'USER', 1);

INSERT INTO medicine_category (category_name, category_image, description, sort_no, status) VALUES
('感冒用药', '/images/category/cold.svg', '常见感冒与发热相关药品', 1, 1),
('肠胃用药', '/images/category/stomach.svg', '日常肠胃不适护理', 2, 1),
('维生素营养', '/images/category/vitamin.svg', '维生素与营养补充品', 3, 1),
('外用护理', '/images/category/care.svg', '外用护理与创可贴用品', 4, 1),
('医疗器械', '/images/category/device.svg', '体温计、口罩等用品', 5, 1),
('清热解毒', '/images/category/heat.svg', '清热与日常咽喉护理', 6, 1);

INSERT INTO medicine (category_id, medicine_name, image_url, description, usage_instruction, precautions, price, stock, warning_stock, status) VALUES
(1, '感冒灵颗粒', '/images/medicine/cold.svg', '课程设计演示药品信息，颗粒冲剂包装。', '请按商品说明使用。', '如有不适请咨询专业人员。', 18.50, 35, 5, 1),
(1, '布洛芬缓释胶囊', '/images/medicine/fever.svg', '常见家庭药箱备用商品。', '请按商品说明使用。', '请阅读包装注意事项。', 22.80, 18, 5, 1),
(2, '蒙脱石散', '/images/medicine/stomach.svg', '日常肠胃护理演示商品。', '请按商品说明使用。', '如有不适请咨询专业人员。', 16.90, 24, 5, 1),
(2, '健胃消食片', '/images/medicine/digest.svg', '咀嚼片演示商品。', '请按商品说明使用。', '请阅读包装注意事项。', 14.50, 7, 5, 1),
(3, '维生素C咀嚼片', '/images/medicine/vitamin-c.svg', '营养补充演示商品。', '请按商品说明使用。', '请阅读包装注意事项。', 28.00, 50, 8, 1),
(3, '复合维生素片', '/images/medicine/vitamin.svg', '日常营养补充演示商品。', '请按商品说明使用。', '请阅读包装注意事项。', 39.90, 4, 5, 1),
(4, '碘伏消毒液', '/images/medicine/iodine.svg', '外用护理演示商品。', '请按商品说明使用。', '避免接触眼睛。', 12.80, 31, 5, 1),
(4, '创可贴（经济装）', '/images/medicine/bandage.svg', '家庭常备护理用品。', '请按商品说明使用。', '保持伤口清洁。', 9.90, 40, 10, 1),
(5, '电子体温计', '/images/medicine/thermometer.svg', '家庭健康监测演示商品。', '请按商品说明使用。', '请按说明书维护。', 35.00, 9, 5, 1),
(5, '医用外科口罩', '/images/medicine/mask.svg', '独立包装演示商品。', '请按商品说明使用。', '建议一次性使用。', 19.90, 120, 20, 1),
(6, '金银花含片', '/images/medicine/lozenge.svg', '日常咽喉护理演示商品。', '请按商品说明使用。', '如有不适请咨询专业人员。', 21.50, 17, 5, 1),
(6, '板蓝根颗粒', '/images/medicine/heat.svg', '清热解毒类演示商品。', '请按商品说明使用。', '请阅读包装注意事项。', 17.80, 0, 5, 1);

INSERT INTO user_address (user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default) VALUES
((SELECT id FROM sys_user WHERE username='user01'), '演示用户', '13900000000', '广东省', '惠州市', '惠城区', '演示路 100 号 2 栋 301', 1),
((SELECT id FROM sys_user WHERE username='user02'), '张小安', '13600000000', '广东省', '惠州市', '惠阳区', '健康大道 66 号', 1);

INSERT INTO delivery_rider (rider_name, phone, status, remark) VALUES
('李骑手', '13700000000', 1, '白天班，可接单'),
('王骑手', '13700000001', 1, '晚班，可接单'),
('陈骑手', '13700000002', 0, '今日休息');

-- 两笔当前演示订单，让控制中心打开即有可视化数据。
INSERT INTO pharmacy_order (order_no, user_id, address_id, receiver_name, receiver_phone, receiver_address, product_amount, delivery_fee, order_amount, order_status, rider_id, rider_name, rider_phone, user_remark, admin_remark, accepted_time, packed_time, dispatched_time, completed_time, create_time) VALUES
('PDDEMO000001', (SELECT id FROM sys_user WHERE username='user01'), (SELECT id FROM user_address WHERE receiver_phone='13900000000' LIMIT 1), '演示用户', '13900000000', '广东省惠州市惠城区演示路 100 号 2 栋 301', 38.40, 5.00, 43.40, 'COMPLETED', (SELECT id FROM delivery_rider WHERE phone='13700000000'), '李骑手', '13700000000', '请放在前台', '已确认送达', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 90 MINUTE, NOW() - INTERVAL 70 MINUTE, NOW() - INTERVAL 15 MINUTE, NOW() - INTERVAL 2 HOUR);
SET @completed_order_id = LAST_INSERT_ID();
INSERT INTO pharmacy_order_item (order_id, medicine_id, medicine_name, medicine_image, medicine_price, quantity, subtotal_amount) VALUES
(@completed_order_id, 1, '感冒灵颗粒', '/images/medicine/cold.svg', 18.50, 1, 18.50),
(@completed_order_id, 8, '创可贴（经济装）', '/images/medicine/bandage.svg', 9.90, 2, 19.80);
INSERT INTO order_status_log (order_id, before_status, after_status, operator_type, operator_id, remark) VALUES
(@completed_order_id, NULL, 'PENDING_ACCEPT', 'SYSTEM', NULL, '用户提交订单'),
(@completed_order_id, 'PENDING_ACCEPT', 'TO_PACK', 'ADMIN', (SELECT id FROM sys_user WHERE username='admin'), '已接单，开始备货'),
(@completed_order_id, 'TO_PACK', 'TO_DISPATCH', 'ADMIN', (SELECT id FROM sys_user WHERE username='admin'), '药品已打包完成'),
(@completed_order_id, 'TO_DISPATCH', 'DELIVERING', 'ADMIN', (SELECT id FROM sys_user WHERE username='admin'), '已安排骑手配送'),
(@completed_order_id, 'DELIVERING', 'COMPLETED', 'ADMIN', (SELECT id FROM sys_user WHERE username='admin'), '已确认送达');

INSERT INTO pharmacy_order (order_no, user_id, address_id, receiver_name, receiver_phone, receiver_address, product_amount, delivery_fee, order_amount, order_status, user_remark, create_time) VALUES
('PDDEMO000002', (SELECT id FROM sys_user WHERE username='user02'), (SELECT id FROM user_address WHERE receiver_phone='13600000000' LIMIT 1), '张小安', '13600000000', '广东省惠州市惠阳区健康大道 66 号', 28.00, 5.00, 33.00, 'PENDING_ACCEPT', '请尽快配送', NOW() - INTERVAL 10 MINUTE);
SET @pending_order_id = LAST_INSERT_ID();
INSERT INTO pharmacy_order_item (order_id, medicine_id, medicine_name, medicine_image, medicine_price, quantity, subtotal_amount) VALUES
(@pending_order_id, 5, '维生素C咀嚼片', '/images/medicine/vitamin-c.svg', 28.00, 1, 28.00);
INSERT INTO order_status_log (order_id, before_status, after_status, operator_type, operator_id, remark) VALUES
(@pending_order_id, NULL, 'PENDING_ACCEPT', 'SYSTEM', NULL, '用户提交订单');
