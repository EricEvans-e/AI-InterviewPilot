# Mimo Realtime ASR

## Connection

- WebSocket path: `/api/ip/v1/mimo/audio-to-text/{userId}`
- `onOpen` authenticates first, then registers session mappings.
- Authentication failure closes the session.

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
- Backend wraps PCM as WAV before calling Mimo ASR.
- Stopping should still emit the final result if audio was buffered.
- `Pipe closed` / `Stream closed` during stop is expected for the shutdown path.
