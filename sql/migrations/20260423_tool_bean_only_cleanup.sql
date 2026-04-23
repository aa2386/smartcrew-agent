UPDATE tool_definition
SET bean_name = CASE tool_code
    WHEN 'basic' THEN 'basicTools'
    WHEN 'document' THEN 'documentTools'
    WHEN 'file' THEN 'fileTools'
    WHEN 'image-search' THEN 'imageSearchTools'
    WHEN 'plant-uml' THEN 'plantUmlTools'
    WHEN 'terminal' THEN 'terminalTools'
    WHEN 'web-page' THEN 'webPageTools'
    WHEN 'web-search' THEN 'webSearchTools'
    ELSE bean_name
END
WHERE bean_name IS NULL;

SET @tool_null_count := (
    SELECT COUNT(*)
    FROM tool_definition
    WHERE bean_name IS NULL
);

SET @tool_null_codes := (
    SELECT GROUP_CONCAT(tool_code ORDER BY tool_code SEPARATOR ', ')
    FROM tool_definition
    WHERE bean_name IS NULL
);

SET @tool_cleanup_sql := IF(
    @tool_null_count = 0,
    'SELECT 1',
    CONCAT(
        'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''tool_definition.bean_name still null for tool_code: ',
        REPLACE(COALESCE(@tool_null_codes, 'unknown'), '''', ''''''),
        ''''
    )
);

PREPARE tool_cleanup_stmt FROM @tool_cleanup_sql;
EXECUTE tool_cleanup_stmt;
DEALLOCATE PREPARE tool_cleanup_stmt;

ALTER TABLE tool_definition
    MODIFY COLUMN bean_name VARCHAR(128) NOT NULL COMMENT 'Spring Bean 名称';

ALTER TABLE tool_definition
    DROP COLUMN execution_mode;

ALTER TABLE tool_definition
    DROP COLUMN flow_definition_json;
