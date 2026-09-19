# MySQL backup scripts

Linux-native bash scripts. No Docker.

- `mysql-backup.sh` — dump + gzip + meta(sha256, table row estimates)
- `mysql-verify.sh` — checksum verification
- `mysql-restore-to-temp.sh` — restore only into temporary DB names
- `test-safety-guards.sh` / `test-safety-guards.ps1` — offline rejection tests
- `mysql-drill-windows.ps1` — Windows dump + gzip + restore-to-temp + COUNT(*) compare

See `docs/ops/backup-restore-drill.md` and `docs/ops/backup-restore-evidence.md`.
Dump files stay in `out/` (gitignored).
