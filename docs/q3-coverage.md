# Q3：覆盖率提升与强制门禁

任务分支：`codex/q3-coverage-gate`  
基线：`origin/codex/v2-enterprise-upgrade` @ `6e200fb`（P1 已合入）  
本地 `mvn verify`（无 `DB_URL`）基线：行覆盖率 **34.76%**（496 / 1427），分支 **20.17%**（186 / 922）。指令覆盖率 25%。14 个 MySQL 集成测试因无 `DB_URL` 跳过，CI 必须执行。

本地补测后 `mvn verify`（2026-09-17，无 `DB_URL`）：**265 通过，0 失败，14 跳过**（原生 MySQL 集成测试）。JaCoCo 报告合计 **行 1345 / 1430（94%）**，**分支 765 / 930（82%）**，指令 **93%**。`jacoco-check` 已通过：BUNDLE 行 ≥ 70%；下列核心类分支均 ≥ 80%（CSV）：

| 类 | 分支 | 行 |
|---|---|---|
| OrderServiceImpl | 98/104（94.23%） | 102/102 |
| AuthServiceImpl | 46/51（90.20%） | 84/86 |
| InventoryServiceImpl | 77/80（96.25%） | 115/115 |
| InventoryCountService | 49/50（98.00%） | 108/108 |
| PaymentService | 31/38（81.58%） | 59/59 |
| RefundService | 37/44（84.09%） | 72/72 |
| PrescriptionService | 76/81（93.83%） | 35/35 |
| ProcurementService | 58/60（96.67%） | 180/180 |
| OrderTimeoutService | 7/8（87.50%） | 1/1 |
| PermissionService | 2/2（100%） | 1/1 |

以上为**本机** `target/site/jacoco` 数字，不能写成 GitHub CI 已达标；合并前须看同一提交的 CI `mvn verify`。

## 1. 正常 / 异常 / 权限边界

正常路径：下单幂等与 OTC 预占、处方药审核通过后预占、采购草稿审批与分批收货、FEFO 预占/确认/释放/入库/调整/盘点/退款回补、支付与退款回调幂等、支付超时关单。

异常路径：幂等键非法、地址越权、购物车不属于当前用户、处方缺失或超量、库存并发条件更新失败、采购非草稿审批、收货超未收数量、盘点并发完成、退款金额不一致、限流与 Redis 降级。

权限边界：本任务不改 Security 规则。未登录 401、角色不足 403、越权 404 已由 Q1 覆盖；本轮补服务层 `FORBIDDEN` / `NOT_FOUND` 不泄露。

## 2. 将改变的 API / 表 / 状态

- **不改** API 契约、Flyway、前端、`.github/workflows/ci.yml`。
- **改** `backend/pom.xml`：JaCoCo `check` 在 `verify` 阶段启用全项目行覆盖率 ≥ 70%；核心 Service 类分支覆盖率 ≥ 80%。
- CI 已执行 `mvn verify`，门禁随 pom 生效，无需改 `ci.yml`。建议 P0 后续上传 `target/site/jacoco` 为 artifact（本任务仅在 PR 中给出建议差异）。

## 3. 明确拒绝的方案

- 不为 getter/setter、实体、VO record 写无断言测试。
- 不把 entity/dto/vo 整包排除出 JaCoCo。排除项仅 `PharmacyDeliveryApplication`（Spring Boot 入口）。
- 不把门槛写成 70% 却用删除断言或跳过业务分支换绿。
- 不提前降低门槛；若一次 PR 达不到 70%，先启用 50% 并说明缺口，不得宣称达标。

## 4. 自动化测试清单

| 模块 | 新增/扩展用例 | 关键断言 |
|---|---|---|
| 订单 | 幂等重放、OTC 预占、处方校验、取消释放、已支付走退款 | 状态、预占调用、退款调用 |
| 库存 | 并发预占失败、确认/释放失败、入库批号冲突、调整/盘点/回补 | 条件更新返回值、台账类型 |
| 采购 | 停用供应商、拒绝原因、收货超量和部分收货 | 状态 `PARTIALLY_RECEIVED`/`RECEIVED` |
| 支付/退款 | 创建尝试、过期、失败回调、用户申请幂等 | 不重复出库/回补 |
| 处方 | 审核通过预占、库存不足关单、驳回必须有原因 | 订单状态与事件 |
| 消息 | 超时关单短路、Inbox 去重、Confirm Nack、重放失败不抛 | 不重复释放 |
| 权限辅助 | 限流 429、Redis 失败放行、`permissionService` 异常为 false | 不阻断核心正确性 |

失败方案：用 Mock Mapper 宣称支付幂等——拒绝；Q2 已有原生 MySQL 10 次回调，本轮补充分支覆盖但不替代集成测试。
