DROP TABLE IF EXISTS agent_tool_binding;
DROP TABLE IF EXISTS tool_definition;
DROP TABLE IF EXISTS agent_definition;
DROP TABLE IF EXISTS user_preference;
DROP TABLE IF EXISTS mcp_info;
DROP TABLE IF EXISTS prompt_template;

CREATE TABLE prompt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name VARCHAR(128) NOT NULL,
    template_content CLOB NOT NULL,
    category VARCHAR(32) NOT NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL
);

CREATE TABLE mcp_info (
    mcp_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    server_name VARCHAR(64) NOT NULL,
    transport_type VARCHAR(64) NULL,
    command VARCHAR(255) NULL,
    arguments CLOB NULL,
    env CLOB NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255) NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL
);

CREATE TABLE user_preference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    pref_key VARCHAR(64) NOT NULL,
    pref_value CLOB NOT NULL,
    pref_type VARCHAR(32) NOT NULL,
    source VARCHAR(32) NOT NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL,
    CONSTRAINT uk_user_pref UNIQUE (user_id, pref_key)
);

CREATE TABLE agent_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_code VARCHAR(64) NOT NULL,
    agent_name VARCHAR(128) NOT NULL,
    agent_type VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    strategy_type VARCHAR(64) NULL,
    system_prompt CLOB NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config_json CLOB NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL,
    CONSTRAINT uk_agent_code UNIQUE (agent_code)
);

CREATE TABLE tool_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tool_code VARCHAR(64) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NOT NULL,
    bean_name VARCHAR(128) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config_json CLOB NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL,
    CONSTRAINT uk_tool_code UNIQUE (tool_code)
);

CREATE TABLE agent_tool_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_code VARCHAR(64) NOT NULL,
    tool_code VARCHAR(64) NOT NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL,
    CONSTRAINT uk_agent_tool UNIQUE (agent_code, tool_code)
);
