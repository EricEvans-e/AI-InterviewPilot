SET NAMES utf8mb4;

-- DDL 迁移：为已有表添加 ai_provider 列
ALTER TABLE agent_properties ADD COLUMN ai_provider VARCHAR(50) DEFAULT 'xingchen' COMMENT 'AI 提供商: xingchen, anthropic';

-- 原有 XingChen Agent（ai_provider 默认为 xingchen）
INSERT INTO agent_properties (id, agent_name, api_secret, api_key, api_flow_id, create_time, update_time, del_flag) VALUES (8, '面试出题官', 'ZjMyNTgyY2RiYmEzMWQ0NGQzYTVmMTNj', '86f9f60da8d9f65a979dec8ed4978997', '7459045326128074752', '2025-07-14 13:24:03', '2025-07-14 13:24:03', 0);
INSERT INTO agent_properties (id, agent_name, api_secret, api_key, api_flow_id, create_time, update_time, del_flag) VALUES (9, '神态分析官', 'ZjMyNTgyY2RiYmEzMWQ0NGQzYTVmMTNj', '86f9f60da8d9f65a979dec8ed4978997', '7459030807713374208', '2025-07-15 13:46:36', '2025-07-15 13:46:36', 0);
INSERT INTO agent_properties (id, agent_name, api_secret, api_key, api_flow_id, create_time, update_time, del_flag) VALUES (11, '用户答案评分官', 'ZjMyNTgyY2RiYmEzMWQ0NGQzYTVmMTNj', '86f9f60da8d9f65a979dec8ed4978997', '7459027621355634688', '2025-07-15 15:51:27', '2025-07-15 15:51:27', 0);
INSERT INTO agent_properties (id, agent_name, api_secret, api_key, api_flow_id, create_time, update_time, del_flag) VALUES (12, '面试提问官', 'ZjMyNTgyY2RiYmEzMWQ0NGQzYTVmMTNj', '86f9f60da8d9f65a979dec8ed4978997', '7459029937422643200', '2026-03-18 22:04:34', '2026-03-18 22:04:30', 0);

-- Anthropic Agent（ai_provider = anthropic, field reuse: api_secret=模型名, api_flow_id=API URL）
INSERT INTO agent_properties (agent_name, api_secret, api_key, api_flow_id, ai_provider, del_flag, create_time, update_time) VALUES
('Mimo面试评分官', 'mimo-v2.5-pro', 'tp-s7h68tp5edc1zh2co9cw7n9oeakrkwp7fcwwajwom0rdo7wt', 'https://token-plan-sgp.xiaomimimo.com/anthropic', 'anthropic', 0, NOW(), NOW()),
('Mimo面试提问官', 'mimo-v2.5-pro', 'tp-s7h68tp5edc1zh2co9cw7n9oeakrkwp7fcwwajwom0rdo7wt', 'https://token-plan-sgp.xiaomimimo.com/anthropic', 'anthropic', 0, NOW(), NOW()),
('Mimo面试出题官', 'mimo-v2.5-pro', 'tp-s7h68tp5edc1zh2co9cw7n9oeakrkwp7fcwwajwom0rdo7wt', 'https://token-plan-sgp.xiaomimimo.com/anthropic', 'anthropic', 0, NOW(), NOW()),
('Mimo神态分析官', 'mimo-v2.5-pro', 'tp-s7h68tp5edc1zh2co9cw7n9oeakrkwp7fcwwajwom0rdo7wt', 'https://token-plan-sgp.xiaomimimo.com/anthropic', 'anthropic', 0, NOW(), NOW());
