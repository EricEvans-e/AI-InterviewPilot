# 媒体链路排障剧本

## 常见现象

- WebSocket 连不上。
- 已连接但没有心跳。
- `start_transcription` 后发送了音频，但 `stop_transcription` 后没有最终文本。
- TTS 返回成功但没有可播放音频。

## 先看哪些对象

| 现象 | 先看对象 | 真相源 |
| --- | --- | --- |
| 连不上 | `token`、`pathUserId`、`SESSION_USER_MAP` | 鉴权结果 + 内存映射 |
| 没心跳 | `WebSocketResponse`、连接状态 | 连接管理 |
| 没转写 | `TranscriptionSessionContext`、`MimoAudioService`、`RealtimeTranscriptionUpdate` | 内存上下文 + Mimo ASR 结果 |
| TTS 没音频 | `LongTextTtsTaskRespDTO.audioBase64`、Mimo response `message.audio.data` | Mimo TTS 响应 |

## 优先检查

1. WebSocket token 是否有效、path `userId` 是否匹配。
2. `AudioTranscriptionWebSocketHandler.onOpen` 是否成功登记 session。
3. 是否触发了 `transcription_already_started` 或 `transcription_already_stopped`。
4. 停止转写后出现 `Pipe closed` / `Stream closed` 是否属于预期关闭。
5. TTS 返回的 `audioBase64` 是否为空；`/tasks` 也是同步合成兼容入口。

## 常见根因

- 鉴权失败但前端只看到了“连接创建成功”。
- 建连成功，但 `TranscriptionSessionContext` 没真正进入 active 状态。
- 没有发送 `stop_transcription`，导致 pipe 没关闭，Mimo ASR 调用没有开始。
- Mimo TTS 响应成功但没有 `choices[0].message.audio.data`。

## 处理建议

- 先确认连接层，再确认业务层，再确认引擎回调层。
- 如果现象是“能连上但没文本”，优先看上下文是否 active、是否收到了二进制音频、stop 是否关闭了 pipe。
- 如果现象是“TTS 不返回”，先看 Mimo API HTTP 状态和 `audioBase64`，不要按旧异步轮询模型排查。
