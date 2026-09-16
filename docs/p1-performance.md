# P1：k6 压测、慢 SQL 与索引

任务分支：`codex/p1-performance-sql`  
依赖：B1（V4 已合并）、E1（演示数据与独立 E2E 工作流已合并）  
所有权：`performance/**`、`backend/src/main/resources/db/migration/V5__performance_indexes.sql`、独立 `performance.yml`。不修改 `.github/workflows/ci.yml`。

## 1. 正常 / 异常 / 权限

正常路径：

- 使用隔离库 `pharmacy_delivery_perf` 和虚构用户 `p1_user_001`～`p1_user_100`（密码 `test123456`）。
- 读流量走 `GET /api/public/medicines`（默认排序、分类、关键词分页）。
- 写流量走已登录用户：加购 → 提交订单（`Idempotency-Key` 每次唯一）→ 查询自己的订单列表。
- 压测结束后用 SQL 对账：`medicine.stock` 与可售批次可用量、批次非负、ACTIVE 预占与 `reserved_qty`。

异常路径：

- 库存耗尽或状态冲突返回 HTTP 409，计入业务失败，不改断言放宽库存约束。
- 未登录访问 `/api/user/**` 返回 401；USER 角色访问 `/api/warehouse/**` 返回 403（不在主 k6 场景里制造噪声，仅文档约束）。

权限边界：

- 压测账号只有 `USER`。不使用真实支付、真实患者或生产库。
- 普通 PR 不跑 10 分钟 k6；仅本工作流在 `performance/**` 或 V5 变更、以及 `workflow_dispatch` / 周调度时运行。

## 2. API / 表 / 状态与兼容

- 不新增业务 API，不改订单状态机。
- 仅允许新增 `V5__performance_indexes.sql`。V1～V4 只读。
- `medicine.stock` 仍是可售批次聚合读模型；索引不得改写 FEFO 顺序 `expiry_date ASC, id ASC`。
- 目录缓存仍可降级；压测默认使用 Redis，SQL 证据来自 `EXPLAIN ANALYZE`，不把缓存命中写成数据库已优化。

## 3. 拒绝的失败方案

- 把 Redis / k6 阈值当成库存正确性来源。
- 为了让 p95 变绿而关闭条件更新、行锁或唯一键。
- 用 Docker 跑 MySQL/k6，或把 10 分钟压测塞进 `ci.yml`。
- 没有 `EXPLAIN ANALYZE` 前后对比就堆索引（包括改 FEFO 最左列）。

## 4. 自动化清单

| 项 | 何时 | 失败标准 |
|---|---|---|
| Flyway 空库含 V5 | 现有 quality gate 启动应用 | 应用起不来 |
| `EXPLAIN ANALYZE` 前后对比 | `performance.yml` | 脚本失败；索引无收益则不得声称已优化 |
| 对账 SQL | 种子后与 k6 后 | 任一差异计数 > 0 |
| k6 100 VU / 10 min | 路径命中的 PR、手动、周调度 | 对账失败则 job 失败；p95 未达标只写入报告 |

复现：

```bash
# GitHub：Actions → V2 performance → Run workflow
# 或等待命中 performance/** 的 PR job
```
