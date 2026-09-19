# 逻辑 ER（核心表）

与 Flyway V1～V5 一致。`medicine.stock` 不是可编辑主数据。

```mermaid
erDiagram
  sys_user ||--o{ sys_user_role : has
  sys_role ||--o{ sys_user_role : grants
  sys_role ||--o{ sys_role_permission : grants
  sys_permission ||--o{ sys_role_permission : mapped
  sys_user ||--o{ user_address : owns
  sys_user ||--o{ pharmacy_order : places
  medicine_category ||--o{ medicine : contains
  medicine ||--o{ medicine_batch : batches
  inventory_location ||--o{ medicine_batch : stores
  medicine_batch ||--o{ inventory_ledger : ledger
  medicine_batch ||--o{ inventory_reservation : reserved_by
  pharmacy_order ||--o{ pharmacy_order_item : lines
  pharmacy_order ||--o{ inventory_reservation : holds
  pharmacy_order ||--o{ payment_attempt : pays
  payment_attempt ||--o{ refund_record : refunds
  pharmacy_order ||--o{ prescription : may_bind
  supplier ||--o{ purchase_order : supplies
  purchase_order ||--o{ purchase_order_item : lines
  purchase_order ||--o{ purchase_receipt : receipts
  purchase_receipt ||--o{ purchase_receipt_item : lines
  purchase_receipt_item }o--|| medicine_batch : creates
  inventory_count ||--o{ inventory_count_item : snapshots
```

## 不变量

- `inventory_ledger` 只插入，不更新、不删除
- `purchase_order_item`：`received_qty <= ordered_qty`
- `medicine_batch`：`available_qty >= 0 AND reserved_qty >= 0`
- 历史遗留批次 `LEGACY_UNKNOWN` 隔离且不可售，直到仓库收合格批次
