#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.server-nginx.yml}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"
DATABASE_NAME="${DATABASE_NAME:-edificio_app}"
OUTPUT_DIR="${OUTPUT_DIR:-backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "No se encontro $ENV_FILE. Crea un .env basado en .env.example." >&2
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

if [[ -z "${DB_USERNAME:-}" || -z "${DB_PASSWORD:-}" ]]; then
  echo "Faltan DB_USERNAME o DB_PASSWORD en .env." >&2
  exit 1
fi

mkdir -p "$ROOT_DIR/$OUTPUT_DIR"

timestamp="$(date +%Y%m%d-%H%M%S)"
backup_path="$ROOT_DIR/$OUTPUT_DIR/$DATABASE_NAME-$timestamp.sql"

echo "Creando backup de $DATABASE_NAME en $backup_path"

docker compose -f "$COMPOSE_FILE" exec -T \
  -e "PGPASSWORD=$DB_PASSWORD" \
  "$POSTGRES_SERVICE" \
  pg_dump \
  -U "$DB_USERNAME" \
  -d "$DATABASE_NAME" \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges > "$backup_path"

echo "Backup creado correctamente."
echo "$backup_path"

if [[ "$BACKUP_RETENTION_DAYS" =~ ^[0-9]+$ && "$BACKUP_RETENTION_DAYS" -gt 0 ]]; then
  find "$ROOT_DIR/$OUTPUT_DIR" -maxdepth 1 -type f -name "$DATABASE_NAME-*.sql" -mtime +"$BACKUP_RETENTION_DAYS" -delete
  echo "Retencion aplicada: se conservan backups de los ultimos $BACKUP_RETENTION_DAYS dias."
fi
