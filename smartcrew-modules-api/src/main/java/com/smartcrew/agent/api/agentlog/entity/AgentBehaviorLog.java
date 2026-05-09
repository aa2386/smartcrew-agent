package com.smartcrew.agent.api.agentlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 行为日志实体，记录每次会话中各 Agent 的关键行为。
 *
 * <p>日志覆盖：会话进入、Agent 开始/结束、委托开始/结束、工具调用、记忆读写、
 * 任务创建/更新、异常错误等事件类型。不保存完整 Prompt，敏感参数在写入前脱敏。</p>
 */
@Data
@TableName("agent_behavior_log")
public class AgentBehaviorLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 追踪 ID */
    private String traceId;

    /** 用户 ID */
    private Long userId;

    /** 会话 ID */
    private String sessionId;

    /** Agent 编码 */
    private String agentCode;

    /** 来源 Agent（委托场景） */
    private String sourceAgent;

    /** 目标 Agent（委托场景） */
    private String targetAgent;

    /** 事件类型 */
    private String eventType;

    /** 事件状态：SUCCESS / FAILED / SKIPPED / NEEDS_CONFIRMATION */
    private String eventStatus;

    /** 事件摘要 */
    private String eventSummary;

    /** 工具编码 */
    private String toolCode;

    /** 动作名称 */
    private String actionName;

    /** 耗时（毫秒） */
    private Long durationMs;

    /** 错误信息 */
    private String errorMessage;

    /** 扩展元数据 JSON */
    private String metadataJson;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 备注 */
    private String remark;
}
