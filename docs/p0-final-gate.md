# P0 最终回归与发布结论

日期：2026-09-19（演示库与备份演练补完）

审查人：集成 Agent（P0）

任务分支：`codex/p0-demo-release`
集成分支目标：`codex/v2-enterprise-upgrade`

**发布形态：教学 / 作品集演示版 `v2.0.0`。不是可经营真实药店的系统，也不是生产容灾或 GSP 合规证明。**

## 本轮补完

| 项 | 结果 | 证据 |
|---|---|---|
| 虚构演示库 `pharmacy_delivery_demo` | 28 SKU，27 个可售库存，`medicine.stock` 与可售批次一致 | `database/demo-seed.sql`、`database/init-demo-database.ps1` |
| 账号 `admin` / `user01` / 药师采购仓管 | 登录成功，密码 `123456`（Spring `$2a$` BCrypt） | 本机 `POST /api/auth/login` |
| 备份→临时库恢复 | COUNT(*) 六表一致后 DROP 临时库 | [ops/backup-restore-evidence.md](ops/backup-restore-evidence.md) |
| 本机 `mvn verify`（独立库 `pharmacy_delivery_verify`，Redis 开启，`MESSAGING_ENABLED=false`） | Tests run: 265, Failures: 0, Errors: 0, Skipped: 0；`All coverage checks have been met.` | 本机 2026-09-19 |
| 本机前端 | `npm ci`、`npm audit --audit-level=high`、`npm run build` 退出码 0 | 本机 2026-09-19 |

未对 `pharmacy_delivery` 做 DROP 或恢复。种子不进 Flyway。

## 仍未执行 / 未声称通过

- 本机未重跑 Playwright E2E；上一轮 CI 在 `38df924`：[35444698922](https://github.com/LingXI5499/pharmacy-delivery-system/actions/runs/35444698922)
- k6 下单 p95 **仍未覆盖** Publisher Confirm（`MESSAGING_ENABLED=false` 的历史报告仍有效，不能当成 confirm 路径成绩）
- 本机 `/actuator/health` 在未启动 RabbitMQ 时为 DOWN，不影响目录与登录
- 退出登录后 Access 拒绝依赖 Redis；本机无 Redis 时该集成测试失败，启动 Redis 后 `mvn verify` 全绿

## 合入说明

用户要求在测试通过后合入 `main` 并打版本标签。本结论允许合入，标签与说明必须写清：教学演示版，不是完整生产稳定版。
