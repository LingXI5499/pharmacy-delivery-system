# 备份恢复演练证据（P0 演示库）

日期：2026-09-19

环境：Windows 10，MySQL 8.0.34（`localhost:3306`，`useSSL=false` / `--ssl-mode=DISABLED`）

源库：`pharmacy_delivery_demo`（Flyway V1～V5 + `database/demo-seed.sql`）

恢复库：`pharmacy_delivery_restore_tmp`（演练结束已 DROP）

**未**对 `pharmacy_delivery` 做 DROP 或恢复。

## 命令

```powershell
powershell -File deploy/backup/mysql-drill-windows.ps1 -Password "<local mysql password>"
```

脚本：`deploy/backup/mysql-drill-windows.ps1`

备份产物目录：`deploy/backup/out/`（不提交）

## 校验和

| 项 | 值 |
|---|---|
| 备份文件 | `pharmacy_delivery_demo_20260919T142415Z.sql.gz` |
| SHA-256 | `c79c38601dc49d70b5468a77959600e35cc946ecfe45c1a28b8ef6e4a34d8070` |
| meta 中 sha256 | 与上项一致 |

## COUNT(*) 源库 vs 临时库

| 表 | 源库 | 恢复库 |
|---|---:|---:|
| sys_user | 8 | 8 |
| medicine | 28 | 28 |
| medicine_batch | 31 | 31 |
| inventory_ledger | 31 | 31 |
| supplier | 2 | 2 |
| purchase_order | 3 | 3 |

结论：备份可解压恢复，关键表行数一致。这是教学库演练，不是生产容灾证明。
