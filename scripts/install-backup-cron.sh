#!/usr/bin/env bash
set -euo pipefail

BACKUP_TIME="${BACKUP_TIME:-02:30}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
LOG_FILE="$LOG_DIR/backup-postgres.log"
BACKUP_SCRIPT="$ROOT_DIR/scripts/backup-postgres.sh"

if [[ ! "$BACKUP_TIME" =~ ^([01][0-9]|2[0-3]):[0-5][0-9]$ ]]; then
  echo "BACKUP_TIME invalido: $BACKUP_TIME. Usa formato HH:MM, por ejemplo 02:30." >&2
  exit 1
fi

if [[ ! "$BACKUP_RETENTION_DAYS" =~ ^[0-9]+$ || "$BACKUP_RETENTION_DAYS" -lt 1 ]]; then
  echo "BACKUP_RETENTION_DAYS debe ser un numero mayor a 0." >&2
  exit 1
fi

if [[ ! -f "$ROOT_DIR/.env" ]]; then
  echo "No se encontro $ROOT_DIR/.env. Crea el archivo antes de instalar cron." >&2
  exit 1
fi

mkdir -p "$LOG_DIR"
chmod +x "$BACKUP_SCRIPT"

hour="${BACKUP_TIME%%:*}"
minute="${BACKUP_TIME##*:}"
cron_marker="# edificio_app_postgres_backup"
cron_command="$minute $hour * * * cd \"$ROOT_DIR\" && BACKUP_RETENTION_DAYS=\"$BACKUP_RETENTION_DAYS\" bash \"$BACKUP_SCRIPT\" >> \"$LOG_FILE\" 2>&1 $cron_marker"

current_cron="$(mktemp)"
new_cron="$(mktemp)"

crontab -l 2>/dev/null > "$current_cron" || true
grep -v "$cron_marker" "$current_cron" > "$new_cron" || true
echo "$cron_command" >> "$new_cron"
crontab "$new_cron"

rm -f "$current_cron" "$new_cron"

echo "Cron de backup instalado."
echo "Horario: $BACKUP_TIME"
echo "Retencion: $BACKUP_RETENTION_DAYS dias"
echo "Log: $LOG_FILE"
echo ""
echo "Ver cron actual:"
echo "crontab -l"
