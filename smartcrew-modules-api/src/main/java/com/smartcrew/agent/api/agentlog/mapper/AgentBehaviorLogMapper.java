package com.smartcrew.agent.api.agentlog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.agentlog.entity.AgentBehaviorLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 行为日志 Mapper 接口。
 */
@Mapper
public interface AgentBehaviorLogMapper extends BaseMapper<AgentBehaviorLog> {
}
