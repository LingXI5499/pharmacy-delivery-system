# 虚构演示账号

全部为教学数据，禁止对接真实患者或支付。密码均为 `123456`。

推荐使用独立库 `pharmacy_delivery_demo`（见 `database/init-demo-database.ps1`），前台可直接浏览有库存的药品并下单。

| 账号 | 角色 | 登录后默认 |
|---|---|---|
| `admin` | ADMIN | `/admin/dashboard` |
| `user01` | USER | `/home`（购物车已预置对乙酰氨基酚、维生素 C） |
| `user02` / `user03` | USER | `/home` |
| `pharmacist` | PHARMACIST | `/pharmacist` |
| `purchaser` | PURCHASER | `/purchaser` |
| `warehouse` | WAREHOUSE | `/warehouse` |
| `rider01` | RIDER | 骑手账号；派单列表另见 `delivery_rider` |

工作台（管理员也可进）：

- 采购 http://localhost:5173/purchaser （本机 5173 被占用时用 5174）
- 仓库 http://localhost:5173/warehouse
- 药师 http://localhost:5173/pharmacist

启动演示档：

```powershell
$env:DB_PASSWORD="你的本地 MySQL 密码"
$env:SPRING_PROFILES_ACTIVE="demo"
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

`application-demo.yml` 指向 `pharmacy_delivery_demo`，并关闭该库上的 Flyway（表结构已由初始化脚本按 V1～V5 打好）。

## 库存现实

- **演示库** `pharmacy_delivery_demo`：28 个虚构 SKU，其中 27 个 `medicine.stock` 与可售批次一致且大于 0；布洛芬 / 维生素 C 有近效期 + 远效期双批次（FEFO）；连花清瘟为待检隔离、板蓝根含过期不可售批次。采购单 `SA-PO-INBOUND` 已批准待收货。
- **旧样本库升级路径**：历史数量在隔离批次 `LEGACY_UNKNOWN`，前台显示缺货。要走完下单，必须先：**采购建单 → 管理员批准 → 仓库收合格批次（未来效期）**。

骑手示例：李骑手、王骑手可用；停用骑手不应出现在派单列表。

压测账号 `p1_user_001` 等只存在于隔离库 `pharmacy_delivery_perf`，不要拿到演示库登录。
