package com.smartcrew.agent.api.collaboration.service;

import com.smartcrew.agent.api.collaboration.domain.entity.AgentCollaborationLog;
import com.smartcrew.agent.api.collaboration.domain.query.AgentCollaborationLogQuery;
import com.smartcrew.agent.api.collaboration.domain.vo.AgentCollaborationLogVo;
import com.smartcrew.agent.api.collaboration.domain.vo.AgentCollaborationStepVo;
import com.smartcrew.agent.core.page.TableDataInfo;

import java.util.List;
import java.util.Optional;

/**
 * 协作日志服务接口。
 */
public interface AgentCollaborationLogService {

    /**
     * 分页查询协作日志。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    TableDataInfo<AgentCollaborationLogVo> listCollaborationLogs(AgentCollaborationLogQuery query);

    /**
     * 查询某条链路下的全部步骤。
     *
     * @param traceId 协作链路 ID
     * @return 步骤列表
     */
    List<AgentCollaborationStepVo> listTraceSteps(String traceId);

    /**
     * 记录协作步骤日志。
     *
     * @param collaborationLog 协作日志实体
     * @return 落库后的协作日志
     */
    AgentCollaborationLog createCollaborationLog(AgentCollaborationLog collaborationLog);

    /**
     * 按主键查询协作日志。
     *
     * @param id 主键 ID
     * @return 匹配结果
     */
    Optional<AgentCollaborationLog> findById(Long id);
}
