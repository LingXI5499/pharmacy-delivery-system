# 参考实现审计

本文件是开发门禁，不是“致谢清单”。每项记录核验提交、业务不变量、采用与拒绝内容；项目未复制第三方源码。

| 项目与核验提交 | 代码级核验点 | 采用的不变量 | 明确拒绝 / 许可证结论 |
|---|---|---|---|
| [Mall4j `f19b355`](https://github.com/gz-yami/mall4j/tree/f19b355fe50485b8c41507e6e646fa431fe064a8) | 订单确认缓存、地址/价格快照、事务订单创建、带库存条件的更新 | 价格和地址在下单时快照；数据库条件更新是库存最终防线 | 不复制 AGPLv3 代码；不引入其多模块商城复杂度 |
| [RuoYi-Vue-Pro `8e43004`](https://github.com/YunaiV/ruoyi-vue-pro/tree/8e43004cf68a405cd3485f98f8a539b97ca6544a) | `SecurityFilterChain`、Token Filter、`@PreAuthorize`、权限服务 | 无状态过滤链、统一 401/403、请求级身份上下文 | 只借鉴 MIT 模式；本项目不照搬平台脚手架 |
| [mall-swarm `04c442f`](https://github.com/macrozheng/mall-swarm/tree/04c442fe318356bae4445a109dde7af297ce1e84) | RabbitMQ TTL 队列、死信交换机、超时取消 | MQ 只触发到期检查，关单与释放仍在数据库事务中判断 | 该示例没有覆盖 Outbox、发布确认、Inbox，不能单独当可靠消息方案 |
| [ERPNext `09dea8b`](https://github.com/frappe/erpnext/tree/09dea8b1b55860646802baab7a8bef8305d47844) | Purchase Receipt、批次强关联、过期批次拦截、Stock Ledger、FEFO 测试 | 收货产生批次；台账只追加；不伪造效期；过期批次不可售 | GPLv3：仅学习领域规则，不复制实现 |
| [OpenBoxes `635b225`](https://github.com/openboxes/openboxes/tree/635b225b0dfc0d07619dad1a6583fbd9d803d5f4) | lot/expiry、库存移动状态、按到期日升序分配 | `expiry_date ASC, id ASC` 的稳定 FEFO 顺序及质量状态过滤 | EPL-1.0：仅交叉验证医疗供应链流程 |

## 本轮落地映射

- `MedicineBatchMapper.selectSellableForUpdate`：可售、合格、未过期、稳定 FEFO，并使用 `FOR UPDATE`。
- `InventoryServiceImpl`：预占 / 确认销售 / 释放 / 入库 / 盘点均生成不可变台账；MySQL 条件更新阻止负库存。
- `AuthServiceImpl`：短期 JWT、随机 Refresh Token 摘要存储、轮换与重放撤销令牌族。
- `ReliableEventPublisher` 与 `OrderTimeoutConsumer`：Modulith JDBC 事件登记、Publisher Confirm、Inbox 唯一键、事务提交后 Ack。
- `PrescriptionService`：Tika 内容检测、随机存储键、根目录归一化、鉴权下载。

后续每个功能 PR 必须在此追加：参考提交、具体代码位置、测试场景、许可证判断和未采用方案。

## Q1：Security 与处方安全测试

| 参考与核验 | 代码级核验点 | 采用的不变量 | 明确拒绝 / 许可证结论 |
|---|---|---|---|
| [RuoYi-Vue-Pro `8e43004`](https://github.com/YunaiV/ruoyi-vue-pro/tree/8e43004cf68a405cd3485f98f8a539b97ca6544a) | `SecurityFilterChain` 401/403、无状态 JWT Filter、角色路径匹配 | 未登录 401、角色不足 403、登出后 Access 进入 deny list | 只借鉴 MIT 模式；不引入其脚手架与代码复制 |
| 本仓库既有实现 | `AuthServiceImpl.refresh` 轮换与 `revokeFamily`；`PrescriptionService` Tika + 路径归一化 + 非本人 `NOT_FOUND` | Refresh 重放撤销令牌族；处方内容检测；越权隐藏存在性；文件仅写入配置目录 | 未改生产代码；本轮仅新增测试。不采用仅 mock Mapper 的假幂等断言 |

测试落点：`JwtServiceTest`、`JwtAuthenticationFilterTest`、`SecurityAccessMvcTest`、`AuthServiceImplTest` 扩展、`PrescriptionServiceTest`（`@TempDir`）、`PrescriptionControllerTest`、`SecurityPrescriptionMySqlIntegrationTest`（`DB_URL` 门控）。

## Q2：支付退款幂等与故障测试

| 参考与核验 | 代码级核验点 | 采用的不变量 | 明确拒绝 / 许可证结论 |
|---|---|---|---|
| [Mall4j `f19b355`](https://github.com/gz-yami/mall4j/tree/f19b355fe50485b8c41507e6e646fa431fe064a8) | 条件更新与订单事务边界 | 支付/退款回调以 DB 行锁 + 状态短路幂等；金额必须与 attempt/refund 一致 | 不复制 AGPLv3 代码 |
| 本仓库既有实现 | `PaymentService.callback` / `RefundService.callback`；`InventoryServiceImpl.refundRestock` | 发货前 `restockRequired=1` 才回补；`COMMITTED→RESTOCKED`；重复回调不重复流水 | 修复 `mark` 误要求 ACTIVE 导致回补后预占未翻转的缺陷；不占用 V4 迁移编号 |

测试落点：`PaymentServiceTest`、`RefundServiceTest`、`PaymentRefundMySqlIntegrationTest`（`DB_URL` 门控，覆盖 10 次并发支付/退款回调）。

## B1：库存盘点、差异对账与近效期

| 参考与核验 | 代码级核验点 | 采用的不变量 | 明确拒绝 / 许可证结论 |
|---|---|---|---|
| [ERPNext `09dea8b`](https://github.com/frappe/erpnext/tree/09dea8b1b55860646802baab7a8bef8305d47844) / [OpenBoxes `635b225`](https://github.com/openboxes/openboxes/tree/635b225b0dfc0d07619dad1a6583fbd9d803d5f4) | 盘点差异入台账、批次强关联、近效期过滤 | `DRAFT→COUNTING→COMPLETED/CANCELED`；完成时锁批次；`STOCK_COUNT` 只追加；对账只读不静默修正；近效期 30/60/90 排除过期/隔离/不可售 | GPLv3/EPL：仅学领域规则；只用 `V4__inventory_count_reconciliation.sql`，不改 V1～V3 |

测试落点：`InventoryCountServiceTest`、`InventoryCountMySqlIntegrationTest`（并发完成、重复完成、对账只读）。

## F1：采购与仓库前端闭环

| 参考与核验 | 代码级核验点 | 采用的不变量 | 明确拒绝 / 许可证结论 |
|---|---|---|---|
| 本仓库既有采购服务 | `ProcurementService` 收货用 PO 行价；状态 `DRAFT/APPROVED/PARTIALLY_RECEIVED/RECEIVED` | 前端不替代后端校验；收货采购价只读展示；角色路由守卫 | 拒绝原因暂写入 `remark` 前缀 `[REJECTED]`，未新增 Flyway 列（避免占 V4）；供应商改删未做 |
| — | 最小契约：`GET` 列表/明细、`POST reject`、仓库可读待收单 | 仅扩展 `procurement` 包只读/拒绝能力以支撑 UI | 不复制第三方 UI 代码 |

前端：`PurchaserView` / `WarehouseView` / `admin/PurchaseOrdersView`；路由 `/admin/purchase-orders`。

## O1：可观测性与运维

| 参考与核验 | 代码级核验点 | 采用的不变量 | 明确拒绝 / 许可证结论 |
|---|---|---|---|
| 本仓库既有 Actuator/Micrometer | `logback-spring.xml` MDC；`RequestLogContext` 脱敏；`PharmacyBusinessMetrics` / `OutstandingEventMetrics` | Trace/user/business/errorCode 结构化日志；禁止记录令牌与处方内容 | 不使用 Docker；不修改主 CI、库存/支付/前端业务 |
| Linux 原生 Prometheus/Grafana | `deploy/prometheus/**`、`deploy/grafana/**` | 仅 loopback scrape；Nginx 继续 deny 公网 `/actuator/` | 仅提供配置与仪表盘导出，不捆绑第三方专有仪表盘授权问题 |
| 备份脚本 | `deploy/backup/*.sh` + `test-safety-guards.sh` | 拒绝空目标/危险路径/生产库恢复 | 本环境未对真实库执行 restore；真实演练需临时实例证据 |

## P1：k6 与索引

| 参考与核验 | 代码级核验点 | 采用的不变量 | 明确拒绝 / 许可证结论 |
|---|---|---|---|
| Grafana k6 `v0.54.0` 官方 Linux amd64 二进制 | `performance/k6/catalog_and_order.js`；独立 `.github/workflows/performance.yml` | 100 VU / 10 min 只在性能工作流运行；对账失败则失败；p95 未达标保留真实结果 | AGPL-3.0：只当压测 CLI 使用，不复制 k6 源码，不引入 Docker |
| 本仓库 V2 批次索引 | `idx_batch_fefo` 保持 `medicine_id` 最左；V5 只加目录 `(is_deleted,status,create_time)` 与跨 SKU 可售批次 `(sellable,quality_status,expiry_date,medicine_id,available_qty)` | FEFO 仍是 `expiry_date ASC, id ASC`；`medicine.stock` 仍是聚合读模型 | 拒绝改 FEFO 最左列、拒绝无 EXPLAIN 堆索引、拒绝把 10 分钟压测写入 `ci.yml` |
