-- 场景绑定：每个场景可有一个激活的 agent
ALTER TABLE agent_properties ADD COLUMN scene_code VARCHAR(100) DEFAULT NULL COMMENT '绑定的业务场景编码';
ALTER TABLE agent_properties ADD COLUMN is_active TINYINT(1) DEFAULT 0 COMMENT '是否为该场景的当前激活agent';

-- Seed: 将现有星辰 agent 绑定为各场景的默认激活 agent
UPDATE agent_properties SET scene_code = 'interview-question-extraction', is_active = 1 WHERE id = 8;
UPDATE agent_properties SET scene_code = 'interview-demeanor', is_active = 1 WHERE id = 9;
UPDATE agent_properties SET scene_code = 'interview-answer-evaluation', is_active = 1 WHERE id = 11;
UPDATE agent_properties SET scene_code = 'interview-question-asking', is_active = 1 WHERE id = 12;

-- Seed: 为 Mimo agent 设置 scene_code（默认不激活）
UPDATE agent_properties SET scene_code = 'interview-answer-evaluation' WHERE id = 13;
UPDATE agent_properties SET scene_code = 'interview-question-asking' WHERE id = 14;
UPDATE agent_properties SET scene_code = 'interview-question-extraction' WHERE id = 15;
UPDATE agent_properties SET scene_code = 'interview-demeanor' WHERE id = 16;
