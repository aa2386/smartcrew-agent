DROP TABLE IF EXISTS llm_conversation_message;
DROP TABLE IF EXISTS llm_conversation_session;
DROP TABLE IF EXISTS sc_user_identity;
DROP TABLE IF EXISTS sc_user;
DROP TABLE IF EXISTS agent_tool_binding;
DROP TABLE IF EXISTS agent_prompt_binding;
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

CREATE TABLE sc_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NULL,
    display_name VARCHAR(128) NOT NULL,
    avatar_url VARCHAR(255) NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_login_at TIMESTAMP NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL,
    CONSTRAINT uk_sc_user_username UNIQUE (username)
);

CREATE TABLE sc_user_identity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_user_id VARCHAR(128) NOT NULL,
    tenant_key VARCHAR(128) NOT NULL DEFAULT '',
    profile_snapshot_json CLOB NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL,
    CONSTRAINT uk_sc_user_identity UNIQUE (provider, provider_user_id, tenant_key)
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

CREATE TABLE agent_prompt_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_code VARCHAR(64) NOT NULL,
    prompt_template_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL,
    CONSTRAINT uk_agent_prompt UNIQUE (agent_code, prompt_template_id)
);

CREATE TABLE llm_conversation_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    last_message_at TIMESTAMP NULL,
    message_count INT NOT NULL DEFAULT 0,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL,
    CONSTRAINT uk_user_session UNIQUE (user_id, session_id)
);

CREATE TABLE llm_conversation_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    message_seq BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    content CLOB NOT NULL,
    trace_id VARCHAR(64) NULL,
    model VARCHAR(128) NULL,
    prompt_tokens INT NULL,
    completion_tokens INT NULL,
    total_tokens INT NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(512) NULL,
    create_dept BIGINT NULL,
    create_by BIGINT NULL,
    create_time TIMESTAMP NULL,
    update_by BIGINT NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(255) NULL
);

CREATE INDEX idx_user_session_seq ON llm_conversation_message (user_id, session_id, message_seq);
CREATE INDEX idx_agent_prompt_order ON agent_prompt_binding (agent_code, sort_order);
