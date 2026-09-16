# MySQL backup scripts

Linux-native bash scripts. No Docker.

- `mysql-backup.sh` — dump + gzip + meta(sha256, table row estimates)
- `mysql-verify.sh` — checksum verification
- `mysql-restore-to-temp.sh` — restore only into temporary DB names
- `test-safety-guards.sh` — offline rejection tests

See `docs/ops/backup-restore-drill.md`.
