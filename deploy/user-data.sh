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

ECR_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}"

dnf update -y
dnf install -y docker awscli python3
systemctl enable --now docker

mkdir -p "${APP_DIR}"
chmod 750 "${APP_DIR}"

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
chmod 600 "${ENV_FILE}"

aws ecr get-login-password --region "${AWS_REGION}" | \
  docker login --username AWS --password-stdin \
  "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

docker pull "${ECR_URI}:${IMAGE_TAG}"

docker rm -f "${CONTAINER_NAME}" || true

docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  --env-file "${ENV_FILE}" \
  -p 8080:8080 \
  "${ECR_URI}:${IMAGE_TAG}"
