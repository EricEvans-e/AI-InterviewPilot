# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

"InterviewPilot" (InterviewPilot) / "InterviewPilot" — an AI-powered mock interview platform. Users upload a resume, an AI generates interview questions, the user answers via voice or text, and multiple AI agents score answers, ask follow-ups, analyze demeanor, and produce a final report. Also includes a general-purpose multi-model AI chat.

This workspace contains two separate repositories side by side:
- `AI-Meeting/` — Java Spring Boot backend
- `AI-Meeting-Frontend/` — React + TypeScript frontend

## Build & Development Commands

### Backend (`AI-Meeting/`)

```bash
cd AI-Meeting

# Start infrastructure (MySQL, MongoDB, Redis)
docker-compose up -d

# Build (skip tests, as CI does)
./mvnw -B -ntp clean verify -Dmaven.test.skip=true

# Run tests
./mvnw test

# Run the application (requires docker-compose services running)
./mvnw spring-boot:run -pl admin
```

Backend runs on port **8002**. Config in `admin/src/main/resources/application.yaml`.

### Frontend (`AI-Meeting-Frontend/`)

```bash
cd AI-Meeting-Frontend

npm ci

# Dev server (proxies /api to localhost:8002)
npm run dev

# Lint + typecheck + test (the "quality gate")
npm run check

# Production build
npm run build

# Run tests only
npm run test
```

Node >= 20 required. Vite dev server defaults to port 5173.

## Architecture

### Backend — DDD-style Modular Monolith

Root package: `com.interviewpilot`. Single Maven module `admin/` containing all business code organized by **domain**:

| Domain | Package | Responsibility |
|--------|---------|----------------|
| **interview** | `interviewpilot.interview` | Core interview flow: state machine, resume parsing, question generation, answer evaluation, follow-up decisions, demeanor analysis, scoring |
| **ai** | `interviewpilot.ai` | General multi-model AI chat (OpenAI/DeepSeek/Doubao/Spark via Spring AI) |
| **agent** | `interviewpilot.agent` | 讯飞星辰 Workflow Agent integration |
| **media** | `interviewpilot.media` | Real-time ASR (WebSocket), long-text TTS |
| **user** | `interviewpilot.user` | User accounts, admin permissions |
| **auth** | `interviewpilot.auth` | Sa-Token auth, WebSocket auth, permission checks |
| **common** | `interviewpilot.common` | Shared infra: DB config, Redis config, thread pools, conventions (Result/Exception/CurrentUser) |

Each domain follows internal layering: `api/` (controllers) → `service/` → `dao/` (MyBatis-Plus mappers for MySQL, Spring Data repos for MongoDB) → `entity/`.

### Three-Database Architecture

- **MySQL** (`mainshi_agent`) — structured data: users, permissions, agent/AI model configs, interview records
- **MongoDB** (`interview_pilot`) — document data: conversations, messages, interview sessions, questions, runtime snapshots (hot/cold split)
- **Redis** — Sa-Token session store, distributed locks, SingleFlight dedup, BloomFilter, interview flow state cache

### AI Integration — Three Engines

1. **Spring AI** (`UniversalAiChatHandler`) — general chat for OpenAI-compatible providers. Dynamically creates `ChatClient` per `aiType` (DeepSeek uses `DeepSeekChatModel`, others use `OpenAiChatModel`). Supports streaming with `reasoning_content` extraction.
2. **Anthropic** (`AnthropicChatHandler`) — Anthropic Messages API protocol. Separate handler because request/response format differs from OpenAI (SSE event chain: `message_start` → `content_block_delta` → `message_stop`). Supports `thinking` (extended reasoning) for `mimo-v2.5-pro`/`v2-pro` models. Auth via `x-api-key` header.
3. **讯飞星辰 Workflow Agents** — interview-specific. 5 agent scenarios defined in `BusinessAgentScene`: question extraction, answer evaluation, demeanor analysis, question asking, general agent chat. Config stored in `agent_properties` table. Also supports **Anthropic** as alternative provider via `ai_provider` field — `InterviewAiInvoker.doChat()` routes to `AnthropicChatHandler.callSync()` when `ai_provider=anthropic`, using `AnthropicPromptBuilder` to convert structured parameters to text prompts.

**AI Handler routing**: `AiChatHandlerFactory` dispatches by `aiType` string. `anthropic` → `AnthropicChatHandler`; all other supported types → `UniversalAiChatHandler`. Both implement `AiChatHandler` interface (`streamToSink` + `callSync`). Model config stored in `ai_properties` table.

> 详细的模型配置与使用说明见 [`docs/ai-model-configuration.md`](docs/ai-model-configuration.md)。

### Interview Flow State Machine

```
INIT → ASKING → EVALUATING → FOLLOW_UP → ASKING → ... → COMPLETED
```

Key components:
- `InterviewFlowStateMachine` — state transitions
- `InterviewSessionFacade` — entry point for all interview operations
- `InterviewAnswerPipeline` — answer submission with idempotency, locking, scoring
- LiteFlow rule chain (`interview-followup-chain.xml`) — follow-up decision logic
- Resilience4j guards — circuit breaking, rate limiting
- Distributed SingleFlight — prevents duplicate AI calls across instances

### Frontend — React 19 + TypeScript

Key architectural patterns:
- **State management**: Redux Toolkit (user auth + chat runtime state) + React Query (server data caching)
- **API layer**: Custom `HttpClient` wrapping Axios with auto-auth, request deduplication, debounce, error mapping
- **Streaming**: `@microsoft/fetch-event-source` for POST-based SSE (AI chat), native WebSocket for ASR
- **Component structure**: Business components by domain (`chat/`, `interview/`, `audio/`, `camera/`), shadcn/ui primitives in `ui/`
- **Hooks pattern**: Controller hooks (`useChatPageController`, `useInterviewPageController`) extract all business logic from components

### Frontend-Backend API Contract

All APIs prefixed: `/api/ip/v1/`

| Frontend Service | Backend Controller | Protocol |
|-----------------|-------------------|----------|
| `authService` | `UserController` | REST (login returns `{ token, username, role }`) |
| `aiService` | `AiMessageController` | SSE (POST) |
| `agentService` | `AgentController` | SSE (POST) |
| `interviewService` | `InterviewSessionController` | REST |
| `xunfeiTtsService` | `XunfeiTtsController` | REST |
| `teacherService` | `AiPropertiesController` + `QuestionController` | REST (AI config CRUD, question CRUD, AI generate) |
| `AudioToTextWebSocket` | `AudioTranscriptionWebSocketHandler` | WebSocket |

### Frontend Route Structure

Defined in `src/lib/constants.ts` as `ROUTES`. All routes are children of `/` (AppLayout).

| Path | Page | Required Role |
|------|------|---------------|
| `/` | MarketingHomePage | public |
| `/auth` | AuthPage (login/register) | public |
| `/lobby` | LobbyPage | any authenticated |
| `/interview/*` | Interview flow pages | any authenticated |
| `/chat/:sessionId?` | AI Chat | any authenticated |
| `/profile` | StudentProfilePage | any authenticated |
| `/teacher` | TeacherDashboard | teacher, admin |
| `/teacher/questions` | QuestionBank management | teacher, admin |
| `/teacher/records` | Interview records & reports | teacher, admin |
| `/teacher/colleges` | College management | teacher, admin |
| `/teacher/ai-config` | AI model configuration (CRUD, enable/disable, set default) | teacher, admin |
| `/admin` | AdminDashboard (stats) | admin |
| `/admin/users` | User management | admin |

> **Note**: Admin dashboard path is `/admin`, NOT `/admin/dashboard`.

## Auth & Role System

Single source of truth for user roles: `t_user.role` column (`student` / `teacher` / `admin`).

- `StpInterfaceImpl` (Sa-Token) reads `t_user.role` for both `getRoleList()` and `getPermissionList()`.
- Login/checkLogin/phoneLogin endpoints return `role` field in response. Frontend `RoleGuard` uses this for route protection.
- Legacy `admin_permission` table is deprecated — do not add new logic depending on it.
- Admin panel (`/admin`) requires `role=admin`. Backend `pageUsers` and `addAdmin` endpoints are guarded by `@SaCheckRole("admin")`.
- `addAdmin` API body: `POST /api/ip/v1/users/admin` with JSON object `{"username":"xxx"}` (not a raw string — Axios JSON serialization wraps strings in quotes, breaking `@RequestBody` deserialization).
- Default admin account: username `admin`, password `admin` (defined in `admin/src/main/resources/sql/t_user.sql`). Passwords are stored in plain text.
- Initial SQL: `t_user.sql` contains the single admin user; `admin_permission.sql` is cleared (deprecated). **Note**: `t_user.sql` only executes on first database creation. For existing databases, manually insert the admin user.

## Key Domain Constraints (Interview)

- Session state and question state are **two separate state machines** — do not merge them.
- `questionNumber` can be either a main question or a follow-up — it is not a database primary key.
- `requestId` is the idempotency boundary for answer submission — must remain stable across retries.
- Workflow output fields (讯飞 YAML) must match Java-side parsing exactly.
- Answer pipeline cannot skip: idempotency check, question number validation, per-question locking, score submission compensation.
- Follow-up decisions depend on both AI output AND the LiteFlow rule engine + max follow-up count.
- Session restore and finalization are first-class business contracts, not auxiliary features.

## Skills (Backend Domain Knowledge)

The `AI-Meeting/skills/` directory contains Claude Code domain knowledge skills. Use them when working on specific backend domains:

- `interview-pilot-repo-map` — route requirements to the correct domain
- `interview-pilot-interview-domain` — interview flow, state machine, answer pipeline, scoring
- `interview-pilot-agent-domain` — 讯飞 Agent integration
- `interview-pilot-ai-runtime` — rate limiting, circuit breaking, SingleFlight, thread pools
- `interview-pilot-media-domain` — ASR, TTS, WebSocket
- `interview-pilot-auth-user` — authentication, permissions
- `interview-pilot-debug-playbook` — troubleshooting by symptom (stuck interview, timeout, restore failure)
- `interview-pilot-change-playbook` — cross-domain change coordination

Each skill has a `SKILL.md` entry point and `references/` directory with detailed docs.
