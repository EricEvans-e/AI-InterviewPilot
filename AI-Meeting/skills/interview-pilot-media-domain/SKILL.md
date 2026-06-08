---
name: interview-pilot-media-domain
description: AI-Meeting media and realtime communication skill. Use for Mimo ASR, TTS, WebSocket push, heartbeat, error notifications, and realtime audio message protocols; trigger on `/api/ip/v1/mimo/**`, `/api/ip/v1/websocket/**`, legacy `/api/ip/v1/xunfei/**`, or realtime `@ServerEndpoint` work.
---

# interview-pilot-media-domain

## Read Order

1. `references/api-map.md`
2. `references/realtime-asr.md`
3. `references/tts.md`
4. `references/websocket-notification.md`
5. `references/object-dictionary.md`
6. `references/gotchas.md`

## Key Entry Points

- `admin/src/main/java/com/interviewpilot/media/api/WebSocketController.java`
- `admin/src/main/java/com/interviewpilot/media/api/XunfeiTtsController.java` (legacy class name, Mimo-backed implementation)
- `admin/src/main/java/com/interviewpilot/media/infrastructure/websocket/AudioTranscriptionWebSocketHandler.java`
- `admin/src/main/java/com/interviewpilot/media/infrastructure/integration/MimoAudioService.java`
- `admin/src/main/java/com/interviewpilot/media/infrastructure/integration/AudioTranscriptionService.java`
- `admin/src/main/java/com/interviewpilot/media/infrastructure/integration/XunfeiAudioService.java` (legacy disabled by default)
- `admin/src/main/java/com/interviewpilot/media/infrastructure/integration/XunfeiLongTextTtsService.java` (legacy disabled by default)

## Invariants

- WebSocket connection session and business `sessionId` are not the same concept.
- WebSocket transcription must authenticate before starting a transcription session.
- A single WebSocket session must not stack unbounded transcription contexts.
- Command/event fields must stay aligned between frontend and backend.
- Mimo TTS is synchronous. `/tts/tasks` and `/tts/tasks/{taskId}` keep a task-shaped compatibility surface, but new code should read `audioBase64` from the synthesis response instead of polling for provider-side completion.
- New code should use `/api/ip/v1/mimo/**`; `/api/ip/v1/xunfei/**` is compatibility only.
