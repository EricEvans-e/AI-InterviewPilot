# 浙江高职提前招生在线模拟面试系统 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于现有 AI-Meeting 代码库，构建面向浙江高职提前招生的在线模拟面试系统 V1.0 MVP，包含学生端面试闭环、教师题库后台、7 维评分体系和基础管理员功能。

**Architecture:** 保留现有 Spring Boot + Sa-Token + 讯飞 ASR/TTS + LiteFlow 面试引擎基础设施，新增结构化题库体系（MySQL）、院校/专业/考纲数据库、三角色权限模型（student/teacher/admin）、7 维可配置评分引擎、教师/管理员前端。数字人 MVP 阶段用 2D 动画 + TTS，后期升级 3D。

**Tech Stack:** Java 17, Spring Boot 3.4.4, Spring AI 1.0, MyBatis-Plus 3.5.9, MySQL 8.4, MongoDB 7.0, Redis 7.2 (Redisson), Sa-Token 1.39, LiteFlow 2.15, Resilience4j 2.2, 讯飞 WebSDK, React 19, TypeScript 5.9, Vite 7.3, Redux Toolkit, React Query, shadcn/ui, Tailwind CSS

**适用范围:** PRD V1.0 MVP（单人模拟面试闭环），不含无领导小组讨论、订单系统、小程序/App。

---

## 文件结构总览

### 后端新增/修改文件

```
admin/src/main/java/com/hewei/hzyjy/interviewpilot/
├── common/config/satoken/StpInterfaceImpl.java          [MODIFY] 三角色
├── user/
│   ├── api/UserController.java                          [MODIFY] 手机号登录
│   ├── api/io/req/UserPhoneLoginReqDTO.java             [NEW]
│   ├── dao/entity/UserDO.java                           [MODIFY] 增加 role/openId
│   ├── service/UserService.java                         [MODIFY]
│   └── service/impl/UserServiceImpl.java                [MODIFY]
├── student/                                             [NEW DOMAIN]
│   ├── api/StudentProfileController.java                [NEW]
│   ├── api/io/req/StudentProfileSaveReqDTO.java         [NEW]
│   ├── api/io/resp/StudentProfileRespDTO.java           [NEW]
│   ├── dao/entity/StudentProfileDO.java                 [NEW]
│   ├── dao/mapper/StudentProfileMapper.java             [NEW]
│   └── service/StudentProfileService.java               [NEW]
├── questionbank/                                        [NEW DOMAIN]
│   ├── api/
│   │   ├── CollegeController.java                       [NEW]
│   │   ├── MajorController.java                         [NEW]
│   │   ├── ExamOutlineController.java                   [NEW]
│   │   └── QuestionBankController.java                  [NEW]
│   ├── api/io/
│   │   ├── req/CollegeSaveReqDTO.java                   [NEW]
│   │   ├── req/MajorSaveReqDTO.java                     [NEW]
│   │   ├── req/QuestionSaveReqDTO.java                  [NEW]
│   │   ├── req/QuestionGenerateReqDTO.java              [NEW]
│   │   ├── resp/CollegeRespDTO.java                     [NEW]
│   │   ├── resp/MajorRespDTO.java                       [NEW]
│   │   ├── resp/QuestionRespDTO.java                    [NEW]
│   │   └── resp/QuestionBankPageRespDTO.java            [NEW]
│   ├── dao/entity/
│   │   ├── CollegeDO.java                               [NEW]
│   │   ├── MajorDO.java                                 [NEW]
│   │   ├── ExamOutlineDO.java                           [NEW]
│   │   └── QuestionDO.java                              [NEW]
│   ├── dao/mapper/
│   │   ├── CollegeMapper.java                           [NEW]
│   │   ├── MajorMapper.java                             [NEW]
│   │   ├── ExamOutlineMapper.java                       [NEW]
│   │   └── QuestionMapper.java                          [NEW]
│   └── service/
│       ├── CollegeService.java                          [NEW]
│       ├── MajorService.java                            [NEW]
│       ├── ExamOutlineService.java                      [NEW]
│       ├── QuestionBankService.java                     [NEW]
│       └── QuestionAiGenerateService.java               [NEW]
├── interview/
│   ├── api/InterviewSessionController.java              [MODIFY] 新增从题库创建会话
│   ├── api/io/resp/RadarChartDTO.java                   [MODIFY] 7 维度
│   ├── application/flow/InterviewFlowStateMachine.java  [MODIFY] 增加 PREPARING 状态
│   ├── application/flow/InterviewFlowStatus.java        [MODIFY] 增加 PREPARING
│   ├── application/strategy/
│   │   ├── WeightedRadarComputationStrategy.java        [MODIFY] 7 维评分
│   │   └── DimensionScoreStrategy.java                  [NEW]
│   ├── dao/entity/
│   │   ├── InterviewSession.java                        [MODIFY] 增加 collegeId/majorId/questionBankMode
│   │   └── InterviewRecordDO.java                       [MODIFY] 增加 7 维分数字段
│   └── service/InterviewQuestionService.java            [MODIFY] 从题库抽题逻辑
└── teacher/                                             [NEW DOMAIN]
    ├── api/TeacherReportController.java                 [NEW]
    ├── api/TeacherReviewController.java                 [NEW]
    └── service/TeacherReviewService.java                [NEW]
```

### 前端新增/修改文件

```
AI-Meeting-Frontend/src/
├── app/router.tsx                                       [MODIFY] 多角色路由
├── store/slices/userSlice.ts                            [MODIFY] 增加 role 字段
├── services/
│   ├── authService.ts                                   [MODIFY] 手机号登录
│   ├── studentService.ts                                [NEW]
│   ├── questionBankService.ts                           [NEW]
│   ├── collegeService.ts                                [NEW]
│   └── teacherService.ts                                [NEW]
├── components/
│   ├── ui/                                              [MODIFY] 新增 shadcn 组件
│   ├── layout/
│   │   ├── TeacherLayout.tsx                            [NEW]
│   │   └── AdminLayout.tsx                              [NEW]
│   ├── lobby/                                           [NEW]
│   │   ├── LobbyFilterBar.tsx                           [NEW]
│   │   ├── LobbyCard.tsx                                [NEW]
│   │   └── LobbyGrid.tsx                                [NEW]
│   ├── interview/
│   │   ├── InterviewPrecheckPage.tsx                    [NEW]
│   │   └── DigitalHumanAvatar.tsx                       [NEW]
│   ├── teacher/                                         [NEW]
│   │   ├── QuestionBankTable.tsx                        [NEW]
│   │   ├── QuestionFormDialog.tsx                       [NEW]
│   │   ├── AiGenerateDialog.tsx                         [NEW]
│   │   ├── StudentReportList.tsx                        [NEW]
│   │   └── TeacherReviewForm.tsx                        [NEW]
│   └── admin/                                           [NEW]
│       ├── UserManageTable.tsx                          [NEW]
│       └── DashboardStats.tsx                           [NEW]
├── pages/
│   ├── lobby/LobbyPage.tsx                              [NEW]
│   ├── interview/InterviewPrecheckPage.tsx              [NEW]
│   ├── profile/StudentProfilePage.tsx                   [NEW]
│   ├── teacher/
│   │   ├── TeacherDashboardPage.tsx                     [NEW]
│   │   ├── TeacherQuestionBankPage.tsx                  [NEW]
│   │   ├── TeacherStudentReportsPage.tsx                [NEW]
│   │   └── TeacherCollegeManagePage.tsx                 [NEW]
│   └── admin/
│       ├── AdminDashboardPage.tsx                       [NEW]
│       ├── AdminUserManagePage.tsx                      [NEW]
│       └── AdminContentSafetyPage.tsx                   [NEW]
└── hooks/
    ├── lobby/useLobbyData.ts                            [NEW]
    ├── profile/useStudentProfile.ts                     [NEW]
    └── teacher/useTeacherQuestions.ts                   [NEW]
```

### SQL 新增表

```sql
-- 学生档案表
-- 院校表
-- 专业表
-- 考纲资料表
-- 题库表
-- 题目能力点表
-- 评分权重配置表
-- 教师点评表
-- 短信验证码表
```

---

## Phase 1: 数据库与角色体系基础（后端）

### Task 1: 新建 MySQL 表结构

**Files:**
- Create: `admin/src/main/resources/sql/v1_tables.sql`
- Modify: `admin/src/main/resources/sql/table.sql` (追加引用)

- [ ] **Step 1: 编写新表 DDL**

在 `admin/src/main/resources/sql/v1_tables.sql` 中创建以下表：

```sql
-- ============================================================
-- 1. 短信验证码表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sms_code` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `phone` varchar(32) NOT NULL,
  `code` varchar(16) NOT NULL,
  `biz_type` varchar(32) NOT NULL DEFAULT 'login' COMMENT 'login|register|reset',
  `used` tinyint(1) NOT NULL DEFAULT 0,
  `expire_time` datetime NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_phone_biz` (`phone`, `biz_type`),
  INDEX `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信验证码';

-- ============================================================
-- 2. 学生档案表
-- ============================================================
CREATE TABLE IF NOT EXISTS `student_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `school_name` varchar(128) DEFAULT NULL COMMENT '所在高中/中职',
  `grade` varchar(32) DEFAULT NULL COMMENT '年级',
  `exam_category` varchar(64) DEFAULT NULL COMMENT '考生类别: 普高招生|单独考试招生',
  `training_stage` varchar(32) DEFAULT NULL COMMENT '入门|强化|冲刺',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生档案';

-- ============================================================
-- 3. 学生目标院校关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `student_target_college` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `college_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_college` (`user_id`, `college_id`),
  INDEX `idx_college` (`college_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生目标院校';

-- ============================================================
-- 4. 学生目标专业关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `student_target_major` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `major_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_major` (`user_id`, `major_id`),
  INDEX `idx_major` (`major_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生目标专业';

-- ============================================================
-- 5. 院校表
-- ============================================================
CREATE TABLE IF NOT EXISTS `college` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL COMMENT '院校名称',
  `code` varchar(32) DEFAULT NULL COMMENT '院校代码',
  `type` varchar(64) DEFAULT NULL COMMENT '院校类型',
  `province` varchar(32) DEFAULT '浙江',
  `city` varchar(64) DEFAULT NULL,
  `level` varchar(32) DEFAULT NULL COMMENT '层次: 高职|本科',
  `official_url` varchar(512) DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_name` (`name`),
  INDEX `idx_province` (`province`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院校表';

-- ============================================================
-- 6. 专业表
-- ============================================================
CREATE TABLE IF NOT EXISTS `major` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `college_id` bigint NOT NULL COMMENT '所属院校',
  `name` varchar(128) NOT NULL COMMENT '专业名称',
  `code` varchar(32) DEFAULT NULL COMMENT '专业代码',
  `category` varchar(64) DEFAULT NULL COMMENT '专业类别',
  `target_type` varchar(64) DEFAULT NULL COMMENT '招生对象: 普高|单独考试',
  `test_form` varchar(128) DEFAULT NULL COMMENT '测试形式',
  `test_content` text DEFAULT NULL COMMENT '测试内容',
  `score_structure` text DEFAULT NULL COMMENT '分值结构',
  `year` int DEFAULT 2026 COMMENT '招生年份',
  `official_url` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_college` (`college_id`),
  INDEX `idx_name` (`name`),
  INDEX `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业表';

-- ============================================================
-- 7. 考纲资料表
-- ============================================================
CREATE TABLE IF NOT EXISTS `exam_outline` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `college_id` bigint DEFAULT NULL,
  `major_id` bigint DEFAULT NULL,
  `year` int DEFAULT 2026,
  `title` varchar(256) NOT NULL,
  `doc_type` varchar(32) NOT NULL COMMENT '章程|大纲|方案|样卷|评分标准|真题|解析',
  `content` longtext DEFAULT NULL COMMENT '解析后文本内容',
  `file_url` varchar(1024) DEFAULT NULL,
  `source_url` varchar(1024) DEFAULT NULL COMMENT '来源链接',
  `status` varchar(32) NOT NULL DEFAULT 'pending' COMMENT 'pending|approved|rejected',
  `uploader_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_college` (`college_id`),
  INDEX `idx_major` (`major_id`),
  INDEX `idx_year` (`year`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考纲资料';

-- ============================================================
-- 8. 题库表
-- ============================================================
CREATE TABLE IF NOT EXISTS `question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(256) NOT NULL COMMENT '题目标题',
  `content` text NOT NULL COMMENT '题目正文',
  `question_type` varchar(32) NOT NULL COMMENT '结构化|半结构化|专业认知|综合素质|情景应变|自我介绍',
  `college_id` bigint DEFAULT NULL COMMENT '适用院校, NULL=通用',
  `major_id` bigint DEFAULT NULL COMMENT '适用专业, NULL=通用',
  `ability_tag` varchar(64) DEFAULT NULL COMMENT '能力点: 表达能力|逻辑思维|专业认知|...',
  `difficulty` varchar(16) DEFAULT 'medium' COMMENT 'easy|medium|hard|pressure',
  `answer_time_seconds` int DEFAULT 120 COMMENT '建议答题时间(秒)',
  `reference_answer` text DEFAULT NULL COMMENT '参考答案/答题要点',
  `scoring_rule` text DEFAULT NULL COMMENT '评分规则(JSON)',
  `follow_up_rule` text DEFAULT NULL COMMENT '追问规则(JSON)',
  `follow_up_questions` text DEFAULT NULL COMMENT '预设追问题(JSON数组)',
  `source_ref` varchar(512) DEFAULT NULL COMMENT '来源依据',
  `is_ai_generated` tinyint(1) NOT NULL DEFAULT 0,
  `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT 'draft|pending_review|approved|rejected',
  `creator_id` bigint DEFAULT NULL,
  `year` int DEFAULT 2026,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_type` (`question_type`),
  INDEX `idx_college` (`college_id`),
  INDEX `idx_major` (`major_id`),
  INDEX `idx_ability` (`ability_tag`),
  INDEX `idx_difficulty` (`difficulty`),
  INDEX `idx_status` (`status`),
  INDEX `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库';

-- ============================================================
-- 9. 面试会话-题目关联表 (从题库抽取的题目序列)
-- ============================================================
CREATE TABLE IF NOT EXISTS `interview_session_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `question_id` bigint NOT NULL,
  `seq_index` int NOT NULL COMMENT '题目序号(0-based)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_session` (`session_id`),
  UNIQUE KEY `uk_session_seq` (`session_id`, `seq_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试会话题目关联';

-- ============================================================
-- 10. 评分权重配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `scoring_weight_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_name` varchar(128) NOT NULL,
  `college_id` bigint DEFAULT NULL COMMENT '适用院校, NULL=全局默认',
  `major_id` bigint DEFAULT NULL COMMENT '适用专业, NULL=全局默认',
  `w_content` decimal(5,2) NOT NULL DEFAULT 30.00 COMMENT '内容质量权重',
  `w_logic` decimal(5,2) NOT NULL DEFAULT 15.00 COMMENT '逻辑结构权重',
  `w_professional` decimal(5,2) NOT NULL DEFAULT 15.00 COMMENT '专业匹配权重',
  `w_expression` decimal(5,2) NOT NULL DEFAULT 15.00 COMMENT '语言表达权重',
  `w_adaptability` decimal(5,2) NOT NULL DEFAULT 10.00 COMMENT '临场应变权重',
  `w_time_control` decimal(5,2) NOT NULL DEFAULT 5.00 COMMENT '时间控制权重',
  `w_etiquette` decimal(5,2) NOT NULL DEFAULT 10.00 COMMENT '礼仪仪态权重',
  `is_default` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_college_major` (`college_id`, `major_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分权重配置';

-- ============================================================
-- 11. 教师点评表
-- ============================================================
CREATE TABLE IF NOT EXISTS `teacher_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `teacher_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `content` text NOT NULL COMMENT '点评内容',
  `adjusted_score` int DEFAULT NULL COMMENT '调整后分数',
  `is_excellent_sample` tinyint(1) DEFAULT 0 COMMENT '标记优秀样本',
  `is_model_misjudge` tinyint(1) DEFAULT 0 COMMENT '标记模型误判',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_session` (`session_id`),
  INDEX `idx_teacher` (`teacher_id`),
  INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师点评';

-- ============================================================
-- 12. 默认评分权重数据
-- ============================================================
INSERT INTO `scoring_weight_config` (`config_name`, `w_content`, `w_logic`, `w_professional`, `w_expression`, `w_adaptability`, `w_time_control`, `w_etiquette`, `is_default`)
VALUES ('默认权重', 30.00, 15.00, 15.00, 15.00, 10.00, 5.00, 10.00, 1);
```

- [ ] **Step 2: 在 Docker Compose 初始化中引用新 SQL**

修改 `docker-compose.yml` 中 MySQL 的 `volumes` 或 `command`，确保 `v1_tables.sql` 在容器启动时被执行。或者手动在已运行的 MySQL 中执行：

```bash
docker exec -i ai-meeting-mysql mysql -uroot -p123456 mainshi_agent < admin/src/main/resources/sql/v1_tables.sql
```

- [ ] **Step 3: 验证表创建成功**

```bash
docker exec -it ai-meeting-mysql mysql -uroot -p123456 mainshi_agent -e "SHOW TABLES LIKE '%college%'; SHOW TABLES LIKE '%question%'; SHOW TABLES LIKE '%student_profile%';"
```

Expected: 看到 `college`, `major`, `exam_outline`, `question`, `student_profile`, `scoring_weight_config`, `teacher_review`, `sms_code` 等表。

- [ ] **Step 4: Commit**

```bash
cd AI-Meeting
git add admin/src/main/resources/sql/v1_tables.sql
git commit -m "feat(db): add V1 MVP tables - college, major, question bank, student profile, scoring weights, teacher review"
```

---

### Task 2: 扩展用户角色体系 (admin/user → student/teacher/admin)

**Files:**
- Modify: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/user/dao/entity/UserDO.java`
- Modify: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/common/config/satoken/StpInterfaceImpl.java`
- Modify: `admin/src/main/resources/sql/table.sql` (或 v1_tables.sql 追加 ALTER)

- [ ] **Step 1: 修改 UserDO 增加 role 和 openId 字段**

在 `UserDO.java` 中新增字段：

```java
/**
 * 角色: student, teacher, admin
 */
private String role;

/**
 * 微信 OpenID
 */
private String openId;
```

- [ ] **Step 2: 编写 ALTER TABLE SQL**

在 `v1_tables.sql` 末尾追加：

```sql
ALTER TABLE `t_user` ADD COLUMN `role` varchar(32) NOT NULL DEFAULT 'student' COMMENT 'student|teacher|admin' AFTER `mail`;
ALTER TABLE `t_user` ADD COLUMN `open_id` varchar(128) DEFAULT NULL COMMENT '微信OpenID' AFTER `role`;
ALTER TABLE `t_user` ADD INDEX `idx_role` (`role`);
ALTER TABLE `t_user` ADD INDEX `idx_open_id` (`open_id`);
```

- [ ] **Step 3: 修改 StpInterfaceImpl 支持三角色**

替换 `getRoleList` 方法：

```java
@Override
public List<String> getRoleList(Object loginId, String loginType) {
    String username = String.valueOf(loginId);
    // 优先从数据库读取 role 字段
    UserDO user = userService.getByUsername(username);
    if (user != null && user.getRole() != null) {
        return List.of(user.getRole());
    }
    // 兼容旧逻辑
    if (adminPermissionService.isAdmin(username)) {
        return List.of("admin");
    }
    return List.of("student");
}
```

替换 `getPermissionList` 方法：

```java
@Override
public List<String> getPermissionList(Object loginId, String loginType) {
    String username = String.valueOf(loginId);
    UserDO user = userService.getByUsername(username);
    if (user != null && "admin".equals(user.getRole())) {
        return List.of("admin");
    }
    if (user != null && "teacher".equals(user.getRole())) {
        return List.of("teacher");
    }
    return List.of("student");
}
```

需要在 `StpInterfaceImpl` 中注入 `UserService`（或新建一个查询方法）。确保 `UserService` 有 `getByUsername(String username)` 方法。

- [ ] **Step 4: 验证编译通过**

```bash
cd AI-Meeting
./mvnw -B -ntp clean compile -Dmaven.test.skip=true
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add admin/src/main/java/com/hewei/hzyjy/interviewpilot/user/dao/entity/UserDO.java \
        admin/src/main/java/com/hewei/hzyjy/interviewpilot/common/config/satoken/StpInterfaceImpl.java \
        admin/src/main/resources/sql/v1_tables.sql
git commit -m "feat(auth): expand role model to student/teacher/admin with openId field"
```

---

### Task 3: 手机号短信验证码登录接口

**Files:**
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/user/api/io/req/UserPhoneLoginReqDTO.java`
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/user/dao/entity/SmsCodeDO.java`
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/user/dao/mapper/SmsCodeMapper.java`
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/user/service/SmsCodeService.java`
- Modify: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/user/api/UserController.java`
- Modify: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/user/service/UserService.java`
- Modify: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/auth/infrastructure/web/SaTokenAuthInterceptorConfig.java`

- [ ] **Step 1: 创建 SmsCodeDO 实体**

```java
@Data
@TableName("sms_code")
public class SmsCodeDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String code;
    private String bizType;
    private Boolean used;
    private Date expireTime;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
```

- [ ] **Step 2: 创建 SmsCodeMapper**

```java
@Mapper
public interface SmsCodeMapper extends BaseMapper<SmsCodeDO> {
}
```

- [ ] **Step 3: 创建 SmsCodeService**

```java
@Service
@RequiredArgsConstructor
public class SmsCodeService extends ServiceImpl<SmsCodeMapper, SmsCodeDO> {

    /**
     * 发送验证码 (MVP: 直接打印到日志, 生产环境对接短信 SDK)
     */
    public void sendCode(String phone, String bizType) {
        // 60s 防重复
        LambdaQueryWrapper<SmsCodeDO> wrapper = new LambdaQueryWrapper<SmsCodeDO>()
            .eq(SmsCodeDO::getPhone, phone)
            .eq(SmsCodeDO::getBizType, bizType)
            .eq(SmsCodeDO::getUsed, false)
            .gt(SmsCodeDO::getExpireTime, new Date())
            .orderByDesc(SmsCodeDO::getCreateTime)
            .last("LIMIT 1");
        if (this.getOne(wrapper) != null) {
            throw new RuntimeException("验证码已发送，请60秒后重试");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        SmsCodeDO entity = new SmsCodeDO();
        entity.setPhone(phone);
        entity.setCode(code);
        entity.setBizType(bizType);
        entity.setUsed(false);
        entity.setExpireTime(DateUtils.addMinutes(new Date(), 5));
        this.save(entity);

        // MVP: 日志输出, 生产替换为阿里云/腾讯云短信 SDK
        log.info("[SMS] phone={}, code={}, bizType={}", phone, code, bizType);
    }

    /**
     * 校验验证码
     */
    public boolean verifyCode(String phone, String code, String bizType) {
        LambdaQueryWrapper<SmsCodeDO> wrapper = new LambdaQueryWrapper<SmsCodeDO>()
            .eq(SmsCodeDO::getPhone, phone)
            .eq(SmsCodeDO::getCode, code)
            .eq(SmsCodeDO::getBizType, bizType)
            .eq(SmsCodeDO::getUsed, false)
            .gt(SmsCodeDO::getExpireTime, new Date())
            .orderByDesc(SmsCodeDO::getCreateTime)
            .last("LIMIT 1");
        SmsCodeDO record = this.getOne(wrapper);
        if (record == null) return false;

        record.setUsed(true);
        this.updateById(record);
        return true;
    }
}
```

- [ ] **Step 4: 创建 UserPhoneLoginReqDTO**

```java
@Data
public class UserPhoneLoginReqDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为6位")
    private String code;
}
```

- [ ] **Step 5: 在 UserController 中新增接口**

```java
@PostMapping("/send-sms-code")
public Result<Void> sendSmsCode(@RequestParam String phone,
                                @RequestParam(defaultValue = "login") String bizType) {
    smsCodeService.sendCode(phone, bizType);
    return Result.success();
}

@PostMapping("/phone-login")
public Result<Map<String, Object>> phoneLogin(@RequestBody @Valid UserPhoneLoginReqDTO req) {
    if (!smsCodeService.verifyCode(req.getPhone(), req.getCode(), "login")) {
        return Results.failure(ErrorCodeEnum.PARAM_ERROR, "验证码错误或已过期");
    }
    // 查找或创建用户
    UserDO user = userService.getByPhone(req.getPhone());
    if (user == null) {
        user = new UserDO();
        user.setUsername("phone_" + req.getPhone());
        user.setPhone(req.getPhone());
        user.setRole("student");
        user.setPassword(""); // 手机号登录无密码
        userService.save(user);
    }
    // Sa-Token 登录
    StpUtil.login(user.getUsername());
    String token = StpUtil.getTokenValue();
    Map<String, Object> result = new HashMap<>();
    result.put("token", token);
    result.put("user", user);
    return Result.success(result);
}
```

- [ ] **Step 6: 在 SaTokenAuthInterceptorConfig 白名单中添加新接口**

在 `addInterceptors` 方法的 `excludePathPatterns` 中追加：

```java
.excludePathPatterns("/api/interviewpilot/v1/users/send-sms-code")
.excludePathPatterns("/api/interviewpilot/v1/users/phone-login")
```

- [ ] **Step 7: 验证编译通过**

```bash
cd AI-Meeting
./mvnw -B -ntp clean compile -Dmaven.test.skip=true
```

- [ ] **Step 8: Commit**

```bash
git add admin/src/main/java/com/hewei/hzyjy/interviewpilot/user/
git commit -m "feat(auth): add phone SMS login with send-code and phone-login endpoints"
```

---

### Task 4: 学生档案 CRUD 后端

**Files:**
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/student/` (整个目录结构)

- [ ] **Step 1: 创建 StudentProfileDO**

```java
@Data
@TableName("student_profile")
public class StudentProfileDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String schoolName;
    private String grade;
    private String examCategory;
    private String trainingStage;
}
```

- [ ] **Step 2: 创建 StudentProfileMapper**

```java
@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfileDO> {
}
```

- [ ] **Step 3: 创建 StudentTargetCollegeDO 和 StudentTargetMajorDO**

```java
@Data
@TableName("student_target_college")
public class StudentTargetCollegeDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long collegeId;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}

@Data
@TableName("student_target_major")
public class StudentTargetMajorDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long majorId;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
```

创建对应 Mapper。

- [ ] **Step 4: 创建 StudentProfileService**

```java
@Service
@RequiredArgsConstructor
public class StudentProfileService extends ServiceImpl<StudentProfileMapper, StudentProfileDO> {

    private final StudentTargetCollegeMapper targetCollegeMapper;
    private final StudentTargetMajorMapper targetMajorMapper;

    @Transactional
    public void saveProfile(Long userId, StudentProfileSaveReqDTO req) {
        // upsert profile
        StudentProfileDO existing = this.getOne(new LambdaQueryWrapper<StudentProfileDO>()
            .eq(StudentProfileDO::getUserId, userId)
            .eq(StudentProfileDO::getDelFlag, 0));
        if (existing != null) {
            existing.setSchoolName(req.getSchoolName());
            existing.setGrade(req.getGrade());
            existing.setExamCategory(req.getExamCategory());
            existing.setTrainingStage(req.getTrainingStage());
            this.updateById(existing);
        } else {
            StudentProfileDO profile = new StudentProfileDO();
            profile.setUserId(userId);
            BeanUtils.copyProperties(req, profile);
            this.save(profile);
        }

        // 更新目标院校
        targetCollegeMapper.delete(new LambdaQueryWrapper<StudentTargetCollegeDO>()
            .eq(StudentTargetCollegeDO::getUserId, userId));
        if (req.getCollegeIds() != null) {
            for (Long collegeId : req.getCollegeIds()) {
                StudentTargetCollegeDO tc = new StudentTargetCollegeDO();
                tc.setUserId(userId);
                tc.setCollegeId(collegeId);
                targetCollegeMapper.insert(tc);
            }
        }

        // 更新目标专业
        targetMajorMapper.delete(new LambdaQueryWrapper<StudentTargetMajorDO>()
            .eq(StudentTargetMajorDO::getUserId, userId));
        if (req.getMajorIds() != null) {
            for (Long majorId : req.getMajorIds()) {
                StudentTargetMajorDO tm = new StudentTargetMajorDO();
                tm.setUserId(userId);
                tm.setMajorId(majorId);
                targetMajorMapper.insert(tm);
            }
        }
    }

    public StudentProfileRespDTO getProfile(Long userId) {
        // 查 profile + 目标院校/专业, 组装 DTO 返回
        // ...
    }
}
```

- [ ] **Step 5: 创建 StudentProfileController**

```java
@RestController
@RequestMapping("/api/interviewpilot/v1/student/profile")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @GetMapping
    public Result<StudentProfileRespDTO> getProfile(@CurrentUser UserContext currentUser) {
        return Result.success(studentProfileService.getProfile(currentUser.getUserId()));
    }

    @PutMapping
    public Result<Void> saveProfile(@CurrentUser UserContext currentUser,
                                    @RequestBody @Valid StudentProfileSaveReqDTO req) {
        studentProfileService.saveProfile(currentUser.getUserId(), req);
        return Result.success();
    }
}
```

- [ ] **Step 6: 编译验证**

```bash
./mvnw -B -ntp clean compile -Dmaven.test.skip=true
```

- [ ] **Step 7: Commit**

```bash
git add admin/src/main/java/com/hewei/hzyjy/interviewpilot/student/
git commit -m "feat(student): add student profile CRUD with target college/major associations"
```

---

## Phase 2: 院校/专业/题库体系（后端）

### Task 5: 院校管理 CRUD

**Files:**
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/questionbank/` (整个目录)

- [ ] **Step 1: 创建 CollegeDO, MajorDO, ExamOutlineDO 实体**

```java
@Data
@TableName("college")
public class CollegeDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String type;
    private String province;
    private String city;
    private String level;
    private String officialUrl;
    private String remark;
}

@Data
@TableName("major")
public class MajorDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long collegeId;
    private String name;
    private String code;
    private String category;
    private String targetType;
    private String testForm;
    private String testContent;
    private String scoreStructure;
    private Integer year;
    private String officialUrl;
}

@Data
@TableName("exam_outline")
public class ExamOutlineDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long collegeId;
    private Long majorId;
    private Integer year;
    private String title;
    private String docType;
    private String content;
    private String fileUrl;
    private String sourceUrl;
    private String status;
    private Long uploaderId;
}
```

- [ ] **Step 2: 创建对应 Mapper (CollegeMapper, MajorMapper, ExamOutlineMapper)**

均继承 `BaseMapper<T>`，使用 `@Mapper` 注解。

- [ ] **Step 3: 创建 CollegeService, MajorService, ExamOutlineService**

每个 Service 继承 `IService<T>`，提供标准 CRUD + 分页查询。CollegeService 增加 `listByProvince(String province)` 等便捷查询。

- [ ] **Step 4: 创建 CollegeController, MajorController, ExamOutlineController**

每个 Controller 提供：
- `POST /` — 新增
- `PUT /{id}` — 修改
- `DELETE /{id}` — 删除 (逻辑删除)
- `GET /{id}` — 详情
- `GET /page` — 分页查询
- `GET /list` — 列表 (不分页)

使用 `@SaCheckRole("admin")` 或 `@SaCheckRole("teacher")` 保护写接口。

- [ ] **Step 5: 编译验证**

```bash
./mvnw -B -ntp clean compile -Dmaven.test.skip=true
```

- [ ] **Step 6: Commit**

```bash
git add admin/src/main/java/com/hewei/hzyjy/interviewpilot/questionbank/
git commit -m "feat(questionbank): add college, major, exam outline CRUD services and controllers"
```

---

### Task 6: 题库管理 CRUD + AI 扩展出题

**Files:**
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/questionbank/dao/entity/QuestionDO.java`
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/questionbank/dao/mapper/QuestionMapper.java`
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/questionbank/service/QuestionBankService.java`
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/questionbank/service/QuestionAiGenerateService.java`
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/questionbank/api/QuestionBankController.java`

- [ ] **Step 1: 创建 QuestionDO 实体**

```java
@Data
@TableName("question")
public class QuestionDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String questionType;
    private Long collegeId;
    private Long majorId;
    private String abilityTag;
    private String difficulty;
    private Integer answerTimeSeconds;
    private String referenceAnswer;
    private String scoringRule;     // JSON
    private String followUpRule;    // JSON
    private String followUpQuestions; // JSON array
    private String sourceRef;
    private Boolean isAiGenerated;
    private String status;          // draft|pending_review|approved|rejected
    private Long creatorId;
    private Integer year;
}
```

- [ ] **Step 2: 创建 QuestionMapper + QuestionBankService**

QuestionBankService 提供：
- 标准 CRUD
- `Page<QuestionDO> pageByFilter(collegeId, majorId, questionType, abilityTag, difficulty, status, page)` — 多条件筛选分页
- `List<QuestionDO> randomSelect(collegeId, majorId, questionType, count)` — 随机抽题
- `List<QuestionDO> selectByAbility(abilityTag, count)` — 按能力点抽题

- [ ] **Step 3: 创建 QuestionAiGenerateService**

利用现有的 `UniversalAiChatHandler` 或讯飞 Agent 实现 AI 扩展出题：

```java
@Service
@RequiredArgsConstructor
public class QuestionAiGenerateService {

    private final AiChatHandlerFactory aiChatHandlerFactory;
    private final AiPropertiesMapper aiPropertiesMapper;

    /**
     * 基于条件 AI 生成题目
     */
    public List<QuestionDO> generateQuestions(QuestionGenerateReqDTO req) {
        // 1. 获取默认 AI 配置
        AiPropertiesDO aiProps = getDefaultAiProperties();

        // 2. 构建 prompt
        String prompt = buildGeneratePrompt(req);

        // 3. 调用 AI (同步模式, 非流式)
        String response = callAiSync(aiProps, prompt);

        // 4. 解析 JSON 响应为 QuestionDO 列表
        List<QuestionDO> questions = parseQuestionsFromAi(response, req);

        // 5. 标记为 AI 生成 + 待审核
        questions.forEach(q -> {
            q.setIsAiGenerated(true);
            q.setStatus("pending_review");
        });

        return questions;
    }

    private String buildGeneratePrompt(QuestionGenerateReqDTO req) {
        return String.format("""
            你是一位浙江高职提前招生面试出题专家。请根据以下条件生成 %d 道面试题：
            
            目标院校：%s
            目标专业：%s
            题型：%s
            能力点：%s
            难度：%s
            是否生成追问题：%s
            是否生成评分标准：%s
            
            请以 JSON 数组格式返回，每道题包含：
            - title: 题目标题
            - content: 题目正文
            - questionType: 题型
            - abilityTag: 能力点
            - difficulty: 难度
            - answerTimeSeconds: 建议答题时间(秒)
            - referenceAnswer: 参考答案/答题要点
            - scoringRule: 评分规则(JSON字符串)
            - followUpQuestions: 预设追问题(JSON数组字符串)
            - sourceRef: 来源依据说明
            
            只返回 JSON 数组，不要其他文字。
            """,
            req.getCount(),
            req.getCollegeName() != null ? req.getCollegeName() : "通用",
            req.getMajorName() != null ? req.getMajorName() : "通用",
            req.getQuestionType(),
            req.getAbilityTag() != null ? req.getAbilityTag() : "综合",
            req.getDifficulty() != null ? req.getDifficulty() : "medium",
            req.getGenerateFollowUp() ? "是" : "否",
            req.getGenerateScoringRule() ? "是" : "否"
        );
    }
}
```

- [ ] **Step 4: 创建 QuestionBankController**

```java
@RestController
@RequestMapping("/api/interviewpilot/v1/questions")
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService questionBankService;
    private final QuestionAiGenerateService aiGenerateService;

    @PostMapping
    @SaCheckRole("teacher")
    public Result<Long> createQuestion(@RequestBody @Valid QuestionSaveReqDTO req,
                                       @CurrentUser UserContext currentUser) {
        return Result.success(questionBankService.create(req, currentUser.getUserId()));
    }

    @PutMapping("/{id}")
    @SaCheckRole("teacher")
    public Result<Void> updateQuestion(@PathVariable Long id, @RequestBody @Valid QuestionSaveReqDTO req) {
        questionBankService.update(id, req);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckRole("teacher")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        questionBankService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<QuestionRespDTO> getQuestion(@PathVariable Long id) {
        return Result.success(questionBankService.getDetail(id));
    }

    @GetMapping("/page")
    public Result<IPage<QuestionRespDTO>> pageQuestions(QuestionPageReqDTO req) {
        return Result.success(questionBankService.pageByFilter(req));
    }

    @PostMapping("/ai-generate")
    @SaCheckRole("teacher")
    public Result<List<QuestionRespDTO>> aiGenerate(@RequestBody @Valid QuestionGenerateReqDTO req) {
        return Result.success(aiGenerateService.generateQuestions(req));
    }

    @PutMapping("/{id}/status")
    @SaCheckRole("teacher")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        questionBankService.updateStatus(id, status);
        return Result.success();
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
./mvnw -B -ntp clean compile -Dmaven.test.skip=true
```

- [ ] **Step 6: Commit**

```bash
git add admin/src/main/java/com/hewei/hzyjy/interviewpilot/questionbank/
git commit -m "feat(questionbank): add question CRUD, AI generation, and teacher review workflow"
```

---

## Phase 3: 面试流程改造 + 7 维评分引擎（后端）

### Task 7: 扩展面试会话支持题库模式

**Files:**
- Modify: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/interview/dao/entity/InterviewSession.java`
- Modify: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/interview/dao/entity/InterviewRecordDO.java`
- Modify: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/interview/api/InterviewSessionController.java`
- Modify: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/interview/flow/session/InterviewSessionFacade.java`

- [ ] **Step 1: 扩展 InterviewSession 字段**

在 `InterviewSession.java` (MongoDB) 中新增：

```java
/**
 * 会话模式: resume(简历出题) | questionBank(题库抽题)
 */
private String sessionMode;

/**
 * 题库模式下的目标院校ID
 */
private Long collegeId;

/**
 * 题库模式下的目标专业ID
 */
private Long majorId;

/**
 * 题库模式下的面试类型: 结构化|半结构化|专业认知|综合素质
 */
private String interviewMode;

/**
 * 题库模式下的题目ID列表(JSON)
 */
private String questionIds;
```

- [ ] **Step 2: 扩展 InterviewRecordDO 字段**

在 `InterviewRecordDO.java` (MySQL) 中新增：

```java
@TableField("content_score")
private Integer contentScore;        // 内容质量 0-100

@TableField("logic_score")
private Integer logicScore;          // 逻辑结构 0-100

@TableField("professional_score")
private Integer professionalScore;   // 专业匹配 0-100

@TableField("expression_score")
private Integer expressionScore;     // 语言表达 0-100

@TableField("adaptability_score")
private Integer adaptabilityScore;   // 临场应变 0-100

@TableField("time_control_score")
private Integer timeControlScore;    // 时间控制 0-100

@TableField("etiquette_score")
private Integer etiquetteScore;      // 礼仪仪态 0-100

@TableField("composite_score")
private Integer compositeScore;      // 加权总分

@TableField("college_id")
private Long collegeId;

@TableField("major_id")
private Long majorId;

@TableField("session_mode")
private String sessionMode;
```

编写对应的 ALTER TABLE SQL 追加到 `v1_tables.sql`：

```sql
ALTER TABLE `interview_record` ADD COLUMN `content_score` int DEFAULT NULL AFTER `resume_score`;
ALTER TABLE `interview_record` ADD COLUMN `logic_score` int DEFAULT NULL AFTER `content_score`;
ALTER TABLE `interview_record` ADD COLUMN `professional_score` int DEFAULT NULL AFTER `logic_score`;
ALTER TABLE `interview_record` ADD COLUMN `expression_score` int DEFAULT NULL AFTER `professional_score`;
ALTER TABLE `interview_record` ADD COLUMN `adaptability_score` int DEFAULT NULL AFTER `expression_score`;
ALTER TABLE `interview_record` ADD COLUMN `time_control_score` int DEFAULT NULL AFTER `adaptability_score`;
ALTER TABLE `interview_record` ADD COLUMN `etiquette_score` int DEFAULT NULL AFTER `time_control_score`;
ALTER TABLE `interview_record` ADD COLUMN `composite_score` int DEFAULT NULL AFTER `etiquette_score`;
ALTER TABLE `interview_record` ADD COLUMN `college_id` bigint DEFAULT NULL AFTER `composite_score`;
ALTER TABLE `interview_record` ADD COLUMN `major_id` bigint DEFAULT NULL AFTER `college_id`;
ALTER TABLE `interview_record` ADD COLUMN `session_mode` varchar(32) DEFAULT 'resume' AFTER `major_id`;
```

- [ ] **Step 3: 在 InterviewSessionController 新增题库模式创建接口**

```java
/**
 * 从题库创建面试会话 (新模式)
 */
@PostMapping("/sessions/from-bank")
public Result<InterviewSessionCreateRespDTO> createSessionFromBank(
        @RequestBody @Valid InterviewFromBankReqDTO req,
        @CurrentUser UserContext currentUser) {
    return Result.success(interviewSessionFacade.createFromBank(currentUser, req));
}
```

`InterviewFromBankReqDTO`:
```java
@Data
public class InterviewFromBankReqDTO {
    private Long collegeId;
    private Long majorId;
    @NotBlank
    private String interviewMode; // 结构化|半结构化|专业认知|综合素质
    private Integer questionCount = 5;
    private String difficulty;
}
```

- [ ] **Step 4: 在 InterviewSessionFacade 实现 createFromBank 方法**

核心逻辑：
1. 从 `QuestionBankService` 按条件随机抽取 N 道题
2. 创建 `InterviewSession`，设置 `sessionMode = "questionBank"`
3. 将抽取的题目写入 `interview_session_question` 表
4. 调用 `InterviewFlowStateMachine.ensureInitialized(sessionId, totalQuestions)`
5. 返回 sessionId + 第一道题

- [ ] **Step 5: 修改答题流程支持题库模式**

在 `InterviewAnswerPipeline` 或对应的答题处理逻辑中，判断 `sessionMode`：
- `resume` 模式：保持现有逻辑（AI 评分 + AI 追问）
- `questionBank` 模式：从 `interview_session_question` 获取下一题，使用题库中预设的 `scoringRule` 和 `followUpQuestions`

- [ ] **Step 6: 编译验证**

```bash
./mvnw -B -ntp clean compile -Dmaven.test.skip=true
```

- [ ] **Step 7: Commit**

```bash
git add admin/src/main/java/com/hewei/hzyjy/interviewpilot/interview/
git commit -m "feat(interview): add question-bank mode for session creation with preset questions"
```

---

### Task 8: 7 维评分引擎

**Files:**
- Modify: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/interview/application/strategy/WeightedRadarComputationStrategy.java`
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/interview/application/strategy/DimensionScoreStrategy.java`
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/interview/application/strategy/DimensionScoreResult.java`
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/interview/dao/entity/ScoringWeightConfigDO.java`
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/interview/dao/mapper/ScoringWeightConfigMapper.java`

- [ ] **Step 1: 创建 DimensionScoreResult**

```java
@Data
public class DimensionScoreResult {
    private Integer contentScore;       // 内容质量
    private Integer logicScore;         // 逻辑结构
    private Integer professionalScore;  // 专业匹配
    private Integer expressionScore;    // 语言表达
    private Integer adaptabilityScore;  // 临场应变
    private Integer timeControlScore;   // 时间控制
    private Integer etiquetteScore;     // 礼仪仪态
    private Integer compositeScore;     // 加权总分
}
```

- [ ] **Step 2: 创建 ScoringWeightConfigDO**

```java
@Data
@TableName("scoring_weight_config")
public class ScoringWeightConfigDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configName;
    private Long collegeId;
    private Long majorId;
    private BigDecimal wContent;
    private BigDecimal wLogic;
    private BigDecimal wProfessional;
    private BigDecimal wExpression;
    private BigDecimal wAdaptability;
    private BigDecimal wTimeControl;
    private BigDecimal wEtiquette;
    private Boolean isDefault;
}
```

- [ ] **Step 3: 创建 DimensionScoreStrategy**

```java
@Component
@RequiredArgsConstructor
public class DimensionScoreStrategy {

    private final ScoringWeightConfigMapper weightConfigMapper;

    /**
     * 计算 7 维分数 + 加权总分
     *
     * @param aiRawScore     AI Agent 输出的原始分数 (0-100)
     * @param aiFeedback     AI Agent 输出的反馈文本
     * @param demeanorScore  神态分析分数 (0-100)
     * @param answerDuration 答题用时(秒)
     * @param timeLimit      建议答题时间(秒)
     * @param collegeId      院校ID (用于查找权重配置)
     * @param majorId        专业ID
     */
    public DimensionScoreResult compute(int aiRawScore, String aiFeedback,
                                         Integer demeanorScore, int answerDuration,
                                         int timeLimit, Long collegeId, Long majorId) {
        DimensionScoreResult result = new DimensionScoreResult();

        // 基于 AI 分数和反馈进行维度拆分 (MVP: 简单规则)
        // 后续可接入更精细的 AI 维度评分
        result.setContentScore(clamp(aiRawScore));
        result.setLogicScore(clamp((int)(aiRawScore * 0.9)));  // 略低于内容分
        result.setProfessionalScore(clamp((int)(aiRawScore * 0.85)));
        result.setExpressionScore(clamp((int)(aiRawScore * 0.9)));
        result.setAdaptabilityScore(clamp((int)(aiRawScore * 0.8)));

        // 时间控制评分
        if (timeLimit > 0) {
            double ratio = (double) answerDuration / timeLimit;
            if (ratio >= 0.5 && ratio <= 1.0) {
                result.setTimeControlScore(100);
            } else if (ratio < 0.5) {
                result.setTimeControlScore((int)(ratio * 2 * 80));
            } else {
                result.setTimeControlScore(Math.max(0, (int)(100 - (ratio - 1.0) * 100)));
            }
        } else {
            result.setTimeControlScore(70); // 无时间限制默认中等
        }

        // 礼仪评分 (来自神态分析)
        result.setEtiquetteScore(demeanorScore != null ? clamp(demeanorScore) : 70);

        // 加权总分
        ScoringWeightConfigDO weights = findWeightConfig(collegeId, majorId);
        result.setCompositeScore(computeWeighted(result, weights));

        return result;
    }

    private int computeWeighted(DimensionScoreResult scores, ScoringWeightConfigDO w) {
        double total = scores.getContentScore() * w.getWContent().doubleValue()
            + scores.getLogicScore() * w.getWLogic().doubleValue()
            + scores.getProfessionalScore() * w.getWProfessional().doubleValue()
            + scores.getExpressionScore() * w.getWExpression().doubleValue()
            + scores.getAdaptabilityScore() * w.getWAdaptability().doubleValue()
            + scores.getTimeControlScore() * w.getwTimeControl().doubleValue()
            + scores.getEtiquetteScore() * w.getWEtiquette().doubleValue();
        return clamp((int) Math.round(total / 100.0));
    }

    private ScoringWeightConfigDO findWeightConfig(Long collegeId, Long majorId) {
        // 优先精确匹配 collegeId+majorId, 再匹配 collegeId, 最后取默认
        // ...
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
```

- [ ] **Step 4: 修改 RadarChartDTO 扩展为 7 维度**

```java
@Data
public class RadarChartDTO {
    // 保留旧字段兼容
    private Integer resumeScore;
    private Integer interviewPerformance;
    private Integer demeanorEvaluation;
    private Integer potentialIndex;
    private Integer professionalSkills;

    // 新增 7 维度
    private Integer contentScore;
    private Integer logicScore;
    private Integer professionalMatchScore;
    private Integer expressionScore;
    private Integer adaptabilityScore;
    private Integer timeControlScore;
    private Integer etiquetteScore;
    private Integer compositeScore;
}
```

- [ ] **Step 5: 修改 WeightedRadarComputationStrategy 适配新维度**

在 `compute` 方法中，除了原有 5 维度计算外，增加从 `DimensionScoreResult` 填充新 7 维度字段的逻辑。

- [ ] **Step 6: 在面试完成时调用 DimensionScoreStrategy**

在面试收尾流程（`InterviewSessionFacade` 的 finish/complete 逻辑）中，调用 `DimensionScoreStrategy.compute()` 计算 7 维分数，写入 `InterviewRecordDO`。

- [ ] **Step 7: 编译验证 + Commit**

```bash
./mvnw -B -ntp clean compile -Dmaven.test.skip=true
git add admin/src/main/java/com/hewei/hzyjy/interviewpilot/interview/
git commit -m "feat(scoring): add 7-dimension scoring engine with configurable weights per college/major"
```

---

### Task 9: 教师点评后端

**Files:**
- Create: `admin/src/main/java/com/hewei/hzyjy/interviewpilot/teacher/` (整个目录)

- [ ] **Step 1: 创建 TeacherReviewDO**

```java
@Data
@TableName("teacher_review")
public class TeacherReviewDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Long teacherId;
    private Long studentId;
    private String content;
    private Integer adjustedScore;
    private Boolean isExcellentSample;
    private Boolean isModelMisjudge;
}
```

- [ ] **Step 2: 创建 TeacherReviewService**

提供：
- `createReview(Long teacherId, String sessionId, TeacherReviewSaveReqDTO req)` — 创建点评
- `getReviewsBySession(String sessionId)` — 获取某次面试的所有点评
- `getReviewsByStudent(Long studentId, Page page)` — 分页查看某学生的所有点评
- `getStudentReportList(Long teacherId, Page page)` — 教师查看授权范围内的学生报告列表

- [ ] **Step 3: 创建 TeacherReportController**

```java
@RestController
@RequestMapping("/api/interviewpilot/v1/teacher")
@RequiredArgsConstructor
@SaCheckRole("teacher")
public class TeacherReportController {

    private final TeacherReviewService teacherReviewService;
    private final InterviewRecordService interviewRecordService;

    @GetMapping("/students/{studentId}/records")
    public Result<IPage<InterviewRecordDO>> getStudentRecords(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(interviewRecordService.pageByStudent(studentId, pageNum, pageSize));
    }

    @PostMapping("/sessions/{sessionId}/review")
    public Result<Long> createReview(@PathVariable String sessionId,
                                     @RequestBody @Valid TeacherReviewSaveReqDTO req,
                                     @CurrentUser UserContext currentUser) {
        return Result.success(teacherReviewService.createReview(
            currentUser.getUserId(), sessionId, req));
    }

    @GetMapping("/sessions/{sessionId}/reviews")
    public Result<List<TeacherReviewDO>> getSessionReviews(@PathVariable String sessionId) {
        return Result.success(teacherReviewService.getReviewsBySession(sessionId));
    }
}
```

- [ ] **Step 4: 编译验证 + Commit**

```bash
./mvnw -B -ntp clean compile -Dmaven.test.skip=true
git add admin/src/main/java/com/hewei/hzyjy/interviewpilot/teacher/
git commit -m "feat(teacher): add teacher review/comment backend for student interview reports"
```

---

## Phase 4: 前端多角色路由 + 学生端改造

### Task 10: 前端角色体系 + 路由重构

**Files:**
- Modify: `src/store/slices/userSlice.ts`
- Modify: `src/types/auth.ts`
- Modify: `src/app/router.tsx`
- Modify: `src/components/auth/AuthGuard.tsx`
- Modify: `src/services/authService.ts`

- [ ] **Step 1: 扩展 UserRespDTO 类型**

在 `src/types/auth.ts` 中：

```typescript
export type UserRole = "student" | "teacher" | "admin";

export type UserRespDTO = {
  id?: number;
  username: string;
  realName?: string;
  phone?: string;
  mail?: string;
  avatar?: string | null;
  role?: UserRole;
  createTime?: string;
};
```

- [ ] **Step 2: userSlice 增加 role 选择器**

在 `src/store/slices/userSlice.ts` 中添加：

```typescript
// selectors
export const selectUserRole = (state: { user: UserState }): UserRole =>
  (state.user.currentUser?.role as UserRole) ?? "student";

export const selectIsTeacher = (state: { user: UserState }): boolean =>
  state.user.currentUser?.role === "teacher";

export const selectIsAdmin = (state: { user: UserState }): boolean =>
  state.user.currentUser?.role === "admin";
```

- [ ] **Step 3: authService 增加手机号登录方法**

在 `src/services/authService.ts` 中添加：

```typescript
async sendSmsCode(phone: string, bizType: string = "login"): Promise<void> {
  await httpClient.post(buildApiUrl("/interviewpilot/v1/users/send-sms-code"), null, {
    params: { phone, bizType },
  });
},

async phoneLogin(phone: string, code: string): Promise<UserRespDTO> {
  const res = await httpClient.post<{ token: string; user: UserRespDTO }>(
    buildApiUrl("/interviewpilot/v1/users/phone-login"),
    { phone, code }
  );
  if (res.token) setAuthToken(res.token);
  return res.user;
},
```

同时在 `request.ts` 的 `requiresAuthTokenForRequest` 白名单中增加 `/interviewpilot/v1/users/send-sms-code` 和 `/interviewpilot/v1/users/phone-login`。

- [ ] **Step 4: 重构 AuthGuard 支持角色路由**

创建 `RoleGuard` 组件：

```tsx
// src/components/auth/RoleGuard.tsx
type RoleGuardProps = {
  allowedRoles: UserRole[];
  children: React.ReactNode;
};

export function RoleGuard({ allowedRoles, children }: RoleGuardProps) {
  const role = useAppSelector(selectUserRole);
  if (!allowedRoles.includes(role)) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}
```

- [ ] **Step 5: 重构 router.tsx 增加教师/管理员路由**

```tsx
const router = createBrowserRouter([
  {
    path: "/",
    element: <AppLayout />,
    children: [
      // 公开页面
      { index: true, element: <MarketingHomePage /> },
      { path: "auth", element: <AuthPage /> },

      // 学生页面 (需要登录)
      {
        element: <AuthGuard />,
        children: [
          { path: "profile", element: <StudentProfilePage /> },
          { path: "lobby", element: <LobbyPage /> },
          { path: "interview", element: <InterviewIntroPage /> },
          { path: "interview/precheck", element: <InterviewPrecheckPage /> },
          { path: "interview/room", element: <InterviewPage /> },
          { path: "interview/room/:sessionId", element: <InterviewPage /> },
          { path: "interview/report", element: <InterviewReportPage /> },
          { path: "interview/report/detail", element: <InterviewReportDetailPage /> },
          { path: "chat/:sessionId?", element: <ChatPage /> },
        ],
      },

      // 教师页面
      {
        element: <AuthGuard />,
        children: [
          {
            element: <RoleGuard allowedRoles={["teacher", "admin"]}><TeacherLayout /></RoleGuard>,
            children: [
              { path: "teacher", element: <TeacherDashboardPage /> },
              { path: "teacher/questions", element: <TeacherQuestionBankPage /> },
              { path: "teacher/students", element: <TeacherStudentReportsPage /> },
              { path: "teacher/colleges", element: <TeacherCollegeManagePage /> },
            ],
          },
        ],
      },

      // 管理员页面
      {
        element: <AuthGuard />,
        children: [
          {
            element: <RoleGuard allowedRoles={["admin"]}><AdminLayout /></RoleGuard>,
            children: [
              { path: "admin", element: <AdminDashboardPage /> },
              { path: "admin/users", element: <AdminUserManagePage /> },
            ],
          },
        ],
      },
    ],
  },
]);
```

- [ ] **Step 6: 编译验证**

```bash
cd AI-Meeting-Frontend
npm run typecheck
```

- [ ] **Step 7: Commit**

```bash
git add src/
git commit -m "feat(auth): add multi-role routing with student/teacher/admin RoleGuard"
```

---

### Task 11: 手机号登录前端 UI

**Files:**
- Modify: `src/components/auth/AuthFormCard.tsx`
- Modify: `src/services/authService.ts` (已在 Task 10 完成)

- [ ] **Step 1: 在 AuthFormCard 中增加手机号登录 Tab**

修改 `AuthFormCard.tsx`，在现有用户名密码表单上方增加 Tab 切换：

```tsx
const [loginMode, setLoginMode] = useState<"password" | "phone">("password");

// Tab 切换 UI
<div className="flex gap-2 mb-4">
  <button
    className={cn("flex-1 py-2 text-sm rounded-md", loginMode === "password" ? "bg-primary text-primary-foreground" : "bg-muted")}
    onClick={() => setLoginMode("password")}
  >
    密码登录
  </button>
  <button
    className={cn("flex-1 py-2 text-sm rounded-md", loginMode === "phone" ? "bg-primary text-primary-foreground" : "bg-muted")}
    onClick={() => setLoginMode("phone")}
  >
    验证码登录
  </button>
</div>

// 手机号表单 (loginMode === "phone" 时显示)
{loginMode === "phone" && (
  <form onSubmit={handlePhoneLogin}>
    <Input placeholder="手机号" value={phone} onChange={e => setPhone(e.target.value)} />
    <div className="flex gap-2">
      <Input placeholder="验证码" value={smsCode} onChange={e => setSmsCode(e.target.value)} />
      <Button type="button" onClick={handleSendCode} disabled={countdown > 0}>
        {countdown > 0 ? `${countdown}s` : "发送验证码"}
      </Button>
    </div>
    <Button type="submit" className="w-full">登录</Button>
  </form>
)}
```

- [ ] **Step 2: 实现 handlePhoneLogin 和 handleSendCode**

```typescript
const handleSendCode = async () => {
  if (!phone || !/^1[3-9]\d{9}$/.test(phone)) return;
  await authService.sendSmsCode(phone);
  setCountdown(60);
  // countdown 倒计时逻辑
};

const handlePhoneLogin = async (e: FormEvent) => {
  e.preventDefault();
  const user = await authService.phoneLogin(phone, smsCode);
  dispatch(loginUser.fulfilled(user, "", { username: "", password: "" }));
  navigate("/");
};
```

- [ ] **Step 3: 本地验证 UI**

```bash
cd AI-Meeting-Frontend
npm run dev
# 浏览器打开 http://localhost:5173/auth, 测试 Tab 切换和手机号表单
```

- [ ] **Step 4: Commit**

```bash
git add src/components/auth/AuthFormCard.tsx
git commit -m "feat(auth): add phone SMS login tab to auth form"
```

---

### Task 12: 学生个人中心页面

**Files:**
- Create: `src/services/studentService.ts`
- Create: `src/hooks/profile/useStudentProfile.ts`
- Create: `src/pages/profile/StudentProfilePage.tsx`

- [ ] **Step 1: 创建 studentService.ts**

```typescript
import { httpClient, buildApiUrl } from "@/lib/request";

export const studentService = {
  async getProfile(): Promise<StudentProfileRespDTO> {
    return httpClient.get(buildApiUrl("/interviewpilot/v1/student/profile"));
  },

  async saveProfile(data: StudentProfileSaveReqDTO): Promise<void> {
    return httpClient.put(buildApiUrl("/interviewpilot/v1/student/profile"), data);
  },
};
```

- [ ] **Step 2: 创建 useStudentProfile hook**

使用 React Query 的 `useQuery` + `useMutation` 管理档案数据。

- [ ] **Step 3: 创建 StudentProfilePage**

包含表单：姓名、学校、年级、考生类别（下拉）、目标院校（多选）、目标专业（多选）、训练阶段。使用 `react-hook-form` + `zod` 校验。

- [ ] **Step 4: 编译验证 + Commit**

```bash
npm run typecheck
git add src/
git commit -m "feat(profile): add student profile page with target college/major selection"
```

---

## Phase 5: 面试大厅 + 设备预检 + 数字人 MVP（前端）

### Task 13: 面试大厅页面

**Files:**
- Create: `src/services/questionBankService.ts`
- Create: `src/hooks/lobby/useLobbyData.ts`
- Create: `src/components/lobby/LobbyFilterBar.tsx`
- Create: `src/components/lobby/LobbyCard.tsx`
- Create: `src/components/lobby/LobbyGrid.tsx`
- Create: `src/pages/lobby/LobbyPage.tsx`

- [ ] **Step 1: 创建 questionBankService.ts**

```typescript
export const questionBankService = {
  async pageQuestions(params: QuestionPageParams): Promise<QuestionPageResult> {
    return httpClient.get(buildApiUrl("/interviewpilot/v1/questions/page"), { params });
  },

  async listColleges(): Promise<CollegeRespDTO[]> {
    return httpClient.get(buildApiUrl("/interviewpilot/v1/colleges/list"));
  },

  async listMajors(collegeId?: number): Promise<MajorRespDTO[]> {
    return httpClient.get(buildApiUrl("/interviewpilot/v1/majors/list"), {
      params: collegeId ? { collegeId } : undefined,
    });
  },
};
```

- [ ] **Step 2: 创建 LobbyFilterBar 组件**

筛选器：院校（下拉）、专业（联动下拉）、题型（多选 Chip）、能力点（多选 Chip）、难度（Chip）。使用 React Query 获取院校/专业列表。

- [ ] **Step 3: 创建 LobbyCard 组件**

展示单个面试训练卡片：训练名称、预计时长、题目数量、难度标签、适用对象、最近得分、推荐指数。点击后进入设备预检或直接创建会话。

- [ ] **Step 4: 创建 LobbyPage**

组合 `LobbyFilterBar` + `LobbyGrid`。提供多种入口模式：
- 按院校练习
- 按专业练习
- 按题型练习
- 随机模拟
- 冲刺全真模拟（固定题目数量 + 严格时间控制）

- [ ] **Step 5: 编译验证 + Commit**

```bash
npm run typecheck
git add src/
git commit -m "feat(lobby): add interview lobby page with college/major/type filters"
```

---

### Task 14: 设备预检页面

**Files:**
- Create: `src/pages/interview/InterviewPrecheckPage.tsx`
- Modify: `src/app/router.tsx` (已在 Task 10 添加路由)

- [ ] **Step 1: 创建 InterviewPrecheckPage**

页面流程：
1. 摄像头检测 — 调用 `navigator.mediaDevices.getUserMedia({ video: true })`
2. 麦克风检测 — 调用 `navigator.mediaDevices.getUserMedia({ audio: true })` + 音量可视化
3. 网络检测 — 简单 ping 测速
4. 亮度检测 — 从 video 元素截帧，计算平均亮度
5. 人脸位置检测 — MVP 阶段使用 `canvas` 截帧 + 提示用户自行调整，后期接入前端人脸检测模型

```tsx
export default function InterviewPrecheckPage() {
  const [step, setStep] = useState<"camera" | "mic" | "network" | "ready">("camera");
  const videoRef = useRef<HTMLVideoElement>(null);
  const [cameraOk, setCameraOk] = useState(false);
  const [micOk, setMicOk] = useState(false);
  const [brightness, setBrightness] = useState<number>(0);

  // 1. 启动摄像头
  useEffect(() => {
    navigator.mediaDevices.getUserMedia({ video: true, audio: true })
      .then(stream => {
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
        }
        setCameraOk(true);
        setMicOk(true);
      })
      .catch(err => {
        console.error("设备检测失败:", err);
      });
  }, []);

  // 2. 亮度检测
  const checkBrightness = useCallback(() => {
    if (!videoRef.current) return;
    const canvas = document.createElement("canvas");
    canvas.width = 100;
    canvas.height = 75;
    const ctx = canvas.getContext("2d");
    ctx?.drawImage(videoRef.current, 0, 0, 100, 75);
    const data = ctx?.getImageData(0, 0, 100, 75).data;
    if (!data) return;
    let sum = 0;
    for (let i = 0; i < data.length; i += 4) {
      sum += (data[i] + data[i + 1] + data[i + 2]) / 3;
    }
    setBrightness(sum / (data.length / 4));
  }, []);

  return (
    <div className="max-w-2xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-6">设备预检</h1>
      <video ref={videoRef} autoPlay playsInline muted className="w-full rounded-lg mb-4" />

      {/* 检测结果 */}
      <div className="space-y-2 mb-6">
        <CheckItem label="摄像头" ok={cameraOk} />
        <CheckItem label="麦克风" ok={micOk} />
        <CheckItem label="亮度" ok={brightness > 50} detail={`亮度: ${Math.round(brightness)}`} />
      </div>

      {/* 进入面试 */}
      <Button
        className="w-full"
        disabled={!cameraOk || !micOk}
        onClick={() => navigate("/interview/room")}
      >
        {cameraOk && micOk ? "进入面试" : "请先完成设备检测"}
      </Button>
    </div>
  );
}
```

- [ ] **Step 2: 编译验证 + Commit**

```bash
npm run typecheck
git add src/
git commit -m "feat(precheck): add camera/mic/brightness detection page before interview"
```

---

### Task 15: 数字人 MVP（2D 动画 + TTS 集成）

**Files:**
- Create: `src/components/interview/DigitalHumanAvatar.tsx`
- Modify: `src/pages/interview/InterviewPage.tsx`
- Modify: `src/hooks/interview/session/useInterviewSessionFlow.ts`

- [ ] **Step 1: 创建 DigitalHumanAvatar 组件**

MVP 阶段使用 CSS 动画 + Lottie 实现简单的 2D 数字人形象：

```tsx
type DigitalHumanAvatarProps = {
  isSpeaking: boolean;    // 是否正在播报
  isListening: boolean;   // 是否正在听答
  isThinking: boolean;    // 是否正在思考
  text?: string;          // 当前同步显示的文字
};

export function DigitalHumanAvatar({ isSpeaking, isListening, isThinking, text }: DigitalHumanAvatarProps) {
  return (
    <div className="relative w-full h-48 flex items-center justify-center bg-gradient-to-b from-slate-50 to-slate-100 rounded-lg overflow-hidden">
      {/* 数字人形象区域 */}
      <div className={cn(
        "w-32 h-32 rounded-full bg-gradient-to-br from-cyan-400 to-indigo-500 flex items-center justify-center transition-all duration-300",
        isSpeaking && "animate-pulse scale-105",
        isThinking && "animate-spin-slow",
      )}>
        <span className="text-4xl text-white font-bold">AI</span>
      </div>

      {/* 状态指示 */}
      <div className="absolute bottom-3 left-0 right-0 text-center">
        {isSpeaking && <span className="text-sm text-cyan-600">正在播报...</span>}
        {isListening && <span className="text-sm text-green-600">请回答...</span>}
        {isThinking && <span className="text-sm text-amber-600">思考中...</span>}
      </div>

      {/* 文字同步显示 */}
      {text && (
        <div className="absolute bottom-10 left-4 right-4 bg-white/90 backdrop-blur p-3 rounded text-sm">
          {text}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: 在 InterviewPage 中集成数字人**

修改 `InterviewPage.tsx`，在 ChatRoom 的 `contentOverlay` 中加入 `DigitalHumanAvatar`：

```tsx
contentOverlay={
  <>
    <InterviewCameraOverlay ref={cameraRef} isOpen={isCameraOpen} />
    <DigitalHumanAvatar
      isSpeaking={isTTSSpeaking}
      isListening={isReady && !isInterviewSubmitting}
      isThinking={isInterviewSubmitting}
      text={currentQuestionContent}
    />
  </>
}
```

- [ ] **Step 3: 在 useInterviewSessionFlow 中集成 TTS 播放**

当新题目到达时，自动调用 TTS 播放题目内容：

```typescript
// 在 syncNextQuestion 成功后
import { xunfeiTtsService } from "@/services/xunfeiTtsService";

// 获取题目文本后触发 TTS
const questionText = result.questionContent;
if (questionText) {
  // 调用 TTS 服务获取音频
  const audioUrl = await xunfeiTtsService.synthesize(questionText);
  // 播放音频
  const audio = new Audio(audioUrl);
  setIsTTSSpeaking(true);
  audio.onended = () => setIsTTSSpeaking(false);
  audio.play();
}
```

- [ ] **Step 4: 编译验证 + Commit**

```bash
npm run typecheck
git add src/
git commit -m "feat(digital-human): add 2D avatar MVP with TTS integration for interview room"
```

---

## Phase 6: 教师端 + 管理员端（前端）

### Task 16: 教师端 Layout + 题库管理页面

**Files:**
- Create: `src/components/layout/TeacherLayout.tsx`
- Create: `src/services/teacherService.ts`
- Create: `src/hooks/teacher/useTeacherQuestions.ts`
- Create: `src/components/teacher/QuestionBankTable.tsx`
- Create: `src/components/teacher/QuestionFormDialog.tsx`
- Create: `src/components/teacher/AiGenerateDialog.tsx`
- Create: `src/pages/teacher/TeacherQuestionBankPage.tsx`

- [ ] **Step 1: 创建 TeacherLayout**

```tsx
export default function TeacherLayout() {
  return (
    <div className="flex h-full">
      {/* 教师侧边栏 */}
      <aside className="w-56 border-r bg-muted/30 p-4">
        <h2 className="font-bold mb-4">教师后台</h2>
        <nav className="space-y-1">
          <NavLink to="/teacher" end>总览</NavLink>
          <NavLink to="/teacher/questions">题库管理</NavLink>
          <NavLink to="/teacher/students">学生报告</NavLink>
          <NavLink to="/teacher/colleges">院校管理</NavLink>
        </nav>
      </aside>
      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}
```

- [ ] **Step 2: 创建 teacherService.ts**

封装教师相关 API：题库 CRUD、AI 出题、学生报告查询、教师点评。

- [ ] **Step 3: 创建 QuestionBankTable + QuestionFormDialog + AiGenerateDialog**

- `QuestionBankTable`：DataTable 展示题目列表，支持筛选、排序、批量审核
- `QuestionFormDialog`：新建/编辑题目表单（标题、正文、题型、院校、专业、能力点、难度、参考答案、评分规则、追问题）
- `AiGenerateDialog`：AI 出题表单（院校、专业、题型、能力点、数量、难度、是否生成追问/评分标准），调用 `questionBankService.aiGenerate()` 后展示结果供审核

- [ ] **Step 4: 创建 TeacherQuestionBankPage**

组合筛选器 + QuestionBankTable + AI 出题按钮 + 新建题目按钮。

- [ ] **Step 5: 编译验证 + Commit**

```bash
npm run typecheck
git add src/
git commit -m "feat(teacher): add teacher layout and question bank management page with AI generation"
```

---

### Task 17: 教师端 — 学生报告查看 + 点评

**Files:**
- Create: `src/components/teacher/StudentReportList.tsx`
- Create: `src/components/teacher/TeacherReviewForm.tsx`
- Create: `src/pages/teacher/TeacherStudentReportsPage.tsx`

- [ ] **Step 1: 创建 StudentReportList**

展示学生列表（头像、姓名、训练次数、最近得分、高频问题）。点击展开该学生的面试记录列表。复用现有的 `InterviewScoreAndRadarCard`、`InterviewQaReplayCard` 组件展示单次报告。

- [ ] **Step 2: 创建 TeacherReviewForm**

点评输入表单：点评内容（富文本）、调整分数（可选）、标记优秀样本（Checkbox）、标记模型误判（Checkbox）。

- [ ] **Step 3: 创建 TeacherStudentReportsPage**

组合 StudentReportList + 报告详情面板 + TeacherReviewForm。

- [ ] **Step 4: 编译验证 + Commit**

```bash
npm run typecheck
git add src/
git commit -m "feat(teacher): add student report viewing and teacher review/comment form"
```

---

### Task 18: 教师端 — 院校/专业/考纲管理

**Files:**
- Create: `src/services/collegeService.ts`
- Create: `src/pages/teacher/TeacherCollegeManagePage.tsx`

- [ ] **Step 1: 创建 collegeService.ts**

封装院校 CRUD、专业 CRUD、考纲 CRUD API。

- [ ] **Step 2: 创建 TeacherCollegeManagePage**

Tab 切换三个管理面板：
- 院校管理：DataTable + 新建/编辑 Dialog
- 专业管理：关联院校的 DataTable + 新建/编辑 Dialog
- 考纲管理：上传 PDF/Word + AI 解析 + 审核流程

- [ ] **Step 3: 编译验证 + Commit**

```bash
npm run typecheck
git add src/
git commit -m "feat(teacher): add college/major/exam outline management page"
```

---

### Task 19: 管理员端 Layout + 用户管理 + 数据看板

**Files:**
- Create: `src/components/layout/AdminLayout.tsx`
- Create: `src/pages/admin/AdminDashboardPage.tsx`
- Create: `src/pages/admin/AdminUserManagePage.tsx`

- [ ] **Step 1: 创建 AdminLayout**

类似 TeacherLayout，侧边栏包含：总览、用户管理、内容安全。

- [ ] **Step 2: 创建 AdminUserManagePage**

用户列表（DataTable）：用户名、手机号、角色、注册时间、最近活跃。支持搜索、角色筛选、角色修改（student/teacher/admin 切换）。

- [ ] **Step 3: 创建 AdminDashboardPage**

数据看板：
- 统计卡片：注册人数、今日活跃、本周训练次数、平均得分
- 趋势图：近 7 天训练次数折线图
- 热门院校/专业排行
- AI 评分与教师评分偏差统计

MVP 阶段使用简单的统计卡片 + 数字展示，图表后期引入 recharts。

- [ ] **Step 4: 编译验证 + Commit**

```bash
npm run typecheck
git add src/
git commit -m "feat(admin): add admin layout, user management, and data dashboard pages"
```

---

## Phase 7: 面试报告增强（前端）

### Task 20: 面试报告 7 维评分展示

**Files:**
- Modify: `src/components/interview/report/InterviewScoreAndRadarCard.tsx`
- Modify: `src/pages/interview/InterviewReportPage.tsx`
- Modify: `src/services/interviewService.ts` (normalize 函数)

- [ ] **Step 1: 扩展 InterviewRadarMetric 类型**

在类型定义中增加 7 维度字段映射：

```typescript
type InterviewRadarMetric = {
  label: string;
  value: number;
  maxValue: number;
  color?: string;
};

// 报告数据中新增
type InterviewDimensionScores = {
  contentScore?: number;
  logicScore?: number;
  professionalScore?: number;
  expressionScore?: number;
  adaptabilityScore?: number;
  timeControlScore?: number;
  etiquetteScore?: number;
  compositeScore?: number;
};
```

- [ ] **Step 2: 修改 InterviewScoreAndRadarCard 展示 7 维度**

在现有雷达图下方新增 7 维度条形图：

```tsx
const dimensionLabels = [
  { key: "contentScore", label: "内容质量", max: 30, color: "bg-blue-500" },
  { key: "logicScore", label: "逻辑结构", max: 15, color: "bg-green-500" },
  { key: "professionalScore", label: "专业匹配", max: 15, color: "bg-purple-500" },
  { key: "expressionScore", label: "语言表达", max: 15, color: "bg-amber-500" },
  { key: "adaptabilityScore", label: "临场应变", max: 10, color: "bg-red-500" },
  { key: "timeControlScore", label: "时间控制", max: 5, color: "bg-cyan-500" },
  { key: "etiquetteScore", label: "礼仪仪态", max: 10, color: "bg-pink-500" },
];

{dimensionLabels.map(dim => (
  <div key={dim.key} className="flex items-center gap-2">
    <span className="w-20 text-sm text-muted-foreground">{dim.label}</span>
    <div className="flex-1 bg-muted rounded-full h-3">
      <div
        className={cn("h-full rounded-full", dim.color)}
        style={{ width: `${(scores[dim.key] / 100) * 100}%` }}
      />
    </div>
    <span className="w-8 text-sm font-medium">{scores[dim.key]}</span>
  </div>
))}
```

- [ ] **Step 3: 修改 normalizeInterviewRadarChart 解析新字段**

在 `interviewService.ts` 的 normalize 函数中，映射后端返回的 7 维度字段。

- [ ] **Step 4: 修改 InterviewReportPage 传递新数据**

从 `useInterviewReportData` 中提取 7 维度分数，传递给 `InterviewScoreAndRadarCard`。

- [ ] **Step 5: 编译验证 + Commit**

```bash
npm run typecheck
git add src/
git commit -m "feat(report): display 7-dimension scoring in interview report with bar chart"
```

---

### Task 21: 面试报告 — 礼仪反馈 + 参考答案 + 教师点评展示

**Files:**
- Create: `src/components/interview/report/InterviewEtiquetteCard.tsx`
- Create: `src/components/interview/report/InterviewReferenceAnswerCard.tsx`
- Create: `src/components/interview/report/InterviewTeacherReviewCard.tsx`
- Modify: `src/pages/interview/InterviewReportPage.tsx`

- [ ] **Step 1: 创建 InterviewEtiquetteCard**

展示礼仪评分详情：入镜位置、坐姿、视线、表情、着装、低头频率、打招呼。数据来自后端 `demeanor-evaluation` 接口的扩展输出。

- [ ] **Step 2: 创建 InterviewReferenceAnswerCard**

展示每道题的参考答案/高分示范思路。数据从题库的 `referenceAnswer` 字段获取，或由 AI 生成。

- [ ] **Step 3: 创建 InterviewTeacherReviewCard**

展示教师点评列表：点评内容、调整分数、优秀样本/模型误判标记。数据从 `teacherService.getSessionReviews()` 获取。

- [ ] **Step 4: 在 InterviewReportPage 中集成新组件**

在现有布局下方增加新 section：

```tsx
{/* 礼仪反馈 */}
<InterviewEtiquetteCard demeanorData={demeanorData} isLoading={isLoading} />

{/* 参考答案 */}
<InterviewReferenceAnswerCard qaReviews={qaReviews} isLoading={isLoading} />

{/* 教师点评 */}
<InterviewTeacherReviewCard sessionId={sessionId} />
```

- [ ] **Step 5: 编译验证 + Commit**

```bash
npm run typecheck
git add src/
git commit -m "feat(report): add etiquette feedback, reference answers, and teacher review display"
```

---

## Phase 8: 收尾与集成

### Task 22: 登录页默认跳转逻辑适配

**Files:**
- Modify: `src/hooks/auth/useAuthPageController.ts`
- Modify: `src/pages/auth/AuthPage.tsx`

- [ ] **Step 1: 修改登录成功后的跳转逻辑**

```typescript
// 登录成功后根据角色跳转
const onLoginSuccess = (user: UserRespDTO) => {
  switch (user.role) {
    case "admin":
      navigate("/admin");
      break;
    case "teacher":
      navigate("/teacher");
      break;
    default:
      navigate("/lobby");  // 学生默认跳面试大厅
  }
};
```

- [ ] **Step 2: 修改营销首页 CTA 按钮**

已登录学生跳 `/lobby`，未登录跳 `/auth`。

- [ ] **Step 3: Commit**

```bash
git add src/
git commit -m "feat(nav): redirect to role-appropriate page after login"
```

---

### Task 23: 首页营销页 + 导航适配

**Files:**
- Modify: `src/components/layout/Sidebar.tsx` (或 Sidebar 子组件)
- Modify: `src/pages/marketing/MarketingHomePage.tsx`

- [ ] **Step 1: 侧边栏增加学生导航**

在 Sidebar 中增加：
- 面试大厅入口
- 个人中心入口
- 教师后台入口（仅 teacher/admin 可见）
- 管理后台入口（仅 admin 可见）

- [ ] **Step 2: Commit**

```bash
git add src/
git commit -m "feat(nav): add lobby, profile, and role-based admin links to sidebar"
```

---

### Task 24: 全量编译 + 测试 + 部署验证

- [ ] **Step 1: 后端全量编译**

```bash
cd AI-Meeting
./mvnw -B -ntp clean verify -Dmaven.test.skip=true
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 前端全量检查**

```bash
cd AI-Meeting-Frontend
npm run check
npm run build
```

Expected: 无 lint 错误，无类型错误，build 成功

- [ ] **Step 3: 数据库初始化**

```bash
# 执行所有 SQL
docker exec -i ai-meeting-mysql mysql -uroot -p123456 mainshi_agent < admin/src/main/resources/sql/v1_tables.sql
```

- [ ] **Step 4: 启动后端 + 前端，手动验证核心流程**

1. 注册学生账号 → 手机号登录
2. 完善个人档案（选择目标院校/专业）
3. 进入面试大厅 → 选择院校/专业 → 开始面试
4. 设备预检 → 进入面试间 → 回答问题 → 查看报告
5. 教师登录 → 查看题库 → AI 出题 → 审核发布
6. 教师查看学生报告 → 添加点评
7. 管理员登录 → 查看用户管理 → 数据看板

- [ ] **Step 5: Final Commit**

```bash
cd AI-Meeting
git add -A
git commit -m "feat: complete V1 MVP - vocational interview system with question bank, 7-dim scoring, teacher/admin backends"

cd ../AI-Meeting-Frontend
git add -A
git commit -m "feat: complete V1 MVP frontend - lobby, precheck, digital human, teacher/admin pages"
```

---

## 附录: V1.5+ 后续规划

| 功能 | 复杂度 | 说明 |
|------|--------|------|
| RAG 知识库 | 高 | 引入向量数据库 (Milvus/PGVector)，构建院校/考纲/优秀答案知识库 |
| AI 拓题增强 | 中 | 基于已审核考纲 + 优秀答案做 few-shot 出题 |
| 老师复评增强 | 低 | 批量审核、评分偏差统计 |
| 音视频录制存储 | 中 | ASR 流录制为音频文件，视频截帧存储 |
| 无领导小组讨论 | 极高 | 多人实时音视频 + AI 主持 + 多人发言识别 |
| 订单/套餐系统 | 中 | 支付对接 + 套餐管理 + 免费体验次数 |
| 移动端 H5 深度适配 | 中 | 微信浏览器兼容 + 响应式优化 |
| 3D 数字人升级 | 高 | Unity WebGL / Ready Player Me / 第三方 SaaS |
| 智能学习路径 | 高 | 基于薄弱项的个性化训练计划 |
