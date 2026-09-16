-- P1: indexes justified by catalog listing and cross-medicine batch filters.
-- FEFO per-SKU path already uses idx_batch_fefo (medicine_id, sellable, quality_status, expiry_date, id).
-- Before/after EXPLAIN ANALYZE is captured by performance/scripts/capture-explain.sh.

-- Public catalog default: MyBatis-Plus adds is_deleted=0, service filters status=1, sorts create_time DESC.
-- Existing idx_medicine_category_status is category_id-leftmost and does not serve unfiltered listing.
CREATE INDEX idx_medicine_catalog_ctime ON medicine (is_deleted, status, create_time);

-- Near-expiry listing and stock-mismatch aggregation filter sellable/quality/expiry without medicine_id first.
CREATE INDEX idx_batch_sellable_expiry
    ON medicine_batch (sellable, quality_status, expiry_date, medicine_id, available_qty);
