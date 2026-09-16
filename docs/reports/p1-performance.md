# P1 性能报告

本文件在 GitHub Actions `V2 performance` 跑完后，用产物 `performance/out/p1-report.md` 覆盖。当前提交**没有**把未运行的 k6 数字写成已达标。

- 工作流：`.github/workflows/performance.yml`（`workflow_dispatch`、每周日、以及命中 `performance/**` 或 V5 的 PR）
- 脚本：`performance/scripts/run-perf.sh`
- 对账：`performance/sql/reconcile.sql`
- 索引：`V5__performance_indexes.sql`（收益以同一次 job 的 `explain-before.txt` / `explain-after.txt` 为准）

未从 Actions 下载产物前：k6 p95、错误率、EXPLAIN 实际时间均为**未验证**。
