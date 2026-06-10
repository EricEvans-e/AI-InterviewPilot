# Agent 场景绑定

## 固定场景目录

`BusinessAgentScene` 当前定义了这些场景：

- `general-agent-chat` -> 默认名 `通用智能体`，当前配置默认绑定 `Mimo 2.5 通用智能体`，可切到 `Mimo 2.5 Pro 通用智能体`
- `interview-question-extraction` -> 默认名 `面试出题官`，当前配置默认绑定 `Mimo 2.5 面试出题官`
- `interview-answer-evaluation` -> 默认名 `用户答案评分官`，当前配置默认绑定 `Mimo 2.5 答案评分官`，可切到 `Mimo 2.5 Pro 答案评分官`
- `interview-demeanor` -> 默认名 `神态分析官`，当前配置默认绑定 `Mimo 2.5 神态分析官`
- `interview-question-asking` -> 默认名 `面试提问官`，当前配置默认绑定 `Mimo 2.5 面试提问官`，可切到 `Mimo 2.5 Pro 面试提问官`

## 解析顺序

- 先读 `interview-pilot.agent-binding` 中的配置名。
- 如果配置名存在，先尝试配置名。
- 再按枚举里的默认名和别名顺序尝试。
- 找到第一个能命中的 Agent 配置就返回。
- `interview-question-extraction` 和 `interview-demeanor` 需要视觉能力；即使数据库里误启用了 `mimo-v2.5-pro`，`BusinessAgentScene.supportsAgent()` 和解析器也会过滤掉。

## 为什么重要

- 这层决定代码里的“业务场景”最终调用哪个 Agent 配置。
- 配置名改了但 Agent 表里没同步，运行时就会 fallback，严重时直接报错。
- 面试域和通用 Agent 都依赖这个解析过程，但不要把它们的业务边界混在一起。
- Mimo Pro 只适合纯文本高推理链路，不要绑定到简历图片 OCR 或神态图片分析链路。
