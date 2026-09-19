# 虚构演示账号

全部为教学数据，禁止对接真实患者或支付。

| 账号 | 密码 | 角色 | 登录后默认 |
|---|---|---|---|
| `admin` | `123456` | ADMIN | `/admin/dashboard` |
| `user01` | `123456` | USER | `/home` |

工作台（管理员也可进）：

- 采购 http://localhost:5173/purchaser （本机 5173 被占用时用 5174）
- 仓库 http://localhost:5173/warehouse
- 药师 http://localhost:5173/pharmacist

## 库存现实

升级后的样本库里，历史数量在隔离批次 `LEGACY_UNKNOWN`，前台显示缺货。要走完下单，必须先：**采购建单 → 管理员批准 → 仓库收合格批次（未来效期）**。

骑手示例：李骑手、王骑手可用；停用骑手不应出现在派单列表。

压测账号 `p1_user_001` 等只存在于隔离库 `pharmacy_delivery_perf`，不要拿到演示库登录。
