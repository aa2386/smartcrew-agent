package com.smartcrew.agent.controller.admin;

import com.smartcrew.agent.api.collaboration.domain.query.AgentCollaborationLogQuery;
import com.smartcrew.agent.api.collaboration.domain.vo.AgentCollaborationLogVo;
import com.smartcrew.agent.api.collaboration.domain.vo.AgentCollaborationStepVo;
import com.smartcrew.agent.api.collaboration.service.AgentCollaborationLogService;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.core.page.TableDataInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台协作日志控制器。
 */
@RestController
@RequestMapping("/api/admin/collaboration-logs")
@ConditionalOnProperty(prefix = "smartcrew.api.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminCollaborationLogController {

    private final AgentCollaborationLogService agentCollaborationLogService;

    public AdminCollaborationLogController(AgentCollaborationLogService agentCollaborationLogService) {
        this.agentCollaborationLogService = agentCollaborationLogService;
    }

    /**
     * 分页查询协作日志。
     */
    @GetMapping
    public TableDataInfo<AgentCollaborationLogVo> list(AgentCollaborationLogQuery query) {
        return agentCollaborationLogService.listCollaborationLogs(query);
    }

    /**
     * 查询某个 traceId 下的步骤时间线。
     */
    @GetMapping("/{traceId}/steps")
    public R<List<AgentCollaborationStepVo>> listSteps(@PathVariable("traceId") String traceId) {
        return R.ok(agentCollaborationLogService.listTraceSteps(traceId));
    }
}
