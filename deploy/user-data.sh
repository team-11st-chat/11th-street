#!/bin/bash
set -euo pipefail

AWS_REGION="__AWS_REGION__"
AWS_ACCOUNT_ID="__AWS_ACCOUNT_ID__"
ECR_REPOSITORY="__ECR_REPOSITORY__"
IMAGE_TAG="__IMAGE_TAG__"

APP_DIR="/opt/11th-street"
PARAMETER_PREFIX="/11th-street/prod"
ENV_FILE="${APP_DIR}/.env.runtime"
COMPOSE_FILE="${APP_DIR}/docker-compose.yml"

ECR_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}"

dnf update -y
dnf install -y docker awscli python3 curl
systemctl enable --now docker

mkdir -p "${APP_DIR}"
chmod 750 "${APP_DIR}"

ensure_docker_compose() {
  if docker compose version >/dev/null 2>&1; then
    return
  fi

  local arch
  arch="$(uname -m)"
  case "${arch}" in
    x86_64) arch="x86_64" ;;
    aarch64 | arm64) arch="aarch64" ;;
    *) echo "Unsupported architecture for Docker Compose plugin: ${arch}" >&2; exit 1 ;;
  esac

  mkdir -p /usr/local/lib/docker/cli-plugins
  curl -fsSL \
    "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-${arch}" \
    -o /usr/local/lib/docker/cli-plugins/docker-compose
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
}

write_env_file() {
  local next_token=""
  : > "${ENV_FILE}"

  while true; do
    local token_args=()
    if [ -n "${next_token}" ]; then
      token_args=(--next-token "${next_token}")
    fi

    local response
    response=$(aws ssm get-parameters-by-path \
      --region "${AWS_REGION}" \
      --path "${PARAMETER_PREFIX}" \
      --with-decryption \
      --recursive \
      --max-results 10 \
      "${token_args[@]}" \
      --output json)

    RESPONSE_JSON="${response}" python3 - <<'PY' >> "${ENV_FILE}"
import json
import os

payload = json.loads(os.environ["RESPONSE_JSON"])
for parameter in payload.get("Parameters", []):
    name = parameter["Name"].rsplit("/", 1)[-1]
    value = parameter.get("Value", "")
    print(f"{name}={value}")
PY

    next_token=$(RESPONSE_JSON="${response}" python3 - <<'PY'
import json
import os

payload = json.loads(os.environ["RESPONSE_JSON"])
print(payload.get("NextToken", ""))
PY
)

    if [ -z "${next_token}" ]; then
      break
    fi
  done
}

write_env_file
{
  echo "APP_IMAGE=${ECR_URI}:${IMAGE_TAG}"
  echo "APP_PORT=8080"
} >> "${ENV_FILE}"
chmod 600 "${ENV_FILE}"

cat > "${COMPOSE_FILE}" <<'YAML'
name: 11th-street-prod

services:
  app:
    image: ${APP_IMAGE}
    env_file:
      - .env.runtime
    ports:
      - "${APP_PORT:-8080}:8080"
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "wget -q --spider http://localhost:8080/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 12
      start_period: 30s

  mysql:
    image: mysql:8.4
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-time-zone=+09:00
    env_file:
      - .env.runtime
    volumes:
      - mysql-data:/var/lib/mysql
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h localhost -uroot -p$${MYSQL_ROOT_PASSWORD}"]
      interval: 5s
      timeout: 5s
      retries: 20

  redis:
    image: redis:8-alpine
    command: ["redis-server", "--appendonly", "yes"]
    volumes:
      - redis-data:/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 20

networks:
  default:
    name: 11th-street-prod-network

volumes:
  mysql-data:
  redis-data:
YAML
chmod 640 "${COMPOSE_FILE}"

ensure_docker_compose

aws ecr get-login-password --region "${AWS_REGION}" | \
  docker login --username AWS --password-stdin \
  "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

cd "${APP_DIR}"
docker compose --env-file "${ENV_FILE}" pull
docker compose --env-file "${ENV_FILE}" up -d --remove-orphans
