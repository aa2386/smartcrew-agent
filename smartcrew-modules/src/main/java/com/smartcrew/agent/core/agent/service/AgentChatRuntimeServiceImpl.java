package com.smartcrew.agent.core.agent.service;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.AgentChatRuntimeService;
import com.smartcrew.agent.api.llm.service.LlmConversationStore;
import com.smartcrew.agent.api.rag.domain.vo.RagAugmentationResult;
import com.smartcrew.agent.api.rag.service.RagAugmentationService;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.tool.ToolCallContextHolder;
import dev.langchain4j.service.Result;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Agent 通用对话运行时服务实现，封装 LLM 调用的完整流程。
 *
 * <p>初始 Agent 的对话处理流程被抽取到此服务中，所有 Agent（initial-agent、
 * life-tool-agent、life-memory-agent）均可通过各自的 agentCode 复用该运行时。
 * 会话锁 key 格式为 {@code agentCode::userId::sessionId}，保证不同 Agent
 * 的会话互不干扰。</p>
 */
@Service
public class AgentChatRuntimeServiceImpl implements AgentChatRuntimeService {

    private final InitialAgentPromptService promptService;
    private final Optional<RagAugmentationService> ragAugmentationService;
    private final ObjectProvider<InitialAgentChatService> chatServiceProvider;
    private final LlmConversationStore conversationStore;

    /**
     * 会话级并发锁映射，key 格式为 {@code agentCode::userId::sessionId}。
     */
    private final ConcurrentHashMap<String, ReentrantLock> conversationLocks = new ConcurrentHashMap<>();

    /**
     * 构造通用运行时服务实例。
     *
     * @param promptService          提示词构建服务（已支持 agentCode 参数）
     * @param ragAugmentationService RAG 增强服务（可选）
     * @param chatServiceProvider    对话服务提供者，延迟获取以避免循环依赖
     * @param conversationStore      会话持久化存储
     */
    public AgentChatRuntimeServiceImpl(InitialAgentPromptService promptService,
                                        Optional<RagAugmentationService> ragAugmentationService,
                                        ObjectProvider<InitialAgentChatService> chatServiceProvider,
                                        LlmConversationStore conversationStore) {
        this.promptService = promptService;
        this.ragAugmentationService = ragAugmentationService;
        this.chatServiceProvider = chatServiceProvider;
        this.conversationStore = conversationStore;
    }

    /**
     * 执行 Agent 对话处理流程。
     *
     * <p>流程：检测 LLM 可用性 → RAG 增强 → 构建 Prompt → 加锁 → 设置工具上下文
     * → 调用对话服务 → 清理上下文 → 解锁。任意步骤失败均持久化降级记录。</p>
     *
     * @param command Agent 调度命令
     * @return 调度响应
     */
    @Override
    public AgentDispatchResponse process(AgentDispatchCommand command) {
        String agentCode = StringUtils.isBlank(command.getAgentCode()) ? "initial-agent" : command.getAgentCode();
        Long userId = command.getUserId();
        String sessionId = command.getSessionId();
        String traceId = command.getTraceId();

        InitialAgentChatService chatService = chatServiceProvider.getIfAvailable();
        if (chatService == null) {
            persistFallbackConversation(agentCode, userId, sessionId,
                    command.getMessage(), "当前未启用大模型服务", traceId);
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(agentCode)
                    .accepted(false)
                    .message("当前未启用大模型服务")
                    .build();
        }

        RagAugmentationResult augmentationResult = resolveRagAugmentation(agentCode, command.getMessage(), traceId);
        String memoryId = InitialAgentMemoryId.encode(agentCode, userId, sessionId);
        String systemPrompt = promptService.buildSystemPrompt(agentCode, userId, augmentationResult.getPromptBlock());
        String lockKey = agentCode + "::" + userId + "::" + sessionId;
        ReentrantLock lock = conversationLocks.computeIfAbsent(lockKey, key -> new ReentrantLock());

        lock.lock();
        try {
            Map<String, Object> toolContext = buildToolContext(command);
            ToolCallContextHolder.set(traceId, toolContext);
            Result<String> result = chatService.chat(memoryId, command.getMessage(), systemPrompt);
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(agentCode)
                    .accepted(true)
                    .message(result == null ? "" : result.content())
                    .build();
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? "当前无法处理请求，请稍后再试" : exception.getMessage();
            persistFallbackConversation(agentCode, userId, sessionId, command.getMessage(), message, traceId);
            return AgentDispatchResponse.builder()
                    .traceId(traceId)
                    .agentCode(agentCode)
                    .accepted(false)
                    .message(message)
                    .build();
        } finally {
            ToolCallContextHolder.clear();
            lock.unlock();
        }
    }

    /**
     * 构建工具调用上下文，将关键业务字段写入 ThreadLocal 供工具链读取。
     * <p>上下文包含 userId、sessionId、agentCode 以及 traceId 等，工具执行时
     * 可通过 {@link ToolCallContextHolder#get()} 获取。</p>
     */
    private Map<String, Object> buildToolContext(AgentDispatchCommand command) {
        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put("userId", command.getUserId());
        toolContext.put("sessionId", command.getSessionId());
        toolContext.put("agentCode", StringUtils.isBlank(command.getAgentCode()) ? "initial-agent" : command.getAgentCode());
        if (command.getContext() != null) {
            toolContext.putAll(command.getContext());
        }
        return toolContext;
    }

    /**
     * 执行 RAG 检索增强。
     */
    private RagAugmentationResult resolveRagAugmentation(String agentCode, String message, String traceId) {
        return ragAugmentationService
                .map(service -> service.augment(agentCode, message, traceId))
                .orElseGet(() -> RagAugmentationResult.builder()
                        .enabled(false)
                        .promptBlock("")
                        .hitCount(0)
                        .build());
    }

    /**
     * 持久化降级对话记录，确保 LLM 不可用时用户消息不丢失。
     */
    private void persistFallbackConversation(String agentCode, Long userId, String sessionId,
                                              String userMessage, String assistantMessage, String traceId) {
        String pid = agentCode + "::" + sessionId;
        conversationStore.ensureSession(userId, pid);
        long userSeq = conversationStore.nextMessageSeq(userId, pid);
        conversationStore.saveUserMessage(userId, pid, userSeq, userMessage, traceId);
        conversationStore.saveAssistantMessage(userId, pid, userSeq + 1, assistantMessage, traceId,
                null, null, null, null);
    }
}
