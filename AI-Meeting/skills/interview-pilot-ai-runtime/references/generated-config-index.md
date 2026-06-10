# Runtime Config Index (Generated)

Generated from `application.yaml` and `interview-followup-rule.yaml`. Re-run `scripts/extract_config_index.py` after config changes.

## spring.ai.openai

| Key | Value |
| --- | --- |
| `spring.ai.openai.api-key` | `${SPRING_AI_OPENAI_API_KEY:${MIMO_API_KEY:}}` |
| `spring.ai.openai.base-url` | `${SPRING_AI_OPENAI_BASE_URL:https://token-plan-cn.xiaomimimo.com/v1}` |
| `spring.ai.openai.chat.options.model` | `${SPRING_AI_OPENAI_MODEL:mimo-v2.5}` |
| `spring.ai.openai.chat.options.temperature` | `${SPRING_AI_OPENAI_TEMPERATURE:0.7}` |
| `spring.ai.openai.embedding.options.model` | `${SPRING_AI_OPENAI_EMBEDDING_MODEL:mimo-v2.5}` |

## mimo

| Key | Value |
| --- | --- |
| `mimo.anthropic-base-url` | `${MIMO_ANTHROPIC_BASE_URL:https://token-plan-cn.xiaomimimo.com/anthropic}` |
| `mimo.api-key` | `${MIMO_API_KEY:${SPRING_AI_OPENAI_API_KEY:}}` |
| `mimo.asr-language` | `${MIMO_ASR_LANGUAGE:auto}` |
| `mimo.asr-model` | `${MIMO_ASR_MODEL:mimo-v2.5-asr}` |
| `mimo.chat-model` | `${MIMO_CHAT_MODEL:mimo-v2.5}` |
| `mimo.openai-base-url` | `${MIMO_OPENAI_BASE_URL:https://token-plan-cn.xiaomimimo.com/v1}` |
| `mimo.pcm-bits-per-sample` | `${MIMO_PCM_BITS_PER_SAMPLE:16}` |
| `mimo.pcm-channels` | `${MIMO_PCM_CHANNELS:1}` |
| `mimo.pcm-sample-rate` | `${MIMO_PCM_SAMPLE_RATE:16000}` |
| `mimo.pro-model` | `${MIMO_PRO_MODEL:mimo-v2.5-pro}` |
| `mimo.tts-format` | `${MIMO_TTS_FORMAT:wav}` |
| `mimo.tts-model` | `${MIMO_TTS_MODEL:mimo-v2.5-tts}` |
| `mimo.tts-voice` | `${MIMO_TTS_VOICE:Chloe}` |

## legacy.xunfei

| Key | Value |
| --- | --- |
| `legacy.xunfei.enabled` | `${LEGACY_XUNFEI_ENABLED:false}` |

## interview-pilot.agent-binding

| Key | Value |
| --- | --- |
| `interview-pilot.agent-binding.general-agent-chat` | `${INTERVIEW_PILOT_AGENT_GENERAL_CHAT:Mimo 2.5 通用智能体}` |
| `interview-pilot.agent-binding.interview-answer-evaluation` | `${INTERVIEW_PILOT_AGENT_INTERVIEW_ANSWER_EVALUATION:Mimo 2.5 答案评分官}` |
| `interview-pilot.agent-binding.interview-demeanor` | `${INTERVIEW_PILOT_AGENT_INTERVIEW_DEMEANOR:Mimo 2.5 神态分析官}` |
| `interview-pilot.agent-binding.interview-question-asking` | `${INTERVIEW_PILOT_AGENT_INTERVIEW_QUESTION_ASKING:Mimo 2.5 面试提问官}` |
| `interview-pilot.agent-binding.interview-question-extraction` | `${INTERVIEW_PILOT_AGENT_INTERVIEW_QUESTION_EXTRACTION:Mimo 2.5 面试出题官}` |

## interview-pilot.flow-limit

| Key | Value |
| --- | --- |
| `interview-pilot.flow-limit.enable` | `True` |
| `interview-pilot.flow-limit.interview-ai-call-max-access-count` | `6` |
| `interview-pilot.flow-limit.interview-ai-call-time-window-seconds` | `1` |
| `interview-pilot.flow-limit.interview-answer-max-access-count` | `8` |
| `interview-pilot.flow-limit.interview-answer-time-window-seconds` | `1` |
| `interview-pilot.flow-limit.interview-heavy-max-access-count` | `2` |
| `interview-pilot.flow-limit.interview-heavy-time-window-seconds` | `1` |
| `interview-pilot.flow-limit.interview-read-max-access-count` | `15` |
| `interview-pilot.flow-limit.interview-read-time-window-seconds` | `1` |
| `interview-pilot.flow-limit.max-access-count` | `20` |
| `interview-pilot.flow-limit.requested-tokens` | `1` |
| `interview-pilot.flow-limit.time-window-seconds` | `1` |

## interview-pilot.ai-guard

| Key | Value |
| --- | --- |
| `interview-pilot.ai-guard.circuit-failure-rate-threshold` | `50` |
| `interview-pilot.ai-guard.circuit-open-state-wait-millis` | `30000` |
| `interview-pilot.ai-guard.circuit-permitted-calls-in-half-open-state` | `10` |
| `interview-pilot.ai-guard.circuit-sliding-window-size` | `50` |
| `interview-pilot.ai-guard.enable` | `True` |
| `interview-pilot.ai-guard.executor-threads` | `32` |
| `interview-pilot.ai-guard.stages.interview-demeanor.max-concurrent-calls` | `6` |
| `interview-pilot.ai-guard.stages.interview-demeanor.retry-count` | `0` |
| `interview-pilot.ai-guard.stages.interview-demeanor.retry-wait-millis` | `0` |
| `interview-pilot.ai-guard.stages.interview-demeanor.timeout-millis` | `20000` |
| `interview-pilot.ai-guard.stages.interview-evaluation.max-concurrent-calls` | `30` |
| `interview-pilot.ai-guard.stages.interview-evaluation.retry-count` | `1` |
| `interview-pilot.ai-guard.stages.interview-evaluation.retry-wait-millis` | `100` |
| `interview-pilot.ai-guard.stages.interview-evaluation.timeout-millis` | `20000` |
| `interview-pilot.ai-guard.stages.interview-extraction.max-concurrent-calls` | `8` |
| `interview-pilot.ai-guard.stages.interview-extraction.retry-count` | `0` |
| `interview-pilot.ai-guard.stages.interview-extraction.retry-wait-millis` | `0` |
| `interview-pilot.ai-guard.stages.interview-extraction.timeout-millis` | `60000` |
| `interview-pilot.ai-guard.stages.interview-followup.max-concurrent-calls` | `20` |
| `interview-pilot.ai-guard.stages.interview-followup.retry-count` | `1` |
| `interview-pilot.ai-guard.stages.interview-followup.retry-wait-millis` | `100` |
| `interview-pilot.ai-guard.stages.interview-followup.timeout-millis` | `20000` |
| `interview-pilot.ai-guard.stages.interview-reference-answer.max-concurrent-calls` | `6` |
| `interview-pilot.ai-guard.stages.interview-reference-answer.retry-count` | `0` |
| `interview-pilot.ai-guard.stages.interview-reference-answer.retry-wait-millis` | `0` |
| `interview-pilot.ai-guard.stages.interview-reference-answer.timeout-millis` | `300000` |
| `interview-pilot.ai-guard.stages.interview-report-review.max-concurrent-calls` | `6` |
| `interview-pilot.ai-guard.stages.interview-report-review.retry-count` | `0` |
| `interview-pilot.ai-guard.stages.interview-report-review.retry-wait-millis` | `0` |
| `interview-pilot.ai-guard.stages.interview-report-review.timeout-millis` | `300000` |

## interview-pilot.ai-singleflight

| Key | Value |
| --- | --- |
| `interview-pilot.ai-singleflight.cleanup-threshold` | `256` |
| `interview-pilot.ai-singleflight.distributed-enabled` | `True` |
| `interview-pilot.ai-singleflight.enable` | `True` |
| `interview-pilot.ai-singleflight.follower-max-wait-millis` | `20000` |
| `interview-pilot.ai-singleflight.heavy-lock-expire-seconds` | `90` |
| `interview-pilot.ai-singleflight.heavy-lock-wait-millis` | `0` |
| `interview-pilot.ai-singleflight.l1-cache-max-size` | `1000` |
| `interview-pilot.ai-singleflight.mode` | `hybrid` |
| `interview-pilot.ai-singleflight.poll-fallback-interval-millis` | `2000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-demeanor.compression-codec` | `gzip` |
| `interview-pilot.ai-singleflight.stage-policies.interview-demeanor.compression-threshold-bytes` | `2048` |
| `interview-pilot.ai-singleflight.stage-policies.interview-demeanor.failed-result-ttl-millis` | `60000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-demeanor.heartbeat-interval-millis` | `5000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-demeanor.l1-cache-enabled` | `False` |
| `interview-pilot.ai-singleflight.stage-policies.interview-demeanor.result-ttl-millis` | `900000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-demeanor.running-ttl-millis` | `30000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-demeanor.takeover-detect-millis` | `15000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-evaluation.compression-codec` | `gzip` |
| `interview-pilot.ai-singleflight.stage-policies.interview-evaluation.compression-threshold-bytes` | `4096` |
| `interview-pilot.ai-singleflight.stage-policies.interview-evaluation.failed-result-ttl-millis` | `60000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-evaluation.heartbeat-interval-millis` | `3000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-evaluation.l1-cache-enabled` | `True` |
| `interview-pilot.ai-singleflight.stage-policies.interview-evaluation.l1-cache-ttl-millis` | `30000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-evaluation.result-ttl-millis` | `600000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-evaluation.running-ttl-millis` | `15000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-evaluation.takeover-detect-millis` | `10000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-extraction.compression-codec` | `gzip` |
| `interview-pilot.ai-singleflight.stage-policies.interview-extraction.compression-threshold-bytes` | `1024` |
| `interview-pilot.ai-singleflight.stage-policies.interview-extraction.failed-result-ttl-millis` | `60000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-extraction.heartbeat-interval-millis` | `4000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-extraction.l1-cache-enabled` | `True` |
| `interview-pilot.ai-singleflight.stage-policies.interview-extraction.l1-cache-ttl-millis` | `60000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-extraction.result-ttl-millis` | `1800000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-extraction.running-ttl-millis` | `20000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-extraction.takeover-detect-millis` | `12000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-followup.compression-codec` | `gzip` |
| `interview-pilot.ai-singleflight.stage-policies.interview-followup.compression-threshold-bytes` | `2048` |
| `interview-pilot.ai-singleflight.stage-policies.interview-followup.failed-result-ttl-millis` | `30000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-followup.heartbeat-interval-millis` | `2500` |
| `interview-pilot.ai-singleflight.stage-policies.interview-followup.l1-cache-enabled` | `True` |
| `interview-pilot.ai-singleflight.stage-policies.interview-followup.l1-cache-ttl-millis` | `15000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-followup.result-ttl-millis` | `180000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-followup.running-ttl-millis` | `12000` |
| `interview-pilot.ai-singleflight.stage-policies.interview-followup.takeover-detect-millis` | `8000` |
| `interview-pilot.ai-singleflight.stream-block-timeout-millis` | `3000` |
| `interview-pilot.ai-singleflight.ttl-millis` | `65000` |
| `interview-pilot.ai-singleflight.wait-timeout-millis` | `65000` |

## interview-pilot.thread-pool

| Key | Value |
| --- | --- |
| `interview-pilot.thread-pool.ai-io.core-pool-size` | `24` |
| `interview-pilot.thread-pool.ai-io.keep-alive-seconds` | `120` |
| `interview-pilot.thread-pool.ai-io.max-pool-size` | `64` |
| `interview-pilot.thread-pool.ai-io.queue-capacity` | `400` |
| `interview-pilot.thread-pool.ai-io.thread-name-prefix` | `ip-ai-io-` |
| `interview-pilot.thread-pool.cpu.core-pool-size` | `8` |
| `interview-pilot.thread-pool.cpu.keep-alive-seconds` | `60` |
| `interview-pilot.thread-pool.cpu.max-pool-size` | `16` |
| `interview-pilot.thread-pool.cpu.queue-capacity` | `256` |
| `interview-pilot.thread-pool.cpu.thread-name-prefix` | `ip-cpu-` |
| `interview-pilot.thread-pool.general.core-pool-size` | `50` |
| `interview-pilot.thread-pool.general.keep-alive-seconds` | `300` |
| `interview-pilot.thread-pool.general.max-pool-size` | `200` |
| `interview-pilot.thread-pool.general.queue-capacity` | `1000` |
| `interview-pilot.thread-pool.general.thread-name-prefix` | `ip-async-` |
| `interview-pilot.thread-pool.query.core-pool-size` | `16` |
| `interview-pilot.thread-pool.query.keep-alive-seconds` | `120` |
| `interview-pilot.thread-pool.query.max-pool-size` | `48` |
| `interview-pilot.thread-pool.query.queue-capacity` | `600` |
| `interview-pilot.thread-pool.query.thread-name-prefix` | `ip-query-` |
| `interview-pilot.thread-pool.scheduled-pool-size` | `8` |
| `interview-pilot.thread-pool.scheduled-thread-name-prefix` | `ip-schedule-` |

## interview-pilot.interview.answer-guard

| Key | Value |
| --- | --- |
| `interview-pilot.interview.answer-guard.lock-expire-seconds` | `-1` |
| `interview-pilot.interview.answer-guard.lock-wait-millis` | `0` |
| `interview-pilot.interview.answer-guard.lock-watchdog-enabled` | `True` |
| `interview-pilot.interview.answer-guard.processing-expire-seconds` | `120` |
| `interview-pilot.interview.answer-guard.processing-long-tail-expire-seconds` | `300` |
| `interview-pilot.interview.answer-guard.replay-expire-hours` | `24` |

## interview-pilot.interview.turn-repair

| Key | Value |
| --- | --- |
| `interview-pilot.interview.turn-repair.batch-size` | `50` |
| `interview-pilot.interview.turn-repair.enable` | `True` |
| `interview-pilot.interview.turn-repair.fixed-delay-millis` | `3000` |
| `interview-pilot.interview.turn-repair.max-retries` | `6` |

## interview-pilot.redis-session

| Key | Value |
| --- | --- |
| `interview-pilot.redis-session.batch-sync-size` | `100` |
| `interview-pilot.redis-session.clean-interval-seconds` | `300` |
| `interview-pilot.redis-session.enable` | `True` |
| `interview-pilot.redis-session.max-queue-size` | `10000` |
| `interview-pilot.redis-session.message-expire-seconds` | `604800` |
| `interview-pilot.redis-session.sync-delay-seconds` | `30` |

## interview-pilot.interview.rule-engine

| Key | Value |
| --- | --- |
| `interview-pilot.interview.rule-engine.default-chain-id` | `default_followup_chain` |
| `interview-pilot.interview.rule-engine.default-low-score-threshold` | `60` |
| `interview-pilot.interview.rule-engine.default-max-follow-up` | `2` |
| `interview-pilot.interview.rule-engine.enable` | `True` |
| `interview-pilot.interview.rule-engine.fail-open` | `True` |
| `interview-pilot.interview.rule-engine.rule-version` | `v1.0.0` |

## collection.vector

| Key | Value |
| --- | --- |
| `collection.vector.similarity-threshold` | `0.9` |

## liteflow.rule-source

| Key | Value |
| --- | --- |
| `liteflow.rule-source` | `classpath:liteflow/interview-followup-chain.xml` |

