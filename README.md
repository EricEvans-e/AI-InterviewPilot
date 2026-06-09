<div align="center">

<img src="./logo.png" alt="星辰升学 logo" width="180" />

# AI-InterviewPilot

**基于大语言模型的 AI 智能模拟面试平台**

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.2-blue?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-blue?logo=typescript)](https://www.typescriptlang.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)](https://www.mysql.com/)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0-green?logo=mongodb)](https://www.mongodb.com/)
[![Redis](https://img.shields.io/badge/Redis-7.2-red?logo=redis)](https://redis.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 目录

- [项目概述](#项目概述)
- [功能特点](#功能特点)
- [技术栈](#技术栈)
- [目录结构](#目录结构)
- [本地完整运行方式](#本地完整运行方式)
- [使用方法](#使用方法)
- [系统架构](#系统架构)
- [贡献指南](#贡献指南)
- [许可证](#许可证)
- [联系方式](#联系方式)

---

## 项目概述

**AI-InterviewPilot** 是一个基于大语言模型的 AI 智能模拟面试平台，专为浙江高职提前招生面试备考场景打造。系统支持学生上传简历、AI 智能出题、数字人模拟面试、实时语音交互、多维评分诊断与提升建议，帮助考生在真实校测前进行充分的模拟训练。

本项目采用前后端分离架构：
- **后端** (`AI-Meeting/`)：基于 Java 17 + Spring Boot 3.4 + Spring AI 构建的模块化单体应用
- **前端** (`AI-Meeting-Frontend/`)：基于 React 19 + TypeScript + Vite + Tailwind CSS 构建的现代化单页应用

---

## 功能特点

### 学生端

| 功能模块 | 说明 |
|---------|------|
| **智能简历解析** | 上传 PDF 简历后，AI 自动解析内容并生成与经历深度关联的个性化面试题目 |
| **数字人模拟面试** | 支持真人风格数字人面试官，TTS 语音播报题目，模拟真实面试场景 |
| **多种面试模式** | 结构化面试、半结构化面试、专业认知面试、综合素质面试 |
| **实时语音交互** | 基于 WebSocket 接收麦克风音频，停止转写后由 Mimo ASR 返回最终文本 |
| **AI 智能追问** | 根据学生回答内容进行细节追问、案例追问、专业认知追问等多类型追问 |
| **多维评分报告** | 面试结束后生成包含总分、各维度得分、雷达图、答题回放、提升建议的完整报告 |
| **面试记录回放** | 支持查看历史面试记录，回放面试视频与语音转写文本 |

### 教师端

| 功能模块 | 说明 |
|---------|------|
| **题库管理** | 按院校、专业、题型、能力点维护题库，支持评分标准与参考答案配置 |
| **AI 智能拓题** | 基于已审核考纲和专业方向，AI 自动生成相似题目，经老师审核后发布 |
| **学生训练报告** | 查看学生训练次数、得分趋势、高频问题，支持人工复评和补充点评 |
| **院校专业库** | 维护浙江高职提前招生相关院校、专业、考纲资料库 |

### 管理端

| 功能模块 | 说明 |
|---------|------|
| **用户管理** | 学生、教师、管理员账号管理与权限分配 |
| **AI 模型配置** | 运行时切换和管理 Mimo OpenAI 兼容模型 |
| **智能体场景绑定** | 配置面试各环节使用的 AI 智能体（出题官 / 评分官 / 追问官等） |
| **数据看板** | 注册人数、活跃人数、训练次数、平均得分、院校热度等核心指标 |

### 核心技术亮点

- **分布式 Single-flight**：基于 Redis Lua + 状态机实现多实例场景下 AI 调用去重，避免重复扣费
- **长会话状态治理**：基于 MongoDB 热冷分层快照 + Redis 懒加载构建可恢复运行态体系，支持面试中断恢复
- **Mimo 统一接入**：基于 Spring AI + 策略路由统一接入 Mimo OpenAI 兼容模型；`mimo-v2.5` 用于通用/视觉链路，`mimo-v2.5-pro` 仅用于纯文本高推理聊天
- **Mimo ASR 音频缓冲转写**：基于 WebSocket 接收麦克风 PCM 音频，后端封装为 WAV 后调用 Mimo ASR，并通过 `transcription` / `final` 事件回推文本
- **LiteFlow 规则链**：基于 LiteFlow 规则引擎驱动追问裁决与面试流程推进

---

## 技术栈

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.4.4 | 应用框架 |
| Spring AI | 1.0.0 | AI 集成框架，统一多模型接入 |
| Spring Cloud | 2024.0.0 | 微服务基础设施 |
| MyBatis-Plus | 3.5.9 | ORM 持久层框架 |
| MySQL | 8.0 | 关系型数据库，存储结构化业务数据 |
| MongoDB | 7.0 | 文档数据库，存储会话快照与对话消息 |
| Redis + Redisson | 7.2 / 3.27.2 | 分布式锁、缓存、限流、会话存储 |
| Sa-Token | 1.39.0 | 权限认证框架，支持 Redis 共享登录态 |
| Resilience4j | 2.2.0 | 熔断、限流、重试、舱壁隔离 |
| LiteFlow | 2.15.3.2 | 规则引擎，驱动追问裁决等业务规则链 |
| Mimo Token Plan | - | 文本模型、语音转写（ASR）和语音合成（TTS） |
| WebSocket / SSE | - | 实时 ASR 双向通信、AI 流式响应 |
| Maven | 3.6.3+ | 构建工具 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| React | 19.2 | UI 框架 |
| TypeScript | 5.9 | 开发语言 |
| Vite | 7.3 | 构建工具与开发服务器 |
| Tailwind CSS | 3.4 | 原子化样式框架 |
| shadcn/ui + Radix UI | - | 无障碍 UI 组件库 |
| React Router DOM | 7.13 | 路由管理 |
| Redux Toolkit | 2.11 | 全局状态管理（用户认证、聊天运行时状态） |
| TanStack React Query | 5.90 | 服务端状态管理与数据缓存 |
| Framer Motion | 12.35 | 动画库 |
| Axios | 1.13 | HTTP 请求客户端 |
| Zod | 4.3 | 数据校验与表单 Schema |
| React Hook Form | 7.71 | 表单状态管理 |
| React PDF | 10.4 | PDF 简历预览 |
| React Markdown | 10.1 | Markdown 渲染（AI 回复展示） |
| Vitest | 4.0 | 单元测试框架 |

### 运维与部署

| 技术 | 说明 |
|------|------|
| Docker Compose | 容器化一键部署（MySQL + MongoDB + Redis + 应用） |
| GitHub Actions | CI 流水线，自动执行单元测试与构建 |
| Nginx | 前端生产环境部署与反向代理 |

---

## 目录结构

```
AI-InterviewPilot/
├── AI-Meeting/                     # 后端项目（Java Spring Boot）
│   ├── admin/                      # 核心业务模块
│   │   ├── src/main/java/com/interviewpilot/
│   │   │   ├── interview/          # 面试领域（核心状态机、答题pipeline、评分）
│   │   │   ├── ai/                 # AI 对话领域（多模型统一接入）
│   │   │   ├── agent/              # 智能体领域（Mimo 场景绑定，兼容 legacy 讯飞工作流）
│   │   │   ├── media/              # 媒体领域（ASR、TTS）
│   │   │   ├── user/               # 用户领域（账号、权限）
│   │   │   ├── auth/               # 认证领域（Sa-Token）
│   │   │   └── common/             # 公共基础设施
│   │   ├── src/main/resources/
│   │   │   ├── sql/                # 数据库初始化脚本
│   │   │   ├── mapper/             # MyBatis XML 映射文件
│   │   │   ├── workflow/           # AI 智能体工作流 YAML 配置
│   │   │   └── application.yaml    # 应用配置文件
│   │   └── pom.xml
│   ├── skills/                     # 领域知识技能文档（AI Coding 知识库）
│   ├── docker-compose.yml          # Docker Compose 部署配置
│   ├── Dockerfile                  # 后端镜像构建文件
│   └── pom.xml                     # Maven 父 POM
│
├── AI-Meeting-Frontend/            # 前端项目（React + TypeScript）
│   ├── src/
│   │   ├── app/                    # 应用入口与路由配置
│   │   ├── components/             # 业务组件（按领域分层）
│   │   │   ├── chat/               # 聊天组件
│   │   │   ├── interview/          # 面试组件
│   │   │   ├── audio/              # 音频组件
│   │   │   ├── camera/             # 摄像头组件
│   │   │   ├── layout/             # 布局组件
│   │   │   ├── ui/                 # 基础 UI 组件（shadcn/ui）
│   │   │   └── ...
│   │   ├── hooks/                  # 自定义 Hooks（Controller 模式）
│   │   ├── pages/                  # 页面组件
│   │   ├── services/               # API 服务层
│   │   ├── store/                  # Redux 状态管理
│   │   ├── lib/                    # 工具函数与常量
│   │   └── types/                  # TypeScript 类型定义
│   ├── public/                     # 静态资源
│   ├── docker/                     # Docker 部署配置
│   ├── package.json
│   └── vite.config.ts
│
├── docs/                           # 项目文档
├── scripts/                        # 辅助脚本
├── src/                            # 原始题库资源（院校/专业面试题）
├── README.md
└── PRD.md                          # 产品需求文档
```

---

## 本地完整运行方式

更详细的本地启动、验证和排障步骤见 [docs/local-run-guide.md](docs/local-run-guide.md)。生产 Docker Compose 部署见 [docs/deployment-guide.md](docs/deployment-guide.md)。

### Report readiness notes

- The report page is now designed to show base report data first and keep waiting for delayed assets instead of failing fast on the first timeout.
- Interview recording playback can appear a little later than the first report payload. The frontend now keeps polling for the recording URL for about one minute before giving up.
- Reference answers are manual on the report page. Clicking `生成参考答案` sends a longer-lived request and, if the request times out on the client side, the frontend continues polling the saved report result before surfacing an error.
- Final report persistence no longer blocks on synchronous AI review-summary generation. The first saved snapshot uses fast rule-based review content so the page can open earlier.
- Interview conclusion is also manual on the report page. Clicking `生成 AI 结论` triggers a longer AI request, and the current report view updates in place after the AI result is saved.
- If the `生成 AI 结论` request times out on the client side, the frontend continues polling the saved report and replaces the initial rule-based summary once the AI conclusion is ready.

### 环境要求

| 组件 | 版本要求 |
|------|---------|
| JDK | 17+ |
| Node.js | 20+ |
| Maven | 3.6.3+ |
| MySQL | 8.0 |
| MongoDB | 6.x+ |
| Redis | 7.x |
| Docker & Docker Compose | （可选，用于容器化部署） |

### 1. 准备仓库

```bash
git clone https://github.com/EricEvans-e/AI-InterviewPilot.git
cd AI-InterviewPilot
```

如果已经在当前工作区，直接进入项目根目录：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting
```

### 2. 配置 Mimo API Key

后端本地启动现在会自动加载 `AI-Meeting/.env`，并在从 `AI-Meeting/admin` 启动时继续向上查找父目录中的 `.env`。如果 shell 里同时设置了同名环境变量，以 shell 环境变量为准。Windows PowerShell 中仍然推荐在启动后端的同一个终端显式设置环境变量：

```powershell
$env:MIMO_API_KEY="你的-token-plan-api-key"
$env:SPRING_AI_OPENAI_API_KEY=$env:MIMO_API_KEY
$env:MIMO_OPENAI_BASE_URL="https://token-plan-cn.xiaomimimo.com/v1"
$env:MIMO_CHAT_MODEL="mimo-v2.5"
$env:MIMO_PRO_MODEL="mimo-v2.5-pro"
$env:MIMO_ASR_MODEL="mimo-v2.5-asr"
$env:MIMO_TTS_MODEL="mimo-v2.5-tts"
$env:LEGACY_XUNFEI_ENABLED="false"
```

Linux / macOS：

```bash
export MIMO_API_KEY="你的-token-plan-api-key"
export SPRING_AI_OPENAI_API_KEY="$MIMO_API_KEY"
export MIMO_OPENAI_BASE_URL="https://token-plan-cn.xiaomimimo.com/v1"
export MIMO_CHAT_MODEL="mimo-v2.5"
export MIMO_PRO_MODEL="mimo-v2.5-pro"
export MIMO_ASR_MODEL="mimo-v2.5-asr"
export MIMO_TTS_MODEL="mimo-v2.5-tts"
export LEGACY_XUNFEI_ENABLED="false"
```

不要把真实 API Key 写入 README、`.env.example` 或任何会提交到 Git 的文件。

### 3. 启动基础设施

```bash
cd AI-Meeting
docker-compose up -d mysql mongo redis
```

这将会启动：
- MySQL（端口 3307）
- MongoDB（端口 27017）
- Redis（端口 6379）

数据库初始化脚本会自动执行，创建必要的表结构和默认数据。

确认容器状态：

```bash
docker-compose ps
```

如果 MySQL 已经初始化过，确认数据库里的模型配置仍使用 `MIMO_API_KEY` 占位符，不要把真实 key 写入数据库。后端运行时会从当前进程环境变量解析真实 key。

```powershell
docker exec -it ip-mysql mysql -uroot -p122333 mainshi_agent
```

进入 MySQL 后可执行一次配置校正：

```sql
UPDATE ai_properties
SET api_key = 'MIMO_API_KEY'
WHERE api_key <> 'MIMO_API_KEY';

UPDATE agent_properties
SET api_key = 'MIMO_API_KEY',
    api_flow_id = 'https://token-plan-cn.xiaomimimo.com/v1',
    ai_provider = 'openai'
WHERE del_flag = 0;
```

只要后端启动终端里设置了 `MIMO_API_KEY` / `SPRING_AI_OPENAI_API_KEY`，占位符会在运行时自动解析。

### 4. 启动后端服务

```bash
# Linux / macOS
./mvnw -B -ntp clean verify -Dmaven.test.skip=true
./mvnw spring-boot:run -pl admin

# Windows
mvnw.cmd -B -ntp clean verify -Dmaven.test.skip=true
mvnw.cmd spring-boot:run -pl admin
```

后端服务默认运行在 **http://localhost:8002**

健康检查：

```powershell
Invoke-RestMethod http://localhost:8002/actuator/health
```

返回 `status: UP` 表示后端基础服务已启动。

### 5. 启动前端开发服务器

另开一个终端：

```bash
cd ../AI-Meeting-Frontend
npm ci
npm run dev
```

前端开发服务器默认运行在 **http://localhost:5173**，并代理 `/api` 请求到后端服务。

前端环境默认配置：

```env
VITE_API_BASE_URL=/api
VITE_API_TARGET=http://localhost:8002
VITE_WS_BASE_URL=
```

访问：

```text
http://localhost:5173
```

### 6. 默认登录账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin |

### 7. 验证 AI / ASR / TTS 可用

1. 登录后台，进入 AI 模型配置或智能体场景绑定，确认默认模型为 `mimo-v2.5`，Base URL 为 `https://token-plan-cn.xiaomimimo.com/v1`。
2. 在通用 AI 对话或 Agent 对话中发送一条消息，确认 SSE 有文本返回。
3. 在面试页面授权麦克风，开始录音后发送语音，停止转写后应收到最终文本。
4. 面试题播报或 TTS 测试应返回可播放音频；新链路主要读取 `audioBase64`。

补充说明：
- 面试题如果返回 Java Map 风格包装，如 `{question=...}` 或 `{id=1, topic=..., question=..., purpose=...}`，前端会先归一化为纯题目文本，再写入“当前题目”、聊天消息和 TTS 文本。
- Mimo TTS 请求体中的实际播报文本必须放在 `assistant` 角色消息里；如果后端日志出现 `messages must contain an assistant role for TTS model`，说明请求体结构被改坏了。

### 8. 一键 Docker 生产式运行

后端仓库提供 `docker-compose.prod.yml`，会同时构建后端和前端镜像，并通过前端 Nginx 暴露 80 端口：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting\AI-Meeting
Copy-Item .env.example .env
```

编辑 `AI-Meeting/.env`，填入真实 `MIMO_API_KEY` 和 `SPRING_AI_OPENAI_API_KEY`，然后启动：

```powershell
docker-compose -f docker-compose.prod.yml up -d --build
```

访问：

```text
http://localhost
```

查看日志：

```powershell
docker logs -f ip-backend
docker logs -f ip-frontend
```

停止服务：

```powershell
docker-compose -f docker-compose.prod.yml down
```

### 9. 常用检查命令

后端测试：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting\AI-Meeting
.\mvnw.cmd -q -pl admin test
```

前端检查：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting\AI-Meeting-Frontend
npm run check
```

前端构建：

```powershell
npm run build
```

密钥泄露扫描：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting
rg -n "tp-[A-Za-z0-9]{20,}" . -S
```

如果扫描命中真实 key，不要提交；先替换成占位符或从文件中移除。

### 10. 常见问题

| 现象 | 处理方式 |
|------|----------|
| 后端启动但 AI 无响应 | 先确认 `AI-Meeting/.env` 或当前启动终端里已经提供 `MIMO_API_KEY` / `SPRING_AI_OPENAI_API_KEY`；数据库 `ai_properties`、`agent_properties` 应保留 `MIMO_API_KEY` 占位符，由后端运行时解析真实 key |
| 前端请求 404 | 确认后端在 `8002`，前端 `VITE_API_TARGET=http://localhost:8002` |
| WebSocket 连不上 | 确认前端通过 `5173` 访问，Vite proxy 开启 `ws: true`；登录后再进入录音页面 |
| ASR 没有最终文本 | 需要发送 `stop_transcription` 结束音频流，后端才会调用 Mimo ASR 返回最终文本 |
| TTS 成功但没声音 | 检查后端返回的 `audioBase64` 是否为空，查看 `MimoAudioService` 调用日志；如果日志里有 `messages must contain an assistant role for TTS model`，说明 TTS 请求体里的播报文本没有放在 `assistant` 角色消息中 |
| MySQL 密码不对 | 本地 compose 默认 root 密码通常是 `122333`；如果使用 `.env` 覆盖，请以 `.env` 为准 |
| 当前题目显示成 `{id=..., question=...}` 一整串对象文本 | 说明题目文本没有经过 `normalizeInterviewQuestionText()` 归一化，检查前端会话流是否绕过了这一步 |
| Redis 报 `Bind for 0.0.0.0:6379 failed` | 说明本机已有 Redis 占用 6379；先执行 `docker stop interviewpilot-redis`，再重试 `docker compose up -d mysql mongo redis` |

---

## 使用方法

### 学生模拟面试流程

1. **注册/登录**：访问首页，使用手机号或默认管理员账号登录
2. **完善档案**：填写姓名、学校、目标院校、目标专业等基本信息
3. **上传简历**：进入面试大厅，上传 PDF 格式简历
4. **AI 出题**：系统解析简历并生成个性化面试题目
5. **设备检测**：授权摄像头和麦克风，检测网络、光线、声音
6. **开始面试**：数字人面试官开场提问，学生通过语音或文字回答
7. **智能追问**：根据回答内容，AI 自动进行多轮追问
8. **查看报告**：面试结束后生成包含评分、点评、回放、建议的完整报告

### 教师管理流程

1. **维护院校专业库**：在教师后台添加院校、专业、考纲资料
2. **管理题库**：手动录入题目，或使用 AI 拓题功能批量生成相似题目
3. **审核发布**：AI 生成的题目默认进入"待审核"状态，经老师确认后发布
4. **查看报告**：查看学生训练记录与 AI 评分报告，支持人工复评

### 管理员配置流程

1. **用户管理**：管理学生、教师、管理员账号与角色权限
2. **AI 模型配置**：添加和管理多模型配置（API Key、模型参数等）
3. **智能体场景绑定**：配置面试各环节（出题/评分/追问/表情分析）使用的 AI 智能体

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端层 (Frontend)                       │
│  React 19 + TypeScript + Vite + Tailwind CSS + Redux        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │  学生端   │ │  教师端   │ │  管理端   │ │ AI 对话  │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP / SSE / WebSocket
┌────────────────────────▼────────────────────────────────────┐
│                     后端层 (Backend)                         │
│  Spring Boot 3.4 + Java 17 + Spring AI                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ Interview│ │    AI    │ │  Agent   │ │  Media   │       │
│  │  面试领域  │ │ 对话领域  │ │ 智能体领域 │ │ 媒体领域  │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                    │
│  │   User   │ │   Auth   │ │  Common  │                    │
│  │ 用户领域  │ │ 认证领域  │ │ 公共基础  │                    │
│  └──────────┘ └──────────┘ └──────────┘                    │
└────────────────────────┬────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼──────┐ ┌───────▼──────┐ ┌───────▼──────┐
│    MySQL     │ │   MongoDB    │ │    Redis     │
│  结构化数据   │ │  文档数据     │ │   缓存/会话   │
│  用户/配置    │ │ 会话/消息     │ │  分布式锁    │
└──────────────┘ └──────────────┘ └──────────────┘
```

---

## 贡献指南

我们欢迎所有形式的贡献，包括但不限于：

- 提交 Issue 反馈 Bug 或提出功能建议
- 提交 Pull Request 修复问题或新增功能
- 完善文档与使用教程
- 分享使用经验与最佳实践

### 提交 Issue

1. 在提交 Issue 前，请先搜索是否已有类似问题
2. 使用对应的 Issue 模板（Bug 报告 / 功能建议）
3. 提供尽可能详细的环境信息与复现步骤

### 提交 Pull Request

1. Fork 本仓库到您的个人账号
2. 基于 `master` 分支创建特性分支：`git checkout -b feature/your-feature`
3. 提交代码变更，遵循现有代码风格与提交规范
4. 确保本地测试通过：`npm run check`（前端）或 `./mvnw test`（后端）
5. 提交 PR 到本仓库的 `master` 分支，并详细描述变更内容

### 代码规范

- **后端**：遵循 Google Java Style Guide，使用 Spotless Maven 插件进行格式化
- **前端**：使用 ESLint + Prettier 进行代码检查与格式化，遵循 Conventional Commits 提交规范

---

## 许可证

本项目采用 [MIT 许可证](LICENSE) 开源。

```
MIT License

Copyright (c) 2026 AI-InterviewPilot Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 联系方式

如有任何问题或建议，欢迎通过以下方式联系：

- **GitHub Issues**: [https://github.com/EricEvans-e/AI-InterviewPilot/issues](https://github.com/EricEvans-e/AI-InterviewPilot/issues)

---

<div align="center">

**如果觉得本项目对您有帮助，欢迎 Star ⭐ 支持！**

</div>
