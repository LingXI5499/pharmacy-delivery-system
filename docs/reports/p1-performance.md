# P1 性能报告

证据来源（同一提交 `5458bf4`）：

- GitHub Actions：https://github.com/LingXI5499/pharmacy-delivery-system/actions/runs/35088568109
- 工作流：`V2 performance`（未写入 `ci.yml`）
- k6 退出码：`0`
- 压测后对账：四项 mismatch 均为 0

## 环境

| 项 | 值 |
|---|---|
| Runner | GitHub-hosted Ubuntu，4 vCPU，15 GiB 内存 |
| OS | Linux 6.17.0-1022-azure x86_64 |
| Java | Temurin 17.0.20.1 |
| MySQL | 8.0.46 |
| k6 | v0.54.0 |
| 数据 | 20 分类、1200 目录 SKU、热销 OTC 500000 可售、100 用户、20000 条历史订单 |
| 应用配置 | `SPRING_CACHE_TYPE=simple`（目录走 MySQL）；`MESSAGING_ENABLED=false`（不跑 Publisher Confirm） |

k6 场景：99 个读 VU + 1 个下单 VU，共 100 VU，持续 10 分钟。下单间隔 2.2s，以遵守现有 `POST /api/user/orders` 每 IP 每分钟 30 次限流。

## k6 结果

| 指标 | 目标 | 结果 |
|---|---|---|
| 目录读 p95 (`catalog_read_ms`) | < 500ms | **18.24 ms** |
| 下单 p95 (`order_create_ms`) | < 1000ms | **32.34 ms** |
| 业务成功比率 (`business_ok`) | > 99% | **99.77%**（286680 / 672） |
| HTTP 失败率 | < 1% | **0.23%** |
| 成功下单 | 记录 | **233**（HTTP 200 且 `code=0` 233，失败 0） |
| 全请求 p95 | 参考 | 22.11 ms |

订单列表有 653 次失败（约 1.5% 的列表请求）。Access Token TTL 为 15 分钟，setup 预登录约 6 分钟，10 分钟压测末期部分早期 token 过期，这是已知窗口，没有放宽鉴权。

## 对账

压测结束后：

```
stock_vs_sellable_batches    0
negative_batch_qty           0
reservation_vs_reserved_qty  0
ledger_without_batch         0
```

热销 SKU 按 FEFO 预占；`medicine.stock` 与可售合格未过期批次可用量一致；无负库存。

## EXPLAIN ANALYZE（同一次 job，V5 前后）

Q1 目录默认列表 `status=1 AND is_deleted=0 ORDER BY create_time DESC LIMIT 12`：

- V4：`Table scan` + filesort，actual time **1.09 ms**，扫描 1201 行。
- V5：`Index range scan` `idx_medicine_catalog_ctime`（reverse），actual time **0.108 ms**，索引直接取 12 行。

Q5 近效期 90 天：

- V4：`Table scan` `medicine_batch` 2482 行后过滤，cost 250。
- V5：`Index range scan` `idx_batch_sellable_expiry`，只读 81 行。

Q4 FEFO 单 SKU 仍使用既有 `idx_batch_fefo`（`medicine_id` 最左），未改排序。

因此保留 `V5__performance_indexes.sql` 两个索引。LIKE `'%keyword%'` 仍是表扫描，本轮不加前缀索引（没有等值/前缀查询证据）。

## 明确未覆盖

- 未在开启 RabbitMQ Publisher Confirm 的路径上测下单 p95。
- 未把 10 分钟 k6 加进每次普通 PR 的 `ci.yml`。
- 本机 Windows 全量 k6 **未验证**。
- 1200 行目录规模下 p95 很容易低于 500ms；更大数量级需要重新跑本工作流，不能外推。
