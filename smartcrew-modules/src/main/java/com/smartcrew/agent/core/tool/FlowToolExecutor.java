package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionResult;
import com.smartcrew.agent.api.tool.domain.model.ToolFlowDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolFlowStep;
import com.smartcrew.agent.api.tool.service.ToolExecutor;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.JsonUtils;
import com.smartcrew.agent.common.util.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FLOW 模式 Tool 执行器。
 */
@Component
public class FlowToolExecutor {

    private static final Pattern SINGLE_EXPRESSION = Pattern.compile("^\\s*\\{\\{\\s*([^}]+?)\\s*}}\\s*$");
    private static final Pattern EMBEDDED_EXPRESSION = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");

    private final ObjectProvider<ToolExecutor> toolExecutorProvider;

    public FlowToolExecutor(ObjectProvider<ToolExecutor> toolExecutorProvider) {
        this.toolExecutorProvider = toolExecutorProvider;
    }

    /**
     * 执行 FLOW 模式的工具调用。
     *
     * @param definition       工具定义
     * @param actionName       动作名称
     * @param arguments        调用参数
     * @param executionContext 执行上下文
     * @return 执行结果
     */
    public ToolExecutionResult execute(ResolvedToolDefinition definition,
                                       String actionName,
                                       Map<String, Object> arguments,
                                       Map<String, Object> executionContext) {
        long startTime = System.currentTimeMillis();
        ToolFlowDefinition flowDefinition = JsonUtils.parse(definition.getFlowDefinitionJson(), ToolFlowDefinition.class);
        String expectedActionName = StringUtils.isBlank(flowDefinition.getActionName())
                ? "execute"
                : flowDefinition.getActionName().trim();
        if (!expectedActionName.equals(actionName)) {
            throw new ServiceException(400, "FLOW Tool 动作不匹配: " + definition.getToolCode() + "#" + actionName);
        }

        Map<String, Object> vars = new LinkedHashMap<>();
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("arguments", arguments);
        model.put("context", executionContext);
        model.put("vars", vars);
        model.put("toolCode", definition.getToolCode());
        model.put("actionName", actionName);

        Object returnValue = null;
        boolean hasReturn = false;
        for (ToolFlowStep step : flowDefinition.getSteps()) {
            if (step == null || StringUtils.isBlank(step.getType())) {
                throw new ServiceException(400, "FLOW 步骤缺少 type");
            }
            String type = step.getType().trim().toLowerCase();
            switch (type) {
                case "template" -> {
                    Object rendered = renderValue(step.getTemplate(), model);
                    if (StringUtils.isBlank(step.getOutput())) {
                        throw new ServiceException(400, "template 步骤必须指定 output");
                    }
                    vars.put(step.getOutput().trim(), rendered);
                    model.put("lastResult", rendered);
                }
                case "tool_call" -> {
                    String targetToolCode = String.valueOf(renderValue(step.getToolCode(), model));
                    String targetActionName = step.getActionName() == null
                            ? null
                            : String.valueOf(renderValue(step.getActionName(), model));
                    Map<String, Object> renderedArguments = renderArguments(step.getArguments(), model);
                    ToolExecutionResult nestedResult = toolExecutorProvider.getObject()
                            .execute(targetToolCode, targetActionName, renderedArguments, executionContext);
                    Object nestedOutput = nestedResult.getOutput();
                    if (StringUtils.isNotBlank(step.getOutput())) {
                        vars.put(step.getOutput().trim(), nestedOutput);
                    }
                    model.put("lastResult", nestedOutput);
                }
                case "return" -> {
                    returnValue = renderValue(step.getTemplate(), model);
                    hasReturn = true;
                }
                default -> throw new ServiceException(400, "暂不支持的 FLOW 步骤类型: " + step.getType());
            }
        }

        if (!hasReturn) {
            throw new ServiceException(400, "FLOW 定义必须包含 return 步骤");
        }

        return ToolExecutionResult.builder()
                .toolCode(definition.getToolCode())
                .actionName(actionName)
                .executionMode(definition.getExecutionMode())
                .success(Boolean.TRUE)
                .output(returnValue)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /* 渲染参数映射中的所有值。 */
    private Map<String, Object> renderArguments(Map<String, Object> arguments, Map<String, Object> model) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (arguments == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            result.put(entry.getKey(), renderValue(entry.getValue(), model));
        }
        return result;
    }

    /* 递归渲染模板值。 */
    private Object renderValue(Object template, Map<String, Object> model) {
        if (template == null) {
            return null;
        }
        if (template instanceof String text) {
            return renderString(text, model);
        }
        if (template instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), renderValue(entry.getValue(), model));
            }
            return result;
        }
        if (template instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                result.add(renderValue(item, model));
            }
            return result;
        }
        return template;
    }

    /* 渲染字符串模板，支持 {{expression}} 语法。 */
    private Object renderString(String text, Map<String, Object> model) {
        Matcher singleMatcher = SINGLE_EXPRESSION.matcher(text);
        if (singleMatcher.matches()) {
            return resolvePath(model, singleMatcher.group(1));
        }
        Matcher matcher = EMBEDDED_EXPRESSION.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Object resolved = resolvePath(model, matcher.group(1));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(resolved == null ? "" : String.valueOf(resolved)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /* 根据点分隔路径从模型中解析值。 */
    @SuppressWarnings("unchecked")
    private Object resolvePath(Object current, String expression) {
        if (current == null || StringUtils.isBlank(expression)) {
            return null;
        }
        String[] parts = expression.trim().split("\\.");
        Object cursor = current;
        for (String part : parts) {
            if (cursor == null) {
                return null;
            }
            if (cursor instanceof Map<?, ?> map) {
                cursor = ((Map<String, Object>) map).get(part);
                continue;
            }
            if (cursor instanceof List<?> list) {
                try {
                    int index = Integer.parseInt(part);
                    cursor = list.get(index);
                    continue;
                } catch (Exception ignored) {
                    return null;
                }
            }
            return null;
        }
        return cursor;
    }
}
