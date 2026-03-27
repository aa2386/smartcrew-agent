package com.smartcrew.agent.api.llm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 大模型会话消息 Mapper，负责会话消息的数据访问。
 */
@Mapper
public interface LlmConversationMessageMapper extends BaseMapper<LlmConversationMessage> {

    /**
     * 按会话加载最近若干条消息，结果按顺序号倒序返回。
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @param limit 查询条数
     * @return 最近消息列表
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
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @return 当前最大顺序号；若不存在则返回 0
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
     *
     * @param userId 用户 ID
     * @param sessionId 会话 ID
     * @return 最新消息；未命中时返回 {@code null}
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

    /**
     * 将指定消息标记为失败。
     *
     * @param id 消息 ID
     * @param errorMessage 错误信息
     * @return 受影响的行数
     */
    @Update("""
            update llm_conversation_message
            set status = 'FAILED',
                error_message = #{errorMessage}
            where id = #{id}
            """)
    int markMessageFailed(@Param("id") Long id, @Param("errorMessage") String errorMessage);
}
