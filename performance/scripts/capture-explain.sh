#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_DIR="${P1_OUT_DIR:-$ROOT/performance/out}"
MYSQL_CMD=(mysql --user="${P1_DB_USER}" --host="${P1_DB_HOST:-127.0.0.1}" --port="${P1_DB_PORT:-3306}" --database="${P1_DB_NAME}" --batch)
if [[ -n "${P1_DB_PASSWORD:-}" ]]; then
  export MYSQL_PWD="${P1_DB_PASSWORD}"
fi

mkdir -p "${OUT_DIR}"
LABEL="${1:?usage: capture-explain.sh before|after}"
OUT_FILE="${OUT_DIR}/explain-${LABEL}.txt"

{
  echo "# EXPLAIN ANALYZE ${LABEL}"
  echo "captured_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo
  "${MYSQL_CMD[@]}" -e "SELECT @hot_medicine_id := id FROM medicine WHERE medicine_name='P1 HOT 压测装量' LIMIT 1; SELECT @sample_user_id := id FROM sys_user WHERE username='p1_user_001' LIMIT 1; SELECT @sample_category_id := id FROM medicine_category WHERE category_name='P1-CAT-01' LIMIT 1; SELECT @hot_medicine_id AS hot_medicine_id, @sample_user_id AS sample_user_id, @sample_category_id AS sample_category_id;"
  echo
} > "${OUT_FILE}"

# Session variables from -e do not survive a second connection. Bind them in one client.
{
  echo "SET @hot_medicine_id := (SELECT id FROM medicine WHERE medicine_name='P1 HOT 压测装量' LIMIT 1);"
  echo "SET @sample_user_id := (SELECT id FROM sys_user WHERE username='p1_user_001' LIMIT 1);"
  echo "SET @sample_category_id := (SELECT id FROM medicine_category WHERE category_name='P1-CAT-01' LIMIT 1);"
  cat "${ROOT}/performance/sql/explain_hot_queries.sql"
} | "${MYSQL_CMD[@]}" --table >> "${OUT_FILE}"

{
  echo
  echo "# SHOW INDEX"
  "${MYSQL_CMD[@]}" --table -e "SHOW INDEX FROM medicine; SHOW INDEX FROM medicine_batch;"
} >> "${OUT_FILE}"

echo "wrote ${OUT_FILE}"
