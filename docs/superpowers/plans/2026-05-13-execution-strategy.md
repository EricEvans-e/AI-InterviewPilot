# 浙江高职提前招生在线模拟面试系统 — 执行策略

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于实施计划，制定可落地的执行策略，确保 24 个任务按依赖关系高效推进，同时控制风险、保证质量。

**Architecture:** 采用分层执行策略：先基础设施（数据库 + 角色），再核心业务（题库 + 面试），最后前端展示。每层完成后进行集成验证，确保增量可用。

**Tech Stack:** 与实施计划一致

**适用范围:** 实施计划的执行指导，不含具体代码（代码见实施计划）

---

## 1. 执行总览

### 1.1 任务依赖图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          任务依赖关系图                                   │
└─────────────────────────────────────────────────────────────────────────┘

Phase 1: 基础设施层 (后端)
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ Task 1  │───▶│ Task 2  │───▶│ Task 3  │───▶│ Task 4  │
│ 建表    │    │ 角色体系│    │ 手机登录│    │ 学生档案│
└─────────┘    └─────────┘    └─────────┘    └─────────┘
     │              │              │              │
     ▼              ▼              ▼              ▼
  [DDL执行]    [编译验证]    [编译验证]    [编译验证]

Phase 2: 业务数据层 (后端)
┌─────────┐    ┌─────────┐
│ Task 5  │───▶│ Task 6  │
│ 院校管理│    │ 题库管理│
└─────────┘    └─────────┘
     │              │
     ▼              ▼
  [编译验证]    [编译验证]

Phase 3: 核心逻辑层 (后端)
┌─────────┐    ┌─────────┐    ┌─────────┐
│ Task 7  │───▶│ Task 8  │───▶│ Task 9  │
│ 面试改造│    │ 7维评分 │    │ 教师点评│
└─────────┘    └─────────┘    └─────────┘
     │              │              │
     ▼              ▼              ▼
  [编译验证]    [编译验证]    [编译验证]

Phase 4: 前端基础层
┌─────────┐    ┌─────────┐    ┌─────────┐
│ Task 10 │───▶│ Task 11 │───▶│ Task 12 │
│ 路由重构│    │ 手机UI  │    │ 学生档案│
└─────────┘    └─────────┘    └─────────┘
     │              │              │
     ▼              ▼              ▼
  [类型检查]    [本地验证]    [类型检查]

Phase 5: 前端核心层
┌─────────┐    ┌─────────┐    ┌─────────┐
│ Task 13 │───▶│ Task 14 │───▶│ Task 15 │
│ 面试大厅│    │ 设备预检│    │ 数字人  │
└─────────┘    └─────────┘    └─────────┘
     │              │              │
     ▼              ▼              ▼
  [类型检查]    [类型检查]    [类型检查]

Phase 6: 前端管理端
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ Task 16 │───▶│ Task 17 │───▶│ Task 18 │───▶│ Task 19 │
│ 教师Layout│  │ 学生报告│    │ 院校管理│    │ 管理员  │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
     │              │              │              │
     ▼              ▼              ▼              ▼
  [类型检查]    [类型检查]    [类型检查]    [类型检查]

Phase 7: 报告增强 (前端)
┌─────────┐    ┌─────────┐
│ Task 20 │───▶│ Task 21 │
│ 7维展示 │    │ 礼仪+点评│
└─────────┘    └─────────┘
     │              │
     ▼              ▼
  [类型检查]    [类型检查]

Phase 8: 集成收尾
┌─────────┐    ┌─────────┐    ┌─────────┐
│ Task 22 │───▶│ Task 23 │───▶│ Task 24 │
│ 登录跳转│    │ 导航适配│    │ 全量验证│
└─────────┘    └─────────┘    └─────────┘
     │              │              │
     ▼              ▼              ▼
  [类型检查]    [类型检查]    [端到端测试]
```

### 1.2 关键路径

**后端关键路径:** Task 1 → Task 2 → Task 3 → Task 7 → Task 8 → Task 9
- 这是后端最长依赖链，决定了后端可用时间点
- 总计约 6 个任务，每个任务约 2-4 小时

**前端关键路径:** Task 10 → Task 13 → Task 14 → Task 15 → Task 20 → Task 21 → Task 24
- 这是前端最长依赖链，决定了前端可用时间点
- 总计约 7 个任务，每个任务约 2-4 小时

### 1.3 并行执行机会

**后端并行:**
- Task 1-4 (Phase 1) 必须串行
- Task 5-6 (Phase 2) 可与 Task 3-4 (Phase 1) 并行
- Task 7-9 (Phase 3) 必须串行

**前端并行:**
- Task 10-12 (Phase 4) 必须串行
- Task 13-15 (Phase 5) 可与 Task 16-19 (Phase 6) 并行
- Task 20-21 (Phase 7) 可与 Task 22-23 (Phase 8) 并行

**跨端并行:**
- Phase 1 (后端) 可与 Phase 4 (前端) 并行，只要 Task 10 不依赖后端 API
- Phase 2 (后端) 可与 Phase 5 (前端) 并行
- Phase 3 (后端) 可与 Phase 6 (前端) 并行

---

## 2. 执行阶段详解

### 2.1 阶段一：基础设施层 (Day 1-2)

**目标:** 建立数据库表结构、角色体系、登录机制、学生档案

**任务清单:**
- [ ] Task 1: 新建 MySQL 表结构
- [ ] Task 2: 扩展用户角色体系
- [ ] Task 3: 手机号短信验证码登录接口
- [ ] Task 4: 学生档案 CRUD 后端

**执行策略:**

1. **Task 1 (建表)**
   - 执行 DDL 脚本
   - 验证所有表创建成功
   - 记录表结构供后续开发参考

2. **Task 2 (角色体系)**
   - 修改 UserDO 增加 role/openId 字段
   - 修改 StpInterfaceImpl 支持三角色
   - 验证编译通过

3. **Task 3 (手机登录)**
   - 创建 SmsCodeDO/Mapper/Service
   - 创建 UserPhoneLoginReqDTO
   - 在 UserController 新增接口
   - 在白名单中添加新接口
   - 验证编译通过

4. **Task 4 (学生档案)**
   - 创建 StudentProfileDO/Mapper/Service
   - 创建 StudentProfileController
   - 验证编译通过

**验证点:**
- [ ] 所有新表创建成功
- [ ] 角色体系扩展编译通过
- [ ] 手机登录接口编译通过
- [ ] 学生档案 CRUD 编译通过

**风险点:**
- DDL 执行失败（表已存在、字段冲突）
- StpInterfaceImpl 修改影响现有登录逻辑
- 手机登录白名单配置遗漏

**回滚方案:**
- DDL 失败：检查表结构，手动修复或重建
- 角色体系问题：回退 StpInterfaceImpl 修改
- 登录问题：检查白名单配置，补充遗漏路径

---

### 2.2 阶段二：业务数据层 (Day 2-3)

**目标:** 建立院校、专业、考纲、题库的 CRUD 体系

**任务清单:**
- [ ] Task 5: 院校管理 CRUD
- [ ] Task 6: 题库管理 CRUD + AI 扩展出题

**执行策略:**

1. **Task 5 (院校管理)**
   - 创建 CollegeDO/MajorDO/ExamOutlineDO 实体
   - 创建对应 Mapper
   - 创建 CollegeService/MajorService/ExamOutlineService
   - 创建 CollegeController/MajorController/ExamOutlineController
   - 验证编译通过

2. **Task 6 (题库管理)**
   - 创建 QuestionDO 实体
   - 创建 QuestionMapper + QuestionBankService
   - 创建 QuestionAiGenerateService
   - 创建 QuestionBankController
   - 验证编译通过

**验证点:**
- [ ] 院校 CRUD 接口编译通过
- [ ] 题库 CRUD 接口编译通过
- [ ] AI 出题接口编译通过

**风险点:**
- 院校/专业关联关系设计不合理
- 题库筛选条件过多导致查询性能问题
- AI 出题 prompt 效果不佳

**回滚方案:**
- 院校管理问题：简化关联关系，使用单表设计
- 题库性能问题：添加索引，优化查询逻辑
- AI 出题问题：调整 prompt，增加示例

---

### 2.3 阶段三：核心逻辑层 (Day 3-5)

**目标:** 实现面试流程改造、7 维评分引擎、教师点评

**任务清单:**
- [ ] Task 7: 扩展面试会话支持题库模式
- [ ] Task 8: 7 维评分引擎
- [ ] Task 9: 教师点评后端

**执行策略:**

1. **Task 7 (面试改造)**
   - 扩展 InterviewSession 字段
   - 扩展 InterviewRecordDO 字段
   - 在 InterviewSessionController 新增题库模式创建接口
   - 在 InterviewSessionFacade 实现 createFromBank 方法
   - 修改答题流程支持题库模式
   - 验证编译通过

2. **Task 8 (7 维评分)**
   - 创建 DimensionScoreResult
   - 创建 ScoringWeightConfigDO
   - 创建 DimensionScoreStrategy
   - 修改 RadarChartDTO 扩展为 7 维度
   - 修改 WeightedRadarComputationStrategy 适配新维度
   - 在面试完成时调用 DimensionScoreStrategy
   - 验证编译通过

3. **Task 9 (教师点评)**
   - 创建 TeacherReviewDO
   - 创建 TeacherReviewService
   - 创建 TeacherReportController
   - 验证编译通过

**验证点:**
- [ ] 面试会话支持题库模式编译通过
- [ ] 7 维评分引擎编译通过
- [ ] 教师点评后端编译通过

**风险点:**
- 面试流程改造影响现有逻辑
- 7 维评分算法权重配置不合理
- 教师点评与面试记录关联问题

**回滚方案:**
- 面试流程问题：保留 sessionMode 字段，默认使用 resume 模式
- 评分算法问题：调整权重配置，使用默认权重
- 点评关联问题：检查 sessionId 关联，修复查询逻辑

---

### 2.4 阶段四：前端基础层 (Day 4-6)

**目标:** 建立前端多角色路由、手机号登录 UI、学生档案页面

**任务清单:**
- [ ] Task 10: 前端角色体系 + 路由重构
- [ ] Task 11: 手机号登录前端 UI
- [ ] Task 12: 学生个人中心页面

**执行策略:**

1. **Task 10 (路由重构)**
   - 扩展 UserRespDTO 类型
   - userSlice 增加 role 选择器
   - authService 增加手机号登录方法
   - 重构 AuthGuard 支持角色路由
   - 重构 router.tsx 增加教师/管理员路由
   - 验证类型检查通过

2. **Task 11 (手机 UI)**
   - 在 AuthFormCard 中增加手机号登录 Tab
   - 实现 handlePhoneLogin 和 handleSendCode
   - 本地验证 UI

3. **Task 12 (学生档案)**
   - 创建 studentService.ts
   - 创建 useStudentProfile hook
   - 创建 StudentProfilePage
   - 验证类型检查通过

**验证点:**
- [ ] 多角色路由类型检查通过
- [ ] 手机号登录 UI 本地验证通过
- [ ] 学生档案页面类型检查通过

**风险点:**
- 路由重构影响现有页面
- 手机号登录逻辑与后端不匹配
- 学生档案表单校验规则不合理

**回滚方案:**
- 路由问题：保留原有路由，新增路由使用条件渲染
- 登录问题：检查 API 路径和参数格式
- 档案问题：调整表单校验规则，简化必填项

---

### 2.5 阶段五：前端核心层 (Day 6-8)

**目标:** 实现面试大厅、设备预检、数字人 MVP

**任务清单:**
- [ ] Task 13: 面试大厅页面
- [ ] Task 14: 设备预检页面
- [ ] Task 15: 数字人 MVP

**执行策略:**

1. **Task 13 (面试大厅)**
   - 创建 questionBankService.ts
   - 创建 LobbyFilterBar 组件
   - 创建 LobbyCard 组件
   - 创建 LobbyGrid 组件
   - 创建 LobbyPage
   - 验证类型检查通过

2. **Task 14 (设备预检)**
   - 创建 InterviewPrecheckPage
   - 实现摄像头/麦克风检测
   - 实现亮度检测
   - 验证类型检查通过

3. **Task 15 (数字人)**
   - 创建 DigitalHumanAvatar 组件
   - 在 InterviewPage 中集成数字人
   - 在 useInterviewSessionFlow 中集成 TTS 播放
   - 验证类型检查通过

**验证点:**
- [ ] 面试大厅页面类型检查通过
- [ ] 设备预检页面类型检查通过
- [ ] 数字人集成类型检查通过

**风险点:**
- 面试大厅筛选逻辑复杂
- 设备预检浏览器兼容性问题
- 数字人动画效果不佳

**回滚方案:**
- 筛选问题：简化筛选条件，使用默认筛选
- 兼容性问题：降级处理，提示用户使用推荐浏览器
- 动画问题：使用简单 CSS 动画，不依赖 Lottie

---

### 2.6 阶段六：前端管理端 (Day 8-10)

**目标:** 实现教师端、管理员端完整功能

**任务清单:**
- [ ] Task 16: 教师端 Layout + 题库管理页面
- [ ] Task 17: 教师端 — 学生报告查看 + 点评
- [ ] Task 18: 教师端 — 院校/专业/考纲管理
- [ ] Task 19: 管理员端 Layout + 用户管理 + 数据看板

**执行策略:**

1. **Task 16 (教师 Layout + 题库)**
   - 创建 TeacherLayout
   - 创建 teacherService.ts
   - 创建 QuestionBankTable + QuestionFormDialog + AiGenerateDialog
   - 创建 TeacherQuestionBankPage
   - 验证类型检查通过

2. **Task 17 (学生报告)**
   - 创建 StudentReportList
   - 创建 TeacherReviewForm
   - 创建 TeacherStudentReportsPage
   - 验证类型检查通过

3. **Task 18 (院校管理)**
   - 创建 collegeService.ts
   - 创建 TeacherCollegeManagePage
   - 验证类型检查通过

4. **Task 19 (管理员)**
   - 创建 AdminLayout
   - 创建 AdminUserManagePage
   - 创建 AdminDashboardPage
   - 验证类型检查通过

**验证点:**
- [ ] 教师端 Layout 类型检查通过
- [ ] 学生报告页面类型检查通过
- [ ] 院校管理页面类型检查通过
- [ ] 管理员端类型检查通过

**风险点:**
- 教师端 Layout 与现有 Layout 冲突
- 学生报告数据量大导致性能问题
- 管理员数据看板统计逻辑复杂

**回滚方案:**
- Layout 冲突：使用独立路由，不嵌套现有 Layout
- 性能问题：分页加载，虚拟滚动
- 统计逻辑：简化统计，使用缓存

---

### 2.7 阶段七：报告增强 (Day 10-11)

**目标:** 增强面试报告展示，添加 7 维评分、礼仪反馈、教师点评

**任务清单:**
- [ ] Task 20: 面试报告 7 维评分展示
- [ ] Task 21: 面试报告 — 礼仪反馈 + 参考答案 + 教师点评展示

**执行策略:**

1. **Task 20 (7 维展示)**
   - 扩展 InterviewRadarMetric 类型
   - 修改 InterviewScoreAndRadarCard 展示 7 维度
   - 修改 normalizeInterviewRadarChart 解析新字段
   - 修改 InterviewReportPage 传递新数据
   - 验证类型检查通过

2. **Task 21 (礼仪+点评)**
   - 创建 InterviewEtiquetteCard
   - 创建 InterviewReferenceAnswerCard
   - 创建 InterviewTeacherReviewCard
   - 在 InterviewReportPage 中集成新组件
   - 验证类型检查通过

**验证点:**
- [ ] 7 维评分展示类型检查通过
- [ ] 礼仪反馈展示类型检查通过
- [ ] 教师点评展示类型检查通过

**风险点:**
- 7 维评分数据格式不匹配
- 礼仪反馈数据来源不稳定
- 教师点评加载性能问题

**回滚方案:**
- 数据格式问题：使用默认值填充，降级展示
- 数据来源问题：显示"暂无数据"提示
- 性能问题：分页加载，懒加载

---

### 2.8 阶段八：集成收尾 (Day 11-12)

**目标:** 完成登录跳转、导航适配、全量验证

**任务清单:**
- [ ] Task 22: 登录页默认跳转逻辑适配
- [ ] Task 23: 首页营销页 + 导航适配
- [ ] Task 24: 全量编译 + 测试 + 部署验证

**执行策略:**

1. **Task 22 (登录跳转)**
   - 修改登录成功后的跳转逻辑
   - 修改营销首页 CTA 按钮
   - 验证类型检查通过

2. **Task 23 (导航适配)**
   - 侧边栏增加学生导航
   - 验证类型检查通过

3. **Task 24 (全量验证)**
   - 后端全量编译
   - 前端全量检查
   - 数据库初始化
   - 启动后端 + 前端，手动验证核心流程

**验证点:**
- [ ] 登录跳转逻辑类型检查通过
- [ ] 导航适配类型检查通过
- [ ] 后端编译成功
- [ ] 前端检查通过
- [ ] 核心流程手动验证通过

**风险点:**
- 登录跳转逻辑与现有逻辑冲突
- 导航适配影响现有页面
- 全量验证发现集成问题

**回滚方案:**
- 跳转逻辑：保留原有跳转，新增角色判断条件
- 导航问题：保留原有导航，新增入口使用独立组件
- 集成问题：定位问题模块，逐个修复

---

## 3. 执行工具与环境

### 3.1 开发环境要求

**后端:**
- Java 17
- Maven 3.6.3+
- Docker Compose (MySQL, MongoDB, Redis)
- IDE: IntelliJ IDEA / VS Code

**前端:**
- Node.js 20+
- npm 9+
- IDE: VS Code

### 3.2 执行工具

**后端执行:**
```bash
# 编译验证
./mvnw -B -ntp clean compile -Dmaven.test.skip=true

# 运行测试
./mvnw test

# 启动应用
./mvnw spring-boot:run -pl admin
```

**前端执行:**
```bash
# 类型检查
npm run typecheck

# 代码检查
npm run lint

# 构建
npm run build

# 启动开发服务器
npm run dev
```

### 3.3 版本控制策略

**分支策略:**
- `main`: 生产分支，只接受 PR
- `develop`: 开发分支，日常提交
- `feature/*`: 功能分支，每个任务一个分支

**提交规范:**
```
feat(scope): description
fix(scope): description
refactor(scope): description
test(scope): description
docs(scope): description
```

**PR 流程:**
1. 从 develop 创建 feature 分支
2. 完成任务后提交 PR
3. 代码审查（自动或人工）
4. 合并到 develop
5. 删除 feature 分支

---

## 4. 质量保障

### 4.1 代码质量检查

**后端:**
- 编译检查：`./mvnw clean compile`
- 单元测试：`./mvnw test`
- 代码规范：Checkstyle / SpotBugs (可选)

**前端:**
- 类型检查：`npm run typecheck`
- 代码规范：`npm run lint`
- 构建检查：`npm run build`

### 4.2 测试策略

**后端测试:**
- 单元测试：Service 层逻辑
- 集成测试：Controller 层接口
- API 测试：Postman / curl

**前端测试:**
- 组件测试：Vitest + React Testing Library
- 集成测试：Cypress / Playwright (可选)
- 手动测试：浏览器验证

### 4.3 验收标准

**后端验收:**
- [ ] 所有接口编译通过
- [ ] 核心接口单元测试通过
- [ ] API 文档更新

**前端验收:**
- [ ] 类型检查通过
- [ ] 代码规范检查通过
- [ ] 核心页面手动验证通过
- [ ] 响应式布局验证

---

## 5. 风险管理

### 5.1 技术风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| DDL 执行失败 | 高 | 低 | 预先测试 DDL，备份数据库 |
| 角色体系影响现有逻辑 | 高 | 中 | 保留兼容逻辑，渐进式迁移 |
| 面试流程改造破坏现有功能 | 高 | 中 | 使用 sessionMode 字段隔离 |
| 前端路由重构影响现有页面 | 中 | 中 | 保留原有路由，新增路由独立 |
| AI 出题效果不佳 | 中 | 中 | 调整 prompt，增加示例 |
| 设备预检浏览器兼容性 | 低 | 高 | 降级处理，提示用户 |

### 5.2 进度风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 任务复杂度超预期 | 高 | 中 | 预留缓冲时间，及时调整计划 |
| 依赖问题阻塞 | 中 | 中 | 提前识别依赖，准备备选方案 |
| 人员不足 | 高 | 低 | 优先核心功能，推迟非核心功能 |

### 5.3 质量风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 代码质量不达标 | 中 | 中 | 代码审查，自动化检查 |
| 测试覆盖不足 | 中 | 中 | 核心功能必须测试 |
| 性能问题 | 中 | 低 | 性能测试，优化关键路径 |

---

## 6. 沟通与协作

### 6.1 进度跟踪

**每日站会:**
- 昨天完成了什么
- 今天计划做什么
- 遇到什么阻塞

**进度看板:**
- TODO: 待办任务
- IN PROGRESS: 进行中任务
- DONE: 已完成任务

### 6.2 问题升级

**技术问题:**
1. 尝试自行解决（30 分钟）
2. 查阅文档和社区
3. 询问团队成员
4. 升级到技术负责人

**进度问题:**
1. 识别阻塞原因
2. 尝试调整计划
3. 申请额外资源
4. 升级到项目经理

### 6.3 知识共享

**文档:**
- API 文档：Swagger / OpenAPI
- 架构文档：README.md
- 开发文档：CLAUDE.md

**代码审查:**
- 每个 PR 必须经过审查
- 审查重点：代码质量、安全性、性能
- 审查时间：24 小时内完成

---

## 7. 里程碑与交付物

### 7.1 里程碑

| 里程碑 | 时间 | 交付物 | 验收标准 |
|--------|------|--------|----------|
| M1: 基础设施层 | Day 2 | 数据库表、角色体系、登录接口 | 后端编译通过 |
| M2: 业务数据层 | Day 3 | 院校/题库 CRUD | 后端编译通过 |
| M3: 核心逻辑层 | Day 5 | 面试流程、7维评分、教师点评 | 后端编译通过 |
| M4: 前端基础层 | Day 6 | 路由、登录、档案 | 前端类型检查通过 |
| M5: 前端核心层 | Day 8 | 大厅、预检、数字人 | 前端类型检查通过 |
| M6: 前端管理端 | Day 10 | 教师端、管理员端 | 前端类型检查通过 |
| M7: 报告增强 | Day 11 | 7维展示、礼仪反馈、教师点评 | 前端类型检查通过 |
| M8: 集成收尾 | Day 12 | 全量验证 | 端到端测试通过 |

### 7.2 交付物清单

**后端交付物:**
- [ ] 数据库 DDL 脚本
- [ ] 新增/修改的 Java 代码
- [ ] API 接口文档
- [ ] 单元测试代码

**前端交付物:**
- [ ] 新增/修改的 TypeScript/React 代码
- [ ] 组件文档
- [ ] 类型定义文件
- [ ] 构建产物

**文档交付物:**
- [ ] 实施计划
- [ ] 执行策略
- [ ] API 文档
- [ ] 用户手册 (可选)

---

## 8. 附录

### 8.1 任务时间估算

| 任务 | 预估时间 | 实际时间 | 备注 |
|------|----------|----------|------|
| Task 1 | 2h | - | DDL 执行 + 验证 |
| Task 2 | 2h | - | 角色体系扩展 |
| Task 3 | 3h | - | 手机登录完整实现 |
| Task 4 | 2h | - | 学生档案 CRUD |
| Task 5 | 3h | - | 院校管理 CRUD |
| Task 6 | 4h | - | 题库管理 + AI 出题 |
| Task 7 | 4h | - | 面试流程改造 |
| Task 8 | 4h | - | 7 维评分引擎 |
| Task 9 | 2h | - | 教师点评后端 |
| Task 10 | 3h | - | 前端路由重构 |
| Task 11 | 2h | - | 手机号 UI |
| Task 12 | 2h | - | 学生档案页面 |
| Task 13 | 3h | - | 面试大厅页面 |
| Task 14 | 2h | - | 设备预检页面 |
| Task 15 | 3h | - | 数字人 MVP |
| Task 16 | 3h | - | 教师 Layout + 题库 |
| Task 17 | 2h | - | 学生报告 + 点评 |
| Task 18 | 2h | - | 院校管理页面 |
| Task 19 | 3h | - | 管理员端 |
| Task 20 | 2h | - | 7 维展示 |
| Task 21 | 2h | - | 礼仪 + 点评展示 |
| Task 22 | 1h | - | 登录跳转 |
| Task 23 | 1h | - | 导航适配 |
| Task 24 | 2h | - | 全量验证 |
| **总计** | **58h** | - | 约 7-8 个工作日 |

### 8.2 优先级排序

**P0 (必须完成):**
- Task 1-4: 基础设施层
- Task 7-8: 面试流程 + 评分
- Task 10-12: 前端基础层
- Task 13-15: 前端核心层
- Task 24: 全量验证

**P1 (应该完成):**
- Task 5-6: 院校/题库管理
- Task 9: 教师点评
- Task 16-17: 教师端核心
- Task 20-21: 报告增强

**P2 (可以推迟):**
- Task 18: 院校管理页面
- Task 19: 管理员端
- Task 22-23: 导航优化

### 8.3 参考资料

- 实施计划: `docs/superpowers/plans/2026-05-13-zhejiang-vocational-interview-system.md`
- PRD 文档: `PRD.md`
- CLAUDE.md: `CLAUDE.md`
- 后端 README: `AI-Meeting/README.md`
- 前端 README: `AI-Meeting-Frontend/README.md`

---

**文档版本:** 1.0
**创建时间:** 2026-05-13
**最后更新:** 2026-05-13
**作者:** AI Assistant
**审核人:** 待定
