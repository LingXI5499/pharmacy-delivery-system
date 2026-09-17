# D1：文档、复盘与面试材料

任务分支：`codex/d1-docs-interview`

基线：`origin/codex/v2-enterprise-upgrade` @ `6e200fb`（P1 已合入）

Q3 覆盖率门禁在独立 PR [#11](https://github.com/LingXI5499/pharmacy-delivery-system/pull/11)，**本分支不夹带测试代码**。Q3 未合并前，不得把本机 94% 写成 GitHub CI 已达标。

本项目是单店药房教学 / 求职作品集，不是可经营真实药店的医疗系统。

## 交付索引

| 材料 | 路径 |
|---|---|
| PRD | [docs/prd.md](../prd.md) |
| 用例 | [docs/use-cases.md](../use-cases.md) |
| ER 图 | [docs/er.md](../er.md) |
| 权限矩阵 | [docs/permission-matrix.md](../permission-matrix.md) |
| 状态图 | [docs/state-machines.md](../state-machines.md) |
| 时序图 | [docs/sequence-diagrams.md](../sequence-diagrams.md) |
| OpenAPI | [docs/openapi.md](../openapi.md) |
| ADR 模块化单体 | [docs/adr/001-modular-monolith.md](../adr/001-modular-monolith.md) |
| ADR 可靠事件 | [docs/adr/002-reliable-events.md](../adr/002-reliable-events.md) |
| ADR 不拆微服务 | [docs/adr/003-why-not-microservices.md](../adr/003-why-not-microservices.md) |
| Redis 复盘 | [docs/reviews/redis.md](../reviews/redis.md) |
| RabbitMQ 复盘 | [docs/reviews/rabbitmq.md](../reviews/rabbitmq.md) |
| 库存并发复盘 | [docs/reviews/inventory-concurrency.md](../reviews/inventory-concurrency.md) |
| 手册入口 | [docs/handbooks.md](../handbooks.md) |
| 演示账号 | [docs/demo-accounts.md](../demo-accounts.md) |
| 简历与讲稿 | [docs/interview.md](../interview.md) |

## 已有证据（可引用）

| 声明 | 证据 |
|---|---|
| 目录读 p95 18.24ms、下单 p95 32.34ms、对账四项为 0 | [docs/reports/p1-performance.md](../reports/p1-performance.md)，Actions [35088568109](https://github.com/LingXI5499/pharmacy-delivery-system/actions/runs/35088568109) |
| V5 两个索引有 EXPLAIN ANALYZE 前后对比 | 同上 |
| 备份脚本拒绝危险路径 | [docs/ops/backup-restore-drill.md](../ops/backup-restore-drill.md)；真实临时库恢复 **未在 D1 环境执行** |
| Q3 行覆盖本机 94%、门禁已写入 pom | 仅 [#11](https://github.com/LingXI5499/pharmacy-delivery-system/pull/11) 本地 `mvn verify`；CI 未验证时不得当作成绩 |

## 明确未完成

- Q3 未合入本基线，CI 覆盖率报告未下载。
- 真实 MySQL 备份→恢复校验和：O1 文档写明未在 Agent 环境执行。
- 开启 RabbitMQ Publisher Confirm 的下单 p95 **未压测**。
- 未合入 `main`，P0 最终回归未做。
