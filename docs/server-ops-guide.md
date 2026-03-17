# 服务器服务与内存分配清单

## 文档用途

本文档用于记录 `zoumh` 服务器当前项目服务、基础服务、部署方式与建议内存分配，便于后续运维排查与扩容。

## 部署目录

- 后端目录：`/zoumh/java/zmh/backend`
- 前端目录：`/zoumh/java/zmh/frontend`
- 后端包目录：`/zoumh/java/zmh/backend/packages`
- 后端日志目录：`/zoumh/java/zmh/backend/logs`
- 前端静态目录：`/zoumh/java/zmh/frontend/html`
- 前端配置目录：`/zoumh/java/zmh/frontend/nginx`

## 当前部署方式

- 前端：Docker `nginx` 容器部署
- 后端：Docker 容器部署，每个微服务单独运行
- Java：容器内挂载 `/zoumh/jdk/jdk21`
- 配置中心：Nacos
- 缓存：Redis
- 数据库：MySQL

## 项目服务建议内存

| 服务名 | 建议内存上限 | 说明 |
| --- | --- | --- |
| `ruoyi-gateway` | `512m` | 网关入口，保留稍高余量 |
| `ruoyi-system` | `512m` | 核心业务模块 |
| `ruoyi-auth` | `288m` | 登录认证服务 |
| `ruoyi-gen` | `288m` | 代码生成模块 |
| `ruoyi-file` | `288m` | 文件服务 |
| `zoumh-tools` | `288m` | 自定义工具模块 |
| `zoumh-hotel-monitor` | `352m` | 酒店价格监控模块 |
| `ruoyi-nginx` | `128m` | 前端反向代理与静态资源 |

## 基础服务建议内存

| 服务名 | 建议内存上限 | 说明 |
| --- | --- | --- |
| `ruoyi-mysql` | `768m` | 数据库，当前已做保守收缩 |
| `ruoyi-redis` | `192m` | 已配置 `maxmemory 128mb` |
| `ruoyi-nacos` | `512m` | 配置中心 |

## 当前已做优化

1. 修复了服务器登录 shell 环境，恢复 `docker` 命令可用。
2. 清理了错误注入的宿主机 JDK 环境变量。
3. 前后端均恢复为 Docker 化部署。
4. 后端各 Java 微服务增加了：
   - JVM 堆上限
   - Docker 内存上限
   - Docker 内存预留
   - `pids-limit`
   - Docker 日志轮转
5. 前端 `nginx` 容器增加了：
   - 内存上限
   - 内存预留
   - `pids-limit`
   - 日志轮转
6. `docker-compose` 中的 `mysql`、`redis`、`nacos`、`nginx` 已补充资源限制配置。
7. `ruoyi-job` 模块已从代码与默认部署中移除。
8. `redis` 已增加：
   - `maxmemory 128mb`
   - `maxmemory-policy allkeys-lru`

## 重要配置文件

- 后端部署脚本：`/zoumh/java/zmh/backend/bin/deploy-backend-host.sh`
- 前端部署脚本：`/zoumh/java/zmh/frontend/bin/deploy-frontend-host.sh`
- 后端仓库脚本：`scripts/deploy-backend-host.sh`
- 前端仓库脚本：`scripts/deploy-frontend-host.sh`
- Docker Compose：`docker/docker-compose.yml`
- Redis 配置：`docker/redis/conf/redis.conf`

## 运维建议

1. 如果某个服务出现 OOM，只单独上调该服务，不要整体放大所有服务。
2. 发布后优先观察：
   - `docker stats`
   - 服务启动日志
   - 网关与系统模块是否正常访问
3. 如果后续启用新的基础服务，也要同步设置：
   - 内存上限
   - 日志轮转
   - 进程数限制

## 文档更新时间

- 维护时间：2026-03-17
- 维护说明：由 Codex 根据当前 Jenkins 部署与服务器运行状态整理
