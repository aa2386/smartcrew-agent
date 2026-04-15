package com.smartcrew.agent.api.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.rag.domain.entity.AgentKnowledgeBinding;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Agent 知识库绑定数据访问接口。
 */
@Mapper
public interface AgentKnowledgeBindingMapper extends BaseMapper<AgentKnowledgeBinding> {

    /**
     * 按 Agent 编码查询绑定记录。
     *
     * @param agentCode Agent 编码。
     * @return 绑定列表。
     */
    @Select("select * from agent_knowledge_binding where agent_code = #{agentCode} order by id asc")
    List<AgentKnowledgeBinding> selectByAgentCode(@Param("agentCode") String agentCode);

    /**
     * 按 Agent 编码删除绑定记录。
     *
     * @param agentCode Agent 编码。
     * @return 影响行数。
     */
    @Delete("delete from agent_knowledge_binding where agent_code = #{agentCode}")
    int deleteByAgentCode(@Param("agentCode") String agentCode);
}
