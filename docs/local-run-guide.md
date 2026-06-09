# InterviewPilot 本地完整运行指南

本文档用于在本机完整启动 InterviewPilot，包括 MySQL、MongoDB、Redis、后端、前端，以及 Mimo 文本模型、ASR 和 TTS 的验证方式。

真实 API Key 只放在本机环境变量、未提交的 `.env` 文件或数据库运行时配置中，不要写入 README、`.env.example`、SQL、脚本或任何会提交到 Git 的文件。

---

## 一、运行入口

推荐本地开发方式：

1. 用 Docker Compose 启动 MySQL、MongoDB、Redis。
2. 在当前终端设置 Mimo 环境变量。
3. 本机启动后端 `admin` 模块。
4. 本机启动前端 Vite 开发服务器。
5. 浏览器访问 `http://localhost:5173`。

生产式容器运行方式见 [deployment-guide.md](deployment-guide.md)。

---

## 二、环境要求

| 组件 | 要求 |
|------|------|
| JDK | 17+ |
| Node.js | 20+ |
| npm | 10+ |
| Maven | 可用 Maven Wrapper，无需全局安装 |
| Docker Desktop / Docker Engine | 用于启动 MySQL、MongoDB、Redis |
| 浏览器 | Chrome / Edge，测试 ASR 需要麦克风权限 |

---

## 三、准备仓库

如果已经在本机项目目录：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting
```

如果从远程克隆：

```bash
git clone https://github.com/EricEvans-e/AI-InterviewPilot.git
cd AI-InterviewPilot
```

---

## 四、设置 Mimo 环境变量

Windows PowerShell 中，在启动后端的同一个终端执行：

```powershell
$env:MIMO_API_KEY="你的-token-plan-api-key"
$env:SPRING_AI_OPENAI_API_KEY=$env:MIMO_API_KEY
$env:MIMO_OPENAI_BASE_URL="https://token-plan-cn.xiaomimimo.com/v1"
$env:MIMO_CHAT_MODEL="mimo-v2.5"
$env:MIMO_PRO_MODEL="mimo-v2.5-pro"
$env:MIMO_ASR_MODEL="mimo-v2.5-asr"
$env:MIMO_TTS_MODEL="mimo-v2.5-tts"
$env:LEGACY_XUNFEI_ENABLED="false"
```

Linux / macOS 中执行：

```bash
export MIMO_API_KEY="你的-token-plan-api-key"
export SPRING_AI_OPENAI_API_KEY="$MIMO_API_KEY"
export MIMO_OPENAI_BASE_URL="https://token-plan-cn.xiaomimimo.com/v1"
export MIMO_CHAT_MODEL="mimo-v2.5"
export MIMO_PRO_MODEL="mimo-v2.5-pro"
export MIMO_ASR_MODEL="mimo-v2.5-asr"
export MIMO_TTS_MODEL="mimo-v2.5-tts"
export LEGACY_XUNFEI_ENABLED="false"
```

说明：

- `MIMO_API_KEY` 是项目统一读取的 Mimo key。
- `SPRING_AI_OPENAI_API_KEY` 指向同一个 key，用于 Spring AI OpenAI 兼容客户端。
- `mimo-v2.5` 用于通用/视觉链路；`mimo-v2.5-pro` 只用于纯文本高推理聊天，两者都走 `MIMO_OPENAI_BASE_URL`。
- 讯飞 legacy 默认关闭，除非明确需要旧链路，否则保持 `LEGACY_XUNFEI_ENABLED=false`。
- 后端本地启动时会自动加载 `AI-Meeting/.env`；如果你是从 `AI-Meeting/admin` 启动，它也会继续向上查找父目录中的 `.env`。shell 里手动设置的同名环境变量仍然优先。

---

## 五、启动基础设施

进入后端目录：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting\AI-Meeting
```

启动 MySQL、MongoDB、Redis：

```powershell
docker compose up -d mysql mongo redis
```

如果本机 Docker 只支持旧命令，也可以使用：

```powershell
docker-compose up -d mysql mongo redis
```

默认端口：

| 服务 | 容器名 | 宿主机端口 |
|------|--------|------------|
| MySQL | `ip-mysql` | `3307` |
| MongoDB | `ip-mongo` | `27017` |
| Redis | `ip-redis` | `6379` |

查看状态：

```powershell
docker compose ps
```

首次启动 MySQL 时会自动执行：

- `AI-Meeting/admin/src/main/resources/sql/table.sql`
- `AI-Meeting/admin/src/main/resources/sql/admin_permission.sql`
- `AI-Meeting/admin/src/main/resources/sql/agent_properties.sql`
- `AI-Meeting/admin/src/main/resources/sql/ai_properties.sql`
- `AI-Meeting/admin/src/main/resources/sql/t_user.sql`

---

## 六、校正数据库中的 Mimo 配置

如果数据库已经初始化过，SQL 中的 `MIMO_API_KEY` 占位符可能已经写入 `ai_properties` 和 `agent_properties`。这是预期行为：真实 key 只放在后端启动环境变量中，不写入数据库。

进入 MySQL：

```powershell
docker exec -it ip-mysql mysql -uroot -p122333 mainshi_agent
```

进入 MySQL 后可执行一次配置校正：

```sql
UPDATE ai_properties
SET api_key = 'MIMO_API_KEY'
WHERE api_key <> 'MIMO_API_KEY';

UPDATE agent_properties
SET api_key = 'MIMO_API_KEY',
    api_flow_id = 'https://token-plan-cn.xiaomimimo.com/v1',
    ai_provider = 'openai'
WHERE del_flag = 0;
```

检查是否还有非占位符 key：

```sql
SELECT COUNT(*) AS remaining_ai_non_placeholders
FROM ai_properties
WHERE api_key <> 'MIMO_API_KEY';

SELECT COUNT(*) AS remaining_agent_non_placeholders
FROM agent_properties
WHERE api_key <> 'MIMO_API_KEY';
```

两个结果都为 `0` 后退出：

```sql
exit;
```

只要启动后端的终端设置了 `MIMO_API_KEY` / `SPRING_AI_OPENAI_API_KEY`，后端会在运行时解析数据库占位符。

---

## 七、启动后端

确认仍在 `AI-Meeting` 目录，并且当前终端已经设置好 Mimo 环境变量。

Windows：

```powershell
.\mvnw.cmd -B -ntp clean verify -Dmaven.test.skip=true
.\mvnw.cmd spring-boot:run -pl admin
```

Linux / macOS：

```bash
./mvnw -B -ntp clean verify -Dmaven.test.skip=true
./mvnw spring-boot:run -pl admin
```

后端默认地址：

```text
http://localhost:8002
```

健康检查：

```powershell
Invoke-RestMethod http://localhost:8002/actuator/health
```

返回 `status: UP` 表示后端已经启动。

---

## 八、启动前端

新开一个终端：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting\AI-Meeting-Frontend
npm ci
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

默认前端代理配置：

```env
VITE_API_BASE_URL=/api
VITE_API_TARGET=http://localhost:8002
VITE_WS_BASE_URL=
```

Vite 会把 `/api` 和 WebSocket 请求代理到后端 `http://localhost:8002`。

---

## 九、默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `admin` |

登录后可以进入管理端检查 AI 模型配置和智能体场景绑定。

---

## 十、功能验证

### 10.1 基础服务

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8002/actuator/health
```

浏览器打开：

```text
http://localhost:5173
```

### 10.2 AI 文本模型

1. 登录系统。
2. 进入 AI 对话或智能体对话页面。
3. 发送一条短消息。
4. 预期结果：页面收到 SSE 流式文本回复，后端日志无鉴权失败或模型不存在错误。

### 10.3 ASR 语音转写

1. 进入面试或录音相关页面。
2. 允许浏览器麦克风权限。
3. 开始录音，说一段中文。
4. 停止录音，前端发送 `stop_transcription`。
5. 预期结果：后端调用 Mimo ASR，并通过 `transcription` / `final` 事件返回最终文本。

### 10.4 TTS 语音合成

1. 进入面试题播报或 TTS 测试入口。
2. 输入一段中文文本。
3. 预期结果：后端返回可播放音频，新链路主要读取 `audioBase64`。

### 10.5 面试页前端交互

1. 进入面试房间后，右上角应显示摄像头/录像浮窗。
2. 紧凑状态下可以拖动该浮窗；浮窗会被限制在聊天内容区域内，不会被拖出可见范围。
3. 点击浮窗右上角按钮可切换展开/收起；展开状态保持原有大尺寸覆盖层，不参与拖拽。
4. AI 返回的面试题如果带有 `{question=...}`，或者更完整的 `{id=..., topic=..., question=..., purpose=...}` 这类 Java Map 风格包装，前端会提取 `question` 字段，清理为纯题目文本，并在聊天区显示为“当前题目”卡片。

---

## 十一、生产式 Docker Compose 运行

如果希望前后端都用容器运行：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting\AI-Meeting
Copy-Item .env.example .env
```

编辑 `AI-Meeting/.env`，填入真实 Mimo key：

```dotenv
MIMO_API_KEY=你的-token-plan-api-key
SPRING_AI_OPENAI_API_KEY=你的-token-plan-api-key
LEGACY_XUNFEI_ENABLED=false
```

启动：

```powershell
docker compose -f docker-compose.prod.yml up -d --build
```

访问：

```text
http://localhost
```

查看日志：

```powershell
docker logs -f ip-backend
docker logs -f ip-frontend
```

停止：

```powershell
docker compose -f docker-compose.prod.yml down
```

---

## 十二、常用检查命令

后端测试：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting\AI-Meeting
.\mvnw.cmd -q -pl admin test
```

前端检查：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting\AI-Meeting-Frontend
npm run check
```

前端构建：

```powershell
npm run build
```

密钥泄露扫描：

```powershell
cd E:\Users\Eric\Desktop\AIMeeting
rg -n "tp-[A-Za-z0-9]{20,}" . -S
```

如果扫描命中真实 key，不要提交；先替换成占位符或移除。

---

## 十三、常见问题

| 现象 | 处理方式 |
|------|----------|
| 后端启动但 AI 无响应 | 先确认 `AI-Meeting/.env` 或当前启动后端的终端里已经提供 `MIMO_API_KEY` / `SPRING_AI_OPENAI_API_KEY`；数据库 `ai_properties`、`agent_properties` 应保留 `MIMO_API_KEY` 占位符，由后端运行时解析真实 key |
| 前端请求 404 | 确认后端在 `8002` 端口运行，前端代理 `VITE_API_TARGET=http://localhost:8002` |
| WebSocket 连接失败 | 确认通过 `http://localhost:5173` 访问前端，Vite proxy 已开启 `ws: true`，并且登录后再进入录音页面 |
| ASR 没有最终文本 | 停止录音时必须发送 `stop_transcription`，后端才会关闭音频流并调用 Mimo ASR |
| TTS 成功但无声音 | 检查后端返回的 `audioBase64` 是否为空，并查看 `MimoAudioService` 日志；如果出现 `messages must contain an assistant role for TTS model`，说明播报文本没有放在 `assistant` 角色消息中 |
| 面试页看不到摄像头浮窗 | 确认顶部摄像头按钮处于“关闭摄像头”状态（表示当前已开启）；如果刚更新前端代码，重启 `npm run dev` 并强制刷新浏览器。浮窗定位按聊天内容区域计算，避免被左侧侧边栏和 `overflow-hidden` 裁剪 |
| 当前题目显示成 `{id=..., question=...}` 原始对象串 | 说明前端题目文本没有经过 `normalizeInterviewQuestionText()` 归一化，检查会话流里是否绕过了这一步 |
| MySQL 密码不对 | 本地 compose 默认 root 密码是 `122333`；如果通过 `.env` 覆盖，以 `.env` 为准 |
| 端口冲突 | 修改 `docker-compose.yml` 或 `docker-compose.prod.yml` 中对应端口映射，或停止本机已有服务 |
| Redis 启动时报 `Bind for 0.0.0.0:6379 failed` | 先执行 `docker stop interviewpilot-redis` 释放 6379，再重新执行 `docker compose up -d mysql mongo redis` |
---

## Report and Reference Answer Runtime Notes

- Final report persistence now uses a fast first snapshot. The backend does not block report creation on a synchronous AI review-summary call.
- When report creation or finalize is still in progress, the frontend treats timeout/finalize states as transient and keeps polling instead of failing immediately.
- Recording playback can appear after the first report payload because `recordingUrl` may be written back later than the base interview record. The frontend now polls for the recording URL for roughly 60 seconds.
- Reference answers remain manual on the report page. Clicking `生成参考答案` uses a longer request timeout and then polls the saved report if the client-side request times out before the backend finishes.
- Interview conclusion is also manual on the report page. The first screen shows the fast rule-based summary; clicking `生成 AI 结论` sends a longer AI request and the page updates in place after the saved AI result is available.
- If `生成 AI 结论` times out in the browser before the backend completes, the frontend continues polling the report until the saved AI conclusion is returned or the retry window is exhausted.
- If the report still looks incomplete after the polling window, refresh once and then inspect backend logs for finalize completion, recording upload completion, and `/recordings/` exposure.
