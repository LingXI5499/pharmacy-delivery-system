# V2 开发与验收台账

执行顺序遵守：需求与验收条件 → 风险 / 参考审计 → API、数据、状态设计 → 测试设计 → 实现 → 单元 / 集成 / E2E → 故障验证 → 文档 → PR 复查。

## 当前已实现

- [x] 独立 `codex/v2-enterprise-upgrade` 分支和工作树，保留原工作区用户改动。
- [x] Spring Boot 3.5.16、Security 6、Flyway、Redis、Redisson（可选）、AMQP、Modulith JDBC、OpenAPI、Actuator、Micrometer。
- [x] Session / 明文密码替换为 BCrypt、JWT、HttpOnly Refresh 轮换与重放撤销。
- [x] 采购单审批、部分收货、药品批次、旧库存隔离、FEFO 预占、库存台账。
- [x] 处方内容检测与安全存储、药师审核、支付尝试与幂等模拟回调。
- [x] TTL / DLX 超时关单、Publisher Confirm、事件重放和消费 Inbox。
- [x] 原 24 个测试改为无外部依赖的可重复测试并全绿；前端生产构建全绿。
- [x] Linux systemd / Nginx 模板、Ubuntu 原生 MySQL / Redis / RabbitMQ CI。

## 合并前仍需在 CI 环境完成的证据

- [x] GitHub Actions 已完成空库 Flyway V1→V3 启动，以及仓库现有 V1 样本库原地升级的双路径验证。
- [x] 原生 MySQL 集成测试已验证 100 个并发请求竞争库存 50 时仅成功预占 50，批次和聚合库存均不为负；后续商品失败会回滚此前预占与流水。
- [x] Redis 停止时公共商品读取回退 MySQL 的 CI 故障演练。
- [ ] RabbitMQ 停止与恢复后的持久化事件重放故障演练。
- [ ] 浏览器 E2E、100 VU 10 分钟 k6 报告与索引优化对比。
- [x] 已支付取消进入退款状态机，并实现回调幂等与发货前库存回补规则。
- [ ] 退款故障集成测试、库存盘点 / 对账、监控仪表盘仍属于里程碑 B，不应在完成验证前写入简历。

## 当前覆盖率基线

2026-09-15 本地 `mvn verify` 生成的 JaCoCo 报告为：行覆盖率 16.97%（157 / 925），分支覆盖率 4.74%（36 / 760）。CI 当前只生成报告，不设置虚假的通过阈值。核心服务分支覆盖率 80%、全项目行覆盖率 70% 仍是后续门禁，在补齐 MySQL 集成测试、Security、支付、退款、消息和处方异常用例后才能启用。

质量门禁不允许用删除断言、降低并发正确性或伪造性能数字换取通过。
