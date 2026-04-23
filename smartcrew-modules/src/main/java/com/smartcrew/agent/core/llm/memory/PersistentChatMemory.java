package com.smartcrew.agent.core.llm.memory;

import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.service.LlmConversationStore;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.agent.service.InitialAgentMemoryId;
import com.smartcrew.agent.core.llm.util.LlmClientUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;

import java.util.ArrayList;
import java.util.List;

/**
 * 持久化对话记忆实现，将对话历史存储到数据库以支持跨请求的上下文延续。
 *
 * <p>实现 LangChain4j 的 {@link ChatMemory} 接口，在内存中维护最近 {@value #HISTORY_WINDOW_SIZE} 条消息，
 * 同时将用户消息和助手回复持久化到数据库，确保对话上下文在服务重启后仍可恢复。</p>
 *
 * @see ChatMemory
 * @see PersistentChatMemoryProvider
 * @see LlmConversationStore
 */
public class PersistentChatMemory implements ChatMemory {

    /**
     * 历史消息加载窗口大小，仅加载最近的 N 条消息以控制上下文长度。
     */
    private static final int HISTORY_WINDOW_SIZE = 20;

    private final Object memoryId;
    private final LlmConversationStore conversationStore;
    private final InitialAgentMemoryId parsedMemoryId;

    /**
     * 内存中的消息列表，包含已加载的历史消息和新增消息。
     */
    private final List<ChatMessage> messages = new ArrayList<>();

    /**
     * 构造持久化对话记忆实例，自动加载历史消息。
     *
     * @param memoryId           会话记忆标识，格式为 {@code agentCode::userId::sessionId}
     * @param conversationStore  会话持久化存储服务
     */
    public PersistentChatMemory(Object memoryId, LlmConversationStore conversationStore) {
        this.memoryId = memoryId;
        this.conversationStore = conversationStore;
        this.parsedMemoryId = InitialAgentMemoryId.parse(memoryId);
        this.conversationStore.ensureSession(parsedMemoryId.userId(), parsedMemoryId.persistedSessionId());
        loadHistory();
    }

    /**
     * 返回会话记忆标识。
     *
     * @return 记忆 ID 对象
     */
    @Override
    public Object id() {
        return memoryId;
    }

    /**
     * 添加消息到记忆中，并根据消息类型执行持久化。
     *
     * <p>用户消息直接持久化；助手消息仅在非工具调用响应且内容非空时持久化。</p>
     *
     * @param message 对话消息
     */
    @Override
    public void add(ChatMessage message) {
        messages.add(message);
        if (message instanceof UserMessage userMessage) {
            persistUserMessage(userMessage);
            return;
        }
        if (message instanceof AiMessage aiMessage && !aiMessage.hasToolExecutionRequests()
                && StringUtils.isNotBlank(aiMessage.text())) {
            persistAssistantMessage(aiMessage);
        }
    }

    /**
     * 返回当前记忆中所有消息的不可变副本。
     *
     * @return 消息列表的不可变视图
     */
    @Override
    public List<ChatMessage> messages() {
        return List.copyOf(messages);
    }

    /**
     * 清空内存中的消息列表。
     */
    @Override
    public void clear() {
        messages.clear();
    }

    /**
     * 从数据库加载最近的历史消息到内存中。
     */
    private void loadHistory() {
        List<LlmConversationMessage> historyMessages = conversationStore.loadRecentMessages(
                parsedMemoryId.userId(),
                parsedMemoryId.persistedSessionId(),
                HISTORY_WINDOW_SIZE
        );
        for (LlmConversationMessage historyMessage : historyMessages) {
            ChatMessage mappedMessage = LlmClientUtils.mapPersistedMessage(historyMessage);
            if (mappedMessage != null) {
                messages.add(mappedMessage);
            }
        }
    }

    /**
     * 持久化用户消息到数据库。
     *
     * @param userMessage 用户消息对象
     */
    private void persistUserMessage(UserMessage userMessage) {
        long messageSeq = conversationStore.nextMessageSeq(parsedMemoryId.userId(), parsedMemoryId.persistedSessionId());
        conversationStore.saveUserMessage(
                parsedMemoryId.userId(),
                parsedMemoryId.persistedSessionId(),
                messageSeq,
                userMessage.singleText(),
                null
        );
    }

    /**
     * 持久化助手消息到数据库。
     *
     * @param aiMessage 助手消息对象
     */
    private void persistAssistantMessage(AiMessage aiMessage) {
        long messageSeq = conversationStore.nextMessageSeq(parsedMemoryId.userId(), parsedMemoryId.persistedSessionId());
        conversationStore.saveAssistantMessage(
                parsedMemoryId.userId(),
                parsedMemoryId.persistedSessionId(),
                messageSeq,
                aiMessage.text(),
                null,
                null,
                null,
                null,
                null
        );
    }
}
