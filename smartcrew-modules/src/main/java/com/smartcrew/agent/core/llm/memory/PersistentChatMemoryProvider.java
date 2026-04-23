package com.smartcrew.agent.core.llm.memory;

import com.smartcrew.agent.api.llm.service.LlmConversationStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import org.springframework.stereotype.Component;

/**
 * 持久化对话记忆提供者，为每个会话创建独立的 {@link PersistentChatMemory} 实例。
 *
 * <p>实现 LangChain4j 的 {@link ChatMemoryProvider} 接口，
 * 根据会话记忆 ID 创建关联数据库存储的对话记忆对象。</p>
 *
 * @see PersistentChatMemory
 * @see ChatMemoryProvider
 */
@Component
public class PersistentChatMemoryProvider implements ChatMemoryProvider {

    private final LlmConversationStore conversationStore;

    /**
     * 构造持久化对话记忆提供者实例。
     *
     * @param conversationStore 会话持久化存储服务
     */
    public PersistentChatMemoryProvider(LlmConversationStore conversationStore) {
        this.conversationStore = conversationStore;
    }

    /**
     * 根据记忆 ID 获取或创建持久化对话记忆实例。
     *
     * @param memoryId 会话记忆标识，格式为 {@code agentCode::userId::sessionId}
     * @return 关联数据库存储的对话记忆对象
     */
    @Override
    public ChatMemory get(Object memoryId) {
        return new PersistentChatMemory(memoryId, conversationStore);
    }
}
