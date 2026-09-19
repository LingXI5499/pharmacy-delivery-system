#!/usr/bin/env bash
# Verify a pharmacy MySQL backup artifact (checksum + meta presence).
set -euo pipefail

BACKUP=""
META=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --backup) BACKUP="$2"; shift 2 ;;
    --meta) META="$2"; shift 2 ;;
    *) echo "Unknown arg: $1" >&2; exit 2 ;;
  esac
done

[[ -n "$BACKUP" && -n "$META" ]] || { echo "Require --backup and --meta" >&2; exit 2; }
[[ -f "$BACKUP" && -f "$META" ]] || { echo "Backup or meta file missing" >&2; exit 2; }
[[ -s "$BACKUP" ]] || { echo "Backup file is empty" >&2; exit 2; }

EXPECTED="$(awk -F= '/^sha256=/{print $2}' "$META")"
ACTUAL="$(sha256sum "$BACKUP" | awk '{print $1}')"
[[ -n "$EXPECTED" ]] || { echo "Meta missing sha256" >&2; exit 2; }
[[ "$EXPECTED" == "$ACTUAL" ]] || { echo "Checksum mismatch" >&2; exit 2; }

echo "Backup verification OK"
echo "sha256=$ACTUAL"
