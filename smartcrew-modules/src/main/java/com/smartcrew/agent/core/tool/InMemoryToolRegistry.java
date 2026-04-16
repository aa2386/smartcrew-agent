package com.smartcrew.agent.core.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolActionMetadata;
import com.smartcrew.agent.api.tool.domain.model.ToolActionParameter;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionModes;
import com.smartcrew.agent.api.tool.domain.model.ToolFlowDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolSourceStatuses;
import com.smartcrew.agent.api.tool.mapper.ToolDefinitionMapper;
import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.JsonUtils;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 Tool 注册表实现，统一解析代码 Tool 与数据库 Tool。
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

    /**
     * 刷新工具注册表，重新扫描代码 Tool 与数据库 Tool。
     */
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

    /**
     * 获取所有已注册的工具定义。
     *
     * @return 工具定义列表
     */
    @Override
    public List<ResolvedToolDefinition> listAll() {
        return definitionMap.values().stream()
                .sorted(Comparator.comparing(ResolvedToolDefinition::getToolCode))
                .toList();
    }

    /**
     * 根据编码获取工具定义。
     *
     * @param toolCode 工具编码
     * @return 工具定义（可选）
     */
    @Override
    public Optional<ResolvedToolDefinition> getByCode(String toolCode) {
        return Optional.ofNullable(definitionMap.get(toolCode));
    }

    /**
     * 获取指定工具的动作元数据。
     *
     * @param toolCode   工具编码
     * @param actionName 动作名称
     * @return 动作元数据（可选）
     */
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

    /**
     * 设置工具启用状态。
     *
     * @param toolCode 工具编码
     * @param enabled  是否启用
     */
    @Override
    public void setEnabled(String toolCode, boolean enabled) {
        ResolvedToolDefinition definition = definitionMap.get(toolCode);
        if (definition == null) {
            throw new ServiceException("Unknown tool: " + toolCode);
        }
        definition.setEnabled(enabled);
        definition.setExecutable(enabled && StringUtils.isBlank(definition.getResolveError()));
    }

    /* 扫描并发现代码中定义的 Tool。 */
    private Map<String, CodeToolDescriptor> discoverCodeTools() {
        Map<String, CodeToolDescriptor> result = new LinkedHashMap<>();
        for (Map.Entry<String, SmartCrewTool> entry : toolBeanMap.entrySet()) {
            SmartCrewTool tool = entry.getValue();
            CodeToolDescriptor descriptor = buildCodeDescriptor(entry.getKey(), tool);
            CodeToolDescriptor previous = result.putIfAbsent(descriptor.toolCode(), descriptor);
            if (previous != null) {
                throw new ServiceException("Duplicate tool code found: " + descriptor.toolCode());
            }
        }
        return result;
    }

    /* 构建代码 Tool 的描述符。 */
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

    /* 构建合并后的工具定义，整合代码 Tool 与数据库 Tool。 */
    private ResolvedToolDefinition buildResolvedDefinition(CodeToolDescriptor codeTool, ToolDefinition databaseTool) {
        String toolCode = databaseTool != null ? databaseTool.getToolCode() : codeTool.toolCode();
        boolean hasCodeBean = codeTool != null;
        boolean hasDatabaseConfig = databaseTool != null;
        String executionMode = resolveExecutionMode(databaseTool);

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

        if (ToolExecutionModes.FLOW.equals(executionMode)) {
            sourceStatus = ToolSourceStatuses.DB_ONLY;
            if (!hasDatabaseConfig) {
                resolveError = "FLOW 模式缺少数据库配置";
                executable = false;
            } else {
                try {
                    ToolFlowDefinition flowDefinition = JsonUtils.parse(databaseTool.getFlowDefinitionJson(), ToolFlowDefinition.class);
                    String actionName = StringUtils.isBlank(flowDefinition.getActionName())
                            ? "execute"
                            : flowDefinition.getActionName().trim();
                    actions.add(ToolActionMetadata.builder()
                            .toolCode(toolCode)
                            .actionName(actionName)
                            .description(firstNonBlank(flowDefinition.getDescription(), description, "执行数据库流程"))
                            .build());
                    if (flowDefinition.getSteps() == null || flowDefinition.getSteps().isEmpty()) {
                        resolveError = "FLOW 模式步骤为空";
                        executable = false;
                    }
                } catch (Exception exception) {
                    resolveError = "FLOW 定义解析失败: " + safeMessage(exception);
                    executable = false;
                }
            }
        } else if (hasCodeBean) {
            sourceStatus = hasDatabaseConfig ? ToolSourceStatuses.LINKED : ToolSourceStatuses.CODE_ONLY;
            actions.addAll(codeTool.actions());
            if (hasDatabaseConfig) {
                if (StringUtils.isBlank(databaseTool.getBeanName())) {
                    resolveError = "BEAN 模式缺少 beanName";
                    executable = false;
                } else if (!isSameBean(databaseTool.getBeanName(), codeTool.beanName())) {
                    resolveError = "BEAN 模式 beanName 与代码实现不匹配";
                    executable = false;
                }
            }
            if (actions.isEmpty()) {
                resolveError = "代码 Tool 未声明任何 @Tool 动作";
                executable = false;
            }
        } else {
            sourceStatus = ToolSourceStatuses.DB_ONLY;
            resolveError = "BEAN 模式缺少代码实现";
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
                .executionMode(executionMode)
                .riskLevel(riskLevel)
                .enabled(enabled)
                .configJson(databaseTool == null ? null : databaseTool.getConfigJson())
                .flowDefinitionJson(databaseTool == null ? null : databaseTool.getFlowDefinitionJson())
                .sourceStatus(sourceStatus)
                .hasCodeBean(hasCodeBean)
                .hasDatabaseConfig(hasDatabaseConfig)
                .executable(executable)
                .resolveError(resolveError)
                .actions(actions)
                .build();
    }

    /* 解析工具启用状态，优先使用数据库配置。 */
    private boolean resolveEnabled(String toolCode, CodeToolDescriptor codeTool, ToolDefinition databaseTool) {
        if (databaseTool != null && databaseTool.getEnabled() != null) {
            return databaseTool.getEnabled();
        }
        if (codeTool != null) {
            return smartCrewProperties.getTools().isEnabled(toolCode) && codeTool.enabledByDefault();
        }
        return true;
    }

    /* 解析工具执行模式。 */
    private String resolveExecutionMode(ToolDefinition databaseTool) {
        if (databaseTool == null || StringUtils.isBlank(databaseTool.getExecutionMode())) {
            return ToolExecutionModes.BEAN;
        }
        return databaseTool.getExecutionMode().trim().toUpperCase(Locale.ROOT);
    }

    /* 判断两个 Bean 名称是否指向同一个 Bean 实例。 */
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

    /* 解析方法参数名称。 */
    private String resolveParameterName(Parameter parameter) {
        P pAnnotation = parameter.getAnnotation(P.class);
        if (pAnnotation != null && StringUtils.isNotBlank(pAnnotation.value())) {
            return pAnnotation.value().trim();
        }
        return parameter.getName();
    }

    /* 返回第一个非空字符串。 */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /* 安全获取异常消息。 */
    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    /* 代码 Tool 描述符，用于存储扫描结果。 */
    private record CodeToolDescriptor(String toolCode,
                                      String toolName,
                                      String description,
                                      String beanName,
                                      String riskLevel,
                                      boolean enabledByDefault,
                                      List<ToolActionMetadata> actions) {
    }
}
