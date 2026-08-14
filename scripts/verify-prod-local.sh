#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

step() {
  echo
  echo "==> $1"
}

cd "$ROOT_DIR"

step "Ejecutando tests del backend"
mvn test

step "Validando docker compose"
docker compose -f "$COMPOSE_FILE" config > /dev/null
echo "OK docker compose config"

step "Construyendo y levantando stack local de produccion"
docker compose -f "$COMPOSE_FILE" up --build -d

step "Ejecutando smoke test"
BASE_URL="$BASE_URL" COMPOSE_FILE="$COMPOSE_FILE" "$ROOT_DIR/scripts/smoke-prod-local.sh"

step "Verificacion completa finalizada"
echo "Todo se ve bien para produccion local."
