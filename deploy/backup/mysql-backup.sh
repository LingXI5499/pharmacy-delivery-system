#!/usr/bin/env bash
# Safe MySQL logical backup for 速安药房 V2 (Linux native, no Docker).
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  mysql-backup.sh --host HOST --port PORT --user USER --password PASS --database DB --output DIR

Safety guards:
  - --database and --output are required and must be non-empty
  - refuses production-looking defaults: root@empty password, database names prod/production
  - refuses dangerous output paths: /, /etc, /usr, /var, /root, empty
EOF
}

HOST=""
PORT="3306"
USER=""
PASSWORD=""
DATABASE=""
OUTPUT=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host) HOST="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --user) USER="$2"; shift 2 ;;
    --password) PASSWORD="$2"; shift 2 ;;
    --database) DATABASE="$2"; shift 2 ;;
    --output) OUTPUT="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown arg: $1" >&2; usage; exit 2 ;;
  esac
done

[[ -n "$HOST" && -n "$USER" && -n "$DATABASE" && -n "$OUTPUT" ]] || { echo "Missing required args" >&2; usage; exit 2; }
[[ -n "$PASSWORD" ]] || { echo "Refusing empty password" >&2; exit 2; }
[[ "$USER" != "root" || "$PASSWORD" != "root" ]] || { echo "Refusing insecure root/root credentials" >&2; exit 2; }
case "$DATABASE" in
  ""|prod|production|mysql|sys|information_schema|performance_schema)
    echo "Refusing database name: $DATABASE" >&2; exit 2 ;;
esac
case "$OUTPUT" in
  ""|"/"|"/etc"|"/usr"|"/var"|"/root"|"/bin"|"/sbin")
    echo "Refusing dangerous output path: $OUTPUT" >&2; exit 2 ;;
esac

mkdir -p "$OUTPUT"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
TARGET="$OUTPUT/${DATABASE}_${STAMP}.sql.gz"
META="$OUTPUT/${DATABASE}_${STAMP}.meta"

export MYSQL_PWD="$PASSWORD"
mysqldump --host="$HOST" --port="$PORT" --user="$USER" \
  --single-transaction --routines --triggers --events \
  --default-character-set=utf8mb4 "$DATABASE" | gzip -c > "$TARGET"

ROW_COUNTS="$(mysql --host="$HOST" --port="$PORT" --user="$USER" --batch --skip-column-names "$DATABASE" -e \
  "SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema='${DATABASE}' ORDER BY table_name;")"
CHECKSUM="$(sha256sum "$TARGET" | awk '{print $1}')"

{
  echo "database=$DATABASE"
  echo "created_at=$STAMP"
  echo "backup_file=$(basename "$TARGET")"
  echo "sha256=$CHECKSUM"
  echo "table_row_estimates<<"
  echo "$ROW_COUNTS"
  echo ">>"
} > "$META"

echo "Backup written: $TARGET"
echo "Meta written: $META"
unset MYSQL_PWD
