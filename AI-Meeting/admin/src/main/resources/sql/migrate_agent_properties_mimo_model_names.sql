SET NAMES utf8mb4;

-- Keep agent names aligned with the actual Mimo model each scene uses.
-- Text-only scenes expose both mimo-v2.5 and mimo-v2.5-pro choices.
-- Resume question extraction and demeanor/image analysis remain on mimo-v2.5 because
-- scanned resumes and camera frames require a vision-capable model.

INSERT INTO agent_properties (
    id, agent_name, api_secret, api_key, api_flow_id, ai_provider,
    scene_code, is_active, create_time, update_time, del_flag
) VALUES
(8, 'Mimo 2.5 面试出题官', 'mimo-v2.5', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'interview-question-extraction', 1, NOW(), NOW(), 0),
(9, 'Mimo 2.5 神态分析官', 'mimo-v2.5', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'interview-demeanor', 1, NOW(), NOW(), 0),
(11, 'Mimo 2.5 答案评分官', 'mimo-v2.5', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'interview-answer-evaluation', 1, NOW(), NOW(), 0),
(12, 'Mimo 2.5 面试提问官', 'mimo-v2.5', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'interview-question-asking', 1, NOW(), NOW(), 0),
(13, 'Mimo 2.5 通用智能体', 'mimo-v2.5', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'general-agent-chat', 1, NOW(), NOW(), 0),
(14, 'Mimo 2.5 Pro 答案评分官', 'mimo-v2.5-pro', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'interview-answer-evaluation', 0, NOW(), NOW(), 0),
(15, 'Mimo 2.5 Pro 面试提问官', 'mimo-v2.5-pro', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'interview-question-asking', 0, NOW(), NOW(), 0),
(16, 'Mimo 2.5 Pro 通用智能体', 'mimo-v2.5-pro', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'general-agent-chat', 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    agent_name = VALUES(agent_name),
    api_secret = VALUES(api_secret),
    api_key = VALUES(api_key),
    api_flow_id = VALUES(api_flow_id),
    ai_provider = VALUES(ai_provider),
    scene_code = VALUES(scene_code),
    is_active = VALUES(is_active),
    update_time = NOW(),
    del_flag = VALUES(del_flag);

UPDATE agent_properties
SET is_active = 0, update_time = NOW()
WHERE del_flag = 0
  AND id NOT IN (8, 9, 11, 12, 13)
  AND scene_code IN (
      'interview-question-extraction',
      'interview-answer-evaluation',
      'interview-question-asking',
      'interview-demeanor',
      'general-agent-chat'
  );
