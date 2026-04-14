DROP TABLE IF EXISTS agent_knowledge_binding;
DROP TABLE IF EXISTS document_chunk;
DROP TABLE IF EXISTS knowledge_document;
DROP TABLE IF EXISTS knowledge_base;
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

CREATE TABLE sc_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NULL COMMENT '密码哈希',
    display_name VARCHAR(128) NOT NULL COMMENT '显示名称',
    avatar_url VARCHAR(255) NULL COMMENT '头像地址',
    role VARCHAR(32) NOT NULL COMMENT '角色',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    last_login_at DATETIME NULL COMMENT '最后登录时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    CONSTRAINT uk_sc_user_username UNIQUE (username)
) COMMENT='系统用户表';

CREATE TABLE sc_user_identity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '系统用户 ID',
    provider VARCHAR(32) NOT NULL COMMENT '身份提供方',
    provider_user_id VARCHAR(128) NOT NULL COMMENT '第三方用户标识',
    tenant_key VARCHAR(128) NOT NULL DEFAULT '' COMMENT '租户标识',
    profile_snapshot_json TEXT NULL COMMENT '身份快照',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    CONSTRAINT uk_sc_user_identity UNIQUE (provider, provider_user_id, tenant_key)
) COMMENT='用户身份映射表';

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

CREATE TABLE agent_prompt_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    agent_code VARCHAR(64) NOT NULL COMMENT 'Agent code',
    prompt_template_id BIGINT NOT NULL COMMENT 'Prompt template id',
    sort_order INT NOT NULL COMMENT 'Sort order',
    create_dept BIGINT NULL COMMENT 'Create dept',
    create_by BIGINT NULL COMMENT 'Create by',
    create_time DATETIME NULL COMMENT 'Create time',
    update_by BIGINT NULL COMMENT 'Update by',
    update_time DATETIME NULL COMMENT 'Update time',
    remark VARCHAR(255) NULL COMMENT 'Remark',
    CONSTRAINT uk_agent_prompt UNIQUE (agent_code, prompt_template_id),
    INDEX idx_agent_prompt_order (agent_code, sort_order)
) COMMENT='Agent prompt binding';

CREATE TABLE knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '涓婚敭 ID',
    base_code VARCHAR(64) NOT NULL COMMENT '鐭ヨ瘑搴撶紪鐮?',
    base_name VARCHAR(128) NOT NULL COMMENT '鐭ヨ瘑搴撳悕绉?',
    description VARCHAR(512) NULL COMMENT '鎻忚堪淇℃伅',
    embedding_model VARCHAR(128) NOT NULL COMMENT '宓屽叆妯″瀷鍚嶇О',
    collection_name VARCHAR(128) NOT NULL COMMENT '鍚戦噺鍛藉悕绌洪棿',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '鏄惁鍚敤',
    create_dept BIGINT NULL COMMENT '鍒涘缓閮ㄩ棬',
    create_by BIGINT NULL COMMENT '鍒涘缓浜?',
    create_time DATETIME NULL COMMENT '鍒涘缓鏃堕棿',
    update_by BIGINT NULL COMMENT '鏇存柊浜?',
    update_time DATETIME NULL COMMENT '鏇存柊鏃堕棿',
    remark VARCHAR(255) NULL COMMENT '澶囨敞淇℃伅',
    CONSTRAINT uk_base_code UNIQUE (base_code)
) COMMENT='鐭ヨ瘑搴撹〃';

CREATE TABLE knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '涓婚敭 ID',
    base_id BIGINT NOT NULL COMMENT '鐭ヨ瘑搴?ID',
    document_code VARCHAR(64) NOT NULL COMMENT '鏂囨。缂栫爜',
    document_name VARCHAR(256) NOT NULL COMMENT '鏂囨。鍚嶇О',
    file_path VARCHAR(512) NOT NULL COMMENT '鏂囦欢璺緞',
    file_type VARCHAR(32) NULL COMMENT '鏂囦欢绫诲瀷',
    file_size BIGINT NULL COMMENT '鏂囦欢澶у皬',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '澶勭悊鐘舵€?',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '鍒囩墖鏁伴噺',
    error_message VARCHAR(512) NULL COMMENT '閿欒淇℃伅',
    create_dept BIGINT NULL COMMENT '鍒涘缓閮ㄩ棬',
    create_by BIGINT NULL COMMENT '鍒涘缓浜?',
    create_time DATETIME NULL COMMENT '鍒涘缓鏃堕棿',
    update_by BIGINT NULL COMMENT '鏇存柊浜?',
    update_time DATETIME NULL COMMENT '鏇存柊鏃堕棿',
    remark VARCHAR(255) NULL COMMENT '澶囨敞淇℃伅',
    CONSTRAINT uk_document_code UNIQUE (document_code),
    INDEX idx_knowledge_document_base_id (base_id)
) COMMENT='鐭ヨ瘑鏂囨。琛?';

CREATE TABLE document_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '涓婚敭 ID',
    document_id BIGINT NOT NULL COMMENT '鏂囨。 ID',
    chunk_index INT NOT NULL COMMENT '鍒囩墖搴忓彿',
    content TEXT NOT NULL COMMENT '鍒囩墖鍐呭',
    vector_id VARCHAR(128) NULL COMMENT '鍚戦噺 ID',
    token_count INT NULL COMMENT 'Token 鏁伴噺',
    metadata JSON NULL COMMENT '鍏冩暟鎹?JSON',
    create_dept BIGINT NULL COMMENT '鍒涘缓閮ㄩ棬',
    create_by BIGINT NULL COMMENT '鍒涘缓浜?',
    create_time DATETIME NULL COMMENT '鍒涘缓鏃堕棿',
    update_by BIGINT NULL COMMENT '鏇存柊浜?',
    update_time DATETIME NULL COMMENT '鏇存柊鏃堕棿',
    remark VARCHAR(255) NULL COMMENT '澶囨敞淇℃伅',
    INDEX idx_document_chunk_document_id (document_id),
    INDEX idx_document_chunk_vector_id (vector_id)
) COMMENT='鏂囨。鍒囩墖琛?';

CREATE TABLE agent_knowledge_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '涓婚敭 ID',
    agent_code VARCHAR(64) NOT NULL COMMENT 'Agent 缂栫爜',
    base_code VARCHAR(64) NOT NULL COMMENT '鐭ヨ瘑搴撶紪鐮?',
    create_dept BIGINT NULL COMMENT '鍒涘缓閮ㄩ棬',
    create_by BIGINT NULL COMMENT '鍒涘缓浜?',
    create_time DATETIME NULL COMMENT '鍒涘缓鏃堕棿',
    update_by BIGINT NULL COMMENT '鏇存柊浜?',
    update_time DATETIME NULL COMMENT '鏇存柊鏃堕棿',
    remark VARCHAR(255) NULL COMMENT '澶囨敞淇℃伅',
    CONSTRAINT uk_agent_knowledge UNIQUE (agent_code, base_code)
) COMMENT='Agent 鐭ヨ瘑搴撶粦瀹氳〃';

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
