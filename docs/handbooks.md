# 手册入口

详细步骤已拆在 ops / performance 文档，这里只做索引，避免复制过时数字。

## SQL 与性能

- 设计与拒绝事项：[p1-performance.md](p1-performance.md)
- 带硬件、k6、EXPLAIN 的报告：[reports/p1-performance.md](reports/p1-performance.md)
- 工作流：`.github/workflows/performance.yml`（不在每次普通 PR 的 `ci.yml` 里跑 10 分钟）
- V5 仅两个有证据的索引；`LIKE %keyword%` 仍是表扫描

## 部署

- Linux 原生模板在 `deploy/`（进程、Prometheus、Grafana）
- 不提供 Docker Compose / K8s
- 本地演示：MySQL `pharmacy_delivery`、可选 Redis、可选 RabbitMQ；消息可关 `MESSAGING_ENABLED=false`

## 备份恢复

- 脚本与护栏：[ops/backup-restore-drill.md](ops/backup-restore-drill.md)
- 真实临时库恢复：**未在 D1 验证**，不能写成已完成演练

## 可观测性

- 日志字段：[ops/structured-logging.md](ops/structured-logging.md)
- 指标与仪表盘：[ops/metrics-and-dashboards.md](ops/metrics-and-dashboards.md)
- 故障步骤：[ops/fault-diagnosis.md](ops/fault-diagnosis.md)
