# AI 模型配置与使用指南

本文描述 InterviewPilot 当前的 AI 接入方式。项目默认使用小米 Mimo Token Plan，覆盖通用聊天、面试链路、ASR 语音识别和 TTS 语音合成。

## 默认模型

| 用途 | 协议 | 默认端点 | 默认模型 |
| --- | --- | --- | --- |
| 通用聊天 / 面试链路 | OpenAI 兼容 | `https://token-plan-cn.xiaomimimo.com/v1` | `mimo-v2.5` |
| 高推理聊天 | Anthropic 兼容 | `https://token-plan-cn.xiaomimimo.com/anthropic` | `mimo-v2.5-pro` |
| 语音识别 ASR | OpenAI 兼容 chat completions | `https://token-plan-cn.xiaomimimo.com/v1` | `mimo-v2.5-asr` |
| 语音合成 TTS | OpenAI 兼容 chat completions | `https://token-plan-cn.xiaomimimo.com/v1` | `mimo-v2.5-tts` |

可用 Mimo 模型包括：`mimo-v2.5-pro`、`mimo-v2.5`、`mimo-v2.5-asr`、`mimo-v2.5-tts-voiceclone`、`mimo-v2.5-tts-voicedesign`、`mimo-v2.5-tts`、`mimo-v2-pro`、`mimo-v2-omni`、`mimo-v2-tts`。

## 环境变量

生产或本地运行时在 `.env` / shell 环境中配置：

```env
MIMO_API_KEY=tp-your-token-plan-api-key
MIMO_OPENAI_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
MIMO_ANTHROPIC_BASE_URL=https://token-plan-cn.xiaomimimo.com/anthropic
MIMO_CHAT_MODEL=mimo-v2.5
MIMO_PRO_MODEL=mimo-v2.5-pro
MIMO_ASR_MODEL=mimo-v2.5-asr
MIMO_TTS_MODEL=mimo-v2.5-tts

SPRING_AI_OPENAI_API_KEY=tp-your-token-plan-api-key
SPRING_AI_OPENAI_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
SPRING_AI_OPENAI_MODEL=mimo-v2.5
SPRING_AI_OPENAI_EMBEDDING_MODEL=mimo-v2.5
```

不要把真实 API Key 提交到 Git。`XUNFEI_*` 变量仅供 legacy 代码路径使用，默认不需要配置；只有显式设置 `LEGACY_XUNFEI_ENABLED=true` 时才会启用旧讯飞 Bean。

## 后端路由

通用 AI 聊天：

```text
Frontend model selector
  -> AiMessageController (SSE)
  -> AiChatHandlerFactory
  -> ai_properties
```

面试链路：

```text
InterviewSessionFacade
  -> InterviewAiInvoker.doChat()
  -> agent_properties scene binding
```

当前推荐两种 provider：

| Provider | 字段值 | Handler | 说明 |
| --- | --- | --- | --- |
| Mimo OpenAI 兼容 | `openai` | `UniversalAiChatHandler` | 面试默认链路，使用 `/v1/chat/completions` |
| Mimo Anthropic 兼容 | `anthropic` | `AnthropicChatHandler` | 通用聊天高推理链路，支持 thinking |

`xingchen` / 讯飞工作流仍作为 legacy 兼容保留，但默认初始化 SQL 和前端配置已经切换到 Mimo。

## 数据库配置

`ai_properties` 用于通用聊天和教师后台 AI 题目生成。默认初始化记录位于 `AI-Meeting/admin/src/main/resources/sql/ai_properties.sql`，使用占位符 `MIMO_API_KEY`，部署时请在数据库或环境变量中替换为真实 key。

示例：

```sql
INSERT INTO ai_properties (
    ai_name, ai_type, api_key, api_url, model_name,
    max_tokens, temperature, is_enabled, is_default, del_flag, create_time, update_time
) VALUES (
    'Mimo V2.5',
    'openai',
    'MIMO_API_KEY',
    'https://token-plan-cn.xiaomimimo.com/v1',
    'mimo-v2.5',
    32768,
    0.7,
    1,
    1,
    0,
    NOW(),
    NOW()
);
```

`agent_properties` 用于面试各场景绑定。OpenAI 兼容 provider 的字段约定：

| 字段 | 用途 |
| --- | --- |
| `api_key` | Mimo API Key |
| `api_secret` | 模型名，例如 `mimo-v2.5` |
| `api_flow_id` | OpenAI 兼容基础地址，例如 `https://token-plan-cn.xiaomimimo.com/v1` |
| `ai_provider` | 固定为 `openai` |
| `scene_code` | 面试业务场景编码 |
| `is_active` | 当前场景是否启用该 agent |

## ASR / TTS

前端实时 ASR WebSocket：

```text
/api/ip/v1/mimo/audio-to-text/{userId}
```

前端会发送 PCM 音频块，后端在 `MimoAudioService` 中封装为 WAV 后调用 Mimo ASR 模型。

TTS REST 接口：

```text
POST /api/ip/v1/mimo/tts/tasks
GET  /api/ip/v1/mimo/tts/tasks/{taskId}
POST /api/ip/v1/mimo/tts/synthesize
```

`/api/ip/v1/xunfei/tts/**` 暂时作为旧前端兼容别名保留，内部已经走 Mimo TTS。

## 前端配置页面

教师后台 `/teacher/ai-config` 只展示 `openai` 和 `anthropic` 两类 Mimo 兼容模型。管理员后台 `/admin/agent-config` 可为出题、评分、追问、神态分析等场景切换 active agent。

## 常见问题

### 401 Unauthorized

检查 `MIMO_API_KEY` 或数据库里的 `api_key` 是否为有效 `tp-` 开头 key。不要把 OpenAI `sk-` key 填到 Mimo 端点。

### 404 Not Found

检查端点是否使用中国区 Token Plan 地址：

```text
https://token-plan-cn.xiaomimimo.com/v1
https://token-plan-cn.xiaomimimo.com/anthropic
```

OpenAI 兼容地址保留 `/v1`，代码会自动拼接 `/chat/completions`。Anthropic 兼容地址不要手动追加 `/messages`，代码会自动拼接。

### ASR / TTS 没有响应

检查 `MIMO_ASR_MODEL=mimo-v2.5-asr`、`MIMO_TTS_MODEL=mimo-v2.5-tts`，并确认后端日志里没有 `Mimo API key is missing`。

### 模型选择器不显示 Mimo

检查 `ai_properties` 表中记录满足 `is_enabled = 1` 且 `del_flag = 0`。前端通过 `GET /api/ip/v1/ai-properties/options` 拉取模型列表。
