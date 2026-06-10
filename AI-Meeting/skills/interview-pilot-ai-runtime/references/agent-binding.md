# Agent 绑定

## 配置入口

`application.yaml` 中的 `interview-pilot.agent-binding` 负责把业务场景绑定到实际 Agent 名称：

- `general-agent-chat -> Mimo 2.5 通用智能体`
- `interview-question-extraction -> Mimo 2.5 面试出题官`
- `interview-answer-evaluation -> Mimo 2.5 答案评分官`
- `interview-demeanor -> Mimo 2.5 神态分析官`
- `interview-question-asking -> Mimo 2.5 面试提问官`

## 代码入口

- `BusinessAgentScene`：声明场景 code、默认名称、别名集合。
- `BusinessAgentBindingProperties`：从配置读取绑定名。
- `BusinessAgentResolver`：按“配置名 -> 默认名 -> 别名”的顺序解析实际 Agent，并过滤不支持当前场景的模型。

## 回退规则

- 如果配置名存在且能找到 Agent，优先使用配置名。
- 如果配置名找不到，会继续尝试默认名和别名。
- 如果视觉场景绑定到了 `mimo-v2.5-pro`，解析器会跳过该 Agent，因为 Pro 当前不用于图片 OCR 或神态图片分析。
- 如果最终一个都找不到，会抛 `AGENT_CONFIG_NOT_FOUND`。

## 运行时影响

- 这层决定面试评分、提题、追问、神态分析到底调哪个 Agent 配置。
- 所以改名字不是纯展示改动，而是正式运行时改动。
- `mimo-v2.5-pro` 只能用于答案评分、面试提问/追问、通用智能体等纯文本高推理链路；简历出题和神态分析保留 `mimo-v2.5`。
