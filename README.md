<div align="center">

# InterviewPilot2

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
- [安装步骤](#安装步骤)
- [使用方法](#使用方法)
- [系统架构](#系统架构)
- [贡献指南](#贡献指南)
- [许可证](#许可证)
- [联系方式](#联系方式)

---

## 项目概述

**InterviewPilot2** 是一个基于大语言模型的 AI 智能模拟面试平台，专为浙江高职提前招生面试备考场景打造。系统支持学生上传简历、AI 智能出题、数字人模拟面试、实时语音交互、多维评分诊断与提升建议，帮助考生在真实校测前进行充分的模拟训练。

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
| **实时语音交互** | 基于 WebSocket + 讯飞 ASR 实现端到端实时语音识别与流式文本展示 |
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
| **AI 模型配置** | 运行时切换和管理 AI 模型（DeepSeek / 星火 / 豆包 / Anthropic 等） |
| **智能体场景绑定** | 配置面试各环节使用的 AI 智能体（出题官 / 评分官 / 追问官等） |
| **数据看板** | 注册人数、活跃人数、训练次数、平均得分、院校热度等核心指标 |

### 核心技术亮点

- **分布式 Single-flight**：基于 Redis Lua + 状态机实现多实例场景下 AI 调用去重，避免重复扣费
- **长会话状态治理**：基于 MongoDB 热冷分层快照 + Redis 懒加载构建可恢复运行态体系，支持面试中断恢复
- **多模型统一接入**：基于 Spring AI + 策略路由统一接入 DeepSeek、星火、豆包、Anthropic 等模型
- **实时 ASR 增量去重**：基于 TreeMap + seg_id/pgs 重叠比对实现分段增量去重，解决重复文本与前缀误删问题
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
| iFlytek SDK | 3.0.2 | 实时语音转写（ASR）、语音合成（TTS）、表情识别 |
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
InterviewPilot2/
├── AI-Meeting/                     # 后端项目（Java Spring Boot）
│   ├── admin/                      # 核心业务模块
│   │   ├── src/main/java/com/interviewpilot/
│   │   │   ├── interview/          # 面试领域（核心状态机、答题pipeline、评分）
│   │   │   ├── ai/                 # AI 对话领域（多模型统一接入）
│   │   │   ├── agent/              # 智能体领域（讯飞工作流集成）
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

## 安装步骤

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

### 1. 克隆仓库

```bash
git clone https://github.com/EricEvans-e/InterviewPilot2.git
cd InterviewPilot2
```

### 2. 启动基础设施（Docker Compose）

```bash
cd AI-Meeting
docker-compose up -d mysql mongo redis
```

这将会启动：
- MySQL（端口 3307）
- MongoDB（端口 27017）
- Redis（端口 6379）

数据库初始化脚本会自动执行，创建必要的表结构和默认数据。

### 3. 启动后端服务

```bash
# Linux / macOS
./mvnw -B -ntp clean verify -Dmaven.test.skip=true
./mvnw spring-boot:run -pl admin

# Windows
mvnw.cmd -B -ntp clean verify -Dmaven.test.skip=true
mvnw.cmd spring-boot:run -pl admin
```

后端服务默认运行在 **http://localhost:8002**

### 4. 启动前端开发服务器

```bash
cd ../AI-Meeting-Frontend
npm ci
npm run dev
```

前端开发服务器默认运行在 **http://localhost:5173**，并代理 `/api` 请求到后端服务。

### 5. 默认登录账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin |

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

Copyright (c) 2026 InterviewPilot2 Contributors

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

- **GitHub Issues**: [https://github.com/EricEvans-e/InterviewPilot2/issues](https://github.com/EricEvans-e/InterviewPilot2/issues)

---

<div align="center">

**如果觉得本项目对您有帮助，欢迎 Star ⭐ 支持！**

</div>
