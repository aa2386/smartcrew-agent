package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionModes;
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
 * 默认 Tool 执行器，实现 BEAN / FLOW 的统一分发。
 */
@RequiredArgsConstructor
@Service
public class DefaultToolExecutor implements ToolExecutor {

    private final ToolRegistry toolRegistry;
    private final BeanToolExecutor beanToolExecutor;
    private final FlowToolExecutor flowToolExecutor;

    /**
     * 执行工具调用，根据执行模式分发到对应的执行器。
     *
     * @param toolCode         工具编码
     * @param actionName       动作名称
     * @param arguments        调用参数
     * @param executionContext 执行上下文
     * @return 执行结果
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

        if (ToolExecutionModes.FLOW.equals(definition.getExecutionMode())) {
            return flowToolExecutor.execute(definition, resolvedActionName, safeArguments, safeExecutionContext);
        }
        return beanToolExecutor.execute(definition, resolvedActionName, safeArguments, safeExecutionContext);
    }

    /* 解析动作名称，若未指定则尝试使用唯一动作。 */
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
