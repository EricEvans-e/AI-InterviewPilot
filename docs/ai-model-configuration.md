# AI 模型配置与使用指南

本文档说明 InterviewPilot 平台的 AI 模型接入方式、配置方法和使用流程。

## 架构概览

平台采用**策略模式**管理多种 AI 模型，核心组件：

```
通用 AI 聊天：
前端模型选择器 → AiMessageController (SSE) → AiChatHandlerFactory
    ├─ MimoChatHandler (mimo, Anthropic 协议)
    └─ UniversalAiChatHandler (openai/deepseek/doubao/spark, OpenAI 兼容)
    → ai_properties 表

面试链路：
InterviewSessionFacade → InterviewAiInvoker.doChat()
    ├─ aiProvider=xingchen → XingChenAIClient (讯飞星辰 Workflow)
    └─ aiProvider=mimo     → MimoPromptBuilder + MimoChatHandler.callSync()
    → agent_properties 表
```

### 支持的 AI 提供方

| 提供方 | aiType | 协议 | Handler | 说明 |
|--------|--------|------|---------|------|
| OpenAI | `openai` | OpenAI 兼容 | UniversalAiChatHandler | GPT-4o 等 |
| 豆包 | `doubao` | OpenAI 兼容 | UniversalAiChatHandler | 字节跳动火山引擎 |
| 讯飞星火 | `spark` | OpenAI 兼容 | UniversalAiChatHandler | 讯飞星火大模型 |
| DeepSeek | `deepseek` | DeepSeek 专用 | UniversalAiChatHandler | 支持 reasoning_content 思维链 |
| **Mimo** | `mimo` | **Anthropic Messages API** | **MimoChatHandler** | 小米 Mimo 系列模型，支持 thinking 扩展思维 |

## Mimo 配置

### 前置条件

- 后端服务已启动（MySQL、MongoDB、Redis 已就绪）
- 拥有 Mimo API Key（从 [小米 Mimo 平台](https://xiaomimimo.com) 获取）

### 方式一：SQL 直接插入

连接 MySQL `mainshi_agent` 数据库，执行：

```sql
INSERT INTO ai_properties (
    ai_name, ai_type, api_key, api_url, model_name,
    max_tokens, temperature, system_prompt, is_enabled, is_default, del_flag, create_time, update_time
) VALUES (
    'Mimo V2.5 Pro',           -- 显示名称（前端模型选择器展示）
    'mimo',                     -- aiType 标识（必须为 'mimo'）
    'tp-s7h68tp5edc1zh2co9cw7n9oeakrkwp7fcwwajwom0rdo7wt',  -- API Key
    'https://token-plan-sgp.xiaomimimo.com/anthropic',       -- API 地址
    'mimo-v2.5-pro',            -- 模型名称
    8192,                       -- 最大输出 token 数
    1.0,                        -- 采样温度 (0-1.5)
    '你是InterviewPilot平台的智能助手，专注于浙江高职提前招生面试辅导。',  -- 系统提示词
    1,                          -- 启用状态 (1=启用, 0=禁用)
    1,                          -- 默认模型 (1=是, 0=否)
    0,                          -- 软删除 (0=正常)
    NOW(), NOW()
);
```

### 方式二：管理后台配置

1. 以管理员身份登录后台
2. 进入 `AI 配置管理` 页面（`/api/ip/v1/ai-properties`）
3. 点击「新增」，填写以下字段：

| 字段 | 值 | 说明 |
|------|-----|------|
| AI 名称 | Mimo V2.5 Pro | 前端展示名 |
| AI 类型 | mimo | 选择 mimo 类型 |
| API Key | tp-s7h68tp5... | 你的 Mimo API Key |
| API 地址 | https://token-plan-sgp.xiaomimimo.com/anthropic | 留空则使用默认值 |
| 模型名称 | mimo-v2.5-pro | 可选值见下方 |
| 最大 Token | 8192 | 范围 1-131072 |
| 温度 | 1.0 | 范围 0-1.5 |
| 系统提示词 | 你是... | 可选 |
| 启用 | 是 | 开关 |

### 可用模型

| 模型名称 | 特点 | 默认 max_tokens | thinking 支持 |
|----------|------|-----------------|---------------|
| `mimo-v2.5-pro` | 最强推理能力 | 131072 | 默认启用 |
| `mimo-v2.5` | 平衡性能与速度 | 32768 | 默认启用 |
| `mimo-v2-pro` | 高性能 | 131072 | 默认启用 |
| `mimo-v2-omni` | 多模态 | 32768 | 默认启用 |
| `mimo-v2-flash` | 快速响应 | 65536 | 默认关闭 |

### API 地址说明

| 地址 | 用途 |
|------|------|
| `https://token-plan-sgp.xiaomimimo.com/anthropic` | Token 计划端点（新加坡） |
| `https://api.xiaomimimo.com/anthropic/v1` | 官方默认端点 |

配置时 `api_url` 填写**不含** `/messages` 的基础路径，代码会自动拼接 `/messages` 端点。

## 其他模型配置

### DeepSeek

```sql
INSERT INTO ai_properties (ai_name, ai_type, api_key, api_url, model_name, max_tokens, temperature, is_enabled, is_default, del_flag, create_time, update_time)
VALUES ('DeepSeek V4 Flash', 'deepseek', 'sk-xxx', 'https://api.deepseek.com', 'deepseek-v4-flash', 8192, 0.7, 1, 1, 0, NOW(), NOW());
```

### 豆包（火山引擎）

```sql
INSERT INTO ai_properties (ai_name, ai_type, api_key, api_url, model_name, max_tokens, temperature, is_enabled, is_default, del_flag, create_time, update_time)
VALUES ('豆包 Pro', 'doubao', 'ak-xxx', 'https://ark.cn-beijing.volces.com/api/v3', 'doubao-pro-32k', 8192, 0.7, 1, 0, 0, NOW(), NOW());
```

### 讯飞星火

```sql
INSERT INTO ai_properties (ai_name, ai_type, api_key, api_url, model_name, max_tokens, temperature, is_enabled, is_default, del_flag, create_time, update_time)
VALUES ('星火 4.0', 'spark', 'ak-xxx', 'https://spark-api-open.xf-yun.com/v1', 'generalv3.5', 8192, 0.7, 1, 0, 0, NOW(), NOW());
```

## 使用方式

### 前端 AI 聊天

1. 登录后进入 AI 聊天页面
2. 点击模型选择器，会自动列出所有已启用的模型（通过 `GET /api/ip/v1/ai-properties/options` 获取）
3. 选择 "Mimo V2.5 Pro" 即可使用 Mimo 进行对话
4. 对话以 SSE 流式输出，支持 thinking 思维链展示

### 面试系统中的 AI 模型

面试系统的模型配置在 `agent_properties` 表中，不走 `ai_properties`。支持两种 AI 提供商：

| 提供商 | ai_provider | 说明 |
|--------|-------------|------|
| 讯飞星辰 | `xingchen`（默认） | Workflow Agent，支持文件上传 |
| Mimo | `mimo` | Anthropic 协议，纯文本 prompt |

面试链路的四个场景均可使用 Mimo：评分、追问、出题、神态分析。

#### 配置方式

`agent_properties` 表的 `ai_provider` 字段决定使用哪个提供商。当 `ai_provider = mimo` 时，字段复用约定：

| 字段 | 用途 |
|------|------|
| `api_key` | Mimo API Key（`tp-` 开头） |
| `api_secret` | 模型名称（如 `mimo-v2.5-pro`） |
| `api_flow_id` | API 地址（如 `https://token-plan-sgp.xiaomimimo.com/anthropic`） |

```sql
-- 插入 Mimo 面试 Agent
INSERT INTO agent_properties (agent_name, api_secret, api_key, api_flow_id, ai_provider, del_flag, create_time, update_time) VALUES
('Mimo面试评分官', 'mimo-v2.5-pro', 'tp-xxx', 'https://token-plan-sgp.xiaomimimo.com/anthropic', 'mimo', 0, NOW(), NOW());
```

然后在 `application.yaml` 的 `agent-binding` 中切换绑定：

```yaml
interview-pilot:
  agent-binding:
    interview-answer-evaluation: Mimo面试评分官
    interview-question-asking: Mimo面试提问官
    interview-question-extraction: Mimo面试出题官
    interview-demeanor: Mimo神态分析官
```

#### 技术细节

面试链路 Mimo 调用流程：

```
InterviewAiInvoker.doChat()
    ↓ agentProperties.getAiProvider()
    ├─ "xingchen" → XingChenAIClient.chat()（原有逻辑）
    └─ "mimo"    → MimoPromptBuilder.build() → MimoChatHandler.callSync()
```

`MimoPromptBuilder` 将 XingChen 的结构化 workflow 参数（`AGENT_USER_INPUT`、`question`、`resume_context` 等）转换为纯文本 prompt，自动适配评分、追问等场景。

注意事项：
- Mimo 没有文件上传 API，出题场景跳过简历上传，直接用纯文本 prompt
- 神态分析场景在 Mimo 模式下跳过图片上传，使用文本分析

### AI 题目生成

题库管理的「AI 拓题」功能支持选择 AI 模型：

1. 进入题库管理后台
2. 点击「AI 生成题目」
3. 在「AI 模型」下拉框选择具体模型（按 `id` 精确选择，不依赖 `aiType`）
4. 填写生成条件（院校、专业、题型等）
5. 系统通过 `QuestionAiGenerateService.callAiSync()` 同步调用所选模型生成题目

模型选择优先级：`aiPropertiesId`（精确 ID）> `aiType`（按类型查默认）> 系统默认 DeepSeek。前端传递 `aiPropertiesId`（模型记录的数据库 ID），后端直接 `getById()` 查找。

## 教师后台 — AI 模型配置页面

教师和管理员可通过 `/teacher/ai-config` 页面管理 AI 模型，无需直接操作数据库。

### 功能

- **分页浏览**：支持按名称、类型筛选
- **新增/编辑**：填写 AI 名称、类型、API Key、API 地址、模型名称、Token 数、温度、系统提示词
- **启用/禁用**：开关切换 `is_enabled` 状态
- **删除**：软删除（`del_flag=1`）
- **设为默认**：点击星标按钮，将该模型设为其 `ai_type` 下的默认模型（`is_default=1`），同一 `ai_type` 下其他记录自动取消默认

### API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ip/v1/ai-properties` | 分页查询（支持 `aiName`、`aiType`、`isEnabled` 筛选） |
| GET | `/api/ip/v1/ai-properties/enabled` | 获取所有启用的模型（用于下拉选择器） |
| GET | `/api/ip/v1/ai-properties/{id}` | 获取单个配置详情 |
| POST | `/api/ip/v1/ai-properties` | 新增配置 |
| PUT | `/api/ip/v1/ai-properties` | 更新配置 |
| DELETE | `/api/ip/v1/ai-properties/{id}` | 删除配置 |
| PUT | `/api/ip/v1/ai-properties/{id}/status?isEnabled=0\|1` | 启用/禁用 |
| PUT | `/api/ip/v1/ai-properties/{id}/default` | 设为默认模型 |

所有端点需要 `teacher` 或 `admin` 角色（`@SaCheckRole(value = {"teacher", "admin"}, mode = SaMode.OR)`）。

## 技术细节

### MimoChatHandler 工作原理

```
请求流程：
1. 前端发送 POST /api/ip/v1/ai/sessions/{sessionId}/chat
2. AiMessageServiceImpl 解析 aiId → 加载 AiPropertiesDO (aiType=mimo)
3. AiChatHandlerFactory.getHandler("mimo") → 返回 MimoChatHandler
4. MimoChatHandler.streamToSink():
   a. 构建 Anthropic Messages API 请求体
   b. WebClient POST 到 {apiUrl}/messages
   c. 请求头: x-api-key + anthropic-version: 2023-06-01
   d. 解析 SSE 事件流 (content_block_delta → text_delta / thinking_delta)
   e. 包装为 AiChatStreamRespDTO 推送到 FluxSink
5. 前端收到 SSE 数据，渲染文本和思考过程
```

### Anthropic SSE 事件格式

Mimo 使用 Anthropic Messages API 的流式输出格式：

```
event: message_start
data: {"type":"message_start","message":{"id":"msg_xxx","role":"assistant","model":"mimo-v2.5-pro"}}

event: content_block_start
data: {"type":"content_block_start","index":0,"content_block":{"type":"thinking","thinking":""}}

event: content_block_delta
data: {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"让我分析..."}}

event: content_block_stop
data: {"type":"content_block_stop","index":0}

event: content_block_start
data: {"type":"content_block_start","index":1,"content_block":{"type":"text","text":""}}

event: content_block_delta
data: {"type":"content_block_delta","index":1,"delta":{"type":"text_delta","text":"根据您的问题..."}}

event: content_block_stop
data: {"type":"content_block_stop","index":1}

event: message_delta
data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"input_tokens":100,"output_tokens":500}}

event: message_stop
data: {"type":"message_stop"}
```

### 前端兼容性

Mimo 的 SSE 数据会被转换为与现有前端兼容的格式：

| Anthropic 事件 | 转换后的 DTO | 前端处理 |
|---------------|-------------|---------|
| `thinking_delta` | `{"type":"reasoning_content","content":"..."}` | 思维链展示 |
| `text_delta` | `{"type":"content","content":"..."}` | 正常文本展示 |

前端无需任何改动，通过 `ai_properties/options` API 自动发现新模型。

### thinking 模式

对于 `mimo-v2.5-pro` 和 `mimo-v2-pro` 模型，代码会自动启用 thinking 扩展思维：

```json
{
  "model": "mimo-v2.5-pro",
  "thinking": {
    "type": "enabled"
  },
  "messages": [...]
}
```

thinking 输出会以 `reasoning_content` 类型推送到前端，与 DeepSeek 的深度思考展示一致。

## 添加新的 AI 提供方

如果需要接入新的 AI 模型，根据其协议类型选择不同方案：

### 方案 A：OpenAI 兼容协议

大多数模型（如通义千问、文心一言、Claude via OpenAI-compatible proxy）都提供 OpenAI 兼容接口：

1. 在 `AiPropritiesType` 枚举中新增值
2. 在 `ai_properties` 表插入配置
3. 无需代码改动，`UniversalAiChatHandler` 自动处理

### 方案 B：非 OpenAI 协议（如 Anthropic 原生、自定义协议）

1. 在 `AiPropritiesType` 枚举中新增值
2. 创建新的 `XxxChatHandler implements AiChatHandler`
3. 在 `AiChatHandlerFactory` 中添加路由逻辑
4. 实现 `streamToSink()` 和 `callSync()` 方法

参考 `MimoChatHandler.java` 的实现。

## 常见问题

### Q: Mimo 调用返回 401

检查 `api_key` 是否正确，格式应为 `tp-` 开头的字符串。确认 `api_url` 不含 `/messages` 后缀。

### Q: Mimo 调用返回 404

确认 `api_url` 正确：
- Token 计划：`https://token-plan-sgp.xiaomimimo.com/anthropic`
- 官方：`https://api.xiaomimimo.com/anthropic/v1`

代码会自动拼接 `/messages`，所以 `api_url` 末尾不要加 `/messages`。

### Q: thinking 内容不展示

确认前端 SSE 消费逻辑支持 `reasoning_content` 类型。InterviewPilot 前端已内置支持。

### Q: 如何切换默认模型

`ai_properties` 表有 `is_default` 字段（`TINYINT(1)`）。同一 `ai_type` 下 `is_default=1` 的记录优先被选中。教师后台 `/teacher/ai-config` 页面可点击星标按钮设置默认模型。后端 `getEnabledByAiType()` 按 `is_default DESC, create_time DESC` 排序，取第一条。

### Q: 模型选择器不显示 Mimo

检查 `ai_properties` 表中 Mimo 记录的 `is_enabled = 1` 且 `del_flag = 0`。前端通过 `GET /api/ip/v1/ai-properties/options` 拉取选项列表。

## 数据库表结构参考

```sql
CREATE TABLE ai_properties (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ai_name       VARCHAR(100)  COMMENT '显示名称',
    ai_type       VARCHAR(50)   COMMENT '类型标识: openai/doubao/spark/deepseek/mimo',
    api_key       VARCHAR(500)  COMMENT 'API 密钥',
    api_secret    VARCHAR(500)  COMMENT 'API 密钥（部分提供方需要）',
    api_url       VARCHAR(500)  COMMENT 'API 地址（留空使用枚举默认值）',
    project_id    VARCHAR(200)  COMMENT '项目 ID（OpenAI 需要）',
    organization_id VARCHAR(200) COMMENT '组织 ID（OpenAI 需要）',
    model_name    VARCHAR(100)  COMMENT '模型名称',
    max_tokens    INT           COMMENT '最大输出 token',
    temperature   DECIMAL(3,1)  COMMENT '采样温度',
    system_prompt TEXT          COMMENT '系统提示词',
    is_enabled    TINYINT DEFAULT 1 COMMENT '启用状态',
    is_default    TINYINT DEFAULT 0 COMMENT '是否为该 ai_type 的默认模型',
    del_flag      TINYINT DEFAULT 0 COMMENT '软删除',
    create_time   DATETIME,
    update_time   DATETIME
) COMMENT 'AI 模型配置表';
```
