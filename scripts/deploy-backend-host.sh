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
DEFAULT_JAVA_OPTS="${DEFAULT_JAVA_OPTS:--Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom -Djava.io.tmpdir=/tmp/zoumh-java -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+ExitOnOutOfMemoryError -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=128m -XX:MaxDirectMemorySize=128m}"
JAVA_OPTS_AUTH="${JAVA_OPTS_AUTH:--Xms128m -Xmx256m}"
JAVA_OPTS_SYSTEM="${JAVA_OPTS_SYSTEM:--Xms256m -Xmx512m}"
JAVA_OPTS_GEN="${JAVA_OPTS_GEN:--Xms128m -Xmx256m}"
JAVA_OPTS_JOB="${JAVA_OPTS_JOB:--Xms128m -Xmx256m}"
JAVA_OPTS_FILE="${JAVA_OPTS_FILE:--Xms128m -Xmx256m}"
JAVA_OPTS_TOOLS="${JAVA_OPTS_TOOLS:--Xms128m -Xmx256m}"
JAVA_OPTS_HOTEL="${JAVA_OPTS_HOTEL:--Xms128m -Xmx256m}"
JAVA_OPTS_GATEWAY="${JAVA_OPTS_GATEWAY:--Xms256m -Xmx512m -XX:MaxDirectMemorySize=256m}"

mkdir -p "${PACKAGE_DIR}" "${LOG_DIR}"

ensure_docker_shell_env() {
  local docker_bin docker_dir env_file helper_file bashrc_snippet
  docker_bin="$(command -v docker || true)"
  if [[ -z "${docker_bin}" ]]; then
    echo "docker command not found in deploy environment" >&2
    exit 1
  fi

  docker_dir="$(dirname "${docker_bin}")"
  env_file="/etc/profile.d/zoumh-docker.sh"
  helper_file="/zoumh/sh/docker.sh"
  bashrc_snippet="# zoumh docker env"

  mkdir -p /etc/profile.d /zoumh/sh
  ln -sf "${docker_bin}" /usr/local/bin/docker || true
  ln -sf "${docker_bin}" /usr/bin/docker || true

  cat > "${env_file}" <<EOF
export DOCKER_HOME='${docker_dir}'
case ":\$PATH:" in
  *:"${docker_dir}":*) ;;
  *) export PATH="${docker_dir}:\$PATH" ;;
esac
EOF
  chmod 644 "${env_file}"

  if ! grep -Fq "${bashrc_snippet}" /etc/bashrc 2>/dev/null; then
    cat >> /etc/bashrc <<EOF

${bashrc_snippet}
if [ -f /etc/profile.d/zoumh-docker.sh ]; then
  . /etc/profile.d/zoumh-docker.sh
fi
EOF
  fi

  mkdir -p /root
  for shell_file in /root/.bashrc /root/.bash_profile; do
    touch "${shell_file}"
    if ! grep -Fq "${bashrc_snippet}" "${shell_file}"; then
      cat >> "${shell_file}" <<EOF

${bashrc_snippet}
if [ -f /etc/profile.d/zoumh-docker.sh ]; then
  . /etc/profile.d/zoumh-docker.sh
fi
EOF
    fi
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

write_host_java_env() {
  local env_file="/host/etc/profile.d/zoumh-jdk21.sh"
  docker run --rm -v /:/host alpine:3.20 sh -lc "
    mkdir -p /host/etc/profile.d &&
    cat > '${env_file}' <<'EOF'
export JAVA_HOME='${JDK_HOME}'
export PATH='${JDK_HOME}/bin:\$PATH'
EOF
    chmod 644 '${env_file}'
  " >/dev/null 2>&1 || true
}

ensure_jdk
write_host_java_env
ensure_docker_shell_env

docker rm -f "ruoyi-monitor" >/dev/null 2>&1 || true

run_java_service() {
  local name="$1"
  local jar_name="$2"
  local java_opts="$3"
  shift 3

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
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "ruoyi-system" \
  "ruoyi-modules-system.jar" \
  "${JAVA_OPTS_SYSTEM}" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}" \
  -e "SPRING_CLOUD_NACOS_DISCOVERY_SERVICE=ruoyi-system"

run_java_service \
  "ruoyi-gen" \
  "ruoyi-modules-gen.jar" \
  "${JAVA_OPTS_GEN}" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "ruoyi-job" \
  "ruoyi-modules-job.jar" \
  "${JAVA_OPTS_JOB}" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "ruoyi-file" \
  "ruoyi-modules-file.jar" \
  "${JAVA_OPTS_FILE}" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "zoumh-tools" \
  "zoumh-tools.jar" \
  "${JAVA_OPTS_TOOLS}" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "zoumh-hotel-monitor" \
  "zoumh-hotel-monitor.jar" \
  "${JAVA_OPTS_HOTEL}" \
  -e "SPRING_CLOUD_NACOS_SERVER_ADDR=${NACOS_ADDR}" \
  -e "SPRING_CLOUD_NACOS_USERNAME=${NACOS_USERNAME}" \
  -e "SPRING_CLOUD_NACOS_PASSWORD=${NACOS_PASSWORD}"

run_java_service \
  "ruoyi-gateway" \
  "ruoyi-gateway.jar" \
  "${JAVA_OPTS_GATEWAY}" \
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

echo "JAVA_HOME=${JDK_HOME}"
"${JDK_HOME}/bin/java" -version 2>&1 | head -n 1 || true
docker stats --no-stream --format '{{.Name}}\t{{.MemUsage}}' | grep -E 'ruoyi-|zoumh-' || true
docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -E 'ruoyi-|zoumh-tools|zoumh-hotel-monitor' || true
