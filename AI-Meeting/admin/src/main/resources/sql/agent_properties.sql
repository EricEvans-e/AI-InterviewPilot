SET NAMES utf8mb4;

INSERT INTO agent_properties (
    id, agent_name, api_secret, api_key, api_flow_id, ai_provider,
    scene_code, is_active, create_time, update_time, del_flag
) VALUES
(8, 'Mimo Interview Question Extractor', 'mimo-v2.5', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'interview-question-extraction', 1, NOW(), NOW(), 0),
(9, 'Mimo Demeanor Analyst', 'mimo-v2.5', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'interview-demeanor', 1, NOW(), NOW(), 0),
(11, 'Mimo Answer Evaluator', 'mimo-v2.5', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'interview-answer-evaluation', 1, NOW(), NOW(), 0),
(12, 'Mimo Interview Questioner', 'mimo-v2.5', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'interview-question-asking', 1, NOW(), NOW(), 0),
(13, 'Mimo General Agent', 'mimo-v2.5', 'MIMO_API_KEY', 'https://token-plan-cn.xiaomimimo.com/v1', 'openai',
 'general-agent-chat', 1, NOW(), NOW(), 0);
