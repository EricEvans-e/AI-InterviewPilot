# Module Routing

## Responsibilities

- `auth`: login state, `@CurrentUser`, roles, WebSocket authentication.
- `agent`: generic agent chat, SSE, agent properties, scene binding, file assets.
- `interview`: interview sessions, question extraction, answer pipeline, scoring, follow-up, restore, finalize.
- `media`: Mimo ASR, WebSocket push, TTS.
- `conversation`: chat history, streaming message persistence, session ownership.
- `ai`: model providers, unified AI invocation, chat handlers.
- `shared`: common DTOs, enums, result wrappers, shared constraints.

## Routing Rules

- `/api/ip/v1/interview/**` -> `interview-pilot-interview-domain`.
- `/api/ip/v1/agents/**` -> `interview-pilot-agent-domain`.
- `/api/ip/v1/mimo/**`, `/api/ip/v1/websocket/**`, or realtime `@ServerEndpoint` -> `interview-pilot-media-domain`.
- `/api/ip/v1/xunfei/**` -> legacy media compatibility path; still start with `interview-pilot-media-domain`.
- Login, token, `@CurrentUser`, admin checks -> `interview-pilot-auth-user`.
- `interview-pilot.ai-guard`, `interview-pilot.ai-singleflight`, `interview-pilot.flow-limit` -> `interview-pilot-ai-runtime`.
