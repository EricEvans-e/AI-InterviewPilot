# Mimo Model Migration Completion Implementation Plan

> Superseded on 2026-06-09: current runtime policy prefers Mimo OpenAI-compatible `/v1` for normal chat, pro pure-text chat, interview AI, ASR, and TTS. Keep Anthropic-compatible code as legacy compatibility only. See `docs/ai-model-configuration.md` for the current model matrix.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish replacing default model/API usage with Xiaomi Mimo, including chat, Agent chat, interview AI, ASR, and TTS, while keeping Xunfei only as explicitly enabled legacy compatibility.

**Architecture:** Mimo uses the OpenAI-compatible endpoint for normal chat, pro pure-text chat, interview AI, ASR, and TTS. Anthropic-compatible and Xunfei workflow/audio classes remain legacy compatibility only; production services must lazily access Xunfei clients only from `xingchen` legacy branches.

**Tech Stack:** Java 17, Spring Boot 3.4, Spring AI OpenAI-compatible client, OkHttp, JUnit 5, Mockito, React/Vite.

---

### Task 1: Lock Down Legacy Boundaries

**Files:**
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/toolkit/iflytek/XunfeiWorkflowClient.java`
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/agent/service/impl/AgentMessageServiceImpl.java`
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/agent/service/impl/AgentFileAssetServiceImpl.java`
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/interview/shared/InterviewAiInvoker.java`
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/interview/flow/extraction/InterviewQuestionExtractionService.java`
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/interview/flow/demeanor/InterviewDemeanorService.java`
- Test: `AI-Meeting/admin/src/test/java/com/interviewpilot/agent/service/impl/AgentMessageServiceImplTest.java`
- Test: `AI-Meeting/admin/src/test/java/com/interviewpilot/interview/flow/demeanor/InterviewDemeanorServiceTest.java`

- [ ] Add tests proving Mimo providers do not call `XunfeiWorkflowClient`.
- [ ] Add `@ConditionalOnProperty(prefix = "legacy.xunfei", name = "enabled", havingValue = "true")` to `XunfeiWorkflowClient`.
- [ ] Replace direct `XunfeiWorkflowClient` constructor injection with `ObjectProvider<XunfeiWorkflowClient>` and fail with clear legacy-only errors inside `xingchen` paths.
- [ ] Run:

```powershell
cd AI-Meeting
.\mvnw.cmd -q -pl admin "-Dtest=AgentMessageServiceImplTest,InterviewDemeanorServiceTest,InterviewQuestionExtractionServiceTest,InterviewQuestionExtractionAnthropicTest,InterviewAiInvokerAnthropicTest" test
```

### Task 2: Finish Mimo Demeanor Routing

**Files:**
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/interview/flow/demeanor/InterviewDemeanorService.java`
- Test: `AI-Meeting/admin/src/test/java/com/interviewpilot/interview/flow/demeanor/InterviewDemeanorServiceTest.java`

- [ ] Treat both `openai` and `anthropic` providers as Mimo-compatible non-Xunfei routes.
- [ ] For Mimo providers, skip legacy upload and pass a text prompt to `InterviewAiInvoker`.
- [ ] For `xingchen`, keep legacy file upload and workflow-readable URL behavior.
- [ ] Run:

```powershell
cd AI-Meeting
.\mvnw.cmd -q -pl admin "-Dtest=InterviewDemeanorServiceTest" test
```

### Task 3: Remove Old Active Model Signals

**Files:**
- Modify: `AI-Meeting/admin/pom.xml`
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/ai/service/AiPropertiesService.java`
- Modify: `AI-Meeting/admin/src/main/java/com/interviewpilot/ai/service/impl/AiPropertiesServiceImpl.java`
- Modify comments in `AI-Meeting/admin/src/main/java/com/interviewpilot/ai/**`
- Modify: `AI-Meeting/skills/interview-pilot-ai-runtime/references/object-dictionary.md`

- [ ] Remove unused `spring-ai-deepseek` dependency.
- [ ] Replace `getDefaultDoubaoConfig()` with `getDefaultMimoConfig()` in the public service contract where callers no longer need the old name.
- [ ] Update comments that describe active defaults as DeepSeek/Doubao/Spark.
- [ ] Run:

```powershell
cd AI-Meeting
.\mvnw.cmd -q -pl admin "-Dtest=AiPropritiesTypeTest" test
```

### Task 4: Verify End To End

**Files:**
- No production edits unless a verification failure exposes a root cause.

- [ ] Run backend tests:

```powershell
cd AI-Meeting
.\mvnw.cmd -q -pl admin test
```

- [ ] Run frontend checks:

```powershell
cd AI-Meeting-Frontend
npm run check
```

- [ ] Run secret and old endpoint scans from an operator-local private checklist. Do not write real API keys or old leaked tokens into this plan file.

```powershell
rg -n "<real-key-or-known-leaked-token-patterns>" . -S
rg -n "<old-default-endpoint-patterns>" README.md CLAUDE.md docs AI-Meeting AI-Meeting-Frontend -S
```

- [ ] Smoke test Mimo text, ASR, and TTS with the real key only in process environment, never in files.

### Task 5: Commit

**Files:**
- Commit all migration changes after tests and scans pass.

- [ ] Review:

```powershell
git diff --stat
git status --short
```

- [ ] Commit:

```powershell
git add .
git commit -m "feat: migrate model runtime to mimo"
```
