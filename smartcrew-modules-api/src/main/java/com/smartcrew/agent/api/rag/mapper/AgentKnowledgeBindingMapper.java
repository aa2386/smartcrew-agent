package com.smartcrew.agent.api.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.rag.domain.entity.AgentKnowledgeBinding;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Agent 鐭ヨ瘑搴撶粦瀹氭暟鎹闂帴鍙ｃ€?
 */
@Mapper
public interface AgentKnowledgeBindingMapper extends BaseMapper<AgentKnowledgeBinding> {

    /**
     * 鎸?Agent 缂栫爜鏌ヨ缁戝畾璁板綍銆?     *
     * @param agentCode Agent 缂栫爜銆?     * @return 缁戝畾鍒楄〃銆?     */
    @Select("select * from agent_knowledge_binding where agent_code = #{agentCode} order by id asc")
    List<AgentKnowledgeBinding> selectByAgentCode(@Param("agentCode") String agentCode);

    /**
     * 鎸?Agent 缂栫爜鍒犻櫎缁戝畾璁板綍銆?     *
     * @param agentCode Agent 缂栫爜銆?     * @return 褰卞搷琛屾暟銆?     */
    @Delete("delete from agent_knowledge_binding where agent_code = #{agentCode}")
    int deleteByAgentCode(@Param("agentCode") String agentCode);
}
