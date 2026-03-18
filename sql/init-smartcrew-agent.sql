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
    status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态',
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
    agent_code VARCHAR(64) NOT NULL COMMENT '代理编码',
    agent_name VARCHAR(128) NOT NULL COMMENT '代理名称',
    agent_type VARCHAR(64) NOT NULL COMMENT '代理类型',
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
) COMMENT='代理定义表';

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
    agent_code VARCHAR(64) NOT NULL COMMENT '代理编码',
    tool_code VARCHAR(64) NOT NULL COMMENT '工具编码',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    CONSTRAINT uk_agent_tool UNIQUE (agent_code, tool_code)
) COMMENT='代理工具绑定表';
