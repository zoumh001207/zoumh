#!/usr/bin/env bash
set -euo pipefail

PACKAGE_DIR="${PACKAGE_DIR:-/zoumh/java/zmh/backend/packages}"
LOG_DIR="${LOG_DIR:-/zoumh/java/zmh/backend/logs}"
RUN_DIR="${RUN_DIR:-/zoumh/java/zmh/backend/run}"
JDK_ARCHIVE="${JDK_ARCHIVE:-/zoumh/jdk/openjdk-21.0.2_linux-x64_bin.tar.gz}"
JDK_HOME="${JDK_HOME:-/zoumh/jdk/jdk21}"
NACOS_ADDR="${NACOS_ADDR:-156.225.28.110:8848}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-zoumh}"
REDIS_HOST="${REDIS_HOST:-172.21.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-zoumh}"
REDIS_DATABASE="${REDIS_DATABASE:-0}"
TZ_NAME="${TZ_NAME:-Asia/Shanghai}"
JAVA_TMPDIR="${JAVA_TMPDIR:-/tmp/zoumh-java}"
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS:--Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom -Djava.io.tmpdir=/tmp/zoumh-java -Duser.timezone=Asia/Shanghai -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/zoumh/java/zmh/backend/logs -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=128m}"
JAVA_OPTS_AUTH="${JAVA_OPTS_AUTH:--Xms96m -Xmx192m}"
JAVA_OPTS_SYSTEM="${JAVA_OPTS_SYSTEM:--Xms192m -Xmx384m}"
JAVA_OPTS_GEN="${JAVA_OPTS_GEN:--Xms96m -Xmx192m}"
JAVA_OPTS_JOB="${JAVA_OPTS_JOB:--Xms96m -Xmx192m}"
JAVA_OPTS_FILE="${JAVA_OPTS_FILE:--Xms96m -Xmx192m}"
JAVA_OPTS_TOOLS="${JAVA_OPTS_TOOLS:--Xms96m -Xmx192m}"
JAVA_OPTS_HOTEL="${JAVA_OPTS_HOTEL:--Xms96m -Xmx192m}"
JAVA_OPTS_GATEWAY="${JAVA_OPTS_GATEWAY:--Xms192m -Xmx384m -XX:MaxDirectMemorySize=192m}"

mkdir -p "${PACKAGE_DIR}" "${LOG_DIR}" "${RUN_DIR}" "${JAVA_TMPDIR}"

ensure_jdk() {
  mkdir -p "$(dirname "${JDK_HOME}")"
  if [[ -x "${JDK_HOME}/bin/java" ]]; then
    return 0
  fi
  if [[ ! -f "${JDK_ARCHIVE}" ]]; then
    echo "jdk archive not found: ${JDK_ARCHIVE}" >&2
    exit 1
  fi
  local extract_dir
  extract_dir="$(mktemp -d "$(dirname "${JDK_HOME}")/.jdk21.XXXXXX")"
  tar -xzf "${JDK_ARCHIVE}" -C "${extract_dir}"
  local extracted_home
  extracted_home="$(find "${extract_dir}" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
  if [[ -z "${extracted_home}" || ! -x "${extracted_home}/bin/java" ]]; then
    echo "failed to extract jdk from ${JDK_ARCHIVE}" >&2
    rm -rf "${extract_dir}"
    exit 1
  fi
  rm -rf "${JDK_HOME}"
  mv "${extracted_home}" "${JDK_HOME}"
  rm -rf "${extract_dir}"
}

write_host_java_env() {
  local env_file="/etc/profile.d/zoumh-jdk21.sh"
  cat > "${env_file}" <<EOF
export JAVA_HOME='${JDK_HOME}'
export PATH='${JDK_HOME}/bin:\$PATH'
EOF
  chmod 644 "${env_file}"
}

stop_docker_service() {
  local service_name="$1"
  docker rm -f "${service_name}" >/dev/null 2>&1 || true
}

stop_host_service() {
  local service_name="$1"
  local pid_file="${RUN_DIR}/${service_name}.pid"
  if [[ -f "${pid_file}" ]]; then
    local pid
    pid="$(cat "${pid_file}")"
    if [[ -n "${pid}" ]] && kill -0 "${pid}" >/dev/null 2>&1; then
      kill "${pid}" >/dev/null 2>&1 || true
      for _ in $(seq 1 20); do
        if ! kill -0 "${pid}" >/dev/null 2>&1; then
          break
        fi
        sleep 1
      done
      kill -9 "${pid}" >/dev/null 2>&1 || true
    fi
    rm -f "${pid_file}"
  fi
  pkill -f "/app/${service_name}\.jar" >/dev/null 2>&1 || true
  pkill -f "${PACKAGE_DIR}/.*${service_name}.*\.jar" >/dev/null 2>&1 || true
}

start_host_service() {
  local service_name="$1"
  local jar_name="$2"
  local java_opts="$3"
  shift 3

  if [[ ! -f "${PACKAGE_DIR}/${jar_name}" ]]; then
    echo "skip ${service_name}: ${jar_name} not found"
    return 0
  fi

  stop_docker_service "${service_name}"
  stop_host_service "${service_name}"

  local pid_file="${RUN_DIR}/${service_name}.pid"
  local log_file="${LOG_DIR}/${service_name}.log"

  env \
    JAVA_HOME="${JDK_HOME}" \
    PATH="${JDK_HOME}/bin:${PATH}" \
    TZ="${TZ_NAME}" \
    JAVA_TMPDIR="${JAVA_TMPDIR}" \
    JAVA_TOOL_OPTIONS="" \
    "$@" \
    nohup "${JDK_HOME}/bin/java" ${DEFAULT_JAVA_OPTS} ${java_opts} -jar "${PACKAGE_DIR}/${jar_name}" >> "${log_file}" 2>&1 &

  local pid=$!
  echo "${pid}" > "${pid_file}"

  for _ in $(seq 1 20); do
    if ! kill -0 "${pid}" >/dev/null 2>&1; then
      echo "failed to start ${service_name}, check ${log_file}" >&2
      tail -n 50 "${log_file}" || true
      exit 1
    fi
    sleep 1
    if grep -Eq "Started .* in .* seconds|Tomcat started on port|Netty started on port" "${log_file}" 2>/dev/null; then
      break
    fi
  done

  echo "started ${service_name} pid=${pid}"
}

ensure_jdk
write_host_java_env

stop_docker_service "ruoyi-monitor"

start_host_service \
  "ruoyi-auth" \
  "ruoyi-auth.jar" \
  "${JAVA_OPTS_AUTH}" \
  SPRING_CLOUD_NACOS_SERVER_ADDR="${NACOS_ADDR}" \
  SPRING_CLOUD_NACOS_USERNAME="${NACOS_USERNAME}" \
  SPRING_CLOUD_NACOS_PASSWORD="${NACOS_PASSWORD}"

start_host_service \
  "ruoyi-system" \
  "ruoyi-modules-system.jar" \
  "${JAVA_OPTS_SYSTEM}" \
  SPRING_CLOUD_NACOS_SERVER_ADDR="${NACOS_ADDR}" \
  SPRING_CLOUD_NACOS_USERNAME="${NACOS_USERNAME}" \
  SPRING_CLOUD_NACOS_PASSWORD="${NACOS_PASSWORD}" \
  SPRING_CLOUD_NACOS_DISCOVERY_SERVICE=ruoyi-system

start_host_service \
  "ruoyi-gen" \
  "ruoyi-modules-gen.jar" \
  "${JAVA_OPTS_GEN}" \
  SPRING_CLOUD_NACOS_SERVER_ADDR="${NACOS_ADDR}" \
  SPRING_CLOUD_NACOS_USERNAME="${NACOS_USERNAME}" \
  SPRING_CLOUD_NACOS_PASSWORD="${NACOS_PASSWORD}"

start_host_service \
  "ruoyi-job" \
  "ruoyi-modules-job.jar" \
  "${JAVA_OPTS_JOB}" \
  SPRING_CLOUD_NACOS_SERVER_ADDR="${NACOS_ADDR}" \
  SPRING_CLOUD_NACOS_USERNAME="${NACOS_USERNAME}" \
  SPRING_CLOUD_NACOS_PASSWORD="${NACOS_PASSWORD}"

start_host_service \
  "ruoyi-file" \
  "ruoyi-modules-file.jar" \
  "${JAVA_OPTS_FILE}" \
  SPRING_CLOUD_NACOS_SERVER_ADDR="${NACOS_ADDR}" \
  SPRING_CLOUD_NACOS_USERNAME="${NACOS_USERNAME}" \
  SPRING_CLOUD_NACOS_PASSWORD="${NACOS_PASSWORD}"

start_host_service \
  "zoumh-tools" \
  "zoumh-tools.jar" \
  "${JAVA_OPTS_TOOLS}" \
  SPRING_CLOUD_NACOS_SERVER_ADDR="${NACOS_ADDR}" \
  SPRING_CLOUD_NACOS_USERNAME="${NACOS_USERNAME}" \
  SPRING_CLOUD_NACOS_PASSWORD="${NACOS_PASSWORD}"

start_host_service \
  "zoumh-hotel-monitor" \
  "zoumh-hotel-monitor.jar" \
  "${JAVA_OPTS_HOTEL}" \
  SPRING_CLOUD_NACOS_SERVER_ADDR="${NACOS_ADDR}" \
  SPRING_CLOUD_NACOS_USERNAME="${NACOS_USERNAME}" \
  SPRING_CLOUD_NACOS_PASSWORD="${NACOS_PASSWORD}"

start_host_service \
  "ruoyi-gateway" \
  "ruoyi-gateway.jar" \
  "${JAVA_OPTS_GATEWAY}" \
  SPRING_CLOUD_NACOS_SERVER_ADDR="${NACOS_ADDR}" \
  SPRING_CLOUD_NACOS_USERNAME="${NACOS_USERNAME}" \
  SPRING_CLOUD_NACOS_PASSWORD="${NACOS_PASSWORD}" \
  SPRING_DATA_REDIS_HOST="${REDIS_HOST}" \
  SPRING_DATA_REDIS_PORT="${REDIS_PORT}" \
  SPRING_DATA_REDIS_PASSWORD="${REDIS_PASSWORD}" \
  SPRING_DATA_REDIS_DATABASE="${REDIS_DATABASE}"

if [[ -n "${POST_DEPLOY_CMD:-}" ]]; then
  sh -lc "${POST_DEPLOY_CMD}"
fi

echo "JAVA_HOME=${JDK_HOME}"
"${JDK_HOME}/bin/java" -version 2>&1 | head -n 1 || true
ps -eo pid,rss,cmd --sort=-rss | grep -E 'ruoyi-|zoumh-' | grep -v grep | head -n 20 || true
