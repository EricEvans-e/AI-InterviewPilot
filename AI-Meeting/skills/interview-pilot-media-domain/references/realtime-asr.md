# Mimo ASR WebSocket

## Connection

- WebSocket path: `/api/ip/v1/mimo/audio-to-text/{userId}`
- `onOpen` authenticates first, then registers session mappings.
- Authentication failure closes the session.
- The WebSocket is realtime transport for browser audio chunks. Recognition itself is Mimo batch transcription after the buffered stream is closed by `stop_transcription` or connection cleanup.

## Commands

- `ping` -> `pong`
- `start_transcription` -> start receiving and buffering audio
- `stop_transcription` -> stop, flush final transcription, release resources
- `get_status` -> return health/status
- unknown value -> `unknown_command`

## Events

- `connected`
- `heartbeat`
- `transcription_started`
- `transcription_stopped`
- `transcription_already_started`
- `transcription_already_stopped`
- `transcription`
- `final`
- `error`

## Implementation Notes

- The browser sends PCM chunks.
- Backend buffers the PCM stream, wraps it as WAV, then calls Mimo ASR once.
- Mimo ASR emits a final transcript, not Xunfei-style per-sentence incremental revisions.
- Stopping should still emit the final result if audio was buffered.
- `Pipe closed` / `Stream closed` during stop is expected for the shutdown path.
