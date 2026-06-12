SET NAMES utf8mb4;

INSERT INTO ai_properties (
    id, ai_name, ai_type, api_key, api_secret, api_url, model_name,
    max_tokens, temperature, system_prompt, is_enabled, is_default,
    enable_thinking, thinking_budget_tokens, create_time, update_time,
    del_flag, project_id, organization_id
) VALUES
(1, 'Mimo V2.5', 'openai', 'MIMO_API_KEY', null, 'https://token-plan-cn.xiaomimimo.com/v1', 'mimo-v2.5',
 8192, 0.70, 'You are the InterviewPilot assistant. Provide accurate and useful answers.', 1, 1,
 0, null, NOW(), NOW(), 0, null, null),
(2, 'Mimo V2.5 Pro', 'openai', 'MIMO_API_KEY', null, 'https://token-plan-cn.xiaomimimo.com/v1', 'mimo-v2.5-pro',
 8192, 0.70, 'You are the InterviewPilot assistant. Provide accurate and useful answers.', 1, 0,
 1, 4096, NOW(), NOW(), 0, null, null);
