package com.smartcrew.agent.core.agent;

import com.smartcrew.agent.api.agent.domain.model.AgentDispatchCommand;
import com.smartcrew.agent.api.agent.domain.vo.AgentDispatchResponse;
import com.smartcrew.agent.api.agent.service.Agent;
import com.smartcrew.agent.api.llm.service.LlmConversationStore;
import com.smartcrew.agent.api.rag.domain.vo.RagAugmentationResult;
import com.smartcrew.agent.api.rag.service.RagAugmentationService;
import com.smartcrew.agent.core.agent.service.InitialAgentChatService;
import com.smartcrew.agent.core.agent.service.InitialAgentMemoryId;
import com.smartcrew.agent.core.agent.service.InitialAgentPromptService;
import com.smartcrew.agent.core.tool.ToolCallContextHolder;
import dev.langchain4j.service.Result;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 初始智能体实现，平台默认的对话与编排智能体。
 *
 * <p>负责接收用户调度命令，结合 RAG 增强与系统提示词构建对话上下文，
 * 通过 LangChain4j AI Service 调用大模型完成推理，并返回响应结果。</p>
 *
 * <p>核心处理流程：</p>
 * <ol>
 *   <li>检测大模型服务是否可用</li>
 *   <li>执行 RAG 检索增强（如已启用）</li>
 *   <li>构建系统提示词并编码会话记忆 ID</li>
 *   <li>加锁保护同一会话的并发安全</li>
 *   <li>调用对话服务获取模型回复</li>
 *   <li>异常时持久化降级对话记录</li>
 * </ol>
 *
 * @see Agent
 * @see InitialAgentChatService
 * @see InitialAgentMemoryId
 */
@Component
public class InitialAgent implements Agent {

    private final InitialAgentPromptService promptService;
    private final Optional<RagAugmentationService> ragAugmentationService;
    private final ObjectProvider<InitialAgentChatService> chatServiceProvider;
    private final LlmConversationStore conversationStore;

    /**
     * 会话级并发锁映射，防止同一用户会话的并发请求导致上下文错乱。
     */
    private final ConcurrentHashMap<String, ReentrantLock> conversationLocks = new ConcurrentHashMap<>();

    /**
     * 构造初始智能体实例。
     *
     * @param promptService         提示词构建服务
     * @param ragAugmentationService RAG 增强服务（可选，未配置时为空）
     * @param chatServiceProvider    对话服务提供者，延迟获取以避免循环依赖
     * @param conversationStore      会话持久化存储
     */
    public InitialAgent(InitialAgentPromptService promptService,
                        Optional<RagAugmentationService> ragAugmentationService,
                        ObjectProvider<InitialAgentChatService> chatServiceProvider,
                        LlmConversationStore conversationStore) {
        this.promptService = promptService;
        this.ragAugmentationService = ragAugmentationService;
        this.chatServiceProvider = chatServiceProvider;
        this.conversationStore = conversationStore;
    }

    /**
     * 返回智能体编码标识。
     *
     * @return 固定值 "initial-agent"
     */
    @Override
    public String code() {
        return "initial-agent";
    }

    /**
     * 返回智能体显示名称。
     *
     * @return 固定值 "初始智能体"
     */
    @Override
    public String name() {
        return "初始智能体";
    }

    /**
     * 判断智能体是否支持指定能力。
     *
     * @param capability 能力标识
     * @return 支持 chat、orchestrate、rag 三种能力时返回 true
     */
    @Override
    public boolean supports(String capability) {
        return "chat".equalsIgnoreCase(capability)
                || "orchestrate".equalsIgnoreCase(capability)
                || "rag".equalsIgnoreCase(capability);
    }

    /**
     * 处理智能体调度命令，执行对话推理并返回响应。
     *
     * <p>处理流程：检测大模型可用性 → RAG 增强 → 构建提示词 → 加锁调用对话 → 返回结果。
     * 异常时持久化降级对话记录，确保用户消息不丢失。</p>
     *
     * @param command 智能体调度命令，包含用户消息、会话信息及上下文
     * @return 调度响应，包含推理结果或错误信息
     */
    @Override
    public AgentDispatchResponse handle(AgentDispatchCommand command) {
        InitialAgentChatService chatService = chatServiceProvider.getIfAvailable();
        if (chatService == null) {
            persistFallbackConversation(command, "当前未启用大模型服务");
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(false)
                    .message("当前未启用大模型服务")
                    .build();
        }

        RagAugmentationResult augmentationResult = resolveRagAugmentation(command);
        String memoryId = InitialAgentMemoryId.encode(code(), command.getUserId(), command.getSessionId());
        String systemPrompt = promptService.buildSystemPrompt(code(), command.getUserId(), augmentationResult.getPromptBlock());
        String conversationKey = command.getUserId() + "::" + command.getSessionId();
        ReentrantLock lock = conversationLocks.computeIfAbsent(conversationKey, key -> new ReentrantLock());

        lock.lock();
        try {
            ToolCallContextHolder.set(command.getTraceId(), command.getContext());
            Result<String> result = chatService.chat(memoryId, command.getMessage(), systemPrompt);
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(true)
                    .message(result == null ? "" : result.content())
                    .build();
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? "当前无法处理请求，请稍后再试" : exception.getMessage();
            persistFallbackConversation(command, message);
            return AgentDispatchResponse.builder()
                    .traceId(command.getTraceId())
                    .agentCode(code())
                    .accepted(false)
                    .message(message)
                    .build();
        } finally {
            ToolCallContextHolder.clear();
            lock.unlock();
        }
    }

    /**
     * 执行 RAG 检索增强，获取与用户消息相关的知识片段。
     *
     * @param command 智能体调度命令
     * @return RAG 增强结果，未启用 RAG 时返回空结果
     */
    private RagAugmentationResult resolveRagAugmentation(AgentDispatchCommand command) {
        return ragAugmentationService
                .map(service -> service.augment(code(), command.getMessage(), command.getTraceId()))
                .orElseGet(() -> RagAugmentationResult.builder()
                        .enabled(false)
                        .promptBlock("")
                        .hitCount(0)
                        .build());
    }

    /**
     * 持久化降级对话记录，在大模型不可用或调用异常时确保用户消息不丢失。
     *
     * @param command          智能体调度命令
     * @param assistantMessage 降级回复内容
     */
    private void persistFallbackConversation(AgentDispatchCommand command, String assistantMessage) {
        String sessionId = code() + "::" + command.getSessionId();
        conversationStore.ensureSession(command.getUserId(), sessionId);
        long userMessageSeq = conversationStore.nextMessageSeq(command.getUserId(), sessionId);
        conversationStore.saveUserMessage(
                command.getUserId(),
                sessionId,
                userMessageSeq,
                command.getMessage(),
                command.getTraceId()
        );
        conversationStore.saveAssistantMessage(
                command.getUserId(),
                sessionId,
                userMessageSeq + 1,
                assistantMessage,
                command.getTraceId(),
                null,
                null,
                null,
                null
        );
    }
}
