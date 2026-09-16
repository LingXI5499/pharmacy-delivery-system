-- B1: inventory stock-count sheets and items for warehouse reconciliation.
-- Does not alter V1–V3. medicine.stock remains an aggregate read model.

CREATE TABLE inventory_count (
  id BIGINT NOT NULL AUTO_INCREMENT,
  count_no VARCHAR(40) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  remark VARCHAR(255),
  created_by BIGINT NOT NULL,
  completed_by BIGINT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  completed_time DATETIME,
  PRIMARY KEY (id),
  UNIQUE KEY uk_inventory_count_no (count_no),
  KEY idx_inventory_count_status_time (status, create_time),
  CONSTRAINT ck_inventory_count_status CHECK (status IN ('DRAFT','COUNTING','COMPLETED','CANCELED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_count_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  count_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  medicine_id BIGINT NOT NULL,
  book_qty INT NOT NULL,
  counted_qty INT,
  diff_qty INT,
  reason VARCHAR(255),
  operator_id BIGINT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_count_item_batch (count_id, batch_id),
  KEY idx_count_item_medicine (medicine_id),
  CONSTRAINT fk_count_item_count FOREIGN KEY (count_id) REFERENCES inventory_count(id),
  CONSTRAINT fk_count_item_batch FOREIGN KEY (batch_id) REFERENCES medicine_batch(id),
  CONSTRAINT ck_count_item_book CHECK (book_qty >= 0),
  CONSTRAINT ck_count_item_counted CHECK (counted_qty IS NULL OR counted_qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
