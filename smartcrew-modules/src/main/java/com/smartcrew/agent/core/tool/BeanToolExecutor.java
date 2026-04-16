package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionResult;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.JsonUtils;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Locale;
import java.util.Map;

/**
 * BEAN 模式 Tool 执行器。
 */
@Component
public class BeanToolExecutor {

    private final ApplicationContext applicationContext;

    public BeanToolExecutor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 执行 BEAN 模式的工具调用。
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
        Object bean = applicationContext.getBean(definition.getBeanName());
        Method method = resolveActionMethod(bean, actionName, definition.getToolCode());
        Object[] invocationArguments = buildInvocationArguments(method, arguments);

        try {
            Object output = method.invoke(bean, invocationArguments);
            return ToolExecutionResult.builder()
                    .toolCode(definition.getToolCode())
                    .actionName(actionName)
                    .executionMode(definition.getExecutionMode())
                    .success(Boolean.TRUE)
                    .output(output)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (InvocationTargetException exception) {
            Throwable targetException = exception.getTargetException();
            throw new ServiceException(500, "Tool 执行失败: " + safeMessage(targetException));
        } catch (IllegalAccessException exception) {
            throw new ServiceException(500, "Tool 动作不可访问: " + actionName);
        }
    }

    /* 解析并获取目标方法。 */
    private Method resolveActionMethod(Object bean, String actionName, String toolCode) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        for (Method method : targetClass.getMethods()) {
            if (!actionName.equals(method.getName())) {
                continue;
            }
            if (method.getAnnotation(Tool.class) != null) {
                return method;
            }
        }
        throw new ServiceException(404, "Tool 动作不存在: " + toolCode + "#" + actionName);
    }

    /* 构建方法调用参数数组。 */
    private Object[] buildInvocationArguments(Method method, Map<String, Object> arguments) {
        Parameter[] parameters = method.getParameters();
        Object[] values = new Object[parameters.length];
        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
        for (int index = 0; index < parameters.length; index++) {
            Parameter parameter = parameters[index];
            String externalName = resolveExternalParameterName(parameter);
            Object rawValue = findArgumentValue(safeArguments, externalName, parameter.getName(), parameters.length == 1);
            if (rawValue == null && parameter.getType().isPrimitive()) {
                throw new ServiceException(400, "缺少必填参数: " + externalName);
            }
            values[index] = convertValue(rawValue, parameter.getType());
        }
        return values;
    }

    /* 从参数映射中查找参数值。 */
    private Object findArgumentValue(Map<String, Object> arguments,
                                     String externalName,
                                     String javaName,
                                     boolean allowSingleValueFallback) {
        if (arguments.containsKey(externalName)) {
            return arguments.get(externalName);
        }
        if (arguments.containsKey(javaName)) {
            return arguments.get(javaName);
        }
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(externalName.trim())) {
                return entry.getValue();
            }
        }
        if (allowSingleValueFallback && arguments.size() == 1) {
            return arguments.values().iterator().next();
        }
        return null;
    }

    /* 解析参数的外部名称（优先使用 @P 注解值）。 */
    private String resolveExternalParameterName(Parameter parameter) {
        P pAnnotation = parameter.getAnnotation(P.class);
        if (pAnnotation != null && StringUtils.isNotBlank(pAnnotation.value())) {
            return pAnnotation.value().trim();
        }
        return parameter.getName();
    }

    /* 将原始值转换为目标类型。 */
    private Object convertValue(Object rawValue, Class<?> targetType) {
        if (rawValue == null) {
            return null;
        }
        if (targetType.isInstance(rawValue)) {
            return rawValue;
        }
        if (String.class.equals(targetType)) {
            return String.valueOf(rawValue);
        }
        if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
            return rawValue instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(rawValue));
        }
        if (Long.class.equals(targetType) || long.class.equals(targetType)) {
            return rawValue instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(rawValue));
        }
        if (Double.class.equals(targetType) || double.class.equals(targetType)) {
            return rawValue instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(rawValue));
        }
        if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) {
            if (rawValue instanceof Boolean bool) {
                return bool;
            }
            return Boolean.parseBoolean(String.valueOf(rawValue).toLowerCase(Locale.ROOT));
        }
        return JsonUtils.parse(JsonUtils.toJson(rawValue), targetType);
    }

    /* 安全获取异常消息。 */
    private String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }
}
