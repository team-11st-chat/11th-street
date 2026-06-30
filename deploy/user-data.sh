#!/bin/bash
set -euo pipefail

AWS_REGION="__AWS_REGION__"
AWS_ACCOUNT_ID="__AWS_ACCOUNT_ID__"
ECR_REPOSITORY="__ECR_REPOSITORY__"
IMAGE_TAG="__IMAGE_TAG__"

APP_DIR="/opt/11th-street"
PARAMETER_PREFIX="/11th-street/prod"
CONTAINER_NAME="11th-street-app"
ENV_FILE="${APP_DIR}/.env.runtime"
HEALTHCHECK_ATTEMPTS=20
HEALTHCHECK_INTERVAL_SECONDS=10

ECR_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}"
TARGET_IMAGE="${ECR_URI}:${IMAGE_TAG}"

dnf update -y
dnf install -y docker awscli python3
dnf install -y redis6 || dnf install -y redis
systemctl enable --now docker

mkdir -p "${APP_DIR}"
chmod 750 "${APP_DIR}"

start_redis() {
  if systemctl list-unit-files redis6.service >/dev/null 2>&1; then
    systemctl enable --now redis6
    return
  fi

  systemctl enable --now redis
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

get_env_value() {
  local key="$1"

  if [ ! -f "${ENV_FILE}" ]; then
    return 0
  fi

  awk -F= -v key="${key}" '$1 == key { print substr($0, length(key) + 2); exit }' "${ENV_FILE}"
}

get_server_port() {
  local server_port
  server_port=$(get_env_value "SERVER_PORT")

  if [ -z "${server_port}" ]; then
    echo "8080"
    return
  fi

  echo "${server_port}"
}

get_healthcheck_url() {
  echo "http://localhost:$(get_server_port)/health"
}

run_app_container() {
  local image="$1"

  docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
  docker run -d \
    --name "${CONTAINER_NAME}" \
    --restart unless-stopped \
    --env-file "${ENV_FILE}" \
    --network host \
    "${image}"
}

wait_for_health() {
  local healthcheck_url
  healthcheck_url=$(get_healthcheck_url)

  for attempt in $(seq 1 "${HEALTHCHECK_ATTEMPTS}"); do
    if curl --fail --silent --show-error "${healthcheck_url}" >/dev/null; then
      echo "Health check succeeded on attempt ${attempt}/${HEALTHCHECK_ATTEMPTS}."
      return 0
    fi

    echo "Health check attempt ${attempt}/${HEALTHCHECK_ATTEMPTS} failed."

    if [ "${attempt}" -lt "${HEALTHCHECK_ATTEMPTS}" ]; then
      sleep "${HEALTHCHECK_INTERVAL_SECONDS}"
    fi
  done

  return 1
}

start_redis
write_env_file
chmod 600 "${ENV_FILE}"

aws ecr get-login-password --region "${AWS_REGION}" | \
  docker login --username AWS --password-stdin \
  "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

docker pull "${TARGET_IMAGE}"

run_app_container "${TARGET_IMAGE}"

if wait_for_health; then
  echo "Deployment health check completed successfully."
  exit 0
fi

echo "Deployment health check failed. Launch Template rollback will be handled by the deployment workflow."
exit 1
