# MySQL 备份与临时库恢复演练

脚本目录：`deploy/backup/`（Linux bash + Windows PowerShell；不使用 Docker）。

## 护栏

- 拒绝空目标、空密码、`production`/`prod` 库名
- 拒绝危险输出路径：`/`, `/etc`, `/usr`, `/var`, `/root`（Windows 脚本另拒 `C:\`、`C:\Windows`）
- 恢复只允许名称含 `tmp` / `temp` / `restore` 的临时库
- **禁止**恢复到 `pharmacy_delivery` 主库

本地可先跑离线护栏测试（无需数据库）：

```bash
bash deploy/backup/test-safety-guards.sh
```

```powershell
powershell -File deploy/backup/test-safety-guards.ps1
```

## 推荐演练（临时库）

Linux：

```bash
bash deploy/backup/mysql-backup.sh \
  --host 127.0.0.1 --port 3306 \
  --user pharmacy_app --password "$DB_PASSWORD" \
  --database pharmacy_delivery_demo \
  --output /var/backups/pharmacy

bash deploy/backup/mysql-verify.sh \
  --backup /var/backups/pharmacy/pharmacy_delivery_demo_*.sql.gz \
  --meta /var/backups/pharmacy/pharmacy_delivery_demo_*.meta

bash deploy/backup/mysql-restore-to-temp.sh \
  --host 127.0.0.1 --port 3306 \
  --user pharmacy_app --password "$DB_PASSWORD" \
  --backup /var/backups/pharmacy/pharmacy_delivery_demo_XXXX.sql.gz \
  --temp-database pharmacy_delivery_restore_tmp
```

Windows（本机已执行）：

```powershell
powershell -File deploy/backup/mysql-drill-windows.ps1 -Password $env:DB_PASSWORD
```

源库使用 `pharmacy_delivery_demo`，恢复目标 `pharmacy_delivery_restore_tmp`。备份文件写在 `deploy/backup/out/`（git 忽略，不入库）。

## 本任务验证结果

| 项目 | 结果 |
|---|---|
| 护栏脚本 `test-safety-guards.sh` / `.ps1` | 离线拒绝危险路径与生产库名 |
| 真实临时库备份→恢复→COUNT(*) | **已执行**，见 [backup-restore-evidence.md](backup-restore-evidence.md) |

恢复标准：临时库关键表 `COUNT(*)` 与源库一致；备份 sha256 与 meta 一致；演练后删除临时库。
