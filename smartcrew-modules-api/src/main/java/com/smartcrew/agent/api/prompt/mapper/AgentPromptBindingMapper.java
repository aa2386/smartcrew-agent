package com.smartcrew.agent.api.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.prompt.domain.entity.AgentPromptBinding;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Agent Prompt 绑定关系 Mapper。
 */
@Mapper
public interface AgentPromptBindingMapper extends BaseMapper<AgentPromptBinding> {

    /**
     * 按 Agent 编码查询绑定关系，按顺序升序返回。
     */
    @Select("select * from agent_prompt_binding where agent_code = #{agentCode} order by sort_order asc, id asc")
    List<AgentPromptBinding> selectByAgentCode(@Param("agentCode") String agentCode);

    /**
     * 按 Agent 编码删除全部绑定关系。
     */
    @Delete("delete from agent_prompt_binding where agent_code = #{agentCode}")
    int deleteByAgentCode(@Param("agentCode") String agentCode);

    /**
     * 按 Prompt 模板主键查询关联的 Agent 编码列表。
     */
    @Select("select distinct agent_code from agent_prompt_binding where prompt_template_id = #{promptTemplateId} order by agent_code asc")
    List<String> selectAgentCodesByPromptTemplateId(@Param("promptTemplateId") Long promptTemplateId);

    /**
     * 统计 Prompt 模板关联数量。
     */
    @Select("select count(1) from agent_prompt_binding where prompt_template_id = #{promptTemplateId}")
    long countByPromptTemplateId(@Param("promptTemplateId") Long promptTemplateId);
}
