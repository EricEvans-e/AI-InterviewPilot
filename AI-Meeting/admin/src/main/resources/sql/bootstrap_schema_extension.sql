SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `student_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `school_name` varchar(128) DEFAULT NULL COMMENT 'school name',
  `grade` varchar(32) DEFAULT NULL COMMENT 'grade',
  `exam_category` varchar(64) DEFAULT NULL COMMENT 'exam category',
  `training_stage` varchar(32) DEFAULT NULL COMMENT 'training stage',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='student profile';

CREATE TABLE IF NOT EXISTS `student_target_college` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `college_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_college` (`user_id`, `college_id`),
  KEY `idx_college` (`college_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='student target college';

CREATE TABLE IF NOT EXISTS `student_target_major` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `major_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_major` (`user_id`, `major_id`),
  KEY `idx_major` (`major_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='student target major';

CREATE TABLE IF NOT EXISTS `college` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `code` varchar(32) DEFAULT NULL,
  `type` varchar(64) DEFAULT NULL,
  `province` varchar(32) DEFAULT '浙江',
  `city` varchar(64) DEFAULT NULL,
  `level` varchar(32) DEFAULT NULL,
  `official_url` varchar(512) DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_province` (`province`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='college';

CREATE TABLE IF NOT EXISTS `major` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `college_id` bigint NOT NULL,
  `name` varchar(128) NOT NULL,
  `code` varchar(32) DEFAULT NULL,
  `category` varchar(64) DEFAULT NULL,
  `target_type` varchar(64) DEFAULT NULL,
  `test_form` varchar(128) DEFAULT NULL,
  `test_content` text DEFAULT NULL,
  `score_structure` text DEFAULT NULL,
  `year` int DEFAULT 2026,
  `official_url` varchar(512) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_college` (`college_id`),
  KEY `idx_name` (`name`),
  KEY `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='major';

CREATE TABLE IF NOT EXISTS `exam_outline` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `college_id` bigint DEFAULT NULL,
  `major_id` bigint DEFAULT NULL,
  `year` int DEFAULT 2026,
  `title` varchar(256) NOT NULL,
  `doc_type` varchar(32) NOT NULL,
  `content` longtext DEFAULT NULL,
  `file_url` varchar(1024) DEFAULT NULL,
  `source_url` varchar(1024) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'pending',
  `uploader_id` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_college` (`college_id`),
  KEY `idx_major` (`major_id`),
  KEY `idx_year` (`year`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='exam outline';

CREATE TABLE IF NOT EXISTS `question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(256) NOT NULL,
  `content` text NOT NULL,
  `question_type` varchar(32) NOT NULL,
  `college_id` bigint DEFAULT NULL,
  `major_id` bigint DEFAULT NULL,
  `ability_tag` varchar(64) DEFAULT NULL,
  `difficulty` varchar(16) DEFAULT 'medium',
  `answer_time_seconds` int DEFAULT 120,
  `reference_answer` text DEFAULT NULL,
  `scoring_rule` text DEFAULT NULL,
  `follow_up_rule` text DEFAULT NULL,
  `follow_up_questions` text DEFAULT NULL,
  `source_ref` varchar(512) DEFAULT NULL,
  `is_ai_generated` tinyint(1) NOT NULL DEFAULT 0,
  `status` varchar(32) NOT NULL DEFAULT 'draft',
  `creator_id` bigint DEFAULT NULL,
  `year` int DEFAULT 2026,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`question_type`),
  KEY `idx_college` (`college_id`),
  KEY `idx_major` (`major_id`),
  KEY `idx_ability` (`ability_tag`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_status` (`status`),
  KEY `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='question bank';

CREATE TABLE IF NOT EXISTS `interview_session_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `question_id` bigint NOT NULL,
  `seq_index` int NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`),
  UNIQUE KEY `uk_session_seq` (`session_id`, `seq_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='interview session question';

CREATE TABLE IF NOT EXISTS `scoring_weight_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_name` varchar(128) NOT NULL,
  `college_id` bigint DEFAULT NULL,
  `major_id` bigint DEFAULT NULL,
  `w_content` decimal(5,2) NOT NULL DEFAULT 30.00,
  `w_logic` decimal(5,2) NOT NULL DEFAULT 15.00,
  `w_professional` decimal(5,2) NOT NULL DEFAULT 15.00,
  `w_expression` decimal(5,2) NOT NULL DEFAULT 15.00,
  `w_adaptability` decimal(5,2) NOT NULL DEFAULT 10.00,
  `w_time_control` decimal(5,2) NOT NULL DEFAULT 5.00,
  `w_etiquette` decimal(5,2) NOT NULL DEFAULT 10.00,
  `is_default` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_college_major` (`college_id`, `major_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='scoring weight config';

INSERT IGNORE INTO `scoring_weight_config`
(`config_name`, `w_content`, `w_logic`, `w_professional`, `w_expression`, `w_adaptability`, `w_time_control`, `w_etiquette`, `is_default`)
VALUES ('默认权重', 30.00, 15.00, 15.00, 15.00, 10.00, 5.00, 10.00, 1);

CREATE TABLE IF NOT EXISTS `teacher_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `teacher_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `content` text NOT NULL,
  `adjusted_score` int DEFAULT NULL,
  `is_excellent_sample` tinyint(1) DEFAULT 0,
  `is_model_misjudge` tinyint(1) DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`),
  KEY `idx_teacher` (`teacher_id`),
  KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='teacher review';

ALTER TABLE `t_user`
    COMMENT = 'bootstrap user extension anchor';

SET @current_schema = DATABASE();

SET @t_user_add_role = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 't_user'
              AND COLUMN_NAME = 'role'
        ),
        'SELECT 1',
        'ALTER TABLE `t_user` ADD COLUMN `role` varchar(32) NOT NULL DEFAULT ''student'' COMMENT ''student|teacher|admin'' AFTER `mail`'
    )
);
PREPARE t_user_add_role_stmt FROM @t_user_add_role;
EXECUTE t_user_add_role_stmt;
DEALLOCATE PREPARE t_user_add_role_stmt;

SET @t_user_add_open_id = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 't_user'
              AND COLUMN_NAME = 'open_id'
        ),
        'SELECT 1',
        'ALTER TABLE `t_user` ADD COLUMN `open_id` varchar(128) DEFAULT NULL COMMENT ''wechat open id'' AFTER `role`'
    )
);
PREPARE t_user_add_open_id_stmt FROM @t_user_add_open_id;
EXECUTE t_user_add_open_id_stmt;
DEALLOCATE PREPARE t_user_add_open_id_stmt;

SET @interview_record_add_content_score = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'interview_record'
              AND COLUMN_NAME = 'content_score'
        ),
        'SELECT 1',
        'ALTER TABLE `interview_record` ADD COLUMN `content_score` int DEFAULT NULL AFTER `resume_score`'
    )
);
PREPARE interview_record_add_content_score_stmt FROM @interview_record_add_content_score;
EXECUTE interview_record_add_content_score_stmt;
DEALLOCATE PREPARE interview_record_add_content_score_stmt;

SET @interview_record_add_logic_score = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'interview_record'
              AND COLUMN_NAME = 'logic_score'
        ),
        'SELECT 1',
        'ALTER TABLE `interview_record` ADD COLUMN `logic_score` int DEFAULT NULL AFTER `content_score`'
    )
);
PREPARE interview_record_add_logic_score_stmt FROM @interview_record_add_logic_score;
EXECUTE interview_record_add_logic_score_stmt;
DEALLOCATE PREPARE interview_record_add_logic_score_stmt;

SET @interview_record_add_professional_score = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'interview_record'
              AND COLUMN_NAME = 'professional_score'
        ),
        'SELECT 1',
        'ALTER TABLE `interview_record` ADD COLUMN `professional_score` int DEFAULT NULL AFTER `logic_score`'
    )
);
PREPARE interview_record_add_professional_score_stmt FROM @interview_record_add_professional_score;
EXECUTE interview_record_add_professional_score_stmt;
DEALLOCATE PREPARE interview_record_add_professional_score_stmt;

SET @interview_record_add_expression_score = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'interview_record'
              AND COLUMN_NAME = 'expression_score'
        ),
        'SELECT 1',
        'ALTER TABLE `interview_record` ADD COLUMN `expression_score` int DEFAULT NULL AFTER `professional_score`'
    )
);
PREPARE interview_record_add_expression_score_stmt FROM @interview_record_add_expression_score;
EXECUTE interview_record_add_expression_score_stmt;
DEALLOCATE PREPARE interview_record_add_expression_score_stmt;

SET @interview_record_add_adaptability_score = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'interview_record'
              AND COLUMN_NAME = 'adaptability_score'
        ),
        'SELECT 1',
        'ALTER TABLE `interview_record` ADD COLUMN `adaptability_score` int DEFAULT NULL AFTER `expression_score`'
    )
);
PREPARE interview_record_add_adaptability_score_stmt FROM @interview_record_add_adaptability_score;
EXECUTE interview_record_add_adaptability_score_stmt;
DEALLOCATE PREPARE interview_record_add_adaptability_score_stmt;

SET @interview_record_add_time_control_score = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'interview_record'
              AND COLUMN_NAME = 'time_control_score'
        ),
        'SELECT 1',
        'ALTER TABLE `interview_record` ADD COLUMN `time_control_score` int DEFAULT NULL AFTER `adaptability_score`'
    )
);
PREPARE interview_record_add_time_control_score_stmt FROM @interview_record_add_time_control_score;
EXECUTE interview_record_add_time_control_score_stmt;
DEALLOCATE PREPARE interview_record_add_time_control_score_stmt;

SET @interview_record_add_etiquette_score = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'interview_record'
              AND COLUMN_NAME = 'etiquette_score'
        ),
        'SELECT 1',
        'ALTER TABLE `interview_record` ADD COLUMN `etiquette_score` int DEFAULT NULL AFTER `time_control_score`'
    )
);
PREPARE interview_record_add_etiquette_score_stmt FROM @interview_record_add_etiquette_score;
EXECUTE interview_record_add_etiquette_score_stmt;
DEALLOCATE PREPARE interview_record_add_etiquette_score_stmt;

SET @interview_record_add_composite_score = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'interview_record'
              AND COLUMN_NAME = 'composite_score'
        ),
        'SELECT 1',
        'ALTER TABLE `interview_record` ADD COLUMN `composite_score` int DEFAULT NULL AFTER `etiquette_score`'
    )
);
PREPARE interview_record_add_composite_score_stmt FROM @interview_record_add_composite_score;
EXECUTE interview_record_add_composite_score_stmt;
DEALLOCATE PREPARE interview_record_add_composite_score_stmt;

SET @interview_record_add_college_id = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'interview_record'
              AND COLUMN_NAME = 'college_id'
        ),
        'SELECT 1',
        'ALTER TABLE `interview_record` ADD COLUMN `college_id` bigint DEFAULT NULL AFTER `composite_score`'
    )
);
PREPARE interview_record_add_college_id_stmt FROM @interview_record_add_college_id;
EXECUTE interview_record_add_college_id_stmt;
DEALLOCATE PREPARE interview_record_add_college_id_stmt;

SET @interview_record_add_major_id = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'interview_record'
              AND COLUMN_NAME = 'major_id'
        ),
        'SELECT 1',
        'ALTER TABLE `interview_record` ADD COLUMN `major_id` bigint DEFAULT NULL AFTER `college_id`'
    )
);
PREPARE interview_record_add_major_id_stmt FROM @interview_record_add_major_id;
EXECUTE interview_record_add_major_id_stmt;
DEALLOCATE PREPARE interview_record_add_major_id_stmt;

SET @interview_record_add_session_mode = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'interview_record'
              AND COLUMN_NAME = 'session_mode'
        ),
        'SELECT 1',
        'ALTER TABLE `interview_record` ADD COLUMN `session_mode` varchar(32) DEFAULT ''resume'' AFTER `major_id`'
    )
);
PREPARE interview_record_add_session_mode_stmt FROM @interview_record_add_session_mode;
EXECUTE interview_record_add_session_mode_stmt;
DEALLOCATE PREPARE interview_record_add_session_mode_stmt;
