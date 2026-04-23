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

    private String resolveExternalParameterName(Parameter parameter) {
        P pAnnotation = parameter.getAnnotation(P.class);
        if (pAnnotation != null && StringUtils.isNotBlank(pAnnotation.value())) {
            return pAnnotation.value().trim();
        }
        return parameter.getName();
    }

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

    private String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }
}
