package com.smartcrew.agent.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcrew.agent.api.agentlog.entity.AgentBehaviorLog;
import com.smartcrew.agent.api.agentlog.service.AgentBehaviorLogService;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.page.PageQuery;
import com.smartcrew.agent.core.page.TableDataInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 后台 Agent 行为日志查询控制器。
 *
 * <p>支持按 traceId、sessionId、userId、agentCode、eventType、eventStatus、
 * 时间范围分页查询行为日志，以及按 traceId 查看完整时间线。</p>
 */
@RestController
@RequestMapping("/api/admin/agent-logs")
@ConditionalOnProperty(prefix = "smartcrew.api.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminAgentLogController {

    private final AgentBehaviorLogService agentBehaviorLogService;

    public AdminAgentLogController(AgentBehaviorLogService agentBehaviorLogService) {
        this.agentBehaviorLogService = agentBehaviorLogService;
    }

    /**
     * 分页查询 Agent 行为日志。
     *
     * @param traceId     追踪 ID（可选）
     * @param sessionId   会话 ID（可选）
     * @param userId      用户 ID（可选）
     * @param agentCode   Agent 编码（可选）
     * @param eventType   事件类型（可选）
     * @param eventStatus 事件状态（可选）
     * @param startTime   开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime     结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param pageQuery   分页参数
     * @return 分页日志数据
     */
    @GetMapping
    public TableDataInfo<AgentBehaviorLog> list(
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String agentCode,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String eventStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            PageQuery pageQuery) {

        Page<AgentBehaviorLog> page = agentBehaviorLogService.query(
                StringUtils.isBlank(traceId) ? null : traceId,
                StringUtils.isBlank(sessionId) ? null : sessionId,
                userId,
                StringUtils.isBlank(agentCode) ? null : agentCode,
                StringUtils.isBlank(eventType) ? null : eventType,
                StringUtils.isBlank(eventStatus) ? null : eventStatus,
                startTime,
                endTime,
                pageQuery
        );
        return TableDataInfo.build(page);
    }

    /**
     * 按 traceId 查询完整时间线。
     *
     * @param traceId 追踪 ID
     * @return 按时间升序排列的日志列表
     */
    @GetMapping("/traces/{traceId}")
    public R<Map<String, Object>> traceTimeline(@PathVariable("traceId") String traceId) {
        List<AgentBehaviorLog> logs = agentBehaviorLogService.queryByTraceId(traceId);
        return R.ok(Map.of(
                "traceId", traceId,
                "logs", logs,
                "count", logs.size()
        ));
    }
}
