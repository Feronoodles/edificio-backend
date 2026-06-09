#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.server-nginx.yml}"
POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"
MAIN_DATABASE_NAME="${MAIN_DATABASE_NAME:-edificio_app}"
RESTORE_DATABASE_NAME="${RESTORE_DATABASE_NAME:-edificio_app_restore_check}"
BACKUP_DIR="${BACKUP_DIR:-backups}"
BACKUP_PATH="${1:-}"

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

if [[ "$RESTORE_DATABASE_NAME" == "$MAIN_DATABASE_NAME" ]]; then
  echo "Por seguridad, este script no restaura sobre la base principal '$MAIN_DATABASE_NAME'." >&2
  exit 1
fi

if [[ ! "$RESTORE_DATABASE_NAME" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
  echo "Nombre de base de datos invalido: '$RESTORE_DATABASE_NAME'." >&2
  exit 1
fi

if [[ -z "$BACKUP_PATH" ]]; then
  BACKUP_PATH="$(find "$ROOT_DIR/$BACKUP_DIR" -maxdepth 1 -type f -name "*.sql" -printf "%T@ %p\n" | sort -nr | head -n 1 | cut -d' ' -f2-)"
fi

if [[ -z "$BACKUP_PATH" || ! -f "$BACKUP_PATH" ]]; then
  echo "No se encontro el backup $BACKUP_PATH." >&2
  exit 1
fi

psql_cmd() {
  local database="$1"
  local sql="$2"
  docker compose -f "$COMPOSE_FILE" exec -T \
    -e "PGPASSWORD=$DB_PASSWORD" \
    "$POSTGRES_SERVICE" \
    psql \
    -v ON_ERROR_STOP=1 \
    -U "$DB_USERNAME" \
    -d "$database" \
    -c "$sql"
}

echo "Restaurando backup de prueba:"
echo "$BACKUP_PATH"
echo "Base destino: $RESTORE_DATABASE_NAME"

psql_cmd postgres "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$RESTORE_DATABASE_NAME';"
psql_cmd postgres "DROP DATABASE IF EXISTS $RESTORE_DATABASE_NAME;"
psql_cmd postgres "CREATE DATABASE $RESTORE_DATABASE_NAME;"

docker compose -f "$COMPOSE_FILE" exec -T \
  -e "PGPASSWORD=$DB_PASSWORD" \
  "$POSTGRES_SERVICE" \
  psql \
  -v ON_ERROR_STOP=1 \
  -U "$DB_USERNAME" \
  -d "$RESTORE_DATABASE_NAME" < "$BACKUP_PATH"

table_count="$(docker compose -f "$COMPOSE_FILE" exec -T \
  -e "PGPASSWORD=$DB_PASSWORD" \
  "$POSTGRES_SERVICE" \
  psql \
  -U "$DB_USERNAME" \
  -d "$RESTORE_DATABASE_NAME" \
  -tAc "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';")"

echo "Restauracion de prueba finalizada correctamente."
echo "Tablas restauradas: ${table_count//[[:space:]]/}"
echo "Base de prueba: $RESTORE_DATABASE_NAME"
