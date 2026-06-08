# Mimo TTS

## Endpoints

- `POST /api/ip/v1/mimo/tts/tasks`: create a synthesis task.
- `GET /api/ip/v1/mimo/tts/tasks/{taskId}`: query task status/result.
- `POST /api/ip/v1/mimo/tts/synthesize`: create a task and wait for completion.

Legacy aliases under `/api/ip/v1/xunfei/tts/**` are still mapped by the backend for older frontend code, but new code should use `/mimo/tts/**`.

## Lifecycle

1. `createTask` submits the request and returns `taskId`.
2. `queryTask` checks progress/result and does not create a new task.
3. `synthesizeAndWait` wraps submit + wait into one request.
4. Audio is usable only after `audioBase64` or `audioUrl` is present.

## Key Fields

- `taskId`: task identifier for polling.
- `taskStatus`: provider task status.
- `audioBase64` / `audioUrl`: final audio result.
- `pybufContent` / `pybufUrl`: legacy response aliases normalized by frontend service.
