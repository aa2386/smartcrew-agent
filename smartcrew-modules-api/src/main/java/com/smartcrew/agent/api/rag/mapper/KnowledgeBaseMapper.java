package com.smartcrew.agent.api.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.rag.domain.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 鐭ヨ瘑搴撴暟鎹闂帴鍙ｃ€?
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /**
     * 鎸夌紪鐮佹煡璇㈢煡璇嗗簱銆?     *
     * @param baseCode 鐭ヨ瘑搴撶紪鐮併€?     * @return 鍖归厤璁板綍锛屾湭鍛戒腑鏃惰繑鍥?null銆?     */
    @Select("select * from knowledge_base where base_code = #{baseCode} limit 1")
    KnowledgeBase selectByBaseCode(@Param("baseCode") String baseCode);

    /**
     * 鎸?Agent 缂栫爜鏌ヨ缁戝畾鐨勭煡璇嗗簱銆?     *
     * @param agentCode Agent 缂栫爜銆?     * @return 鐭ヨ瘑搴撳垪琛ㄣ€?     */
    @Select("""
            select kb.*
            from knowledge_base kb
            inner join agent_knowledge_binding akb on akb.base_code = kb.base_code
            where akb.agent_code = #{agentCode}
            order by kb.id asc
            """)
    List<KnowledgeBase> selectByAgentCode(@Param("agentCode") String agentCode);
}
