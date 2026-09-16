#!/usr/bin/env bash
# Orchestrates P1 explain-before / V5 / explain-after / optional k6 on native MySQL.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
MODE="${1:-explain}"
OUT_DIR="${P1_OUT_DIR:-$ROOT/performance/out}"
APP_PORT="${P1_APP_PORT:-18093}"
FLYWAY_DIR="${OUT_DIR}/flyway"
LOG_FILE="${OUT_DIR}/backend.log"
PID_FILE="${OUT_DIR}/backend.pid"

mkdir -p "${OUT_DIR}" "${FLYWAY_DIR}"
export P1_OUT_DIR

copy_migrations() {
  local max_version="$1"
  rm -rf "${FLYWAY_DIR}"
  mkdir -p "${FLYWAY_DIR}"
  local file
  for file in "${ROOT}/backend/src/main/resources/db/migration"/V*.sql; do
    local base
    base="$(basename "${file}")"
    local version="${base#V}"
    version="${version%%__*}"
    if [[ "${version}" -le "${max_version}" ]]; then
      cp "${file}" "${FLYWAY_DIR}/"
    fi
  done
}

stop_app() {
  if [[ -f "${PID_FILE}" ]]; then
    local pid
    pid="$(cat "${PID_FILE}")"
    kill "${pid}" 2>/dev/null || true
    wait "${pid}" 2>/dev/null || true
    rm -f "${PID_FILE}"
  fi
}

start_app() {
  stop_app
  (
    cd "${ROOT}/backend"
    nohup java -jar target/pharmacy-delivery-backend-1.0.0.jar --server.port="${APP_PORT}" \
      --spring.flyway.locations="filesystem:${FLYWAY_DIR}" \
      > "${LOG_FILE}" 2>&1 &
    echo $! > "${PID_FILE}"
  )
  local attempt
  for attempt in $(seq 1 45); do
    if curl --fail --silent "http://127.0.0.1:${APP_PORT}/actuator/health" >/dev/null; then
      return 0
    fi
    sleep 2
  done
  cat "${LOG_FILE}"
  return 1
}

record_hardware() {
  {
    echo "captured_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "runner=${RUNNER_NAME:-local}"
    echo "os=$(uname -a)"
    echo "nproc=$(nproc)"
    command -v free >/dev/null && free -h | head -n 2 || true
    java -version 2>&1 | head -n 1
    mysql --version
    k6 version 2>/dev/null || echo "k6=not-installed"
    echo "data_scale=20 categories, 1200 catalog SKUs, 500000 hot units, 100 users, 20000 historical orders"
  } > "${OUT_DIR}/hardware.txt"
}

mysql_seed() {
  MYSQL_PWD="${P1_DB_PASSWORD}" mysql --user="${P1_DB_USER}" --host="${P1_DB_HOST:-127.0.0.1}" \
    --port="${P1_DB_PORT:-3306}" --database="${P1_DB_NAME}" < "${ROOT}/performance/sql/seed.sql"
}

trap stop_app EXIT

record_hardware
copy_migrations 4
start_app
mysql_seed
stop_app
bash "${ROOT}/performance/scripts/capture-explain.sh" before
bash "${ROOT}/performance/scripts/assert-reconcile.sh"

copy_migrations 5
start_app
bash "${ROOT}/performance/scripts/capture-explain.sh" after

if [[ "${MODE}" == "full" ]]; then
  set +e
  k6 run \
    --summary-export="${OUT_DIR}/k6-summary.json" \
    -e "BASE_URL=http://127.0.0.1:${APP_PORT}" \
    -e "VUS=${VUS:-100}" \
    -e "DURATION=${DURATION:-10m}" \
    "${ROOT}/performance/k6/catalog_and_order.js"
  k6_status=$?
  set -e
  # k6 uses 99 when thresholds miss; keep the summary and continue to reconciliation.
  if [[ "${k6_status}" -ne 0 && "${k6_status}" -ne 99 ]]; then
    exit "${k6_status}"
  fi
  echo "${k6_status}" > "${OUT_DIR}/k6-exit-code.txt"
  bash "${ROOT}/performance/scripts/assert-reconcile.sh"
  python3 "${ROOT}/performance/scripts/summarize.py" "${OUT_DIR}/k6-summary.json" "${OUT_DIR}/p1-report.md"
else
  python3 "${ROOT}/performance/scripts/summarize.py" NONE "${OUT_DIR}/p1-report.md"
fi
