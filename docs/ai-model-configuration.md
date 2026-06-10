# AI 模型配置与使用指南

本文描述 InterviewPilot 当前的 AI 接入方式。项目默认使用小米 Mimo Token Plan，覆盖通用聊天、面试链路、ASR 语音识别和 TTS 语音合成。

## 默认模型

| 用途 | 协议 | 默认端点 | 默认模型 |
| --- | --- | --- | --- |
| 通用聊天 / 面试链路 / 视觉链路 | OpenAI 兼容 | `https://token-plan-cn.xiaomimimo.com/v1` | `mimo-v2.5` |
| 高推理纯文本链路 | OpenAI 兼容 | `https://token-plan-cn.xiaomimimo.com/v1` | `mimo-v2.5-pro` |
| 语音识别 ASR | OpenAI 兼容 chat completions | `https://token-plan-cn.xiaomimimo.com/v1` | `mimo-v2.5-asr` |
| 语音合成 TTS | OpenAI 兼容 chat completions | `https://token-plan-cn.xiaomimimo.com/v1` | `mimo-v2.5-tts` |

可用 Mimo 模型包括：`mimo-v2.5-pro`、`mimo-v2.5`、`mimo-v2.5-asr`、`mimo-v2.5-tts-voiceclone`、`mimo-v2.5-tts-voicedesign`、`mimo-v2.5-tts`、`mimo-v2-pro`、`mimo-v2-omni`、`mimo-v2-tts`。

## 环境变量

生产或本地运行时在 `.env` / shell 环境中配置。后端本地启动时会自动加载 `AI-Meeting/.env`，从 `AI-Meeting/admin` 启动时也会继续向上查找父目录中的 `.env`；shell 中显式设置的环境变量优先级更高：

```env
MIMO_API_KEY=tp-your-token-plan-api-key
MIMO_OPENAI_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
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

当前推荐的 provider：

| Provider | 字段值 | Handler | 说明 |
| --- | --- | --- | --- |
| Mimo OpenAI 兼容 | `openai` | `UniversalAiChatHandler` | 聊天、面试、ASR、TTS 默认链路，使用 `/v1/chat/completions` |

`mimo-v2.5` 可用于通用聊天和需要视觉能力的链路；`mimo-v2.5-pro` 只用于答案评分、面试追问/提问、通用智能体等纯文本高推理链路，不要配置到图片/神态分析等视觉场景。`anthropic` handler 和 `xingchen` / 讯飞工作流仍作为 legacy 兼容保留，但默认初始化 SQL 和前端配置已经切换到 Mimo OpenAI 兼容路径。

## 面试题输出格式

面试题链路的理想返回值是纯题目文本，例如：

```text
请具体描述一下 TF-IDF 与语义匹配在你的系统中如何协同工作？
```

实际模型偶尔会返回 Java Map 风格包装：

```text
{question=请具体描述一下 TF-IDF 与语义匹配在你的系统中如何协同工作？}
```

也可能返回更完整的 Java Map 风格字符串：
```text
{id=1, topic=文本风控, question=请具体描述一下 TF-IDF 与语义匹配在你的系统中如何协同工作？, purpose=考察候选人对多源文本校验链路的理解}
```

前端会在 `normalizeInterviewQuestionText()` 中优先提取 `question=...` 字段，把这类包装统一清理成纯题目文本，再写入当前题目状态、聊天消息和 TTS 文本。修改面试题解析、同步下一题或消息流时，不要绕过该归一化步骤。

## 数据库配置

`ai_properties` 用于通用聊天和教师后台 AI 题目生成。默认初始化记录位于 `AI-Meeting/admin/src/main/resources/sql/ai_properties.sql`，使用占位符 `MIMO_API_KEY`。推荐保持数据库占位符不变，真实 key 只通过后端启动环境变量提供。

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
| `api_key` | `MIMO_API_KEY` 占位符，运行时从环境变量解析 |
| `api_secret` | 模型名，例如 `mimo-v2.5`；视觉相关场景不要使用 `mimo-v2.5-pro` |
| `api_flow_id` | OpenAI 兼容基础地址，例如 `https://token-plan-cn.xiaomimimo.com/v1` |
| `ai_provider` | 固定为 `openai` |
| `scene_code` | 面试业务场景编码 |
| `is_active` | 当前场景是否启用该 agent |

## ASR / TTS

前端实时 ASR WebSocket：

```text
/api/ip/v1/mimo/audio-to-text/{userId}
```

前端会持续发送 PCM 音频块。后端在 `AudioTranscriptionWebSocketHandler` 中缓冲音频流，收到 `stop_transcription` 或连接清理时关闭 pipe，并由 `MimoAudioService` 封装为 WAV 后调用 Mimo ASR。Mimo ASR 返回最终文本，不再提供讯飞式 `pgs` / `rg` 逐句增量修订流。

TTS REST 接口：

```text
POST /api/ip/v1/mimo/tts/tasks
GET  /api/ip/v1/mimo/tts/tasks/{taskId}
POST /api/ip/v1/mimo/tts/synthesize
```

Mimo TTS 是同步合成接口。`POST /tasks` 和 `GET /tasks/{taskId}` 保留 task 形态只是为了兼容旧前端；新代码应优先使用 `/synthesize`，并直接读取 `audioBase64`。`/api/ip/v1/xunfei/tts/**` 暂时作为旧前端兼容别名保留，内部已经走 Mimo TTS。

按当前 Mimo TTS v2.5 协议约束，真正要播报的文本必须放在 `assistant` 角色消息中；`user` 角色更适合放风格说明或控制指令。如果后端日志出现 `messages must contain an assistant role for TTS model`，优先检查 `MimoAudioService.buildTtsRequestBody()` 生成的消息数组结构。

## 前端配置页面

教师后台 `/teacher/ai-config` 默认只新建 `openai` 类型的 Mimo 兼容模型。管理员后台 `/admin/agent-config` 可切换面试场景 active agent：

- `Mimo 2.5 面试出题官`：固定使用 `mimo-v2.5`。简历出题链路是文本优先，但扫描版 PDF 会渲染成图片走视觉 OCR 兜底，因此不能使用 `mimo-v2.5-pro`。
- `Mimo 2.5 神态分析官`：固定使用 `mimo-v2.5`。神态分析依赖摄像头画面，不能使用不支持视觉的 `mimo-v2.5-pro`。
- `Mimo 2.5 答案评分官` / `Mimo 2.5 Pro 答案评分官`：纯文本评分链路，可以在 2.5 和 2.5 Pro 之间切换。
- `Mimo 2.5 面试提问官` / `Mimo 2.5 Pro 面试提问官`：纯文本追问/提问链路，可以在 2.5 和 2.5 Pro 之间切换。
- `Mimo 2.5 通用智能体` / `Mimo 2.5 Pro 通用智能体`：纯文本通用智能体链路，可以在 2.5 和 2.5 Pro 之间切换。

## 常见问题

### 401 Unauthorized

检查启动后端的终端是否设置了 `MIMO_API_KEY` / `SPRING_AI_OPENAI_API_KEY`，以及数据库里的 `api_key` 是否保持为 `MIMO_API_KEY` 占位符。不要把 OpenAI `sk-` key 填到 Mimo 端点。

### 404 Not Found

检查端点是否使用中国区 Token Plan OpenAI 兼容地址：

```text
https://token-plan-cn.xiaomimimo.com/v1
```

OpenAI 兼容地址保留 `/v1`，代码会自动拼接 `/chat/completions`。`mimo-v2.5-pro` 也走这个地址；不要把 Pro 配成 `/anthropic`，否则聊天会返回 404 或 500。

### ASR / TTS 没有响应

检查 `MIMO_ASR_MODEL=mimo-v2.5-asr`、`MIMO_TTS_MODEL=mimo-v2.5-tts`，确认后端日志里没有 `Mimo API key is missing`。ASR 还要确认前端发送了 `stop_transcription`，否则后端不会关闭音频流并发起最终转写。TTS 如果返回 400 且提示 `messages must contain an assistant role for TTS model`，说明播报文本没有放在 `assistant` 角色消息中。

### 模型选择器不显示 Mimo

检查 `ai_properties` 表中记录满足 `is_enabled = 1` 且 `del_flag = 0`。前端通过 `GET /api/ip/v1/ai-properties/options` 拉取模型列表。
## Report generation behavior

- Interview question generation still defaults to `mimo-v2.5` because scanned-resume OCR can require vision.
- Follow-up questions, answer scoring, and normal text agent chat default to `mimo-v2.5`, but may be switched to `mimo-v2.5-pro` when higher pure-text reasoning is needed.
- `mimo-v2.5-pro` should remain reserved for pure-text high-reasoning paths. Do not bind it to visual flows such as resume-image OCR or demeanor/image analysis.
- Resume-based interview question generation is also a possible vision path: normal PDFs use extracted text, but scanned PDFs are rendered to PNG pages and read through the Mimo vision OCR fallback before question generation. Keep the interview-question extraction agent on `mimo-v2.5`.
- Final report persistence no longer waits for a synchronous AI review-summary call. The first saved report snapshot uses fast rule-based review content so the report page can open earlier.
- Manual reference-answer generation still uses AI, but it is no longer part of the first report-load critical path.
- Manual interview-conclusion generation now follows the same pattern: the first report load shows rule-based `reviewFeedback`, and the report page exposes a separate `生成 AI 结论` action for the slower AI summary path.

## Delayed asset behavior

- Report fetch now treats timeout/finalize-processing states as transient and continues polling before surfacing a hard error.
- Recording playback can appear after the base report because the saved `recordingUrl` may arrive later than the main interview record.
- The report-page `生成参考答案` action uses a longer client timeout and then polls the saved report if the backend finishes after the original HTTP request times out on the client side.
- The report-page `生成 AI 结论` action also uses a long timeout. If the backend finishes after the client-side timeout, the frontend keeps polling the saved report and swaps the current rule-based summary to the AI-generated conclusion in place.
