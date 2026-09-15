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
