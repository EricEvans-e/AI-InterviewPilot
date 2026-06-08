# Media API Map

## HTTP

- `POST /api/ip/v1/websocket/send-message`
- `POST /api/ip/v1/websocket/notification/{userId}`
- `POST /api/ip/v1/websocket/transcription/{userId}`
- `POST /api/ip/v1/websocket/error/{userId}`
- `GET /api/ip/v1/websocket/user/{userId}/status`
- `POST /api/ip/v1/mimo/tts/tasks`
- `GET /api/ip/v1/mimo/tts/tasks/{taskId}`
- `POST /api/ip/v1/mimo/tts/synthesize`

Legacy compatibility aliases:

- `POST /api/ip/v1/xunfei/tts/tasks`
- `GET /api/ip/v1/xunfei/tts/tasks/{taskId}`
- `POST /api/ip/v1/xunfei/tts/synthesize`

## WebSocket

- `@ServerEndpoint("/api/ip/v1/mimo/audio-to-text/{userId}")`

## Responsibilities

- `WebSocketController`: server push and status query.
- `AudioTranscriptionWebSocketHandler`: realtime ASR connection, commands, and binary audio flow.
- `XunfeiTtsController`: legacy class name; handles Mimo TTS requests.
