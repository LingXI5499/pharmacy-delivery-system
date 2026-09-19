-- Post-load correctness. Each query returns a single mismatch_count column.
-- Job fails if any count is greater than zero.

SELECT 'stock_vs_sellable_batches' AS check_name, COUNT(*) AS mismatch_count
FROM (
    SELECT m.id
    FROM medicine m
    LEFT JOIN (
        SELECT medicine_id, SUM(available_qty) AS batch_sum
        FROM medicine_batch
        WHERE sellable = 1 AND quality_status = 'QUALIFIED' AND expiry_date > CURDATE()
        GROUP BY medicine_id
    ) b ON b.medicine_id = m.id
    WHERE m.is_deleted = 0
      AND m.medicine_name LIKE 'P1 %'
      AND m.stock <> COALESCE(b.batch_sum, 0)
) x

UNION ALL
SELECT 'negative_batch_qty', COUNT(*)
FROM medicine_batch
WHERE batch_no LIKE 'P1-%' AND (available_qty < 0 OR reserved_qty < 0)

UNION ALL
SELECT 'reservation_vs_reserved_qty', COUNT(*)
FROM (
    SELECT b.id
    FROM medicine_batch b
    LEFT JOIN (
        SELECT batch_id, SUM(quantity) AS active_qty
        FROM inventory_reservation
        WHERE status = 'ACTIVE'
        GROUP BY batch_id
    ) r ON r.batch_id = b.id
    WHERE b.batch_no LIKE 'P1-%'
      AND b.reserved_qty <> COALESCE(r.active_qty, 0)
) y

UNION ALL
SELECT 'ledger_without_batch', COUNT(*)
FROM inventory_ledger l
LEFT JOIN medicine_batch b ON b.id = l.batch_id
WHERE b.id IS NULL;
