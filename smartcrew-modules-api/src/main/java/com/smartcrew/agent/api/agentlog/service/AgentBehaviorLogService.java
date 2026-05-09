package com.smartcrew.agent.api.agentlog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcrew.agent.api.agentlog.entity.AgentBehaviorLog;
import com.smartcrew.agent.core.page.PageQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Agent 行为日志服务接口。
 */
public interface AgentBehaviorLogService {

    /**
     * 写入一条行为日志。写入失败不抛异常，仅记录应用日志。
     *
     * @param log 行为日志实体
     */
    void write(AgentBehaviorLog log);

    /**
     * 分页查询行为日志。
     *
     * @param traceId     追踪 ID（可选）
     * @param sessionId   会话 ID（可选）
     * @param userId      用户 ID（可选）
     * @param agentCode   Agent 编码（可选）
     * @param eventType   事件类型（可选）
     * @param eventStatus 事件状态（可选）
     * @param startTime   开始时间（可选）
     * @param endTime     结束时间（可选）
     * @param pageQuery   分页参数
     * @return 分页结果
     */
    Page<AgentBehaviorLog> query(String traceId, String sessionId, Long userId,
                                  String agentCode, String eventType, String eventStatus,
                                  LocalDateTime startTime, LocalDateTime endTime,
                                  PageQuery pageQuery);

    /**
     * 按 traceId 查询完整时间线。
     *
     * @param traceId 追踪 ID
     * @return 按时间排序的日志列表
     */
    List<AgentBehaviorLog> queryByTraceId(String traceId);

    /**
     * 构建行为日志实体并将扩展信息写入 metadata_json。
     *
     * @return 行为日志实体
     */
    AgentBehaviorLog buildLog(String traceId, Long userId, String sessionId,
                               String agentCode, String eventType, String eventStatus,
                               String eventSummary, Map<String, Object> metadata);
}
