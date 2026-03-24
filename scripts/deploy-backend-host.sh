#!/usr/bin/env bash
set -euo pipefail

PACKAGE_DIR="${PACKAGE_DIR:-/zoumh/java/zmh/backend/packages}"
LOG_DIR="${LOG_DIR:-/zoumh/java/zmh/backend/logs}"
JAVA_IMAGE="${JAVA_IMAGE:-eclipse-temurin:21-jre}"
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
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS:--Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom -Djava.io.tmpdir=/tmp/zoumh-java -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+ExitOnOutOfMemoryError -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=80m}"
JAVA_OPTS_AUTH="${JAVA_OPTS_AUTH:--Xms128m -Xmx320m}"
JAVA_OPTS_SYSTEM="${JAVA_OPTS_SYSTEM:--Xms96m -Xmx320m}"
JAVA_OPTS_FILE="${JAVA_OPTS_FILE:--Xms64m -Xmx160m}"
JAVA_OPTS_GATEWAY="${JAVA_OPTS_GATEWAY:--Xms96m -Xmx320m -XX:MaxDirectMemorySize=128m}"
DOCKER_MEMORY_AUTH="${DOCKER_MEMORY_AUTH:-512m}"
DOCKER_MEMORY_SYSTEM="${DOCKER_MEMORY_SYSTEM:-512m}"
DOCKER_MEMORY_FILE="${DOCKER_MEMORY_FILE:-288m}"
DOCKER_MEMORY_GATEWAY="${DOCKER_MEMORY_GATEWAY:-512m}"
DOCKER_MEMORY_RESERVATION_AUTH="${DOCKER_MEMORY_RESERVATION_AUTH:-256m}"
DOCKER_MEMORY_RESERVATION_SYSTEM="${DOCKER_MEMORY_RESERVATION_SYSTEM:-224m}"
DOCKER_MEMORY_RESERVATION_FILE="${DOCKER_MEMORY_RESERVATION_FILE:-128m}"
DOCKER_MEMORY_RESERVATION_GATEWAY="${DOCKER_MEMORY_RESERVATION_GATEWAY:-224m}"
DOCKER_PIDS_LIMIT="${DOCKER_PIDS_LIMIT:-256}"

mkdir -p "${PACKAGE_DIR}" "${LOG_DIR}"

ensure_docker_shell_env() {
  local docker_cmd docker_bin docker_dir env_file helper_file bashrc_snippet
  docker_cmd="$(command -v docker || true)"
  if [[ -z "${docker_cmd}" ]]; then
    echo "docker command not found in deploy environment" >&2
    exit 1
  fi

  docker_bin="$(readlink -f "${docker_cmd}" 2>/dev/null || true)"
  if [[ -z "${docker_bin}" || ! -x "${docker_bin}" ]]; then
    if [[ -x /usr/bin/docker ]]; then
      docker_bin="/usr/bin/docker"
    else
      docker_bin="${docker_cmd}"
    fi
  fi

  docker_dir="$(dirname "${docker_bin}")"
  env_file="/etc/profile.d/zoumh-docker.sh"
  helper_file="/zoumh/sh/docker.sh"
  bashrc_snippet="# >>> zoumh docker env >>>"

  mkdir -p /etc/profile.d /zoumh/sh
  if [[ -L /usr/local/bin/docker && ! -e /usr/local/bin/docker ]]; then
    rm -f /usr/local/bin/docker
  fi
  ln -sf "${docker_bin}" /usr/local/bin/docker || true
  ln -sf "${docker_bin}" /usr/bin/docker || true

  clean_shell_hook() {
    local shell_file="$1"
    local temp_file
    [[ -f "${shell_file}" ]] || touch "${shell_file}"
    temp_file="$(mktemp)"
    awk '
      BEGIN {
        skip_block = 0
        skip_legacy_fi = 0
      }
      /^# >>> zoumh docker env >>>$/ {
        skip_block = 1
        next
      }
      /^# <<< zoumh docker env <<</ {
        skip_block = 0
        next
      }
      skip_block {
        next
      }
      skip_legacy_fi && /^fi$/ {
        skip_legacy_fi = 0
        next
      }
      /^# zoumh docker env$/ {
        skip_legacy_fi = 1
        next
      }
      /\/etc\/profile\.d\/zoumh-docker\.sh/ {
        next
      }
      /\/zoumh\/sh\/docker\.sh/ {
        next
      }
      {
        skip_legacy_fi = 0
        print
      }
    ' "${shell_file}" > "${temp_file}"
    cat "${temp_file}" > "${shell_file}"
    rm -f "${temp_file}"
  }

  cat > "${env_file}" <<EOF
export DOCKER_HOME='${docker_dir}'
case ":\$PATH:" in
  *:"${docker_dir}":*) ;;
  *) export PATH="${docker_dir}:\$PATH" ;;
esac
EOF
  chmod 644 "${env_file}"

  clean_shell_hook /etc/bashrc
  cat >> /etc/bashrc <<EOF

${bashrc_snippet}
if [ -f /etc/profile.d/zoumh-docker.sh ]; then
  . /etc/profile.d/zoumh-docker.sh
fi
# <<< zoumh docker env <<<
EOF

  mkdir -p /root
  for shell_file in /root/.bashrc /root/.bash_profile; do
    clean_shell_hook "${shell_file}"
    cat >> "${shell_file}" <<EOF

${bashrc_snippet}
if [ -f /etc/profile.d/zoumh-docker.sh ]; then
  . /etc/profile.d/zoumh-docker.sh
fi
# <<< zoumh docker env <<<
EOF
  done

  cat > "${helper_file}" <<EOF
#!/usr/bin/env bash
set -e
export DOCKER_HOME='${docker_dir}'
case ":\$PATH:" in
  *:"${docker_dir}":*) ;;
  *) export PATH="${docker_dir}:\$PATH" ;;
esac

if [[ \$# -eq 0 ]]; then
  exec "${docker_bin}" --version
fi

exec "${docker_bin}" "\$@"
EOF
  chmod +x "${helper_file}"
}

ensure_jdk() {
  mkdir -p "$(dirname "${JDK_HOME}")" "${JAVA_TMPDIR}"
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

clean_host_java_env() {
  local env_file="/host/etc/profile.d/zoumh-jdk21.sh"
  docker run --rm -v /:/host alpine:3.20 sh -lc "
    rm -f '${env_file}'
  " >/dev/null 2>&1 || true
}

ensure_jdk
clean_host_java_env
ensure_docker_shell_env

cleanup_removed_module_menu_data() {
  local sql
  sql="$(cat <<'EOF'
DELETE rm
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE m.perms LIKE 'hotel:monitor:%'
   OR m.path = 'hotel'
   OR m.path = 'monitor'
   OR m.component = 'hotel/monitor/index'
   OR m.route_name IN ('Hotel', 'HotelMonitor');

DELETE FROM sys_menu
WHERE perms LIKE 'hotel:monitor:%'
   OR path = 'hotel'
   OR path = 'monitor'
   OR component = 'hotel/monitor/index'
   OR route_name IN ('Hotel', 'HotelMonitor');
EOF
)"

  if docker ps --format '{{.Names}}' | grep -qx 'mysql8'; then
    docker exec -i mysql8 mysql -uroot -pzoumh zoumh -e "${sql}" >/dev/null
    echo "removed stale zoumh-hotel-monitor menu data"
  else
    echo "skip menu cleanup: mysql8 container not running"
  fi
}

docker rm -f "ruoyi-monitor" >/dev/null 2>&1 || true
docker rm -f "ruoyi-job" >/dev/null 2>&1 || true
docker rm -f "ruoyi-gen" >/dev/null 2>&1 || true
docker rm -f "zoumh-tools" >/dev/null 2>&1 || true
docker rm -f "zoumh-hotel-monitor" >/dev/null 2>&1 || true
docker rm -f "qq-farm-bot-ui" >/dev/null 2>&1 || true
docker rm -f "zentao" >/dev/null 2>&1 || true
docker rm -f "minio" >/dev/null 2>&1 || true
docker rm -f "kafka" >/dev/null 2>&1 || true
docker rm -f "zookeeper" >/dev/null 2>&1 || true
cleanup_removed_module_menu_data

run_java_service() {
  local name="$1"
  local jar_name="$2"
  local java_opts="$3"
  local docker_memory="$4"
  local docker_memory_reservation="$5"
  shift 5

  if [[ ! -f "${PACKAGE_DIR}/${jar_name}" ]]; then
    echo "skip ${name}: ${jar_name} not found"
    return 0
  fi

  docker rm -f "${name}" >/dev/null 2>&1 || true

  docker run -d \
    --name "${name}" \
    --restart unless-stopped \
    --network host \
    --memory="${docker_memory}" \
    --memory-reservation="${docker_memory_reservation}" \
    --pids-limit="${DOCKER_PIDS_LIMIT}" \
    --log-opt max-size=20m \
    --log-opt max-file=3 \
    -e TZ="${TZ_NAME}" \
    -e JAVA_HOME=/opt/jdk \
    -e "PATH=/opt/jdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin" \
    -e "JAVA_OPTS=${DEFAULT_JAVA_OPTS} ${java_opts}" \
    "$@" \
    -v "${PACKAGE_DIR}:/app" \
    -v "${LOG_DIR}:/logs" \
    -v "${JDK_HOME}:/opt/jdk:ro" \
    -v "${JAVA_TMPDIR}:${JAVA_TMPDIR}" \
    "${JAVA_IMAGE}" \
    sh -lc "mkdir -p '${JAVA_TMPDIR}' && exec /opt/jdk/bin/java \$JAVA_OPTS -jar /app/${jar_name} > /logs/${name}.log 2>&1"

  echo "started ${name}"
}

run_java_service \
  "ruoyi-auth" \
  "ruoyi-auth.jar" \
  "${JAVA_OPTS_AUTH}" \
  "${DOCKER_MEMORY_AUTH}" \
  "${DOCKER_MEMORY_RESERVATION_AUTH}" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "ruoyi-system" \
  "ruoyi-modules-system.jar" \
  "${JAVA_OPTS_SYSTEM}" \
  "${DOCKER_MEMORY_SYSTEM}" \
  "${DOCKER_MEMORY_RESERVATION_SYSTEM}" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}" \
  -e "SPRING_CLOUD_NACOS_DISCOVERY_SERVICE=ruoyi-system"

run_java_service \
  "ruoyi-file" \
  "ruoyi-modules-file.jar" \
  "${JAVA_OPTS_FILE}" \
  "${DOCKER_MEMORY_FILE}" \
  "${DOCKER_MEMORY_RESERVATION_FILE}" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "ruoyi-gateway" \
  "ruoyi-gateway.jar" \
  "${JAVA_OPTS_GATEWAY}" \
  "${DOCKER_MEMORY_GATEWAY}" \
  "${DOCKER_MEMORY_RESERVATION_GATEWAY}" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}" \
  -e "SECURITY_CAPTCHA_ENABLED=false" \
  -e "SPRING_DATA_REDIS_HOST=${REDIS_HOST}" \
  -e "SPRING_DATA_REDIS_PORT=${REDIS_PORT}" \
  -e "SPRING_DATA_REDIS_PASSWORD=${REDIS_PASSWORD}" \
  -e "SPRING_DATA_REDIS_DATABASE=${REDIS_DATABASE}"

if [[ -n "${POST_DEPLOY_CMD:-}" ]]; then
  sh -lc "${POST_DEPLOY_CMD}"
fi

echo "JAVA_HOME=${JDK_HOME}"
"${JDK_HOME}/bin/java" -version 2>&1 | head -n 1 || true

login_probe='{"username":"admin","password":"admin123"}'
sleep 20
probe_response="$(curl -ksS -H 'Content-Type: application/json' -d "${login_probe}" https://zoumh.com/prod-api/auth/login || true)"
gateway_probe="$(curl -sS -H 'Content-Type: application/json' -d "${login_probe}" http://127.0.0.1:8080/auth/login || true)"
echo "login_probe_response=${probe_response}"
echo "gateway_probe_response=${gateway_probe}"
if printf '%s\n%s' "${probe_response}" "${gateway_probe}" | grep -Eq '"code":500|502 Bad Gateway'; then
  echo "--- gateway.log tail ---"
  tail -n 80 "${LOG_DIR}/ruoyi-gateway.log" 2>/dev/null || true
  echo "--- auth.log tail ---"
  tail -n 80 "${LOG_DIR}/ruoyi-auth.log" 2>/dev/null || true
  echo "--- system.log tail ---"
  tail -n 80 "${LOG_DIR}/ruoyi-system.log" 2>/dev/null || true
fi

docker stats --no-stream --format '{{.Name}}\t{{.MemUsage}}' | grep -E 'ruoyi-(auth|system|file|gateway)' || true
docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -E 'ruoyi-(auth|system|file|gateway)' || true
echo "--- ruoyi-file.log tail ---"
tail -n 120 "${LOG_DIR}/ruoyi-file.log" 2>/dev/null || true
