ALTER TABLE agent_properties ADD COLUMN scene_code VARCHAR(100) DEFAULT NULL COMMENT '绑定的业务场景编码';
ALTER TABLE agent_properties ADD COLUMN is_active TINYINT(1) DEFAULT 0 COMMENT '是否为该场景的当前激活agent';

UPDATE agent_properties SET scene_code = 'interview-question-extraction', is_active = 1 WHERE id = 8;
UPDATE agent_properties SET scene_code = 'interview-demeanor', is_active = 1 WHERE id = 9;
UPDATE agent_properties SET scene_code = 'interview-answer-evaluation', is_active = 1 WHERE id = 11;
UPDATE agent_properties SET scene_code = 'interview-question-asking', is_active = 1 WHERE id = 12;
UPDATE agent_properties SET scene_code = 'general-agent-chat', is_active = 1 WHERE id = 13;
