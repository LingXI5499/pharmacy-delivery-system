-- Hot-query bundle. Requires @hot_medicine_id, @sample_user_id, @sample_category_id.

SELECT 'Q1 catalog default listing' AS query_label;
EXPLAIN ANALYZE
SELECT id, medicine_name, price, stock, create_time
FROM medicine
WHERE is_deleted = 0 AND status = 1
ORDER BY create_time DESC
LIMIT 12;

SELECT 'Q2 catalog category listing' AS query_label;
EXPLAIN ANALYZE
SELECT id, medicine_name, price, stock, create_time
FROM medicine
WHERE is_deleted = 0 AND status = 1 AND category_id = @sample_category_id
ORDER BY create_time DESC
LIMIT 12;

SELECT 'Q3 catalog keyword listing' AS query_label;
EXPLAIN ANALYZE
SELECT id, medicine_name, price, stock, create_time
FROM medicine
WHERE is_deleted = 0 AND status = 1 AND medicine_name LIKE '%P1 Catalog 12%'
ORDER BY create_time DESC
LIMIT 12;

SELECT 'Q4 FEFO sellable lock list' AS query_label;
START TRANSACTION;
EXPLAIN ANALYZE
SELECT *
FROM medicine_batch
WHERE medicine_id = @hot_medicine_id
  AND sellable = 1
  AND quality_status = 'QUALIFIED'
  AND expiry_date > CURDATE()
  AND available_qty > 0
ORDER BY expiry_date ASC, id ASC
FOR UPDATE;
ROLLBACK;

SELECT 'Q5 near-expiry 90d filter' AS query_label;
EXPLAIN ANALYZE
SELECT id, medicine_id, batch_no, expiry_date, available_qty
FROM medicine_batch
WHERE sellable = 1
  AND quality_status = 'QUALIFIED'
  AND expiry_date > CURDATE()
  AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)
ORDER BY expiry_date ASC, id ASC;

SELECT 'Q6 stock mismatch aggregation' AS query_label;
EXPLAIN ANALYZE
SELECT m.id, m.stock, COALESCE(b.batch_sum, 0) AS batch_sum
FROM medicine m
LEFT JOIN (
    SELECT medicine_id, SUM(available_qty) AS batch_sum
    FROM medicine_batch
    WHERE sellable = 1 AND quality_status = 'QUALIFIED' AND expiry_date > CURDATE()
    GROUP BY medicine_id
) b ON b.medicine_id = m.id
WHERE m.is_deleted = 0 AND m.stock <> COALESCE(b.batch_sum, 0);

SELECT 'Q7 user order history' AS query_label;
EXPLAIN ANALYZE
SELECT id, order_no, order_status, create_time
FROM pharmacy_order
WHERE user_id = @sample_user_id
ORDER BY create_time DESC
LIMIT 10;

SELECT 'Q8 active reservations by order' AS query_label;
START TRANSACTION;
EXPLAIN ANALYZE
SELECT *
FROM inventory_reservation
WHERE order_id = 0 AND status = 'ACTIVE'
ORDER BY id
FOR UPDATE;
ROLLBACK;
