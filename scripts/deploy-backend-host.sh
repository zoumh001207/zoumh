#!/usr/bin/env bash
set -euo pipefail

PACKAGE_DIR="${PACKAGE_DIR:-/zoumh/java/zmh/backend/packages}"
LOG_DIR="${LOG_DIR:-/zoumh/java/zmh/backend/logs}"
JAVA_IMAGE="${JAVA_IMAGE:-eclipse-temurin:21-jre}"
NACOS_ADDR="${NACOS_ADDR:-156.225.28.110:8848}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-zoumh}"
REDIS_HOST="${REDIS_HOST:-172.21.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-zoumh}"
REDIS_DATABASE="${REDIS_DATABASE:-0}"
TZ_NAME="${TZ_NAME:-Asia/Shanghai}"

mkdir -p "${PACKAGE_DIR}" "${LOG_DIR}"

docker rm -f "ruoyi-monitor" >/dev/null 2>&1 || true

run_java_service() {
  local name="$1"
  local jar_name="$2"
  shift 2

  if [[ ! -f "${PACKAGE_DIR}/${jar_name}" ]]; then
    echo "skip ${name}: ${jar_name} not found"
    return 0
  fi

  docker rm -f "${name}" >/dev/null 2>&1 || true

  docker run -d \
    --name "${name}" \
    --restart unless-stopped \
    --network host \
    -e TZ="${TZ_NAME}" \
    "$@" \
    -v "${PACKAGE_DIR}:/app" \
    -v "${LOG_DIR}:/logs" \
    "${JAVA_IMAGE}" \
    sh -lc "java -jar /app/${jar_name} > /logs/${name}.log 2>&1"

  echo "started ${name}"
}

run_java_service \
  "ruoyi-auth" \
  "ruoyi-auth.jar" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "ruoyi-system" \
  "ruoyi-modules-system.jar" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}" \
  -e "SPRING_CLOUD_NACOS_DISCOVERY_SERVICE=ruoyi-system"

run_java_service \
  "ruoyi-gen" \
  "ruoyi-modules-gen.jar" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "ruoyi-job" \
  "ruoyi-modules-job.jar" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "ruoyi-file" \
  "ruoyi-modules-file.jar" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "zoumh-tools" \
  "zoumh-tools.jar" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "zoumh-hotel-monitor" \
  "zoumh-hotel-monitor.jar" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "ruoyi-gateway" \
  "ruoyi-gateway.jar" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}" \
  -e "SPRING_DATA_REDIS_HOST=${REDIS_HOST}" \
  -e "SPRING_DATA_REDIS_PORT=${REDIS_PORT}" \
  -e "SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}" \
  -e "SPRING_DATA_REDIS_DATABASE=${REDIS_DATABASE}"

if [[ -n "${POST_DEPLOY_CMD:-}" ]]; then
  sh -lc "${POST_DEPLOY_CMD}"
fi

docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -E 'ruoyi-|zoumh-tools|zoumh-hotel-monitor' || true
