package com.smartcrew.agent.api.collaboration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.collaboration.domain.entity.AgentCollaborationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 协作日志数据访问接口。
 */
@Mapper
public interface AgentCollaborationLogMapper extends BaseMapper<AgentCollaborationLog> {
}
