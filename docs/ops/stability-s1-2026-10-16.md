# S1 稳定性收敛：交易期限与演示环境

适用：单店药房教学 / 求职演示，全部虚构数据。禁止据此宣称真实药房生产合规。

## 修复范围

1. 无 RabbitMQ 或消息丢失时，从 MySQL 周期性筛选过期且待支付订单，复用现有带行锁、事务的关单逻辑；单轮最多处理 100 单。
2. 模拟支付成功回调在数据库锁后再次检查截止时间，修补过期但尚未被异步消费的窗口。
3. 支付截止时间为空时不自动关单；关闭理由不再写死「30 分钟」。
4. 商品分页缓存由 32 位参数散列改为 Spring 默认组合键，避免不同参数的哈希碰撞造成错页。
5. 正常 Redis 缓存新增默认 5 分钟 TTL，限制缓存失效通知遗漏后的陈旧数据时间；旧缓存需在演示 Redis 中做一次定向清理。
6. demo 关闭 Redis/RabbitMQ 非核心健康指示器，并只在 messaging.enabled 时声明 Rabbit 拓扑；不修改生产健康检测配置。
7. 补充超时、扫描故障、晚到支付、缓存碰撞和 Rabbit 拓扑开关测试；真实 MySQL 验证扫描、竞态、重复关单及释放失败回滚。
8. 在现有依赖版本范围内更新前端锁文件，修复本次 npm audit 阻断；E2E 工作流支持目标为 main 的 PR。
9. 本轮 CI Trace 发现结算页地址初始化结束前就能点击提交，导致首次点击无下单请求、重试累加购物车。结算初始化期间禁用提交，并在处理函数拒绝初始化期及重复提交；新增阻塞地址响应的 Playwright 回归，保留 FEFO 原有严格断言。

## 扫描配置

| 环境变量 | 默认值 | 含义 |
|---|---|---|
| `ORDER_TIMEOUT_RECONCILIATION_INTERVAL_MS` | `60000` | 上轮扫描结束到下轮开始的间隔 |
| `ORDER_TIMEOUT_RECONCILIATION_INITIAL_DELAY_MS` | `10000` | 启动后首次扫描延迟 |
| `REDIS_CACHE_TTL` | `5m` | Redis 缓存 TTL；demo 默认 simple 缓存不使用该 TTL |

每轮最多 100 单。扫描与 `MESSAGING_ENABLED` 无关，消息开启时也保留兜底。多实例可能重复扫描同一 ID，由订单行锁及状态复查守住单次释放。

## 设计边界

- 扫描只获取订单 ID。每个订单必须调用 OrderTimeoutService.closeIfPending，再次检查 status 与 deadline；在同一 MySQL 事务释放预占、更新订单、写入日志。
- RabbitMQ 即使重复投递，超时操作也通过数据库状态和行锁做幂等短路。
- 当前以单店演示规模为目标，100 条/轮上限有积压饥饿风险，需要另行根据订单规模优化游标与索引。
- demo 的 health=UP 只表示启用的健康指示器通过，不代表 Redis/RabbitMQ 可用。

## 回归与验收

1. `cd backend && mvn verify`（若未设 `DB_URL`，部分 MySQL 集成测试将跳过）。
2. `cd frontend && npm ci && npm run build`（前端未改）。
3. 独立 `pharmacy_delivery_demo`，`SPRING_PROFILES_ACTIVE=demo`、`MESSAGING_ENABLED=false`，无需 RabbitMQ 测试登录 / 目录 / OTC 下单。
4. **仅测试库**设置 `app.order.payment-timeout=10s`、`app.order.timeout-reconciliation-interval-ms=1000`；等待自动关闭，验证库存恢复与单次状态日志。
5. 在超过支付期限且扫单尚未完成时发送模拟成功回调；应拒绝且不得出库。
6. Redis 停机时查询目录应回落 MySQL；RabbitMQ 停机并恢复时，不允许重复关单或重复释放库存。
7. 通过 GitHub CI 后再合并；验证不通过即回滚修复分支。

## 2026-10-16 本机验收记录

- Windows、JDK 21（编译目标 Java 17）、MySQL 8.0.34。独立临时 MySQL 实例仅监听 `127.0.0.1:13316`，使用 `pharmacy_s1_verify` / `pharmacy_s1_e2e`；独立 Redis `16316`，不访问原开发库。
- `mvn --batch-mode verify`：**279 测试，0 失败、0 错误、0 跳过**，包括真实 MySQL 集成测试；JaCoCo 门禁通过，行覆盖 1394 / 1463（95.28%），分支覆盖 782 / 936（83.55%）。Java 17 环境由 PR 的 CI 进一步验证。
- 首次完整测试在 Redis 缺失时暴露既有登出 deny-list 降级限制；启动隔离 Redis 后完整通过，未跳过或降低断言。
- `npm ci`、`npm audit --audit-level=high`、`npm run build`：构建通过；首次审计 4 个 high，按现有版本约束更新锁文件后为 **0 漏洞**。保留现有大 chunk 构建提示。
- demo profile + `MESSAGING_ENABLED=false` + 20 秒支付期限 + 1 秒扫描间隔，真实 HTTP 验证登录、目录、购物车、同 Key 重复下单、10 次重复支付回调；数据库断言仅一次销售流水。
- 将测试订单期限设为已到期，由后台调度器自动关单；验证批次可用量恢复、预占清零、单次 `ORDER_RELEASE` 和单次状态日志，迟到成功回调返回 `40902`。
- demo `/actuator/health` 返回 `UP`。本机 Playwright 因缓存 Chromium 启动 `spawn UNKNOWN` 未执行浏览器场景；浏览器 E2E、Linux 原生 RabbitMQ 停机恢复及 V1 样本升级以本次 PR 的 GitHub CI 结果为准。
- 实际 Redis 缓存存在且 TTL 在 300 秒内；关闭 Redis 后目录回退 MySQL，demo 健康仍 `UP`。关闭临时 MySQL 后支付请求被认证过滤器拒绝（HTTP 401，业务码 40101），未虚假报告支付成功。
- 首轮 GitHub quality gate 通过；首轮浏览器 8/9 通过，Trace 定位上述结算初始化竞态，修复后重新执行完整 CI。

## 保留风险

- `OrderServiceImpl` 内管理员状态流转和用户取消部分仍是非锁定读，存在并发竞态，需要单独修补 + 并发测试。
- `application.yml` 存在固定的 JWT 开发密钥默认值；部署到公网必须使用强随机密钥，后续建议默认启动失败而非静默回退。
- Redis deny list 失败时，退出后的 Access JWT 在剩余有效期内仍可能有效。当前是系统原有安全降级策略，不是彻底的即时撤销。
- 不追加 RabbitMQ Publisher Confirm 压测或公网容量结论；教学演示定位保持不变。本次实际验证结果见本文件后续验收记录及 PR，不引用旧报告作为本轮测试结果。
