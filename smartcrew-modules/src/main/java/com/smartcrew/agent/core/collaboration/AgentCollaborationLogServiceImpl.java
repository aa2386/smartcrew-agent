package com.smartcrew.agent.core.collaboration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.collaboration.domain.entity.AgentCollaborationLog;
import com.smartcrew.agent.api.collaboration.domain.query.AgentCollaborationLogQuery;
import com.smartcrew.agent.api.collaboration.domain.vo.AgentCollaborationLogVo;
import com.smartcrew.agent.api.collaboration.domain.vo.AgentCollaborationStepVo;
import com.smartcrew.agent.api.collaboration.mapper.AgentCollaborationLogMapper;
import com.smartcrew.agent.api.collaboration.service.AgentCollaborationLogService;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.page.TableDataInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 协作日志服务实现。
 */
@Service
public class AgentCollaborationLogServiceImpl implements AgentCollaborationLogService {

    private final AgentCollaborationLogMapper agentCollaborationLogMapper;

    public AgentCollaborationLogServiceImpl(AgentCollaborationLogMapper agentCollaborationLogMapper) {
        this.agentCollaborationLogMapper = agentCollaborationLogMapper;
    }

    @Override
    public TableDataInfo<AgentCollaborationLogVo> listCollaborationLogs(AgentCollaborationLogQuery query) {
        AgentCollaborationLogQuery safeQuery = query == null ? new AgentCollaborationLogQuery() : query;
        LambdaQueryWrapper<AgentCollaborationLog> wrapper = buildQueryWrapper(safeQuery);
        wrapper.orderByDesc(AgentCollaborationLog::getStartTime)
                .orderByDesc(AgentCollaborationLog::getId);

        if (safeQuery.hasPaging()) {
            Page<AgentCollaborationLog> page = agentCollaborationLogMapper.selectPage(safeQuery.build(), wrapper);
            Page<AgentCollaborationLogVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
            result.setRecords(page.getRecords().stream()
                    .map(this::toLogVo)
                    .toList());
            return TableDataInfo.build(result);
        }

        return TableDataInfo.build(agentCollaborationLogMapper.selectList(wrapper).stream()
                .map(this::toLogVo)
                .toList());
    }

    @Override
    public List<AgentCollaborationStepVo> listTraceSteps(String traceId) {
        if (StringUtils.isBlank(traceId)) {
            return List.of();
        }
        return agentCollaborationLogMapper.selectList(Wrappers.lambdaQuery(AgentCollaborationLog.class)
                        .eq(AgentCollaborationLog::getTraceId, traceId.trim())
                        .orderByAsc(AgentCollaborationLog::getStartTime)
                        .orderByAsc(AgentCollaborationLog::getId))
                .stream()
                .map(this::toStepVo)
                .toList();
    }

    @Override
    @Transactional
    public AgentCollaborationLog createCollaborationLog(AgentCollaborationLog collaborationLog) {
        if (collaborationLog == null) {
            throw new ServiceException(400, "协作日志不能为空");
        }
        agentCollaborationLogMapper.insert(collaborationLog);
        return collaborationLog;
    }

    @Override
    public Optional<AgentCollaborationLog> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentCollaborationLogMapper.selectById(id));
    }

    /**
     * 构建查询条件。
     */
    private LambdaQueryWrapper<AgentCollaborationLog> buildQueryWrapper(AgentCollaborationLogQuery query) {
        LambdaQueryWrapper<AgentCollaborationLog> wrapper = Wrappers.lambdaQuery(AgentCollaborationLog.class);
        if (StringUtils.isNotBlank(query.getTraceId())) {
            wrapper.eq(AgentCollaborationLog::getTraceId, query.getTraceId().trim());
        }
        if (StringUtils.isNotBlank(query.getRootSessionId())) {
            wrapper.eq(AgentCollaborationLog::getRootSessionId, query.getRootSessionId().trim());
        }
        if (StringUtils.isNotBlank(query.getAgentCode())) {
            wrapper.eq(AgentCollaborationLog::getAgentCode, query.getAgentCode().trim());
        }
        if (StringUtils.isNotBlank(query.getStepType())) {
            wrapper.eq(AgentCollaborationLog::getStepType, query.getStepType().trim());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AgentCollaborationLog::getStatus, query.getStatus().trim());
        }
        if (StringUtils.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(AgentCollaborationLog::getTraceId, keyword)
                    .or()
                    .like(AgentCollaborationLog::getRootSessionId, keyword)
                    .or()
                    .like(AgentCollaborationLog::getAgentCode, keyword)
                    .or()
                    .like(AgentCollaborationLog::getStepName, keyword)
                    .or()
                    .like(AgentCollaborationLog::getInputSnapshot, keyword)
                    .or()
                    .like(AgentCollaborationLog::getOutputSnapshot, keyword)
                    .or()
                    .like(AgentCollaborationLog::getDecisionSnapshot, keyword)
                    .or()
                    .like(AgentCollaborationLog::getErrorMessage, keyword));
        }
        if (query.getStartTimeFrom() != null) {
            wrapper.ge(AgentCollaborationLog::getStartTime, query.getStartTimeFrom());
        }
        if (query.getStartTimeTo() != null) {
            wrapper.le(AgentCollaborationLog::getStartTime, query.getStartTimeTo());
        }
        return wrapper;
    }

    /**
     * 转换为列表视图。
     */
    private AgentCollaborationLogVo toLogVo(AgentCollaborationLog log) {
        AgentCollaborationLogVo vo = new AgentCollaborationLogVo();
        vo.setTraceId(log.getTraceId());
        vo.setRootSessionId(log.getRootSessionId());
        vo.setUserId(log.getUserId());
        vo.setAgentCode(log.getAgentCode());
        vo.setStepType(log.getStepType());
        vo.setStatus(log.getStatus());
        vo.setStartTime(log.getStartTime());
        vo.setDurationMs(log.getDurationMs());
        return vo;
    }

    /**
     * 转换为步骤视图。
     */
    private AgentCollaborationStepVo toStepVo(AgentCollaborationLog log) {
        AgentCollaborationStepVo vo = new AgentCollaborationStepVo();
        vo.setId(log.getId());
        vo.setTraceId(log.getTraceId());
        vo.setRootSessionId(log.getRootSessionId());
        vo.setUserId(log.getUserId());
        vo.setSource(log.getSource());
        vo.setAgentCode(log.getAgentCode());
        vo.setStepType(log.getStepType());
        vo.setStepName(log.getStepName());
        vo.setParentStepId(log.getParentStepId());
        vo.setStatus(log.getStatus());
        vo.setInputSnapshot(log.getInputSnapshot());
        vo.setOutputSnapshot(log.getOutputSnapshot());
        vo.setDecisionSnapshot(log.getDecisionSnapshot());
        vo.setErrorMessage(log.getErrorMessage());
        vo.setStartTime(log.getStartTime());
        vo.setEndTime(log.getEndTime());
        vo.setDurationMs(log.getDurationMs());
        return vo;
    }
}
