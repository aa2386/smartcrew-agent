DROP TABLE IF EXISTS llm_conversation_message;
DROP TABLE IF EXISTS llm_conversation_session;
DROP TABLE IF EXISTS agent_tool_binding;
DROP TABLE IF EXISTS tool_definition;
DROP TABLE IF EXISTS agent_definition;
DROP TABLE IF EXISTS user_preference;
DROP TABLE IF EXISTS mcp_info;
DROP TABLE IF EXISTS prompt_template;

CREATE TABLE prompt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
    template_content TEXT NOT NULL COMMENT '模板内容',
    category VARCHAR(32) NOT NULL COMMENT '模板分类',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息'
) COMMENT='提示词模板表';

CREATE TABLE mcp_info (
    mcp_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'MCP 服务主键 ID',
    server_name VARCHAR(64) NOT NULL COMMENT '服务端名称',
    transport_type VARCHAR(64) NULL COMMENT '传输类型',
    command VARCHAR(255) NULL COMMENT '启动命令',
    arguments TEXT NULL COMMENT '启动参数',
    env TEXT NULL COMMENT '环境变量配置',
    status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(255) NULL COMMENT '描述信息',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息'
) COMMENT='MCP 服务配置表';

CREATE TABLE user_preference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    pref_key VARCHAR(64) NOT NULL COMMENT '偏好键',
    pref_value TEXT NOT NULL COMMENT '偏好值',
    pref_type VARCHAR(32) NOT NULL COMMENT '偏好类型',
    source VARCHAR(32) NOT NULL COMMENT '来源标识',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    CONSTRAINT uk_user_pref UNIQUE (user_id, pref_key)
) COMMENT='用户偏好表';

CREATE TABLE agent_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    agent_code VARCHAR(64) NOT NULL COMMENT 'Agent 编码',
    agent_name VARCHAR(128) NOT NULL COMMENT 'Agent 名称',
    agent_type VARCHAR(64) NOT NULL COMMENT 'Agent 类型',
    description VARCHAR(255) NULL COMMENT '描述信息',
    strategy_type VARCHAR(64) NULL COMMENT '策略类型',
    system_prompt TEXT NULL COMMENT '系统提示词',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    config_json TEXT NULL COMMENT 'JSON 格式配置',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    CONSTRAINT uk_agent_code UNIQUE (agent_code)
) COMMENT='Agent 定义表';

CREATE TABLE tool_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    tool_code VARCHAR(64) NOT NULL COMMENT '工具编码',
    tool_name VARCHAR(128) NOT NULL COMMENT '工具名称',
    description VARCHAR(255) NOT NULL COMMENT '描述信息',
    bean_name VARCHAR(128) NOT NULL COMMENT 'Spring Bean 名称',
    risk_level VARCHAR(32) NOT NULL COMMENT '风险等级',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    config_json TEXT NULL COMMENT 'JSON 格式配置',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    CONSTRAINT uk_tool_code UNIQUE (tool_code)
) COMMENT='工具定义表';

CREATE TABLE agent_tool_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    agent_code VARCHAR(64) NOT NULL COMMENT 'Agent 编码',
    tool_code VARCHAR(64) NOT NULL COMMENT '工具编码',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    CONSTRAINT uk_agent_tool UNIQUE (agent_code, tool_code)
) COMMENT='Agent 工具绑定表';

CREATE TABLE llm_conversation_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    session_id VARCHAR(128) NOT NULL COMMENT '会话 ID',
    last_message_at DATETIME NULL COMMENT '最近一次消息时间',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息总数',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    CONSTRAINT uk_user_session UNIQUE (user_id, session_id)
) COMMENT='大模型会话表';

CREATE TABLE llm_conversation_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    session_id VARCHAR(128) NOT NULL COMMENT '会话 ID',
    message_seq BIGINT NOT NULL COMMENT '消息顺序号',
    role VARCHAR(32) NOT NULL COMMENT '消息角色',
    content TEXT NOT NULL COMMENT '消息内容',
    trace_id VARCHAR(64) NULL COMMENT '追踪 ID',
    model VARCHAR(128) NULL COMMENT '模型名称',
    prompt_tokens INT NULL COMMENT '输入 Token 数',
    completion_tokens INT NULL COMMENT '输出 Token 数',
    total_tokens INT NULL COMMENT '总 Token 数',
    status VARCHAR(32) NOT NULL COMMENT '处理状态',
    error_message VARCHAR(512) NULL COMMENT '错误信息',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    INDEX idx_user_session_seq (user_id, session_id, message_seq)
) COMMENT='大模型会话消息表';
