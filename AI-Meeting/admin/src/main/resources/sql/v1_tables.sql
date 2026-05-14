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

-- ============================================================
-- 13. 用户表扩展字段
-- ============================================================
ALTER TABLE `t_user` ADD COLUMN `role` varchar(32) NOT NULL DEFAULT 'student' COMMENT 'student|teacher|admin' AFTER `mail`;
ALTER TABLE `t_user` ADD COLUMN `open_id` varchar(128) DEFAULT NULL COMMENT '微信OpenID' AFTER `role`;
ALTER TABLE `t_user` ADD INDEX `idx_role` (`role`);
ALTER TABLE `t_user` ADD INDEX `idx_open_id` (`open_id`);

-- ============================================================
-- 14. 面试记录表扩展字段
-- ============================================================
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
