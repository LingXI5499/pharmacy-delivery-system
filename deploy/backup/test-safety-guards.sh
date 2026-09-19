#!/usr/bin/env bash
# Offline safety-guard tests for backup/restore scripts (no live DB required).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
FAIL=0

expect_fail() {
  local name="$1"; shift
  if "$@" >/tmp/o1-backup-test.out 2>/tmp/o1-backup-test.err; then
    echo "FAIL: $name (expected non-zero)"
    FAIL=1
  else
    echo "PASS: $name"
  fi
}

expect_fail "empty-output-rejected" \
  bash "$ROOT/mysql-backup.sh" --host 127.0.0.1 --user app --password secret --database pharmacy_delivery --output ""

expect_fail "root-path-rejected" \
  bash "$ROOT/mysql-backup.sh" --host 127.0.0.1 --user app --password secret --database pharmacy_delivery --output /

expect_fail "production-db-name-rejected" \
  bash "$ROOT/mysql-backup.sh" --host 127.0.0.1 --user app --password secret --database production --output /tmp/pharmacy-backups

expect_fail "restore-to-primary-rejected" \
  bash "$ROOT/mysql-restore-to-temp.sh" --host 127.0.0.1 --user app --password secret --backup /tmp/x.sql.gz --temp-database pharmacy_delivery

expect_fail "restore-without-temp-token-rejected" \
  bash "$ROOT/mysql-restore-to-temp.sh" --host 127.0.0.1 --user app --password secret --backup /tmp/x.sql.gz --temp-database pharmacy_clone

if [[ "$FAIL" -ne 0 ]]; then
  echo "Safety guard tests failed"
  exit 1
fi
echo "All backup safety guard tests passed"
