SET NAMES utf8mb4;

ALTER TABLE ai_properties
    ADD COLUMN is_default TINYINT(1) NULL DEFAULT 0 AFTER is_enabled;

UPDATE ai_properties
SET is_default = CASE WHEN id = 1 THEN 1 ELSE 0 END
WHERE del_flag = 0;
