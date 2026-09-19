# 演示数据库

`pharmacy_delivery.sql` 是 V1 样本，升级后历史库存会进入 `LEGACY_UNKNOWN` 隔离区，前台默认缺货。

教学演示请使用独立库 **`pharmacy_delivery_demo`**：

1. Flyway `V1`～`V5`（与 CI 同一套迁移）
2. `demo-seed.sql` 写入虚构可售批次、角色账号、采购单与期初流水

```powershell
cd database
.\init-demo-database.ps1 -Password "你的本地 MySQL 密码"
```

默认不删除 `pharmacy_delivery`。加 `-Recreate` 只会丢掉并重建 `pharmacy_delivery_demo`。

种子是虚构目录与账号，不是真实药店进销存或患者数据。账号见 [docs/demo-accounts.md](../docs/demo-accounts.md)。
