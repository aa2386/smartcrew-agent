package com.smartcrew.agent.api.llm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 大模型会话消息 Mapper。
 */
@Mapper
public interface LlmConversationMessageMapper extends BaseMapper<LlmConversationMessage> {

    /**
     * 按会话加载最近若干条消息，结果按消息顺序倒序返回。
     */
    @Select("""
            select *
            from llm_conversation_message
            where user_id = #{userId}
              and session_id = #{sessionId}
            order by message_seq desc
            limit #{limit}
            """)
    List<LlmConversationMessage> selectRecentMessages(@Param("userId") Long userId,
                                                      @Param("sessionId") String sessionId,
                                                      @Param("limit") int limit);

    /**
     * 查询指定会话当前最大的消息顺序号。
     */
    @Select("""
            select coalesce(max(message_seq), 0)
            from llm_conversation_message
            where user_id = #{userId}
              and session_id = #{sessionId}
            """)
    Long selectMaxMessageSeq(@Param("userId") Long userId,
                             @Param("sessionId") String sessionId);

    /**
     * 查询指定会话最新的一条消息。
     */
    @Select("""
            select *
            from llm_conversation_message
            where user_id = #{userId}
              and session_id = #{sessionId}
            order by message_seq desc
            limit 1
            """)
    LlmConversationMessage selectLatestMessage(@Param("userId") Long userId,
                                               @Param("sessionId") String sessionId);
}
