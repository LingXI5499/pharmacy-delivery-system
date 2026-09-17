# 复盘：库存并发与 FEFO

## 问题

两个请求同时抢同一可售数量时，应用层 `if (stock >= qty)` 会超卖。药品编辑表单若能写 `medicine.stock`，读模型会和批次对不上。

## 约束

- 可售：未过期、合格、可销售、可用量 > 0
- FEFO 固定 `expiry_date ASC, id ASC`
- 台账只追加
- 遗留库存不得伪造效期

## 方案

1. `selectSellableForUpdate` 按 FEFO 锁行
2. `UPDATE ... WHERE available_qty >= :qty` 一类条件更新；失败即冲突
3. 同步维护 `medicine.stock` 聚合；编辑页库存框禁用
4. V1 升级把旧库存放进隔离批次 `LEGACY_UNKNOWN`

## 失败方案

用 Redis 分布式锁当唯一防线。拒绝：锁过期仍会超卖；最终要以 SQL 条件更新为准。

## 测试 / 结果

- 库存服务单测：预占/确认/释放/入库/盘点/退款回补
- CI 原生 MySQL：库存 50 时 100 并发只成功 50（见 remaining-development-plan 基线描述，以当时 CI 为准）
- P1 压测后对账四项为 0：[reports/p1-performance.md](../reports/p1-performance.md)
