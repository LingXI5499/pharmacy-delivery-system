-- V1 reproduces the pre-V2 schema without destructive DROP DATABASE statements.
-- Existing installations are baselined at version 0, so these IF NOT EXISTS
-- statements are no-ops and V2 can upgrade them in place.
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(32) NOT NULL,
  password VARCHAR(100) NOT NULL,
  nickname VARCHAR(32) NOT NULL,
  phone VARCHAR(20),
  role VARCHAR(16) NOT NULL DEFAULT 'USER',
  status TINYINT NOT NULL DEFAULT 1,
  last_login_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_sys_user_username (username),
  CONSTRAINT ck_sys_user_role CHECK (role IN ('USER','ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS medicine_category (
  id BIGINT NOT NULL AUTO_INCREMENT, category_name VARCHAR(50) NOT NULL,
  category_image VARCHAR(255), description VARCHAR(255), sort_no INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1, is_deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_medicine_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS medicine (
  id BIGINT NOT NULL AUTO_INCREMENT, category_id BIGINT NOT NULL,
  medicine_name VARCHAR(100) NOT NULL, image_url VARCHAR(255), description VARCHAR(500),
  usage_instruction VARCHAR(1000), precautions VARCHAR(1000), price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0, warning_stock INT NOT NULL DEFAULT 5,
  status TINYINT NOT NULL DEFAULT 1, version INT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_medicine_category_status (category_id,status,is_deleted),
  CONSTRAINT fk_medicine_category FOREIGN KEY (category_id) REFERENCES medicine_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_address (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, receiver_name VARCHAR(32) NOT NULL,
  receiver_phone VARCHAR(20) NOT NULL, province VARCHAR(32), city VARCHAR(32), district VARCHAR(32),
  detail_address VARCHAR(255) NOT NULL, is_default TINYINT NOT NULL DEFAULT 0,
  is_deleted TINYINT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_address_user_default (user_id,is_default,is_deleted),
  CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS delivery_rider (
  id BIGINT NOT NULL AUTO_INCREMENT, rider_name VARCHAR(32) NOT NULL, phone VARCHAR(20) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1, remark VARCHAR(255), is_deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_delivery_rider_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shopping_cart (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, medicine_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1, selected TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_cart_user_medicine (user_id,medicine_id),
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_cart_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pharmacy_order (
  id BIGINT NOT NULL AUTO_INCREMENT, order_no VARCHAR(32) NOT NULL, user_id BIGINT NOT NULL,
  address_id BIGINT, receiver_name VARCHAR(32) NOT NULL, receiver_phone VARCHAR(20) NOT NULL,
  receiver_address VARCHAR(400) NOT NULL, product_amount DECIMAL(10,2) NOT NULL,
  delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0, order_amount DECIMAL(10,2) NOT NULL,
  order_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_ACCEPT', rider_id BIGINT,
  rider_name VARCHAR(32), rider_phone VARCHAR(20), user_remark VARCHAR(255), admin_remark VARCHAR(255),
  accepted_time DATETIME, packed_time DATETIME, dispatched_time DATETIME, completed_time DATETIME,
  canceled_time DATETIME, cancel_reason VARCHAR(255), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_pharmacy_order_no (order_no),
  KEY idx_order_user_time (user_id,create_time), KEY idx_order_status_time (order_status,create_time),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_order_address FOREIGN KEY (address_id) REFERENCES user_address(id),
  CONSTRAINT fk_order_rider FOREIGN KEY (rider_id) REFERENCES delivery_rider(id),
  CONSTRAINT ck_order_status CHECK (order_status IN ('PENDING_ACCEPT','TO_PACK','TO_DISPATCH','DELIVERING','COMPLETED','CANCELED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pharmacy_order_item (
  id BIGINT NOT NULL AUTO_INCREMENT, order_id BIGINT NOT NULL, medicine_id BIGINT,
  medicine_name VARCHAR(100) NOT NULL, medicine_image VARCHAR(255), medicine_price DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL, subtotal_amount DECIMAL(10,2) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_order_item_order (order_id),
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES pharmacy_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_status_log (
  id BIGINT NOT NULL AUTO_INCREMENT, order_id BIGINT NOT NULL, before_status VARCHAR(32),
  after_status VARCHAR(32) NOT NULL, operator_type VARCHAR(16) NOT NULL, operator_id BIGINT,
  remark VARCHAR(255), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_order_status_log_order_time (order_id,create_time),
  CONSTRAINT fk_order_status_log_order FOREIGN KEY (order_id) REFERENCES pharmacy_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
