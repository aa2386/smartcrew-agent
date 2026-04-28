package com.smartcrew.agent.core.llm.memory;

import com.smartcrew.agent.api.llm.domain.entity.LlmConversationMessage;
import com.smartcrew.agent.api.llm.service.LlmConversationStore;
import com.smartcrew.agent.common.enums.ConversationHistoryEnum;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.agent.service.InitialAgentMemoryId;
import com.smartcrew.agent.core.tool.ToolCallContextHolder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;

import java.util.ArrayList;
import java.util.List;

/**
 * 持久化对话记忆。
 * LangChain4j 通过该对象加载上下文窗口，并在模型对话过程中写入用户与助手消息。
 */
public class PersistentChatMemory implements ChatMemory {

    private static final int HISTORY_WINDOW_SIZE = 20;

    private final Object memoryId;
    private final LlmConversationStore conversationStore;
    private final InitialAgentMemoryId parsedMemoryId;
    private final List<ChatMessage> messages = new ArrayList<>();

    public PersistentChatMemory(Object memoryId, LlmConversationStore conversationStore) {
        this.memoryId = memoryId;
        this.conversationStore = conversationStore;
        this.parsedMemoryId = InitialAgentMemoryId.parse(memoryId);
        this.conversationStore.ensureSession(parsedMemoryId.userId(), parsedMemoryId.persistedSessionId());
        loadHistory();
    }

    @Override
    public Object id() {
        return memoryId;
    }

    @Override
    public void add(ChatMessage message) {
        if (message instanceof SystemMessage systemMessage) {
            upsertSystemMessage(systemMessage);
            return;
        }
        messages.add(message);
        if (message instanceof UserMessage userMessage) {
            persistUserMessage(userMessage);
            return;
        }
        if (message instanceof AiMessage aiMessage
                && !aiMessage.hasToolExecutionRequests()
                && StringUtils.isNotBlank(aiMessage.text())) {
            persistAssistantMessage(aiMessage);
        }
    }

    @Override
    public List<ChatMessage> messages() {
        return List.copyOf(messages);
    }

    @Override
    public void clear() {
        messages.clear();
    }

    /**
     * Qwen 要求 system message 必须位于消息首位，否则会在适配层清空此前历史。
     */
    private void upsertSystemMessage(SystemMessage systemMessage) {
        messages.removeIf(existing -> existing instanceof SystemMessage);
        messages.add(0, systemMessage);
    }

    private void loadHistory() {
        List<LlmConversationMessage> historyMessages = conversationStore.loadRecentMessages(
                parsedMemoryId.userId(),
                parsedMemoryId.persistedSessionId(),
                HISTORY_WINDOW_SIZE
        );
        for (LlmConversationMessage historyMessage : historyMessages) {
            ChatMessage mappedMessage = mapPersistedMessage(historyMessage);
            if (mappedMessage != null) {
                messages.add(mappedMessage);
            }
        }
    }

    private ChatMessage mapPersistedMessage(LlmConversationMessage message) {
        if (message == null || StringUtils.isBlank(message.getRole()) || StringUtils.isBlank(message.getContent())) {
            return null;
        }
        if (ConversationHistoryEnum.USER.getCode().equalsIgnoreCase(message.getRole())) {
            return UserMessage.from(message.getContent());
        }
        if (ConversationHistoryEnum.ASSISTANT.getCode().equalsIgnoreCase(message.getRole())
                || ConversationHistoryEnum.AI.getCode().equalsIgnoreCase(message.getRole())) {
            return AiMessage.from(message.getContent());
        }
        return null;
    }

    private void persistUserMessage(UserMessage userMessage) {
        long messageSeq = conversationStore.nextMessageSeq(parsedMemoryId.userId(), parsedMemoryId.persistedSessionId());
        conversationStore.saveUserMessage(
                parsedMemoryId.userId(),
                parsedMemoryId.persistedSessionId(),
                messageSeq,
                userMessage.singleText(),
                currentTraceId()
        );
    }

    private void persistAssistantMessage(AiMessage aiMessage) {
        long messageSeq = conversationStore.nextMessageSeq(parsedMemoryId.userId(), parsedMemoryId.persistedSessionId());
        conversationStore.saveAssistantMessage(
                parsedMemoryId.userId(),
                parsedMemoryId.persistedSessionId(),
                messageSeq,
                aiMessage.text(),
                currentTraceId(),
                null,
                null,
                null,
                null
        );
    }

    private String currentTraceId() {
        ToolCallContextHolder.ToolCallContext context = ToolCallContextHolder.get();
        return context == null ? null : context.traceId();
    }
}
