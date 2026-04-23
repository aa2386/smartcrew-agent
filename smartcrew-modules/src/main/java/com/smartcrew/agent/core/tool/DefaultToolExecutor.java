package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionResult;
import com.smartcrew.agent.api.tool.service.ToolExecutor;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

/**
 * 默认工具执行器实现，负责校验工具状态并委托 Bean 模式执行。
 *
 * <p>执行流程：从注册中心获取工具定义 → 校验启用与可执行状态 →
 * 解析动作名称 → 委托 {@link BeanToolExecutor} 执行具体逻辑。</p>
 *
 * @see ToolExecutor
 * @see BeanToolExecutor
 * @see ToolRegistry
 */
@RequiredArgsConstructor
@Service
public class DefaultToolExecutor implements ToolExecutor {

    private final ToolRegistry toolRegistry;
    private final BeanToolExecutor beanToolExecutor;

    /**
     * 执行工具调用，校验工具状态后委托 Bean 执行器处理。
     *
     * @param toolCode          工具编码
     * @param actionName        动作名称，为空时自动解析唯一动作
     * @param arguments         调用参数
     * @param executionContext  执行上下文
     * @return 工具执行结果
     * @throws ServiceException 当工具不存在、已禁用或不可执行时抛出
     */
    @Override
    public ToolExecutionResult execute(String toolCode,
                                       String actionName,
                                       Map<String, Object> arguments,
                                       Map<String, Object> executionContext) {
        ResolvedToolDefinition definition = toolRegistry.getByCode(toolCode)
                .orElseThrow(() -> new ServiceException("Unknown tool: " + toolCode));
        if (!Boolean.TRUE.equals(definition.getEnabled())) {
            throw new ServiceException("Tool is disabled: " + toolCode);
        }
        if (!Boolean.TRUE.equals(definition.getExecutable())) {
            throw new ServiceException("Tool is not executable: " + toolCode
                    + (StringUtils.isBlank(definition.getResolveError()) ? "" : " - " + definition.getResolveError()));
        }

        String resolvedActionName = resolveActionName(definition, actionName);
        Map<String, Object> safeArguments = arguments == null ? Collections.emptyMap() : arguments;
        Map<String, Object> safeExecutionContext = executionContext == null ? Collections.emptyMap() : executionContext;
        return beanToolExecutor.execute(definition, resolvedActionName, safeArguments, safeExecutionContext);
    }

    /**
     * 解析动作名称，若未指定则尝试自动推断唯一动作。
     *
     * @param definition 工具定义
     * @param actionName 显式指定的动作名称，可为空
     * @return 解析后的动作名称
     * @throws ServiceException 当工具无动作或存在多个动作但未显式指定时抛出
     */
    private String resolveActionName(ResolvedToolDefinition definition, String actionName) {
        if (StringUtils.isNotBlank(actionName)) {
            return actionName.trim();
        }
        if (definition.getActions() == null || definition.getActions().isEmpty()) {
            throw new ServiceException(400, "Tool 未声明任何可执行动作: " + definition.getToolCode());
        }
        if (definition.getActions().size() > 1) {
            throw new ServiceException(400, "Tool 存在多个动作，必须显式指定 actionName: " + definition.getToolCode());
        }
        return definition.getActions().get(0).getActionName();
    }
}
