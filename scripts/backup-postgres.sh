#!/usr/bin/env bash
set -euo pipefail

CONTAINER_NAME="${CONTAINER_NAME:-edificio_app-postgres-1}"
DATABASE_NAME="${DATABASE_NAME:-edificio_app}"
OUTPUT_DIR="${OUTPUT_DIR:-backups}"

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

docker exec \
  -e "PGPASSWORD=$DB_PASSWORD" \
  "$CONTAINER_NAME" \
  pg_dump \
  -U "$DB_USERNAME" \
  -d "$DATABASE_NAME" \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges > "$backup_path"

echo "Backup creado correctamente."
echo "$backup_path"
