#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "No se encontro $ENV_FILE. Crea un .env basado en .env.example." >&2
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

step() {
  echo
  echo "==> $1"
}

assert_status_code() {
  local url="$1"
  local expected="$2"
  local actual
  actual="$(curl -sS -o /dev/null -w "%{http_code}" "$url")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Se esperaba HTTP $expected en $url, pero se recibio HTTP $actual." >&2
    exit 1
  fi
  echo "OK $url respondio HTTP $expected"
}

cd "$ROOT_DIR"

step "Revisando contenedores"
docker compose -f "$COMPOSE_FILE" ps

step "Revisando healthcheck interno del backend"
health="$(docker exec edificio_app-backend-1 curl -fsS http://localhost:8080/actuator/health)"
if [[ "$health" != *'"status":"UP"'* ]]; then
  echo "El backend no reporto status UP. Respuesta: $health" >&2
  exit 1
fi
echo "OK backend health: $health"

step "Revisando frontend publico"
assert_status_code "$BASE_URL/" 200

step "Revisando proxy API sin token"
assert_status_code "$BASE_URL/api/buildings" 401

step "Probando login y endpoint protegido"
login_response="$(curl -sS \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$APP_ADMIN_USERNAME\",\"password\":\"$APP_ADMIN_PASSWORD\"}" \
  "$BASE_URL/api/auth/login")"

access_token="$(printf "%s" "$login_response" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"
refresh_token="$(printf "%s" "$login_response" | sed -n 's/.*"refreshToken":"\([^"]*\)".*/\1/p')"

if [[ -z "$access_token" || -z "$refresh_token" ]]; then
  echo "El login no devolvio accessToken y refreshToken." >&2
  exit 1
fi

buildings_response="$(curl -sS -H "Authorization: Bearer $access_token" "$BASE_URL/api/buildings")"
echo "OK login valido. BuildingsResponseLength=${#buildings_response}"

step "Smoke test finalizado"
echo "Todo se ve bien para el stack local de produccion."
