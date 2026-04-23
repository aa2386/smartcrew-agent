package com.smartcrew.agent.core.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolActionMetadata;
import com.smartcrew.agent.api.tool.domain.model.ToolActionParameter;
import com.smartcrew.agent.api.tool.domain.model.ToolParameterTypes;
import com.smartcrew.agent.api.tool.domain.model.ToolSourceStatuses;
import com.smartcrew.agent.api.tool.mapper.ToolDefinitionMapper;
import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时 Tool 注册中心。
 */
@Component
public class InMemoryToolRegistry implements ToolRegistry {

    private final Map<String, SmartCrewTool> toolBeanMap;
    private final ToolDefinitionMapper toolDefinitionMapper;
    private final SmartCrewProperties smartCrewProperties;
    private final ApplicationContext applicationContext;
    private final ConcurrentHashMap<String, ResolvedToolDefinition> definitionMap = new ConcurrentHashMap<>();

    public InMemoryToolRegistry(Map<String, SmartCrewTool> toolBeanMap,
                                ToolDefinitionMapper toolDefinitionMapper,
                                SmartCrewProperties smartCrewProperties,
                                ApplicationContext applicationContext) {
        this.toolBeanMap = toolBeanMap;
        this.toolDefinitionMapper = toolDefinitionMapper;
        this.smartCrewProperties = smartCrewProperties;
        this.applicationContext = applicationContext;
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void refresh() {
        Map<String, CodeToolDescriptor> codeTools = discoverCodeTools();
        Map<String, ToolDefinition> databaseTools = toolDefinitionMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getToolCode(), item), LinkedHashMap::putAll);

        TreeSet<String> codes = new TreeSet<>();
        codes.addAll(codeTools.keySet());
        codes.addAll(databaseTools.keySet());

        definitionMap.clear();
        for (String code : codes) {
            definitionMap.put(code, buildResolvedDefinition(codeTools.get(code), databaseTools.get(code)));
        }
    }

    @Override
    public List<ResolvedToolDefinition> listAll() {
        return definitionMap.values().stream()
                .sorted(Comparator.comparing(ResolvedToolDefinition::getToolCode))
                .toList();
    }

    @Override
    public Optional<ResolvedToolDefinition> getByCode(String toolCode) {
        return Optional.ofNullable(definitionMap.get(toolCode));
    }

    @Override
    public Optional<ToolActionMetadata> getAction(String toolCode, String actionName) {
        if (StringUtils.isBlank(actionName)) {
            return Optional.empty();
        }
        return getByCode(toolCode)
                .flatMap(definition -> definition.getActions().stream()
                        .filter(item -> actionName.equals(item.getActionName()))
                        .findFirst());
    }

    private Map<String, CodeToolDescriptor> discoverCodeTools() {
        Map<String, CodeToolDescriptor> result = new LinkedHashMap<>();
        for (Map.Entry<String, SmartCrewTool> entry : toolBeanMap.entrySet()) {
            CodeToolDescriptor descriptor = buildCodeDescriptor(entry.getKey(), entry.getValue());
            CodeToolDescriptor previous = result.putIfAbsent(descriptor.toolCode(), descriptor);
            if (previous != null) {
                throw new ServiceException("Duplicate tool code found: " + descriptor.toolCode());
            }
        }
        return result;
    }

    private CodeToolDescriptor buildCodeDescriptor(String beanName, SmartCrewTool tool) {
        Class<?> targetClass = AopUtils.getTargetClass(tool);
        List<ToolActionMetadata> actions = new ArrayList<>();
        for (Method method : targetClass.getMethods()) {
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation == null) {
                continue;
            }

            List<ToolActionParameter> parameters = new ArrayList<>();
            for (Parameter parameter : method.getParameters()) {
                P pAnnotation = parameter.getAnnotation(P.class);
                parameters.add(ToolActionParameter.builder()
                        .name(resolveParameterName(parameter))
                        .description(pAnnotation == null ? "" : pAnnotation.value())
                        .type(resolveParameterType(parameter))
                        .required(true)
                        .build());
            }

            actions.add(ToolActionMetadata.builder()
                    .toolCode(tool.toolCode())
                    .actionName(method.getName())
                    .description(String.join(" ", toolAnnotation.value()))
                    .parameters(parameters)
                    .build());
        }
        actions.sort(Comparator.comparing(ToolActionMetadata::getActionName));
        return new CodeToolDescriptor(
                tool.toolCode(),
                tool.toolName(),
                tool.description(),
                beanName,
                tool.riskLevel(),
                tool.enabledByDefault(),
                actions
        );
    }

    private ResolvedToolDefinition buildResolvedDefinition(CodeToolDescriptor codeTool, ToolDefinition databaseTool) {
        String toolCode = databaseTool != null ? databaseTool.getToolCode() : codeTool.toolCode();
        boolean hasCodeBean = codeTool != null;
        boolean hasDatabaseConfig = databaseTool != null;

        String toolName = firstNonBlank(hasDatabaseConfig ? databaseTool.getToolName() : null,
                hasCodeBean ? codeTool.toolName() : null,
                toolCode);
        String description = firstNonBlank(hasDatabaseConfig ? databaseTool.getDescription() : null,
                hasCodeBean ? codeTool.description() : null,
                "");
        String beanName = firstNonBlank(hasDatabaseConfig ? databaseTool.getBeanName() : null,
                hasCodeBean ? codeTool.beanName() : null,
                null);
        String riskLevel = firstNonBlank(hasDatabaseConfig ? databaseTool.getRiskLevel() : null,
                hasCodeBean ? codeTool.riskLevel() : null,
                "MEDIUM");
        boolean enabled = resolveEnabled(toolCode, codeTool, databaseTool);

        List<ToolActionMetadata> actions = new ArrayList<>();
        String sourceStatus;
        String resolveError = null;
        boolean executable = enabled;

        if (hasCodeBean) {
            sourceStatus = hasDatabaseConfig ? ToolSourceStatuses.LINKED : ToolSourceStatuses.CODE_ONLY;
            actions.addAll(codeTool.actions());
            if (hasDatabaseConfig) {
                if (StringUtils.isBlank(databaseTool.getBeanName())) {
                    resolveError = "缺少 Spring Bean 名称";
                    executable = false;
                } else if (!isSameBean(databaseTool.getBeanName(), codeTool.beanName())) {
                    resolveError = "Spring Bean 名称与代码实现不匹配";
                    executable = false;
                }
            }
            if (actions.isEmpty()) {
                resolveError = "代码 Tool 未声明任何 @Tool 动作";
                executable = false;
            }
        } else {
            sourceStatus = ToolSourceStatuses.DB_ONLY;
            resolveError = "缺少代码实现";
            executable = false;
        }

        if (!enabled) {
            executable = false;
        }

        return ResolvedToolDefinition.builder()
                .id(databaseTool == null ? null : databaseTool.getId())
                .toolCode(toolCode)
                .toolName(toolName)
                .description(description)
                .beanName(beanName)
                .riskLevel(riskLevel)
                .enabled(enabled)
                .configJson(databaseTool == null ? null : databaseTool.getConfigJson())
                .sourceStatus(sourceStatus)
                .hasCodeBean(hasCodeBean)
                .hasDatabaseConfig(hasDatabaseConfig)
                .executable(executable)
                .resolveError(resolveError)
                .actions(actions)
                .build();
    }

    private boolean resolveEnabled(String toolCode, CodeToolDescriptor codeTool, ToolDefinition databaseTool) {
        if (databaseTool != null && databaseTool.getEnabled() != null) {
            return databaseTool.getEnabled();
        }
        if (codeTool != null) {
            return smartCrewProperties.getTools().isEnabled(toolCode) && codeTool.enabledByDefault();
        }
        return true;
    }

    private boolean isSameBean(String configuredBeanName, String runtimeBeanName) {
        if (StringUtils.isBlank(configuredBeanName) || StringUtils.isBlank(runtimeBeanName)) {
            return false;
        }
        if (configuredBeanName.equals(runtimeBeanName)) {
            return true;
        }
        if (!applicationContext.containsBean(configuredBeanName) || !applicationContext.containsBean(runtimeBeanName)) {
            return false;
        }
        return applicationContext.getBean(configuredBeanName) == applicationContext.getBean(runtimeBeanName);
    }

    private String resolveParameterName(Parameter parameter) {
        P pAnnotation = parameter.getAnnotation(P.class);
        if (pAnnotation != null && StringUtils.isNotBlank(pAnnotation.value())) {
            return pAnnotation.value().trim();
        }
        return parameter.getName();
    }

    private String resolveParameterType(Parameter parameter) {
        Class<?> type = parameter.getType();
        if (type == Integer.class || type == int.class || type == Long.class || type == long.class
                || type == Short.class || type == short.class || type == Byte.class || type == byte.class) {
            return ToolParameterTypes.INTEGER;
        }
        if (type == Double.class || type == double.class || type == Float.class || type == float.class) {
            return ToolParameterTypes.NUMBER;
        }
        if (type == Boolean.class || type == boolean.class) {
            return ToolParameterTypes.BOOLEAN;
        }
        return ToolParameterTypes.STRING;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private record CodeToolDescriptor(String toolCode,
                                      String toolName,
                                      String description,
                                      String beanName,
                                      String riskLevel,
                                      boolean enabledByDefault,
                                      List<ToolActionMetadata> actions) {
    }
}
