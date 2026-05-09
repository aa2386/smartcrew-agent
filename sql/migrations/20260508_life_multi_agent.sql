-- ====================================================================
-- SmartCrew 生活日程方向多 Agent 协作一期 迁移脚本
-- 创建日期: 2026-05-08
-- 说明: 新增 life-task-agent / life-memory-agent 定义、Prompt 模板、
--        Tool 定义与绑定、任务记录表、行为日志表
-- ====================================================================

-- ------------------------------------------------------------
-- 1. 新增 Agent 定义
-- ------------------------------------------------------------
INSERT IGNORE INTO agent_definition (agent_code, agent_name, agent_type, description, strategy_type, enabled)
VALUES
('life-tool-agent', '生活日程工具 Agent', 'BUILTIN', '负责承接外部能力/平台工具调用，不直接面对用户', 'REACT', 1),
('life-memory-agent', '生活日程记忆 Agent', 'BUILTIN', '负责用户偏好、历史对话摘要、任务记录等记忆读写', 'REACT', 1);

-- 更新 initial-agent 描述（如已存在则忽略）
UPDATE agent_definition SET description = '三 Agent 协作体系中的主 Agent，直接对话、澄清、拆解、委托、汇总' WHERE agent_code = 'initial-agent';

-- ------------------------------------------------------------
-- 2. 新增 Prompt 模板
-- ------------------------------------------------------------
INSERT IGNORE INTO prompt_template (template_name, template_content, category, create_time, update_time)
VALUES
('生活日程主Agent系统提示词', '你是 SmartCrew 平台的生活日程主 Agent，直接与用户对话。
你的职责：
1. 仔细理解用户的生活日程需求，缺少关键信息（如时间、地点、对象、时区）时先澄清，不要猜测。
2. 需要调用外部工具或平台操作时，使用 delegateToToolAgent 委托给工具 Agent 执行，不要假装自己完成了外部操作。
3. 需要读写用户偏好、历史记忆或任务记录时，使用 delegateToMemoryAgent 委托给记忆 Agent 处理。
4. 汇总工具 Agent 和记忆 Agent 的返回结果，用清晰友好的中文回复用户。
5. 保持专业、亲切、有条理的对话风格。
注意事项：
- 你不能直接调用外部执行工具或记忆写入工具，必须通过委托进行。
- 对于模糊的时间表达（如"明天"、"下周"、"晚上"），先确认具体时间再委托创建任务。
- 不要承诺已接入真实第三方日历、邮件或通知系统。', 'SYSTEM', NOW(), NOW()),

('生活日程工具Agent系统提示词', '你是 SmartCrew 的生活日程工具 Agent，负责执行委托给你的工具操作。
你的职责：
1. 根据委托指令中的具体操作描述，调用你绑定的工具（如生活日程工具 life-schedule）完成任务。
2. 如果工具执行成功，返回简洁明确的结果。
3. 如果工具执行失败或参数不足，返回具体的失败原因，帮助主 Agent 决策下一步。
行为约束：
- 只执行工具操作，不进行寒暄或开放式闲聊。
- 不保存长期记忆或用户偏好（这些属于记忆 Agent 的职责）。
- 不主动发起与用户的对话。
- 返回结果必须结构化，方便主 Agent 汇总。', 'SYSTEM', NOW(), NOW()),

('生活日程记忆Agent系统提示词', '你是 SmartCrew 的生活日程记忆 Agent，负责处理用户偏好、会话记忆和任务记录的读写。
你的职责：
1. 根据委托指令中的具体操作描述，调用你绑定的记忆工具（memory）完成读写。
2. 读取偏好/记忆时，返回结构化的键值对数据。
3. 写入偏好/记忆时，先判断内容是否为用户明确表达的偏好，不要将一次性陈述永久化。
4. 查询任务记录时，返回与当前需求相关的任务列表。
行为约束：
- 只做用户范围内的记忆处理，不调用外部执行工具。
- 不保存密码、密钥、身份证、银行卡等敏感信息。
- 写入推断偏好时保持保守：仅在用户明确表达时写入，不主动推断。
- 所有读写操作按 userId 隔离，不跨用户操作。
- 不主动发起与用户的对话。
- 返回结果必须结构化，方便主 Agent 汇总。', 'SYSTEM', NOW(), NOW());

-- ------------------------------------------------------------
-- 3. 新增 Prompt 绑定
-- ------------------------------------------------------------
-- 获取三个模板的 ID（兼容不同数据库）
SET @main_prompt_id = (SELECT id FROM prompt_template WHERE template_name = '生活日程主Agent系统提示词' LIMIT 1);
SET @tool_prompt_id = (SELECT id FROM prompt_template WHERE template_name = '生活日程工具Agent系统提示词' LIMIT 1);
SET @memory_prompt_id = (SELECT id FROM prompt_template WHERE template_name = '生活日程记忆Agent系统提示词' LIMIT 1);

-- initial-agent 绑定主 Agent Prompt
INSERT IGNORE INTO agent_prompt_binding (agent_code, prompt_template_id, sort_order)
VALUES ('initial-agent', @main_prompt_id, 1);

-- life-tool-agent 绑定工具 Agent Prompt
INSERT IGNORE INTO agent_prompt_binding (agent_code, prompt_template_id, sort_order)
VALUES ('life-tool-agent', @tool_prompt_id, 1);

-- life-memory-agent 绑定记忆 Agent Prompt
INSERT IGNORE INTO agent_prompt_binding (agent_code, prompt_template_id, sort_order)
VALUES ('life-memory-agent', @memory_prompt_id, 1);

-- ------------------------------------------------------------
-- 4. 新增 Tool 定义
-- ------------------------------------------------------------
INSERT IGNORE INTO tool_definition (tool_code, tool_name, description, bean_name, risk_level, enabled)
VALUES
('agent-delegation', 'Agent 委托工具', '用于主 Agent 将子任务委托给工具 Agent 或记忆 Agent', 'agentDelegationTools', 'HIGH', 1),
('life-schedule',   '生活日程工具',   '提供生活日程任务记录的创建、查询、状态更新',           'lifeScheduleTools',   'MEDIUM', 1),
('memory',          '记忆工具',       '提供用户偏好、会话记忆、任务记录的读写能力',              'memoryTools',         'HIGH', 1);

-- ------------------------------------------------------------
-- 5. 新增 Agent-Tool 绑定
-- ------------------------------------------------------------
-- initial-agent: 只绑定委托工具 + basic 基础工具
INSERT IGNORE INTO agent_tool_binding (agent_code, tool_code) VALUES ('initial-agent', 'agent-delegation');
INSERT IGNORE INTO agent_tool_binding (agent_code, tool_code) VALUES ('initial-agent', 'basic');

-- life-tool-agent: 绑定生活日程工具 + basic 基础工具（当前时间等）
INSERT IGNORE INTO agent_tool_binding (agent_code, tool_code) VALUES ('life-tool-agent', 'life-schedule');
INSERT IGNORE INTO agent_tool_binding (agent_code, tool_code) VALUES ('life-tool-agent', 'basic');

-- life-memory-agent: 绑定记忆工具
INSERT IGNORE INTO agent_tool_binding (agent_code, tool_code) VALUES ('life-memory-agent', 'memory');

-- ------------------------------------------------------------
-- 6. 新增生活日程任务记录表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS life_task_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    title VARCHAR(256) NOT NULL COMMENT '任务标题',
    description TEXT NULL COMMENT '任务描述',
    due_time DATETIME NULL COMMENT '截止时间',
    time_text VARCHAR(128) NULL COMMENT '原始时间文本（如"明天上午九点"）',
    timezone VARCHAR(64) NULL COMMENT '时区',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING / DONE / CANCELLED / NEEDS_CONFIRMATION',
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级: LOW / MEDIUM / HIGH',
    source VARCHAR(32) NOT NULL DEFAULT 'DELEGATION' COMMENT '来源标识',
    trace_id VARCHAR(64) NULL COMMENT '关联追踪 ID',
    metadata_json TEXT NULL COMMENT '扩展元数据 JSON',
    create_dept BIGINT NULL COMMENT '创建部门',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NULL COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NULL COMMENT '更新时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    INDEX idx_ltr_user_id (user_id),
    INDEX idx_ltr_status (status),
    INDEX idx_ltr_due_time (due_time)
) COMMENT='生活日程任务记录表';

-- ------------------------------------------------------------
-- 7. 新增 Agent 行为日志表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS agent_behavior_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    trace_id VARCHAR(64) NULL COMMENT '追踪 ID',
    user_id BIGINT NULL COMMENT '用户 ID',
    session_id VARCHAR(128) NULL COMMENT '会话 ID',
    agent_code VARCHAR(64) NULL COMMENT 'Agent 编码',
    source_agent VARCHAR(64) NULL COMMENT '来源 Agent（委托场景）',
    target_agent VARCHAR(64) NULL COMMENT '目标 Agent（委托场景）',
    event_type VARCHAR(32) NOT NULL COMMENT '事件类型: SESSION_RECEIVED / AGENT_STARTED / AGENT_FINISHED / DELEGATION_STARTED / DELEGATION_FINISHED / TOOL_STARTED / TOOL_FINISHED / MEMORY_READ / MEMORY_WRITE / TASK_CREATED / TASK_UPDATED / ERROR',
    event_status VARCHAR(32) NOT NULL COMMENT '事件状态: SUCCESS / FAILED / SKIPPED / NEEDS_CONFIRMATION',
    event_summary VARCHAR(256) NULL COMMENT '事件摘要',
    tool_code VARCHAR(64) NULL COMMENT '工具编码',
    action_name VARCHAR(128) NULL COMMENT '动作名称',
    duration_ms BIGINT NULL COMMENT '耗时（毫秒）',
    error_message VARCHAR(512) NULL COMMENT '错误信息',
    metadata_json TEXT NULL COMMENT '扩展元数据 JSON（已脱敏）',
    create_time DATETIME NULL COMMENT '创建时间',
    remark VARCHAR(255) NULL COMMENT '备注信息',
    INDEX idx_abl_trace_id (trace_id),
    INDEX idx_abl_session_id (session_id),
    INDEX idx_abl_user_id (user_id),
    INDEX idx_abl_agent_code (agent_code),
    INDEX idx_abl_event_type (event_type),
    INDEX idx_abl_create_time (create_time)
) COMMENT='Agent 行为日志表';
