# 媒体域对象词典

## 1. WebSocket 连接对象

| 对象 | 含义 | 真相源 | 谁写 | 谁读 |
| --- | --- | --- | --- | --- |
| `USER_SESSIONS` | 用户到连接的映射 | 内存态 | 建连、断连 | 推送层、连接管理 |
| `SESSION_USER_MAP` | 连接到用户的映射 | 内存态 | 建连、断连 | 推送层、鉴权后校验 |
| `WebSocketMessage` | 客户端控制命令 | WebSocket 入参 | 前端 | WebSocket handler |
| `WebSocketResponse` | 服务端统一响应 | WebSocket 出参 | 服务端 | 前端 |
| `TranscriptionSessionContext` | 单连接转写上下文 | 内存态 | start/stop 链路 | 转写执行器、停止逻辑 |

## 2. 实时转写对象

| 对象 | 含义 | 关键字段 | 说明 |
| --- | --- | --- | --- |
| `RealtimeTranscriptionUpdate` | 实时转写增量 | `fullText`、`committedText`、`liveText`、`displayText`、`revision`、`resultStatus`、`segmentId`、`segmentText`、`pgs`、`rg`、`bg`、`ed`、`finalPacket` | 直接决定前端如何展示流式文本 |
| `fullText` | 全量文本 | 完整结果 | 最终展示的完整文本 |
| `committedText` | 已提交文本 | 稳定片段 | 一般可持久展示 |
| `liveText` | 实时文本 | 当前滚动片段 | 会被后续修订覆盖 |
| `displayText` | 前端展示文本 | 渲染用文本 | 前端不一定自己拼接 |
| `revision` | 版本号 | 更新顺序 | 处理乱序和重绘 |

## 3. TTS 对象

| 对象 | 含义 | 真相源 | 说明 |
| --- | --- | --- | --- |
| `LongTextTtsReqDTO` | 长文本合成请求 | HTTP 请求体 | Mimo TTS 合成入口 |
| `LongTextTtsTaskRespDTO` | 长文本合成响应 | HTTP 响应体 | 为兼容旧前端保留 task 形态，Mimo 实际同步返回音频 |
| `taskId` | 兼容任务 ID | Mimo response id 或本地生成值 | 查询接口只返回 completed 兼容状态，不保存历史音频 |
| `taskStatus` | 兼容任务状态 | 后端适配层 | Mimo 同步成功时固定完成态 `5` |
| `audioBase64` / `audioUrl` | 合成结果 | `choices[0].message.audio.data` 或兼容地址 | 新 Mimo 路径主要使用 `audioBase64` |
| `pybufContent` / `pybufUrl` | 旧响应别名 | 前端归一化字段 | 仅为兼容旧调用方 |

## 4. 生命周期

1. WebSocket `onOpen` 先鉴权，再登记连接映射。
2. 客户端发送 `start_transcription` 后创建 `TranscriptionSessionContext`。
3. 客户端持续写入 PCM 音频块，服务端通过 pipe 缓冲。
4. 显式停止或连接清理时，服务端关闭音频流、调用 Mimo ASR、发送 `transcription` 快照和 `final` 事件并清理上下文。
5. TTS 调用 Mimo 同步合成；`/tasks` 和 `/tasks/{taskId}` 只是兼容旧任务接口形态。

## 5. 关键不变量

- `userId` 既是路由参数，也是鉴权输入。
- 同一连接上下文不能重复开启多个转写上下文。
- `ping/pong` 只是保活，不是业务事件。
- `transcription_already_started` 是幂等语义，不是系统故障。
- `final` 是终态，`transcription` 是中间态。
- Mimo ASR 不提供讯飞式逐句修订流，前端不要依赖 `pgs` / `rg` 增量去重语义。

## 6. 常见误判

- WebSocket 连接 session 不等于业务会话 `sessionId`。
- `stop_transcription` 成功不等于一定会立刻拿到最终文本。
- `Pipe closed` / `Stream closed` 在停止路径上可能是正常现象。
- TTS 创建成功后应直接检查 `audioBase64`，不要继续等待外部平台任务完成。
