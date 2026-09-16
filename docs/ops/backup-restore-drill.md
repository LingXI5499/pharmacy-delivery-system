# MySQL 备份与临时库恢复演练

脚本目录：`deploy/backup/`（Linux bash，不使用 Docker）。

## 护栏

- 拒绝空目标、空密码、`production`/`prod` 库名
- 拒绝危险输出路径：`/`, `/etc`, `/usr`, `/var`, `/root`
- 恢复只允许名称含 `tmp` / `temp` / `restore` 的临时库
- **禁止**恢复到 `pharmacy_delivery` 主库

本地可先跑离线护栏测试（无需数据库）：

```bash
bash deploy/backup/test-safety-guards.sh
```

## 推荐演练（临时库）

```bash
# 1) 备份（示例变量请换成非生产临时实例）
bash deploy/backup/mysql-backup.sh \
  --host 127.0.0.1 --port 3306 \
  --user pharmacy_app --password "$DB_PASSWORD" \
  --database pharmacy_delivery \
  --output /var/backups/pharmacy

# 2) 校验
bash deploy/backup/mysql-verify.sh \
  --backup /var/backups/pharmacy/pharmacy_delivery_*.sql.gz \
  --meta /var/backups/pharmacy/pharmacy_delivery_*.meta

# 3) 恢复到临时库
bash deploy/backup/mysql-restore-to-temp.sh \
  --host 127.0.0.1 --port 3306 \
  --user pharmacy_app --password "$DB_PASSWORD" \
  --backup /var/backups/pharmacy/pharmacy_delivery_XXXX.sql.gz \
  --temp-database pharmacy_delivery_restore_tmp

# 4) 对比关键行数（示意）
# 源库与临时库 information_schema.tables.table_rows / COUNT(*) 抽查
```

## 本任务验证结果

| 项目 | 结果 |
|---|---|
| 护栏脚本 `test-safety-guards.sh` | 应在 CI/本地 bash 环境通过（拒绝危险路径与生产库名） |
| 真实临时库备份→恢复→校验和 | **未在本 Agent 环境对真实 MySQL 执行**（避免误操作未确认数据库）；合并前由运维/P0 在临时实例补证据 |

恢复标准：临时库关键表可查询；备份 sha256 与 meta 一致；演练后删除临时库。
