# 题库导入、离线扩题与快速面试出题计划

## 1. 背景

当前系统已经具备题库面试的基础链路：考生选择院校、专业、题型、能力点和难度后，后端从 `question` 表中按 `collegeId`、`majorId`、`questionType`、`abilityTag`、`difficulty` 组合抽取 `approved` 状态题目，并写入 `interview_session_question`，随后面试页直接按题号读取这些题目。

这条链路稳定、速度快，但问题也明显：

- 如果一个学校一个专业只有一套题，学生多次练习会反复遇到同一批题。
- 题库材料和考纲资料没有被充分利用，目前主要是原题抽取。
- 如果每次面试开始时都让 AI 现场根据大量题库和考纲实时生成题目，会增加等待时间和 token 成本。
- 需要支持老师自定义导入题目，并且允许 Word 文档批量导入，降低题库建设成本。

因此推荐采用“题库 + 离线扩题 + 会话快速抽题”的方案：后台把题库和考纲整理为可复用素材，提前生成并审核扩展题；学生开始面试时优先快速抽取已审核题，题量不足时再触发 AI 临时生成。

## 2. 目标

1. 支持题库题目批量导入，预留可扩展的自定义导入接口。
2. 支持 Word 文档导入题目，解析出题目、题型、院校、专业、能力点、难度、参考答案、评分规则、追问建议等字段。
3. 支持后台离线扩题：基于原题、考纲和院校专业信息，生成同类变体题、追问题、参考答案和评分规则。
4. 扩展题默认进入 `pending_review`，必须经老师审核后才能进入学生面试抽题池。
5. 学生面试开始时优先从已审核题库中快速抽题，避免实时生成带来的等待。
6. 当题库数量不足时，允许触发 AI 临时生成本次面试题，并在前端提示“正在生成本次模拟题”。
7. 保持现有题库面试流程兼容，不破坏当前 `from-bank` 抽题链路。

## 3. 非目标

- 本期不建设 RAG、向量数据库、embedding、文档切片和召回链路。
- 第一阶段不要求 Word 文档支持任意复杂排版，只支持约定模板和弱结构化解析。
- 第一阶段不把 AI 扩展题直接发布给学生，必须保留人工审核。
- 本期不把每轮追问做成知识库检索增强，追问仍可沿用现有回答评分与追问逻辑。
- 第一阶段不做跨机构题库权限体系，只保留后续扩展字段和接口边界。

## 4. 核心概念

### 4.1 原题

老师手动录入、Excel/Word 导入、历史题库迁移进来的题目。原题通常来源于真题、机构教研资料或老师整理材料。

### 4.2 扩展题

AI 根据原题、考纲、院校专业背景生成的新题。扩展题不是简单改写原题，而是保留考察方向、能力点和题型风格，生成同类题、变式题或场景题。

示例：

原题：

> 你为什么选择护理专业？

扩展题：

> 如果你在实习中遇到患者情绪失控，你会如何沟通？

> 你认为护理工作中责任心体现在哪些具体场景？

> 结合你报考我校护理专业的原因，说说你未来三年的学习规划。

### 4.3 题库素材包

按 `院校 + 专业 + 题型 + 能力点` 汇总的材料集合，包含：

- 已审核原题
- 已审核扩展题
- 考纲内容
- 专业信息
- 评分规则
- 参考答案
- 来源说明

本期素材包只使用 SQL 精确查询和文本拼接，不接入向量检索。

## 5. 推荐总体方案

### 5.1 阶段一：结构化导入与快速抽题

目标是让题库建设效率变高，并确保学生面试能快速开始。

能力范围：

- 新增题库批量导入接口。
- 支持 Word `.docx` 导入。
- 支持导入预览、字段映射、错误提示。
- 导入后的题目默认 `draft` 或 `pending_review`。
- 继续使用现有 `questionBankService.randomSelect(...)` 快速抽题。
- 抽题策略从“完全随机”升级为“分层抽题”：
  - 优先匹配 `collegeId + majorId + questionType`
  - 不足时回退到 `collegeId + questionType`
  - 再不足时回退到 `major category + questionType`
  - 最后回退到通用题

### 5.2 阶段二：后台离线扩题

目标是解决“一个学校一个专业只有一套题”的问题。

能力范围：

- 在老师后台选择院校、专业、题型、能力点和原题范围。
- 系统汇总对应原题和考纲。
- AI 每个原题生成 3-5 个扩展题。
- 每个扩展题附带：
  - 题目标题
  - 题目正文
  - 题型
  - 能力点
  - 难度
  - 建议答题时间
  - 参考答案或答题要点
  - 评分规则
  - 预设追问题
  - 来源依据
  - 关联原题 ID
  - AI 生成批次 ID
- 扩展题默认 `pending_review`。
- 老师可批量审核、编辑、发布或驳回。

### 5.3 阶段三：题量不足时的会话临时生成

目标是保证学生选了冷门院校或冷门专业时仍然可以开始面试。

触发条件：

- 精确匹配题目数量少于本次面试需要数量。
- 分层回退后仍不足。
- 当前模式允许 AI 临时生成。

流程：

1. 创建面试会话。
2. 后端检测可用题量。
3. 若不足，前端显示“正在生成本次模拟题”。
4. 后端用当前院校、专业、考纲和已找到的少量题目生成本次会话题。
5. 临时题写入会话题目快照，并标记为 `session_generated`。
6. 本次会话可用，但临时题不直接进入公开题库。
7. 老师后台可查看临时题，选择是否转存为扩展题并审核。

### 5.4 本期不做 RAG

本计划明确不建设 RAG。题库和考纲材料的使用方式限定为：

- 用 MySQL 按院校、专业、题型、能力点、年份、难度做精确筛选。
- 将筛选出的原题、考纲、专业说明和评分规则拼接成素材包。
- AI 只基于素材包做离线扩题或题量不足时的临时生成。
- 不引入向量数据库、embedding、相似度召回、文档切片或知识库同步任务。

这样可以降低实现复杂度和维护成本，让本期重点放在题库导入、扩题审核和快速抽题闭环上。

## 6. Word 导入方案

### 6.1 支持格式

第一阶段建议只支持 `.docx`，不支持老式 `.doc`。

原因：

- `.docx` 是标准 OpenXML 格式，Java 可通过 Apache POI 稳定解析。
- `.doc` 解析兼容性差，容易出现乱码和结构丢失。
- 老式 `.doc` 可提示用户另存为 `.docx` 后上传。

### 6.2 推荐 Word 模板

Word 文档建议支持两种模板。

#### 模板 A：表格导入

每行一道题，表头固定：

| 院校 | 专业 | 年份 | 题型 | 能力点 | 难度 | 题目 | 参考答案 | 评分规则 | 追问题 | 来源 |
|------|------|------|------|--------|------|------|----------|----------|--------|------|

字段说明：

- `院校`：可为空，空表示通用题。
- `专业`：可为空，空表示通用题。
- `年份`：可为空。
- `题型`：推荐使用三类规范值：`综合题`、`专业题`、`其他题`。当前后端兼容旧值 `结构化`、`半结构化`、`开放题`、`综合素质`、`情景应变`、`自我介绍`、`专业认知`。
- `能力点`：可为空，例如逻辑思维、语言表达、职业认知。
- `难度`：可为空，默认 `medium`。
- `题目`：必填。
- `参考答案`：可为空。
- `评分规则`：可为空，可为纯文本，后端可转成结构化 JSON。
- `追问题`：可为空，多条追问用换行或分号分隔。
- `来源`：可为空，例如“2026 某校招生简章”“老师整理”。

#### 模板 B：分段导入

适合老师已有普通 Word 题库。

格式示例：

```text
【院校】浙江金融职业学院
【专业】金融管理
【年份】2026
【题型】专业题
【能力点】职业认知
【难度】medium

1. 请谈谈你对金融管理专业的理解。
参考答案：可以从专业课程、职业方向、个人兴趣三个方面回答。
评分规则：表达清晰 30%，专业认知 40%，个人规划 30%。
追问题：你未来希望从事哪类金融岗位？为什么？

2. 如果客户对理财产品风险产生误解，你会如何沟通？
参考答案：先确认客户诉求，再解释风险收益关系，最后给出合规建议。
评分规则：风险意识、沟通逻辑、职业伦理。
```

当前实现额外支持更弱结构化的字段段落格式：

```text
题目：这个专业以后能做什么？
题型*：综合题
能力点*: 职业规划
难度*: medium
参考答案*：这个专业毕业后可以从事人工智能应用开发、数据处理、算法辅助开发、智能产品测试与实施等工作。
评分规则*：岗位理解40%；举例具体30%；表达完整30%
来源*：校内整理
```

说明：

- 字段名后可带 `*` 或 `＊`。
- 中英文冒号 `：`、`:` 均可识别。
- 只有 `题目` 一项时也允许导入，题型和难度回退到默认值。

解析策略：

- 文档头部的 `【院校】`、`【专业】`、`【年份】` 作为默认元数据。
- 遇到 `数字 + .` 或 `数字 + 、` 识别为新题。
- `参考答案：`、`评分规则：`、`追问题：` 作为字段分隔。
- 无法识别的段落进入导入预览，允许老师手动修正。

### 6.3 导入状态

新增导入批次状态：

- `UPLOADED`：文件已上传。
- `PARSED`：解析完成，等待确认。
- `PARTIAL_FAILED`：部分题目解析失败。
- `IMPORTED`：已写入题库。
- `FAILED`：文件解析失败。

题目导入后的默认状态：

- 老师导入：`draft` 或 `pending_review`，建议默认 `pending_review`。
- 管理员导入：可配置为 `approved`，但默认仍建议走审核。

## 7. 自定义导入接口预留

### 7.1 目标

自定义导入接口用于未来接入不同来源的题库，例如：

- Word 文档
- Excel 表格
- JSON 文件
- 第三方教研系统
- 内部运营后台批量同步

### 7.2 后端接口设计

建议新增统一导入入口：

```http
POST /api/ip/v1/questions/import
Content-Type: multipart/form-data
```

请求字段：

- `file`：导入文件，必填。
- `importType`：导入类型，必填。可选值：`word_table`、`word_section`、`excel`、`json`、`custom`。
- `collegeId`：默认院校 ID，可选。
- `majorId`：默认专业 ID，可选。
- `defaultQuestionType`：默认题型，可选。
- `defaultDifficulty`：默认难度，可选。
- `defaultYear`：默认年份，可选。
- `statusAfterImport`：导入后状态，可选，默认 `pending_review`。
- `dryRun`：是否仅预览不入库，默认 `true`。

响应字段：

```json
{
  "batchId": "imp-20260609-001",
  "status": "PARSED",
  "totalRows": 20,
  "validCount": 18,
  "invalidCount": 2,
  "items": [
    {
      "rowIndex": 1,
      "valid": true,
      "title": "请谈谈你对金融管理专业的理解。",
      "questionType": "专业题",
      "collegeId": 1,
      "majorId": 2,
      "warnings": []
    }
  ],
  "errors": [
    {
      "rowIndex": 7,
      "message": "题目内容为空"
    }
  ]
}
```

确认导入接口：

```http
POST /api/ip/v1/questions/import/{batchId}/confirm
```

用途：

- 将预览结果写入 `question` 表。
- 返回成功插入数量和失败数量。

### 7.3 插件化解析器接口

后端建议抽象：

```java
public interface QuestionImportParser {
    boolean supports(String importType, String filename, String contentType);
    QuestionImportParseResult parse(QuestionImportRequest request);
}
```

第一阶段实现：

- `WordTableQuestionImportParser`
- `WordSectionQuestionImportParser`

后续扩展：

- `ExcelQuestionImportParser`
- `JsonQuestionImportParser`
- `CustomQuestionImportParser`

## 8. 数据模型扩展建议

### 8.1 question 表扩展字段

建议新增：

- `origin_question_id`：扩展题关联的原题 ID。
- `generation_batch_id`：AI 扩题批次 ID。
- `source_type`：来源类型，`manual`、`word_import`、`ai_expanded`、`session_generated`、`legacy_import`。
- `import_batch_id`：导入批次 ID。
- `review_comment`：审核备注。
- `published_at`：发布时间。

### 8.2 新增 question_import_batch 表

字段建议：

- `id`
- `batch_id`
- `file_name`
- `file_url`
- `import_type`
- `college_id`
- `major_id`
- `status`
- `total_count`
- `valid_count`
- `invalid_count`
- `created_by`
- `create_time`
- `update_time`

### 8.3 新增 question_import_item 表

字段建议：

- `id`
- `batch_id`
- `row_index`
- `raw_text`
- `parsed_json`
- `valid`
- `error_message`
- `question_id`
- `create_time`

### 8.4 新增 question_generation_batch 表

字段建议：

- `id`
- `batch_id`
- `college_id`
- `major_id`
- `question_type`
- `ability_tag`
- `source_question_ids`
- `source_outline_ids`
- `generate_count_per_question`
- `status`
- `created_by`
- `create_time`
- `update_time`

## 9. 离线扩题流程

### 9.1 后台入口

老师后台新增“AI 扩展题库”入口。

筛选条件：

- 院校
- 专业
- 年份
- 题型
- 能力点
- 难度
- 原题数量
- 每题生成变体数量
- 是否生成参考答案
- 是否生成评分规则
- 是否生成追问题

### 9.2 素材汇总

后端按条件读取：

- `question` 表中 `approved` 原题。
- `exam_outline` 表中对应考纲。
- `major` 表中的 `testForm`、`testContent`、`scoreStructure`。
- `college` 表中的院校基础信息。

素材包需要控制长度：

- 原题最多 10-20 道。
- 考纲内容做摘要，最多 2000-4000 字。
- 每次 AI 请求按题型或能力点分批，不要一次塞完整题库。

### 9.3 AI 输出要求

AI 必须返回 JSON 数组，每道题包含：

- `title`
- `content`
- `questionType`
- `abilityTag`
- `difficulty`
- `answerTimeSeconds`
- `referenceAnswer`
- `scoringRule`
- `followUpQuestions`
- `sourceRef`
- `originQuestionId`
- `similarityRisk`

其中 `similarityRisk` 用于提示老师该题是否过于接近原题。

### 9.4 审核与发布

扩展题生成后：

- 默认写入 `question` 表，状态为 `pending_review`。
- 老师可以批量编辑。
- 老师可以批量设为 `approved`。
- 被拒绝的题保留 `rejected`，用于后续分析模型问题。

## 10. 学生面试抽题策略

### 10.1 快速抽题优先级

面试开始时，后端按以下优先级抽题：

1. 精确匹配：`collegeId + majorId + questionType + approved`
2. 院校匹配：`collegeId + questionType + approved`
3. 专业类目匹配：`major.category + questionType + approved`
4. 能力点匹配：`abilityTag + questionType + approved`
5. 通用题：`collegeId IS NULL AND majorId IS NULL + questionType + approved`
6. AI 临时生成

### 10.2 防重复策略

建议记录用户历史答题题目 ID。

抽题时：

- 优先排除用户最近 30 天答过的题。
- 如果题量不足，再允许重复，但降低重复题优先级。
- 原题和它的扩展题不要在同一场面试中同时出现。

### 10.3 会话题目快照

无论题目来自题库、扩展题还是临时生成，都要在会话创建时固化为本次会话题目快照。

原因：

- 保证中途刷新或断线恢复时题目不变。
- 保证报告回放和参考答案可追溯。
- 避免题库后续编辑影响历史面试记录。

## 11. 前端改造建议

### 11.1 老师题库管理

新增能力：

- Word 导入按钮。
- 下载 Word 模板。
- 导入预览页。
- 错误行提示。
- 批量确认导入。
- AI 扩题入口。
- 扩展题审核列表。

### 11.2 学生面试入口

已有院校和专业选择能力可继续复用。

新增体验：

- 如果题库充足：直接开始面试。
- 如果题库不足但允许 AI 临时生成：显示“正在生成本次模拟题，预计 10-30 秒”。
- 如果生成失败：提示用户更换题型、专业，或使用通用模拟题。

### 11.3 题库覆盖度提示

在选择院校和专业后，可展示：

- 已审核题目数量。
- 覆盖题型数量。
- 是否支持智能扩展题。
- 是否可能需要等待 AI 生成。

## 12. 后端模块拆分建议

新增或改造模块：

- `QuestionImportController`
- `QuestionImportService`
- `QuestionImportParser`
- `WordTableQuestionImportParser`
- `WordSectionQuestionImportParser`
- `QuestionImportBatchDO`
- `QuestionImportItemDO`
- `QuestionExpansionService`
- `QuestionMaterialPackService`
- `QuestionSelectionStrategy`
- `SessionGeneratedQuestionService`

现有模块改造：

- `QuestionBankController`：保留单题 CRUD 和 AI 生成入口，或拆出导入控制器。
- `QuestionAiGenerateService`：升级为可接收素材包，而不是只接收院校专业名称。
- `QuestionBankServiceImpl.randomSelect(...)`：改为调用分层抽题策略。
- `InterviewSessionServiceImpl.createFromBank(...)`：增加题量不足兜底逻辑。

## 13. API 清单

### 13.1 题库导入

```http
POST /api/ip/v1/questions/import
POST /api/ip/v1/questions/import/{batchId}/confirm
GET  /api/ip/v1/questions/import/{batchId}
GET  /api/ip/v1/questions/import/{batchId}/items
```

### 13.2 AI 扩题

```http
POST /api/ip/v1/questions/expansion-batches
GET  /api/ip/v1/questions/expansion-batches/{batchId}
GET  /api/ip/v1/questions/expansion-batches/{batchId}/items
PUT  /api/ip/v1/questions/{id}/status
```

### 13.3 题库覆盖度

```http
GET /api/ip/v1/questions/coverage?collegeId=1&majorId=2&interviewMode=专业题&abilityTag=communication&difficulty=hard
```

返回：

```json
{
  "collegeId": 1,
  "majorId": 2,
  "interviewMode": "专业认知",
  "approvedCount": 42,
  "exactMatchCount": 18,
  "fallbackCount": 24,
  "canStartImmediately": true,
  "mayNeedAiGeneration": false
}
```

## 14. 测试计划

### 14.1 单元测试

- Word 表格模板解析成功。
- Word 分段模板解析成功。
- 缺少题目内容时返回错误行。
- 院校和专业名称能正确映射为 ID。
- 导入预览 dry-run 不写入 `question` 表。
- 确认导入后写入 `question` 表。
- 扩展题生成后默认 `pending_review`。
- 分层抽题优先返回精确匹配题。
- 题量不足时触发回退题。
- 题量仍不足时触发 AI 临时生成。

### 14.2 集成测试

- 老师上传 Word 文档，预览、确认、题库列表可查询。
- 老师发起 AI 扩题，生成结果进入审核列表。
- 学生选择院校和专业后能秒开面试。
- 冷门专业题量不足时，前端显示生成中，完成后进入面试。
- 面试完成后报告能显示题目、回答、评分规则和参考答案。

### 14.3 人工验收

- 用一个真实 Word 题库导入 20 道题，至少 18 道能自动解析。
- 对一个只有 5 道原题的专业生成至少 15 道扩展题。
- 老师审核通过后，学生连续发起 3 次面试，题目组合不完全重复。
- 普通题库充足的专业，创建面试时间小于 2 秒。
- 题库不足且触发 AI 临时生成的专业，前端有明确等待状态和失败兜底。

## 15. 风险与处理

### 15.1 Word 格式不统一

风险：老师上传的 Word 可能没有表格、标题不规范、内容混杂。

处理：

- 提供标准模板下载。
- 第一阶段支持表格模板和分段模板。
- 无法识别的内容进入预览错误列表。
- 不自动丢弃原文，保留 `raw_text` 便于人工修正。

### 15.2 AI 扩题质量不稳定

风险：AI 可能生成过于相似、过难或脱离考纲的题。

处理：

- 默认 `pending_review`。
- prompt 中明确禁止复述原题。
- 输出 `similarityRisk`。
- 老师审核后才能发布。

### 15.3 token 成本过高

风险：一次性塞入过多题库和考纲会很贵。

处理：

- 离线分批扩题。
- 素材包限制长度。
- 优先使用已审核扩展题。
- 会话实时生成只作为题量不足兜底。

### 15.4 面试开始等待过久

风险：实时生成题会影响体验。

处理：

- 题库充足时只抽题，不生成。
- 提前离线扩题。
- 题量不足时才显示生成中。
- 生成失败时回退通用题。

## 16. 分阶段里程碑

### M1：Word 导入与题库预览

- 完成 `.docx` 表格模板解析。
- 完成 `.docx` 分段模板解析。
- 完成 dry-run 预览。
- 完成确认导入。
- 老师可在题库列表看到导入题。

### M2：分层抽题与题库覆盖度

- 完成题库覆盖度接口。
- 完成分层抽题策略。
- 完成防重复策略基础版。
- 学生题库面试继续保持快速启动。

### M3：离线 AI 扩题

- 完成素材包汇总。
- 完成 AI 扩题接口。
- 完成扩展题批次记录。
- 完成扩展题审核入口。

### M4：题量不足兜底生成

- 完成会话临时生成题。
- 前端显示生成中状态。
- 生成结果固化到会话快照。
- 临时题可转存到题库审核。

## 17. 成功指标

- 老师可通过 Word 一次导入至少 20 道题。
- 标准模板导入成功率达到 95% 以上。
- 普通题库面试创建时间小于 2 秒。
- 每个院校专业的可用题量能通过离线扩题提升 3 倍以上。
- 学生连续多次面试题目重复率明显下降。
- AI 生成题必须有来源依据和审核状态。
- 题库不足时用户能看到明确等待状态，不出现无响应。

## 18. 建议优先级

推荐先做：

1. Word 导入接口和模板。
2. 题库覆盖度统计。
3. 分层抽题策略。
4. 离线 AI 扩题和审核。

暂缓：

1. 第三方题库系统同步。
2. 复杂机构权限。
3. 每场面试实时大规模生成。
4. RAG、向量数据库和 embedding 检索链路。

## 19. 结论

最适合当前项目的路径是先把题库建设、导入、扩题、审核和快速抽题闭环跑通，不在本期建设 RAG。

题库负责稳定性，离线扩题负责丰富度，审核负责质量，临时生成负责冷门场景兜底。这样既能提升学生体验，也能控制 token 成本，并避免过早引入向量检索带来的复杂度。
