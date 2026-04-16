ALTER TABLE tool_definition
    MODIFY COLUMN bean_name VARCHAR(128) NULL COMMENT 'Spring Bean 名称';

ALTER TABLE tool_definition
    ADD COLUMN execution_mode VARCHAR(32) NOT NULL DEFAULT 'BEAN' COMMENT '执行模式 BEAN/FLOW' AFTER bean_name;

ALTER TABLE tool_definition
    ADD COLUMN flow_definition_json TEXT NULL COMMENT 'Flow Tool DSL 定义' AFTER config_json;
