#!/bin/bash
set -e

AWS_REGION="ap-northeast-2"
AWS_ACCOUNT_ID="388784542084"
ECR_REPOSITORY="11th-street-app"
IMAGE_TAG="latest"

APP_DIR="/opt/11th-street"
PARAMETER_PREFIX="/11th-street/prod"
CONTAINER_NAME="11th-street-app"

ECR_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}"

dnf update -y
dnf install -y docker awscli
systemctl enable --now docker

mkdir -p "${APP_DIR}"
chmod 750 "${APP_DIR}"

aws ssm get-parameters-by-path \
  --region "${AWS_REGION}" \
  --path "${PARAMETER_PREFIX}" \
  --with-decryption \
  --recursive \
  --query "Parameters[*].[Name,Value]" \
  --output text \
  | awk -F '\t' '{
      name=$1
      value=$2
      sub(".*/", "", name)
      print name "=" value
    }' > "${APP_DIR}/.env.runtime"

chmod 600 "${APP_DIR}/.env.runtime"

aws ecr get-login-password --region "${AWS_REGION}" | \
  docker login --username AWS --password-stdin \
  "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

docker pull "${ECR_URI}:${IMAGE_TAG}"

docker rm -f "${CONTAINER_NAME}" || true

docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  --env-file "${APP_DIR}/.env.runtime" \
  -p 8080:8080 \
  "${ECR_URI}:${IMAGE_TAG}"
