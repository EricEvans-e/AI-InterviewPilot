# CLAUDE.md

This file gives coding agents the current project map and operating constraints for this workspace.

## Project Overview

InterviewPilot is an AI-powered mock interview platform. Users upload a resume, the system generates interview questions, users answer by voice or text, AI agents score answers, ask follow-ups, analyze demeanor, and produce a final report. The app also includes a general multi-model AI chat.

This workspace contains two sibling repositories:

- `AI-Meeting/` - Java Spring Boot backend
- `AI-Meeting-Frontend/` - React + TypeScript frontend

## Commands

Backend:

```bash
cd AI-Meeting
docker-compose up -d
./mvnw -B -ntp clean verify -Dmaven.test.skip=true
./mvnw test
./mvnw spring-boot:run -pl admin
```

Backend runs on port `8002`. Main config: `AI-Meeting/admin/src/main/resources/application.yaml`.

Frontend:

```bash
cd AI-Meeting-Frontend
npm ci
npm run dev
npm run check
npm run build
npm run test
```

Node 20+ is required. Vite dev server defaults to `5173`.

## Architecture

Root backend package: `com.interviewpilot`. The single Maven module `admin/` is a modular monolith organized by domain.

| Domain | Package | Responsibility |
| --- | --- | --- |
| `interview` | `com.interviewpilot.interview` | Interview flow, state machine, resume parsing, question generation, answer evaluation, follow-up decisions, demeanor scoring, final report |
| `ai` | `com.interviewpilot.ai` | General AI chat. Defaults to Xiaomi Mimo through OpenAI-compatible and Anthropic-compatible protocols |
| `agent` | `com.interviewpilot.agent` | Interview agent configuration and scene binding; legacy Xunfei workflow client remains for compatibility |
| `media` | `com.interviewpilot.media` | Mimo-backed ASR WebSocket transport and synchronous TTS REST APIs |
| `user` | `com.interviewpilot.user` | User accounts and admin permissions |
| `auth` | `com.interviewpilot.auth` | Sa-Token auth, WebSocket auth, permission checks |
| `common` | `com.interviewpilot.common` | Shared infra: DB, Redis, thread pools, result and exception conventions |

Data stores:

- MySQL `mainshi_agent`: structured business data, users, permissions, AI configs, interview records
- MongoDB `interview_pilot`: conversations, messages, interview sessions, runtime snapshots
- Redis: Sa-Token sessions, distributed locks, SingleFlight, BloomFilter, interview flow state cache

## Mimo AI Runtime

The project is Mimo-first. Do not treat Xunfei as the default provider.

Default endpoints:

- OpenAI-compatible: `https://token-plan-cn.xiaomimimo.com/v1`
- Anthropic-compatible Mimo endpoint exists, but this project should prefer the OpenAI-compatible endpoint by default.

Default models:

- Chat and interview: `mimo-v2.5`
- Pro/thinking chat: `mimo-v2.5-pro`
- ASR: `mimo-v2.5-asr`
- TTS: `mimo-v2.5-tts`

Environment variables:

```env
MIMO_API_KEY=tp-your-token-plan-api-key
SPRING_AI_OPENAI_API_KEY=tp-your-token-plan-api-key
SPRING_AI_OPENAI_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
SPRING_AI_OPENAI_MODEL=mimo-v2.5
MIMO_OPENAI_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
```

Never commit a real API key.

Runtime components:

- `UniversalAiChatHandler`: OpenAI-compatible calls, default Mimo `/v1/chat/completions`
- `AnthropicChatHandler`: legacy-compatible Anthropic-style calls. Keep it available, but do not make it the default Mimo route.
- `MimoAudioService`: ASR and TTS via Mimo chat-completions models. ASR receives buffered WebSocket audio and returns a final transcript; TTS returns `choices[0].message.audio.data`
- `InterviewAiInvoker`: interview-specific routing through `agent_properties.ai_provider`

Legacy Xunfei classes remain for compatibility only. Beans are disabled by default using `LEGACY_XUNFEI_ENABLED=false`.

## Agent Scene Binding

Admins configure interview scenario providers in `/admin/agent-config`.

`agent_properties` has:

- `scene_code`
- `is_active`
- `ai_provider` (`openai`, `anthropic`, or legacy `xingchen`)

`BusinessAgentResolver` first checks active DB scene bindings, then falls back to YAML `interview-pilot.agent-binding`.

Default seeded agents use:

- provider: `openai`
- model: `mimo-v2.5`
- base URL: `https://token-plan-cn.xiaomimimo.com/v1`

## Frontend API Contract

All APIs are prefixed with `/api/ip/v1/`.

| Frontend service | Backend controller | Protocol |
| --- | --- | --- |
| `authService` | `UserController` | REST |
| `aiService` | `AiMessageController` | SSE |
| `agentService` | `AgentController` | SSE |
| `interviewService` | `InterviewSessionController` + `InterviewRecordController` | REST |
| `mimoTtsService` | `XunfeiTtsController` | REST |
| `xunfeiTtsService` | legacy alias re-exporting `mimoTtsService` | REST |
| `AudioToTextWebSocket` | `AudioTranscriptionWebSocketHandler` | WebSocket |

Current ASR WebSocket path:

```text
/api/ip/v1/mimo/audio-to-text/{userId}
```

The WebSocket is realtime transport for browser PCM chunks. The backend buffers audio, wraps PCM as WAV, and calls Mimo ASR after `stop_transcription` or connection cleanup; do not expect Xunfei-style per-sentence incremental `pgs` / `rg` revisions.

Current TTS paths:

```text
/api/ip/v1/mimo/tts/tasks
/api/ip/v1/mimo/tts/tasks/{taskId}
/api/ip/v1/mimo/tts/synthesize
```

`/api/ip/v1/xunfei/tts/**` remains a backend compatibility alias, but new frontend code should use Mimo paths.

Mimo TTS is synchronous. `/tasks` and `/tasks/{taskId}` keep the old task-shaped API surface for compatibility; new code should prefer `/synthesize` and read `audioBase64`.

## Frontend Interview UI Notes

- Model question output can arrive as `{question=...}` or richer Java map-style text such as `{id=1, topic=..., question=..., purpose=...}`. Keep `normalizeInterviewQuestionText()` in the session flow before updating current question state, chat messages, or TTS text.
- Auto-play assistant question messages render as a distinct "当前题目" / follow-up card in `ChatBubble`; do not classify feedback, system, or progress messages as interview questions.
- `InterviewCameraOverlay` is mounted inside `ChatRoom`'s content overlay. Compact overlay coordinates must be calculated against the parent chat content container, not `window.innerWidth`, because the app shell has a sidebar and the content parent uses `overflow-hidden`.
- The compact camera overlay is draggable and clamped inside the chat content area. Expanded mode keeps the old full overlay layout and should not be draggable.

## Report Runtime Notes

- Base interview report persistence must stay fast. Do not reintroduce synchronous AI review-summary generation into the first report-save path.
- Report-page timeout and `finalize is processing` states are treated as transient by the frontend and should remain poll-friendly.
- `recordingUrl` can arrive after the first interview record payload. Frontend report code intentionally polls for delayed recording availability.
- Reference answers are manual on the report page. Slow AI generation is allowed there, but it should not block the initial report screen.

## Interview Constraints

- Session state and question state are separate state machines; do not merge them.
- `questionNumber` can refer to a main question or a follow-up, not a database primary key.
- `requestId` is the idempotency boundary for answer submission and must remain stable across retries.
- Answer submission cannot skip idempotency checks, question number validation, per-question locking, or score compensation.
- Follow-up decisions depend on AI output, LiteFlow rules, and max follow-up count.
- Session restore and finalization are first-class business contracts.
- Legacy workflow YAML output fields must match Java-side parsing exactly when `LEGACY_XUNFEI_ENABLED=true`.

## Auth And Roles

Single source of truth: `t_user.role` (`student`, `teacher`, `admin`).

- `StpInterfaceImpl` reads `t_user.role`.
- Login endpoints return `role`; frontend `RoleGuard` uses it.
- Legacy `admin_permission` is deprecated.
- Admin panel `/admin` requires `role=admin`.
- Default admin account in `t_user.sql`: username `admin`, password `admin`.

## Domain Skills

`AI-Meeting/skills/` contains domain knowledge for coding agents:

- `interview-pilot-repo-map`
- `interview-pilot-interview-domain`
- `interview-pilot-agent-domain`
- `interview-pilot-ai-runtime`
- `interview-pilot-media-domain`
- `interview-pilot-auth-user`
- `interview-pilot-debug-playbook`
- `interview-pilot-change-playbook`

Use the relevant skill when working on that domain.
