package com.smartcrew.agent.api.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * ????????????????????????
 */
@Mapper
public interface AgentDefinitionMapper extends BaseMapper<AgentDefinition> {

    @Select("select * from agent_definition where agent_code = #{agentCode} limit 1")
    AgentDefinition selectByAgentCode(@Param("agentCode") String agentCode);
}
