SET NAMES utf8mb4;

SET @current_schema = DATABASE();

SET @ai_properties_add_is_default = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @current_schema
              AND TABLE_NAME = 'ai_properties'
              AND COLUMN_NAME = 'is_default'
        ),
        'SELECT 1',
        'ALTER TABLE `ai_properties` ADD COLUMN `is_default` TINYINT(1) NULL DEFAULT 0 AFTER `is_enabled`'
    )
);
PREPARE ai_properties_add_is_default_stmt FROM @ai_properties_add_is_default;
EXECUTE ai_properties_add_is_default_stmt;
DEALLOCATE PREPARE ai_properties_add_is_default_stmt;

UPDATE ai_properties
SET is_default = CASE WHEN id = 1 THEN 1 ELSE 0 END
WHERE del_flag = 0;
