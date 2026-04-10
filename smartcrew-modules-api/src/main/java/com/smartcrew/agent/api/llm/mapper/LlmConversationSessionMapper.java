package com.smartcrew.agent.api.llm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcrew.agent.api.chat.domain.vo.ChatSessionVo;
import com.smartcrew.agent.api.llm.domain.entity.LlmConversationSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 大模型会话 Mapper，负责会话基础信息的数据访问。
 */
@Mapper
public interface LlmConversationSessionMapper extends BaseMapper<LlmConversationSession> {

    /**
     * 按用户 ID 和会话 ID 查询会话。
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

    /**
     * 后台分页查询会话列表。
     */
    @Select("""
            <script>
            select
                replace(s.session_id, 'initial-agent::', '') as sessionId,
                case
                    when m.content is null or m.content = '' then '新对话'
                    else substring(m.content, 1, 16)
                end as title,
                coalesce(m.content, '暂无消息') as preview,
                s.message_count as messageCount,
                s.last_message_at as lastMessageAt,
                case
                    when replace(s.session_id, 'initial-agent::', '') like 'platform::wecom::%' then 'WECOM'
                    when replace(s.session_id, 'initial-agent::', '') like 'platform::feishu::%' then 'FEISHU'
                    else 'WEB'
                end as source
            from llm_conversation_session s
            left join llm_conversation_message m on m.id = (
                select lm.id
                from llm_conversation_message lm
                where lm.user_id = s.user_id
                  and lm.session_id = s.session_id
                order by lm.message_seq desc
                limit 1
            )
            where 1 = 1
            <if test='userId != null'>
                and s.user_id = #{userId}
            </if>
            <if test='provider != null and provider != ""'>
                and case
                        when replace(s.session_id, 'initial-agent::', '') like 'platform::wecom::%' then 'WECOM'
                        when replace(s.session_id, 'initial-agent::', '') like 'platform::feishu::%' then 'FEISHU'
                        else 'WEB'
                    end = #{provider}
            </if>
            <if test='keyword != null and keyword != ""'>
                and (
                    replace(s.session_id, 'initial-agent::', '') like concat('%', #{keyword}, '%')
                    or coalesce(m.content, '') like concat('%', #{keyword}, '%')
                    or substring(coalesce(m.content, '新对话'), 1, 16) like concat('%', #{keyword}, '%')
                )
            </if>
            order by s.last_message_at desc, s.id desc
            </script>
            """)
    IPage<ChatSessionVo> selectAdminSessionPage(Page<ChatSessionVo> page,
                                                @Param("userId") Long userId,
                                                @Param("provider") String provider,
                                                @Param("keyword") String keyword);

    /**
     * 后台全量查询会话列表。
     */
    @Select("""
            <script>
            select
                replace(s.session_id, 'initial-agent::', '') as sessionId,
                case
                    when m.content is null or m.content = '' then '新对话'
                    else substring(m.content, 1, 16)
                end as title,
                coalesce(m.content, '暂无消息') as preview,
                s.message_count as messageCount,
                s.last_message_at as lastMessageAt,
                case
                    when replace(s.session_id, 'initial-agent::', '') like 'platform::wecom::%' then 'WECOM'
                    when replace(s.session_id, 'initial-agent::', '') like 'platform::feishu::%' then 'FEISHU'
                    else 'WEB'
                end as source
            from llm_conversation_session s
            left join llm_conversation_message m on m.id = (
                select lm.id
                from llm_conversation_message lm
                where lm.user_id = s.user_id
                  and lm.session_id = s.session_id
                order by lm.message_seq desc
                limit 1
            )
            where 1 = 1
            <if test='userId != null'>
                and s.user_id = #{userId}
            </if>
            <if test='provider != null and provider != ""'>
                and case
                        when replace(s.session_id, 'initial-agent::', '') like 'platform::wecom::%' then 'WECOM'
                        when replace(s.session_id, 'initial-agent::', '') like 'platform::feishu::%' then 'FEISHU'
                        else 'WEB'
                    end = #{provider}
            </if>
            <if test='keyword != null and keyword != ""'>
                and (
                    replace(s.session_id, 'initial-agent::', '') like concat('%', #{keyword}, '%')
                    or coalesce(m.content, '') like concat('%', #{keyword}, '%')
                    or substring(coalesce(m.content, '新对话'), 1, 16) like concat('%', #{keyword}, '%')
                )
            </if>
            order by s.last_message_at desc, s.id desc
            </script>
            """)
    List<ChatSessionVo> selectAdminSessions(@Param("userId") Long userId,
                                            @Param("provider") String provider,
                                            @Param("keyword") String keyword);
}
