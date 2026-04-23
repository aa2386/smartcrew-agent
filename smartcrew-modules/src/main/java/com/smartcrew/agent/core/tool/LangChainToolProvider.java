package com.smartcrew.agent.core.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smartcrew.agent.api.agent.service.AgentToolBindingService;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolActionMetadata;
import com.smartcrew.agent.api.tool.domain.model.ToolActionParameter;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionResult;
import com.smartcrew.agent.api.tool.domain.model.ToolParameterTypes;
import com.smartcrew.agent.common.util.JsonUtils;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.agent.service.InitialAgentMemoryId;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j 工具提供者实现，将平台工具注册为 LangChain4j 可调用的 Tool。
 *
 * <p>根据智能体绑定的工具列表，动态构建 {@link ToolSpecification} 和对应的 {@link ToolExecutor}，
 * 使大模型在推理过程中能够识别并调用平台注册的工具。</p>
 *
 * @see ToolProvider
 * @see ToolSpecification
 */
@Component
public class LangChainToolProvider implements ToolProvider {

    /**
     * 工具编码与动作名称的分隔符，用于生成扁平化的 LangChain4j 工具名。
     */
    private static final String NAME_DELIMITER = "__";

    private final AgentToolBindingService agentToolBindingService;
    private final com.smartcrew.agent.api.tool.service.ToolExecutor toolExecutor;

    /**
     * 构造 LangChain4j 工具提供者实例。
     *
     * @param agentToolBindingService 智能体工具绑定服务，用于获取可用工具
     * @param toolExecutor            平台工具执行器，用于实际调用工具
     */
    public LangChainToolProvider(AgentToolBindingService agentToolBindingService,
                                 com.smartcrew.agent.api.tool.service.ToolExecutor toolExecutor) {
        this.agentToolBindingService = agentToolBindingService;
        this.toolExecutor = toolExecutor;
    }

    /**
     * 根据请求中的会话记忆 ID 获取智能体绑定的工具，构建 LangChain4j 工具映射。
     *
     * @param request 工具提供请求，包含会话记忆 ID 和用户消息
     * @return LangChain4j 工具规格与执行器的映射结果
     */
    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        InitialAgentMemoryId memoryId = InitialAgentMemoryId.parse(request.chatMemoryId());
        List<ResolvedToolDefinition> tools = agentToolBindingService.listEnabledResolvedToolsByAgentCode(memoryId.agentCode());
        Map<ToolSpecification, ToolExecutor> mappedTools = new LinkedHashMap<>();
        Map<String, Object> executionContext = buildExecutionContext(memoryId, request);

        for (ResolvedToolDefinition tool : tools) {
            for (ToolActionMetadata action : tool.getActions()) {
                String flatName = flatName(tool.getToolCode(), action.getActionName());
                ToolSpecification specification = ToolSpecification.builder()
                        .name(flatName)
                        .description(firstNonBlank(action.getDescription(), tool.getDescription(), flatName))
                        .parameters(buildParametersSchema(action))
                        .build();
                mappedTools.put(specification, (toolRequest, memoryKey) -> executeTool(tool, action, toolRequest, executionContext));
            }
        }

        return new ToolProviderResult(mappedTools);
    }

    /**
     * 执行工具调用，将 LangChain4j 的工具执行请求转换为平台工具执行。
     *
     * @param tool             工具定义
     * @param action           动作元数据
     * @param request          LangChain4j 工具执行请求
     * @param executionContext 执行上下文
     * @return 执行结果的 JSON 字符串
     */
    private String executeTool(ResolvedToolDefinition tool,
                               ToolActionMetadata action,
                               ToolExecutionRequest request,
                               Map<String, Object> executionContext) {
        try {
            Map<String, Object> arguments = parseArguments(request.arguments());
            ToolExecutionResult result = toolExecutor.execute(
                    tool.getToolCode(),
                    action.getActionName(),
                    arguments,
                    executionContext
            );
            if (!Boolean.TRUE.equals(result.getSuccess())) {
                return JsonUtils.toJson(Map.of(
                        "success", false,
                        "errorMessage", firstNonBlank(result.getErrorMessage(), "Tool 执行失败")
                ));
            }
            if (result.getOutput() instanceof String text) {
                return text;
            }
            return JsonUtils.toJson(result.getOutput());
        } catch (Exception exception) {
            return JsonUtils.toJson(Map.of(
                    "success", false,
                    "errorMessage", firstNonBlank(exception.getMessage(), exception.getClass().getSimpleName())
            ));
        }
    }

    /**
     * 构建工具执行上下文，包含智能体编码、用户ID、会话ID及追踪信息。
     *
     * @param memoryId 会话记忆标识
     * @param request  工具提供请求
     * @return 执行上下文映射
     */
    private Map<String, Object> buildExecutionContext(InitialAgentMemoryId memoryId, ToolProviderRequest request) {
        Map<String, Object> executionContext = new LinkedHashMap<>();
        executionContext.put("agentCode", memoryId.agentCode());
        executionContext.put("userId", memoryId.userId());
        executionContext.put("sessionId", memoryId.sessionId());
        executionContext.put("input", request.userMessage() == null ? "" : request.userMessage().singleText());

        ToolCallContextHolder.ToolCallContext context = ToolCallContextHolder.get();
        if (context != null) {
            executionContext.put("traceId", context.traceId());
            executionContext.putAll(context.context());
        }
        return executionContext;
    }

    /**
     * 根据动作参数元数据构建 JSON Schema 参数定义。
     *
     * @param action 动作元数据
     * @return JSON 对象 Schema
     */
    private JsonObjectSchema buildParametersSchema(ToolActionMetadata action) {
        JsonObjectSchema.Builder builder = JsonObjectSchema.builder().additionalProperties(false);
        for (ToolActionParameter parameter : action.getParameters()) {
            String description = firstNonBlank(parameter.getDescription(), parameter.getName());
            switch (firstNonBlank(parameter.getType(), ToolParameterTypes.STRING)) {
                case ToolParameterTypes.INTEGER -> builder.addIntegerProperty(parameter.getName(), description);
                case ToolParameterTypes.NUMBER -> builder.addNumberProperty(parameter.getName(), description);
                case ToolParameterTypes.BOOLEAN -> builder.addBooleanProperty(parameter.getName(), description);
                default -> builder.addStringProperty(parameter.getName(), description);
            }
        }

        List<String> required = action.getParameters().stream()
                .filter(ToolActionParameter::isRequired)
                .map(ToolActionParameter::getName)
                .toList();
        if (!required.isEmpty()) {
            builder.required(required);
        }
        return builder.build();
    }

    /**
     * 解析工具调用参数 JSON 字符串为 Map。
     *
     * @param json 参数 JSON 字符串
     * @return 参数映射，输入为空时返回空 Map
     */
    private Map<String, Object> parseArguments(String json) {
        if (StringUtils.isBlank(json)) {
            return Map.of();
        }
        return JsonUtils.parse(json, new TypeReference<LinkedHashMap<String, Object>>() {
        });
    }

    /**
     * 生成扁平化的 LangChain4j 工具名称，格式为 {@code toolCode__actionName}。
     *
     * @param toolCode   工具编码
     * @param actionName 动作名称
     * @return 扁平化工具名称
     */
    private String flatName(String toolCode, String actionName) {
        return toolCode + NAME_DELIMITER + actionName;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
