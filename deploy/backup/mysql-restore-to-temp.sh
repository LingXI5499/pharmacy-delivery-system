#!/usr/bin/env bash
# Restore a pharmacy backup into a TEMPORARY MySQL database only.
set -euo pipefail

HOST=""
PORT="3306"
USER=""
PASSWORD=""
BACKUP=""
TEMP_DATABASE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host) HOST="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --user) USER="$2"; shift 2 ;;
    --password) PASSWORD="$2"; shift 2 ;;
    --backup) BACKUP="$2"; shift 2 ;;
    --temp-database) TEMP_DATABASE="$2"; shift 2 ;;
    *) echo "Unknown arg: $1" >&2; exit 2 ;;
  esac
done

[[ -n "$HOST" && -n "$USER" && -n "$PASSWORD" && -n "$BACKUP" && -n "$TEMP_DATABASE" ]] || {
  echo "Missing required args" >&2
  exit 2
}
[[ -f "$BACKUP" && -s "$BACKUP" ]] || { echo "Backup missing or empty" >&2; exit 2; }

case "$TEMP_DATABASE" in
  ""|pharmacy_delivery|prod|production|mysql|sys|information_schema|performance_schema)
    echo "Refusing restore target database: $TEMP_DATABASE" >&2
    echo "Use a temporary name like pharmacy_delivery_restore_tmp" >&2
    exit 2
    ;;
esac
[[ "$TEMP_DATABASE" == *tmp* || "$TEMP_DATABASE" == *temp* || "$TEMP_DATABASE" == *restore* ]] || {
  echo "Temp database name must contain tmp, temp, or restore" >&2
  exit 2
}

export MYSQL_PWD="$PASSWORD"
mysql --host="$HOST" --port="$PORT" --user="$USER" -e "CREATE DATABASE IF NOT EXISTS \`${TEMP_DATABASE}\` DEFAULT CHARACTER SET utf8mb4;"
gunzip -c "$BACKUP" | mysql --host="$HOST" --port="$PORT" --user="$USER" "$TEMP_DATABASE"

echo "Restored into temporary database: $TEMP_DATABASE"
echo "Row counts:"
mysql --host="$HOST" --port="$PORT" --user="$USER" --batch --skip-column-names "$TEMP_DATABASE" -e \
  "SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema='${TEMP_DATABASE}' ORDER BY table_name;"
unset MYSQL_PWD
