# P0 最终回归与发布结论

日期：2026-09-19  
审查人：集成 Agent（P0）  
集成分支：`codex/v2-enterprise-upgrade` @ `38df924`  
**发布决定：不合入 `main`。可演示、可写进作品集说明，标记为阶段性演示版，不是完整稳定版。**

`main` 上的总升级 PR [#1](https://github.com/LingXI5499/pharmacy-delivery-system/pull/1) 保持打开，不要在本结论前合并。

## 合并顺序（已执行）

| 顺序 | 任务 | PR | 合入 SHA | 合入后 quality gate |
|---|---|---|---|---|
| 1 | Q3 覆盖率门禁 | [#11](https://github.com/LingXI5499/pharmacy-delivery-system/pull/11) | `1ba624b` | [35444282606](https://github.com/LingXI5499/pharmacy-delivery-system/actions/runs/35444282606) 成功 |
| 2 | D1 文档 | [#12](https://github.com/LingXI5499/pharmacy-delivery-system/pull/12) | `38df924` | [35444438308](https://github.com/LingXI5499/pharmacy-delivery-system/actions/runs/35444438308) 成功 |

Q3 合入前 CI 曾在 `f707fec` 失败（SecurityContext 污染），已由 `33e5453` 修复后再合。

## 门禁对照

| 门禁 | 结论 | 证据 |
|---|---|---|
| 后端单元 + 原生 MySQL 集成 | 通过 | `38df924`：`Tests run: 265, Failures: 0, Errors: 0, Skipped: 0`；`jacoco-check`：`All coverage checks have been met.` |
| 前端 `npm ci` / audit high / build | 通过 | 同上 quality gate 步骤 |
| 空库 Flyway 与 V1 原地升级 | 通过 | 同上 |
| 行覆盖 ≥ 70%、核心 Service 分支 ≥ 80% | 门禁在 CI `mvn verify` 中通过；百分比明细以 JaCoCo HTML/CSV 为准 | 合入后 CI 未上传 artifact（本 PR 补上传）。本机 Q3 报告见 [q3-coverage.md](q3-coverage.md)，**不能替代 CI CSV** |
| k6 + 对账 | 通过（HTTP 路径） | [reports/p1-performance.md](reports/p1-performance.md)，Actions [35088568109](https://github.com/LingXI5499/pharmacy-delivery-system/actions/runs/35088568109)；`MESSAGING_ENABLED=false` |
| Redis / RabbitMQ 故障 | CI 步骤通过 | quality gate Redis 回退与 MQ 重放 |
| Playwright E2E | 通过（当前 SHA） | [35444698922](https://github.com/LingXI5499/pharmacy-delivery-system/actions/runs/35444698922)（`workflow_dispatch` on `38df924`）；此前 E1：[35076836489](https://github.com/LingXI5499/pharmacy-delivery-system/actions/runs/35076836489) |
| 备份恢复真实临时库 | **未通过门禁** | [ops/backup-restore-drill.md](ops/backup-restore-drill.md)：护栏脚本有，真实恢复未执行 |
| 无明文凭据 / 无虚假生产描述 | 文档按教学作品集撰写 | [d1/README.md](d1/README.md) |

## 明确拒绝合入 `main` 的原因

1. 备份→临时库恢复校验和没有本次可引用的日志。
2. 压测 p95 未覆盖 Publisher Confirm 路径。
3. 压测 p95 未覆盖 Publisher Confirm 路径。
4. 仓库 `main` 与作品集升级 PR #1 不是本轮集成目标；AGENTS.md 禁止直接推 `main`。

## 本 PR 额外改动

- `ci.yml`：`mvn verify` 后上传 `backend/target/site/jacoco/` artifact，便于以后下载模块覆盖率，而不是只看「check 通过」。
- 未升级 `actions/setup-java` / Node 20 deprecation：属于清理项，不夹带进本结论。

## 演示注意

样本库升级后历史库存在 `LEGACY_UNKNOWN` 隔离批次。验收下单前需要：采购建草稿 → 管理员批准 → 仓库收合格未来效期批次。账号见 [demo-accounts.md](demo-accounts.md)。
