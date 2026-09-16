#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_DIR="${P1_OUT_DIR:-$ROOT/performance/out}"
MYSQL_CMD=(mysql --user="${P1_DB_USER}" --host="${P1_DB_HOST:-127.0.0.1}" --port="${P1_DB_PORT:-3306}" --database="${P1_DB_NAME}" --batch --skip-column-names)
if [[ -n "${P1_DB_PASSWORD:-}" ]]; then
  export MYSQL_PWD="${P1_DB_PASSWORD}"
fi

mkdir -p "${OUT_DIR}"
REPORT="${OUT_DIR}/reconcile.txt"
"${MYSQL_CMD[@]}" < "${ROOT}/performance/sql/reconcile.sql" | tee "${REPORT}"

failed=0
while IFS=$'\t' read -r name count; do
  [[ -z "${name:-}" ]] && continue
  echo "${name}=${count}"
  if [[ "${count}" != "0" ]]; then
    failed=1
  fi
done < "${REPORT}"

if [[ "${failed}" -ne 0 ]]; then
  echo "inventory reconciliation found mismatches" >&2
  exit 1
fi
echo "inventory reconciliation passed"
