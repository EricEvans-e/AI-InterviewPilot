# InterviewPilot 生产环境部署指南

本文档提供将 InterviewPilot 前后端完整部署到单台 Linux 服务器的详细步骤。

---

## 目录

- [一、概述](#一概述)
- [二、服务器准备](#二服务器准备)
- [三、环境变量配置](#三环境变量配置)
- [四、构建与启动](#四构建与启动)
- [五、验证部署](#五验证部署)
- [六、后续维护](#六后续维护)
- [七、常见问题与解决](#七常见问题与解决)
- [八、进阶：配置 HTTPS 与域名](#八进阶配置-https-与域名)
- [九、部署架构](#九部署架构)

---

## 一、概述

InterviewPilot 采用前后端分离架构：

- **后端**：Java 17 + Spring Boot，运行在 Docker 容器中，暴露 8002 端口（内部）
- **前端**：React 19 + Vite，构建为静态文件后由 Nginx 托管，暴露 80 端口
- **基础设施**：MySQL 8.4 + MongoDB 7.0 + Redis 7.2，均由 Docker Compose 编排

生产部署使用 `AI-Meeting/docker-compose.prod.yml`，一次性启动所有服务。

---

## 二、服务器准备

### 2.1 推荐配置

| 项目 | 最低要求 | 推荐配置 |
|------|---------|---------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 40 GB SSD | 100 GB SSD（面试视频会占用空间） |
| 系统 | Ubuntu 22.04 / CentOS 8 / Debian 12 | Ubuntu 24.04 LTS |
| 网络 | 开放 TCP 80 端口 | 开放 TCP 80/443 端口 |

### 2.2 安装 Docker 与 Docker Compose

```bash
# Ubuntu / Debian
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 验证安装
docker --version
docker compose version
```

---

## 三、环境变量配置

### 3.1 拉取代码

```bash
cd /opt
git clone https://github.com/EricEvans-e/AI-InterviewPilot.git
cd AI-InterviewPilot
```

### 3.2 创建环境变量文件

项目根目录已提供 `.env.example` 模板，复制并修改：

```bash
cp .env.example .env
nano .env
```

### 3.3 必须修改的配置项

```dotenv
# ============================================================
# 数据库配置
# ============================================================
MYSQL_PASSWORD=你的强密码
MYSQL_DATABASE=mainshi_agent
MONGODB_DATABASE=interview_pilot
REDIS_PASSWORD=

# ============================================================
# AI 模型密钥（核心功能依赖，必须配置）
# ============================================================

# 小米 Mimo Token Plan（项目默认，覆盖聊天、面试、ASR、TTS）
MIMO_API_KEY=tp-your-token-plan-api-key
MIMO_OPENAI_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
MIMO_ANTHROPIC_BASE_URL=https://token-plan-cn.xiaomimimo.com/anthropic
MIMO_CHAT_MODEL=mimo-v2.5
MIMO_PRO_MODEL=mimo-v2.5-pro
MIMO_ASR_MODEL=mimo-v2.5-asr
MIMO_TTS_MODEL=mimo-v2.5-tts

# Spring AI 默认也指向 Mimo OpenAI 兼容接口
SPRING_AI_OPENAI_API_KEY=tp-your-token-plan-api-key
SPRING_AI_OPENAI_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
SPRING_AI_OPENAI_MODEL=mimo-v2.5

# 讯飞 legacy 集成默认关闭；只有显式启用 legacy.xunfei 时才需要以下变量
LEGACY_XUNFEI_ENABLED=false
XUNFEI_APP_ID=
XUNFEI_API_KEY=
XUNFEI_API_SECRET=
XUNFEI_RTA_API_KEY=
```

> **警告**：`.env` 文件包含敏感密钥，**切勿提交到 Git**。项目已默认在 `.gitignore` 中忽略该文件。

---

## 四、构建与启动

### 4.1 进入后端目录并启动

```bash
cd AI-Meeting

# 构建镜像并后台启动（首次构建需要 5-15 分钟，取决于网络）
docker compose -f docker-compose.prod.yml up -d --build
```

命令说明：

- `-f docker-compose.prod.yml`：使用生产环境编排文件
- `--build`：构建前后端镜像（后续更新代码时也需要加此参数）
- `-d`：后台运行

### 4.2 启动顺序

Docker Compose 会按依赖顺序自动启动：

1. `mysql` → 自动执行 SQL 初始化脚本（创建表结构、默认管理员账号）
2. `mongo` → 初始化文档数据库
3. `redis` → 启动缓存与会话存储
4. `backend` → 等待前三个服务健康检查通过后启动 Spring Boot 应用
5. `frontend` → 等待后端健康后启动 Nginx 前端容器

---

## 五、验证部署

### 5.1 查看容器状态

```bash
docker compose -f docker-compose.prod.yml ps
```

所有服务应显示 `healthy` 或 `running`。若某服务显示 `unhealthy` 或反复重启，请查看对应容器日志。

### 5.2 查看日志

```bash
# 后端实时日志
docker logs -f ip-backend

# 前端日志
docker logs -f ip-frontend

# MySQL 初始化日志
docker logs -f ip-mysql

# 查看全部服务日志
docker compose -f docker-compose.prod.yml logs -f
```

### 5.3 页面访问验证

在浏览器中访问服务器 IP 或域名：

| 地址 | 说明 |
|------|------|
| `http://服务器IP` | 前端首页 |
| `http://服务器IP/auth` | 登录/注册页面 |

**默认管理员账号**：

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin |

### 5.4 接口连通性验证

```bash
curl http://服务器IP/api/ip/v1/users/check-login
```

应返回 401 或 JSON 响应。若返回 404/502，说明 Nginx 反向代理配置有误或后端未启动。

### 5.5 AI 功能验证

登录后进入 `/chat` 页面，发送一条消息测试 AI 对话；或进入 `/interview` 上传简历测试面试流程。如果 AI 无响应，请检查：

1. `.env` 中的 API Key 是否正确
2. 后端日志中是否有 AI 调用超时或认证失败的错误

---

## 六、后续维护

### 6.1 更新代码后重新部署

```bash
cd /opt/AI-InterviewPilot
git pull

cd AI-Meeting
docker compose -f docker-compose.prod.yml up -d --build
```

### 6.2 仅重启服务（不改代码）

```bash
cd AI-Meeting
docker compose -f docker-compose.prod.yml restart
```

### 6.3 停止全部服务

```bash
cd AI-Meeting
docker compose -f docker-compose.prod.yml down
```

> 使用 `down -v` 会同时删除数据卷（**所有数据将丢失**），请谨慎操作。

### 6.4 数据备份

```bash
# 创建备份目录
mkdir -p /opt/backups

# MySQL 备份
docker exec ip-mysql mysqldump -uroot -p"你的密码" mainshi_agent \
  > /opt/backups/mysql_$(date +%F_%H%M).sql

# MongoDB 备份
docker exec ip-mongo mongodump --archive \
  > /opt/backups/mongo_$(date +%F_%H%M).archive

# 面试录制文件与上传文件备份
docker run --rm -v ai-meeting_backend-data:/data -v /opt/backups:/backup alpine \
  tar czf /backup/recordings_$(date +%F_%H%M).tar.gz -C /data .
```

建议配置 `crontab` 每日自动备份：

```bash
0 2 * * * cd /opt/AI-InterviewPilot && ./scripts/backup.sh
```

### 6.5 调整 JVM 内存

编辑 `AI-Meeting/Dockerfile` 中 `JAVA_OPTS` 一行：

```dockerfile
ENV JAVA_OPTS="-Xms1g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/"
```

然后重新构建：

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

### 6.6 查看资源占用

```bash
# 容器资源使用
docker stats

# 磁盘使用
docker system df -v
```

---

## 七、常见问题与解决

| 问题现象 | 可能原因 | 解决方案 |
|---------|---------|---------|
| `docker compose up` 卡在 backend 的 `start_period` | MySQL 首次初始化较慢 | 等待 2-3 分钟；或先单独启动基础设施 `docker compose -f docker-compose.prod.yml up -d mysql mongo redis` |
| 前端页面能打开，但 API 请求报 404/502 | Nginx 未正确代理 `/api` | 确认使用 `docker-compose.prod.yml` 启动；检查 `ip-frontend` 日志 |
| AI 面试/对话/ASR/TTS 功能无响应 | Mimo API Key 未配置或无效 | 检查 `.env` 中的 `MIMO_API_KEY` / `SPRING_AI_OPENAI_API_KEY`；重启 `ip-backend` |
| ASR 语音转写连接失败 | WebSocket 未正确代理 | `nginx.conf` 已配置 `Upgrade` 和 `Connection` 头；确认前端通过 80 端口访问 |
| 构建时 Maven 下载极慢 | 访问中央仓库网络不畅 | 在 `AI-Meeting/Dockerfile` builder 阶段添加阿里云 Maven 镜像 |
| 80 端口已被占用 | 服务器已有 Nginx/Apache | 修改 `docker-compose.prod.yml` 前端端口为 `"8080:80"` 或其他端口 |
| MongoDB/Redis 端口冲突 | 宿主机已有同名服务 | 修改 `docker-compose.prod.yml` 中的端口映射，或使用宿主机现有实例 |
| 面试视频无法回放 | `/recordings/` 代理异常 | 检查 `ip-backend` 日志；确认 `backend-data` 卷中存在录制文件 |

---

## 八、进阶：配置 HTTPS 与域名

### 方案 A：宿主机 Nginx 反向代理（推荐）

如果服务器已有 Nginx 或需要托管多个站点，将 Docker 前端端口改为仅本地监听：

```yaml
# docker-compose.prod.yml 修改
frontend:
  ports:
    - "127.0.0.1:8080:80"
```

然后在宿主机配置 Nginx：

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket 支持
    location /api/ip/v1/mimo/audio-to-text/ {
        proxy_pass http://127.0.0.1:8080/api/ip/v1/mimo/audio-to-text/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}

server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}
```

### 方案 B：直接使用 Docker 映射 443 端口

如果你有现成的证书文件，也可以修改 `docker-compose.prod.yml` 使用自定义 Nginx 配置，挂载证书目录到前端容器，直接对外暴露 443 端口。

---

## 九、部署架构

```
                               用户浏览器
                                   │
                                   ▼
                          ┌─────────────────┐
                          │   服务器 80 端口  │
                          │  （Nginx 前端容器）│
                          │   ip-frontend   │
                          └────────┬────────┘
                                   │ /api/* 反向代理
                                   │ /recordings/* 反向代理
                                   ▼
                          ┌─────────────────┐
                          │  后端 8002 端口   │
                          │ （Spring Boot）  │
                          │   ip-backend    │
                          └────────┬────────┘
                                   │
            ┌──────────────────────┼──────────────────────┐
            ▼                      ▼                      ▼
   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
   │   ip-mysql      │  │   ip-mongo      │  │   ip-redis      │
   │  3307:3306      │  │ 27017:27017     │  │ 6379:6379       │
   │   结构化数据     │  │   文档数据       │  │   缓存/会话      │
   └─────────────────┘  └─────────────────┘  └─────────────────┘
```

### 数据持久化说明

| 数据类型 | 存储位置 | 持久化方式 |
|---------|---------|-----------|
| MySQL 数据 | `/var/lib/mysql` | Docker 卷 `mysql-data` |
| MongoDB 数据 | `/data/db` | Docker 卷 `mongo-data` |
| Redis 数据 | `/data` | Docker 卷 `redis-data` |
| 面试录制/上传文件 | `/app/data` | Docker 卷 `backend-data` |
| 后端日志 | `/app/logs` | Docker 卷 `backend-logs` |

---

## 附录：相关文件清单

| 文件 | 说明 |
|------|------|
| `AI-Meeting/docker-compose.prod.yml` | 生产环境 Docker Compose 编排 |
| `AI-Meeting/Dockerfile` | 后端多阶段构建镜像 |
| `AI-Meeting-Frontend/Dockerfile` | 前端构建 + Nginx 镜像 |
| `AI-Meeting-Frontend/docker/nginx.conf` | 前端 Nginx 配置（含反向代理） |
| `.env` | 环境变量与密钥配置（需手动创建） |
| `.env.example` | 环境变量模板 |
| `AI-Meeting/admin/src/main/resources/sql/*.sql` | 数据库初始化脚本 |
