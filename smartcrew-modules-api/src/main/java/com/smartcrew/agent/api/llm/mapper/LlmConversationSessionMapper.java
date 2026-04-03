package com.smartcrew.agent.api.llm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.llm.domain.entity.LlmConversationSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 大模型会话 Mapper，负责会话基础信息的数据访问。
 */
@Mapper
public interface LlmConversationSessionMapper extends BaseMapper<LlmConversationSession> {

    /**
     * 按用户 ID 和会话 ID 查询会话。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @return 匹配到的会话；未命中时返回 {@code null}
     */
    @Select("""
            select *
            from llm_conversation_session
            where user_id = #{userId}
              and session_id = #{sessionId}
            limit 1
            """)
    LlmConversationSession selectByUserIdAndSessionId(@Param("userId") Long userId,
                                                      @Param("sessionId") String sessionId);
}
