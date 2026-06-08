# 媒体域易错点

## 最常踩的坑

- 不要把 WebSocket 的 `userId` 只当展示字段，它同时是路由和鉴权输入。
- 不要在同一个 session 上重复开启多个转写上下文。
- 不要把 `heartbeat` 和业务结果混为一谈，它只是连接保活。
- 不要把 `transcription_already_started` 误判成系统异常，这是幂等性提示。
- 不要忘了停止转写后清理上下文和心跳任务。

## 现象到检查点

| 现象 | 第一检查点 | 常见原因 |
| --- | --- | --- |
| WebSocket 连不上 | token、path `userId`、鉴权服务 | token 缺失或 userId 不匹配 |
| 连上了但没心跳 | `heartbeatExecutor` 是否注入 | 线程池未就绪或被关闭 |
| start 后没有结果 | `TRANSCRIPTION_CONTEXTS`、`MimoAudioService` | 未收到音频块、stop 未触发，或 Mimo ASR 调用失败 |
| stop 后还在推结果 | 上下文是否已移除 | pipe 未关闭或回调仍在跑 |
| TTS 返回成功但没有音频 | `LongTextTtsTaskRespDTO.audioBase64` | Mimo TTS 响应没有 `message.audio.data` |

## 一个实用判断

- `transcription_already_started` 通常是并发/重复命令，不是底层失败。
- `unknown_command` 通常是前后端命令枚举不一致。
- `Pipe closed`、`Stream closed` 在停止转写场景里通常是预期结束信号，不一定是报错。
- `/api/ip/v1/mimo/tts/tasks` 是同步合成后的兼容任务外观，不要按外部平台异步任务轮询排障。
