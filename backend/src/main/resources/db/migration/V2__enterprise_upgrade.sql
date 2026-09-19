-- Identity and RBAC
ALTER TABLE sys_user DROP CHECK ck_sys_user_role;
ALTER TABLE sys_user MODIFY password VARCHAR(100) NOT NULL;
ALTER TABLE sys_user MODIFY role VARCHAR(24) NOT NULL DEFAULT 'USER';

CREATE TABLE sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT, role_code VARCHAR(32) NOT NULL, role_name VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sys_permission (
  id BIGINT NOT NULL AUTO_INCREMENT, permission_code VARCHAR(80) NOT NULL, permission_name VARCHAR(100) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL, role_id BIGINT NOT NULL, PRIMARY KEY (user_id,role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sys_role_permission (
  role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL, PRIMARY KEY (role_id,permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
  CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE refresh_token (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, token_hash CHAR(64) NOT NULL,
  family_id CHAR(36) NOT NULL, expires_at DATETIME NOT NULL, revoked_at DATETIME,
  replaced_by_hash CHAR(64), ip_address VARCHAR(64), user_agent VARCHAR(255),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_refresh_token_hash (token_hash), KEY idx_refresh_family (family_id),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Catalog compatibility: old aggregate stock is retained as a read model only.
ALTER TABLE medicine ADD COLUMN prescription_required TINYINT NOT NULL DEFAULT 0 AFTER warning_stock;

-- Procurement
CREATE TABLE supplier (
  id BIGINT NOT NULL AUTO_INCREMENT, supplier_code VARCHAR(32) NOT NULL, supplier_name VARCHAR(100) NOT NULL,
  contact_name VARCHAR(50), phone VARCHAR(20), status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE purchase_order (
  id BIGINT NOT NULL AUTO_INCREMENT, purchase_no VARCHAR(32) NOT NULL, supplier_id BIGINT NOT NULL,
  status VARCHAR(24) NOT NULL, applicant_id BIGINT NOT NULL, approver_id BIGINT, approved_time DATETIME,
  remark VARCHAR(255), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_purchase_no (purchase_no), KEY idx_purchase_status_time (status,create_time),
  CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE purchase_order_item (
  id BIGINT NOT NULL AUTO_INCREMENT, purchase_order_id BIGINT NOT NULL, medicine_id BIGINT NOT NULL,
  ordered_qty INT NOT NULL, received_qty INT NOT NULL DEFAULT 0, purchase_price DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_purchase_item (purchase_order_id,medicine_id),
  CONSTRAINT fk_purchase_item_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_order(id),
  CONSTRAINT fk_purchase_item_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(id),
  CONSTRAINT ck_purchase_qty CHECK (ordered_qty > 0 AND received_qty >= 0 AND received_qty <= ordered_qty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE purchase_receipt (
  id BIGINT NOT NULL AUTO_INCREMENT, receipt_no VARCHAR(32) NOT NULL, purchase_order_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL, received_time DATETIME NOT NULL, remark VARCHAR(255),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_receipt_no (receipt_no),
  CONSTRAINT fk_receipt_purchase FOREIGN KEY (purchase_order_id) REFERENCES purchase_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Batch inventory, reservations, and immutable ledger
CREATE TABLE inventory_location (
  id BIGINT NOT NULL AUTO_INCREMENT, location_code VARCHAR(32) NOT NULL, location_name VARCHAR(64) NOT NULL,
  sellable TINYINT NOT NULL DEFAULT 1, status TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (id), UNIQUE KEY uk_location_code (location_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE medicine_batch (
  id BIGINT NOT NULL AUTO_INCREMENT, medicine_id BIGINT NOT NULL, location_id BIGINT NOT NULL,
  batch_no VARCHAR(64) NOT NULL, production_date DATE, expiry_date DATE,
  purchase_price DECIMAL(10,2), available_qty INT NOT NULL DEFAULT 0, reserved_qty INT NOT NULL DEFAULT 0,
  quality_status VARCHAR(20) NOT NULL DEFAULT 'QUALIFIED', sellable TINYINT NOT NULL DEFAULT 1,
  version INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_batch_medicine_location_no (medicine_id,location_id,batch_no),
  KEY idx_batch_fefo (medicine_id,sellable,quality_status,expiry_date,id),
  CONSTRAINT fk_batch_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(id),
  CONSTRAINT fk_batch_location FOREIGN KEY (location_id) REFERENCES inventory_location(id),
  CONSTRAINT ck_batch_quantities CHECK (available_qty >= 0 AND reserved_qty >= 0),
  CONSTRAINT ck_batch_dates CHECK (production_date IS NULL OR expiry_date IS NULL OR production_date < expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE purchase_receipt_item (
  id BIGINT NOT NULL AUTO_INCREMENT, receipt_id BIGINT NOT NULL, purchase_order_item_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL, qualified_qty INT NOT NULL DEFAULT 0, rejected_qty INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id), CONSTRAINT fk_receipt_item_receipt FOREIGN KEY (receipt_id) REFERENCES purchase_receipt(id),
  CONSTRAINT fk_receipt_item_order_item FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_item(id),
  CONSTRAINT fk_receipt_item_batch FOREIGN KEY (batch_id) REFERENCES medicine_batch(id),
  CONSTRAINT ck_receipt_qty CHECK (qualified_qty >= 0 AND rejected_qty >= 0 AND qualified_qty + rejected_qty > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE inventory_reservation (
  id BIGINT NOT NULL AUTO_INCREMENT, reservation_no VARCHAR(40) NOT NULL, order_id BIGINT NOT NULL,
  order_item_id BIGINT NOT NULL, batch_id BIGINT NOT NULL, quantity INT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', expires_at DATETIME NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_reservation_no (reservation_no), KEY idx_reservation_order_status (order_id,status),
  CONSTRAINT fk_reservation_order FOREIGN KEY (order_id) REFERENCES pharmacy_order(id),
  CONSTRAINT fk_reservation_item FOREIGN KEY (order_item_id) REFERENCES pharmacy_order_item(id),
  CONSTRAINT fk_reservation_batch FOREIGN KEY (batch_id) REFERENCES medicine_batch(id),
  CONSTRAINT ck_reservation_qty CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE inventory_ledger (
  id BIGINT NOT NULL AUTO_INCREMENT, event_no VARCHAR(40) NOT NULL, business_type VARCHAR(32) NOT NULL,
  business_id VARCHAR(64) NOT NULL, medicine_id BIGINT NOT NULL, batch_id BIGINT NOT NULL,
  available_delta INT NOT NULL DEFAULT 0, reserved_delta INT NOT NULL DEFAULT 0,
  available_after INT NOT NULL, reserved_after INT NOT NULL, operator_id BIGINT, reason VARCHAR(255),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_ledger_event_no (event_no),
  KEY idx_ledger_business (business_type,business_id), KEY idx_ledger_batch_time (batch_id,create_time),
  CONSTRAINT fk_ledger_batch FOREIGN KEY (batch_id) REFERENCES medicine_batch(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Prescription, payment and audit
CREATE TABLE prescription (
  id BIGINT NOT NULL AUTO_INCREMENT, prescription_no VARCHAR(32) NOT NULL, user_id BIGINT NOT NULL,
  order_id BIGINT, storage_key VARCHAR(255) NOT NULL, original_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(80) NOT NULL, size_bytes BIGINT NOT NULL, sha256 CHAR(64) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING_REVIEW', reviewer_id BIGINT, review_reason VARCHAR(255), reviewed_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_prescription_no (prescription_no), KEY idx_prescription_review (status,create_time),
  CONSTRAINT fk_prescription_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_prescription_order FOREIGN KEY (order_id) REFERENCES pharmacy_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE prescription_item (
  id BIGINT NOT NULL AUTO_INCREMENT, prescription_id BIGINT NOT NULL, medicine_id BIGINT NOT NULL,
  prescribed_qty INT NOT NULL, PRIMARY KEY (id), UNIQUE KEY uk_prescription_medicine (prescription_id,medicine_id),
  CONSTRAINT fk_pi_prescription FOREIGN KEY (prescription_id) REFERENCES prescription(id),
  CONSTRAINT fk_pi_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE payment_attempt (
  id BIGINT NOT NULL AUTO_INCREMENT, payment_no VARCHAR(40) NOT NULL, order_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', callback_key VARCHAR(80),
  paid_time DATETIME, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_payment_no (payment_no), UNIQUE KEY uk_payment_callback (callback_key),
  CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES pharmacy_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE refund_record (
  id BIGINT NOT NULL AUTO_INCREMENT, refund_no VARCHAR(40) NOT NULL, payment_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL, amount DECIMAL(10,2) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  callback_key VARCHAR(80), reason VARCHAR(255), original_order_status VARCHAR(32) NOT NULL,
  restock_required TINYINT NOT NULL DEFAULT 0, refunded_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_refund_no (refund_no), UNIQUE KEY uk_refund_callback (callback_key), UNIQUE KEY uk_refund_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE inbox_event (
  id BIGINT NOT NULL AUTO_INCREMENT, consumer_name VARCHAR(80) NOT NULL, event_id VARCHAR(80) NOT NULL,
  event_type VARCHAR(120) NOT NULL, processed_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uk_inbox_consumer_event (consumer_name,event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE business_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT, trace_id VARCHAR(64), actor_id BIGINT, actor_role VARCHAR(24),
  event_type VARCHAR(80) NOT NULL, business_type VARCHAR(40) NOT NULL, business_id VARCHAR(64),
  result VARCHAR(16) NOT NULL, details_json JSON, client_ip VARCHAR(64), create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_audit_actor_time (actor_id,create_time), KEY idx_audit_business (business_type,business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE pharmacy_order ADD COLUMN idempotency_key VARCHAR(80) AFTER user_id;
ALTER TABLE pharmacy_order ADD COLUMN prescription_id BIGINT AFTER idempotency_key;
ALTER TABLE pharmacy_order ADD COLUMN payment_deadline DATETIME AFTER order_status;
ALTER TABLE pharmacy_order ADD COLUMN paid_time DATETIME AFTER payment_deadline;
ALTER TABLE pharmacy_order ADD UNIQUE KEY uk_order_user_idempotency (user_id,idempotency_key);
ALTER TABLE pharmacy_order DROP CHECK ck_order_status;
ALTER TABLE pharmacy_order MODIFY order_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT';

INSERT INTO inventory_location(location_code,location_name,sellable,status)
VALUES ('MAIN','主仓可售区',1,1),('LEGACY_QUARANTINE','历史库存隔离区',0,1);
INSERT INTO sys_role(role_code,role_name) VALUES
('USER','普通用户'),('PHARMACIST','药师'),('PURCHASER','采购员'),('WAREHOUSE','仓库人员'),('RIDER','配送员'),('ADMIN','管理员');
INSERT INTO sys_user_role(user_id,role_id)
SELECT u.id,r.id FROM sys_user u JOIN sys_role r ON r.role_code=u.role;
INSERT INTO sys_permission(permission_code,permission_name) VALUES
('prescription.review','处方审核'),('procurement.write','采购管理'),('inventory.write','库存作业'),
('inventory.read','库存查询'),('order.fulfill','订单履约'),('audit.read','审计查询');
INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code='ADMIN'
   OR (r.role_code='PHARMACIST' AND p.permission_code='prescription.review')
   OR (r.role_code='PURCHASER' AND p.permission_code='procurement.write')
   OR (r.role_code='WAREHOUSE' AND p.permission_code IN ('inventory.read','inventory.write'));

-- Never invent an expiry date. Legacy stock remains quarantined until a warehouse
-- operator supplies a real batch number and dates through the receipt/adjustment flow.
INSERT INTO medicine_batch(medicine_id,location_id,batch_no,available_qty,reserved_qty,quality_status,sellable)
SELECT m.id,l.id,'LEGACY_UNKNOWN',m.stock,0,'QUARANTINED',0
FROM medicine m JOIN inventory_location l ON l.location_code='LEGACY_QUARANTINE'
WHERE m.stock > 0;
UPDATE medicine SET stock=0 WHERE stock<>0;
