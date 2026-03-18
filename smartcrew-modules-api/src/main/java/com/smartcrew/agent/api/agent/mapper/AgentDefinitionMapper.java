package com.smartcrew.agent.api.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * AgentDefinitionMapper 接口，负责对应领域对象的数据访问操作。
 */
@Mapper
public interface AgentDefinitionMapper extends BaseMapper<AgentDefinition> {

    /**
     * 按 Agent 编码查询定义。
     *
     * @param agentCode Agent 编码。
     * @return 匹配到的 Agent 定义；未命中时返回 `null`。
     */
    @Select("select * from agent_definition where agent_code = #{agentCode} limit 1")
    AgentDefinition selectByAgentCode(@Param("agentCode") String agentCode);
}
