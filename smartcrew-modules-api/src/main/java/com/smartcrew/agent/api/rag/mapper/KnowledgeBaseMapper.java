package com.smartcrew.agent.api.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库数据访问接口。
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /**
     * 按编码查询知识库。
     *
     * @param baseCode 知识库编码。
     * @return 匹配记录，未命中时返回 null。
     */
    @Select("select * from knowledge_base where base_code = #{baseCode} limit 1")
    KnowledgeBase selectByBaseCode(@Param("baseCode") String baseCode);

    /**
     * 按 Agent 编码查询已绑定的知识库。
     *
     * @param agentCode Agent 编码。
     * @return 知识库列表。
     */
    @Select("""
            select kb.*
            from knowledge_base kb
            inner join agent_knowledge_binding akb on akb.base_code = kb.base_code
            where akb.agent_code = #{agentCode}
            order by kb.id asc
            """)
    List<KnowledgeBase> selectByAgentCode(@Param("agentCode") String agentCode);
}
