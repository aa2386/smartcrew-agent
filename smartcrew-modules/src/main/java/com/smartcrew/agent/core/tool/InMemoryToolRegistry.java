package com.smartcrew.agent.core.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolActionMetadata;
import com.smartcrew.agent.api.tool.domain.model.ToolActionParameter;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionModes;
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
 * 内存工具注册中心实现，融合代码层与数据库层的工具定义。
 *
 * <p>在应用启动时扫描所有 {@link SmartCrewTool} Bean 和数据库中的工具配置，
 * 将两者合并为 {@link ResolvedToolDefinition}，并缓存到内存中供运行时查询。</p>
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>发现代码层工具（通过 {@link Tool} 注解扫描）</li>
 *   <li>加载数据库层工具配置</li>
 *   <li>合并两层定义并判定来源状态与可执行性</li>
 *   <li>提供按编码查询、列表查询、启用/禁用等操作</li>
 * </ul>
 *
 * @see ToolRegistry
 * @see ResolvedToolDefinition
 * @see SmartCrewTool
 */
@Component
public class InMemoryToolRegistry implements ToolRegistry {

    private final Map<String, SmartCrewTool> toolBeanMap;
    private final ToolDefinitionMapper toolDefinitionMapper;
    private final SmartCrewProperties smartCrewProperties;
    private final ApplicationContext applicationContext;

    /**
     * 工具定义缓存，以工具编码为键。
     */
    private final ConcurrentHashMap<String, ResolvedToolDefinition> definitionMap = new ConcurrentHashMap<>();

    /**
     * 构造内存工具注册中心实例。
     *
     * @param toolBeanMap          Spring 容器中所有 {@link SmartCrewTool} Bean 的映射
     * @param toolDefinitionMapper 工具定义数据库 Mapper
     * @param smartCrewProperties  平台配置属性
     * @param applicationContext   Spring 应用上下文，用于 Bean 比对
     */
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
     * 刷新工具注册缓存，重新扫描代码层工具并加载数据库配置后合并。
     *
     * <p>在应用启动完成事件（{@link ApplicationReadyEvent}）触发时自动执行。</p>
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
     * 返回所有已注册工具的排序列表。
     *
     * @return 按工具编码排序的定义列表
     */
    @Override
    public List<ResolvedToolDefinition> listAll() {
        return definitionMap.values().stream()
                .sorted(Comparator.comparing(ResolvedToolDefinition::getToolCode))
                .toList();
    }

    /**
     * 根据工具编码查询已注册的工具定义。
     *
     * @param toolCode 工具编码
     * @return 工具定义（可能为空）
     */
    @Override
    public Optional<ResolvedToolDefinition> getByCode(String toolCode) {
        return Optional.ofNullable(definitionMap.get(toolCode));
    }

    /**
     * 查询指定工具的特定动作元数据。
     *
     * @param toolCode   工具编码
     * @param actionName 动作名称
     * @return 动作元数据（可能为空）
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
     * 设置工具的启用/禁用状态，并同步更新可执行性。
     *
     * @param toolCode 工具编码
     * @param enabled  是否启用
     * @throws ServiceException 当工具编码不存在时抛出
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

    /**
     * 扫描 Spring 容器中所有 {@link SmartCrewTool} Bean，构建代码层工具描述符。
     *
     * @return 以工具编码为键的代码层工具描述符映射
     * @throws ServiceException 当发现重复的工具编码时抛出
     */
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

    /**
     * 根据代码层工具 Bean 构建描述符，扫描其 {@link Tool} 注解方法提取动作元数据。
     *
     * @param beanName Bean 名称
     * @param tool     工具实例
     * @return 代码层工具描述符
     */
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

    /**
     * 合并代码层与数据库层工具定义，构建运行时解析后的工具定义。
     *
     * <p>优先使用数据库配置覆盖代码层默认值，同时校验 Bean 名称匹配性
     * 并判定来源状态与可执行性。</p>
     *
     * @param codeTool    代码层工具描述符，可为 null
     * @param databaseTool 数据库层工具定义，可为 null
     * @return 合并后的运行时工具定义
     */
    private ResolvedToolDefinition buildResolvedDefinition(CodeToolDescriptor codeTool, ToolDefinition databaseTool) {
        String toolCode = databaseTool != null ? databaseTool.getToolCode() : codeTool.toolCode();
        boolean hasCodeBean = codeTool != null;
        boolean hasDatabaseConfig = databaseTool != null;
        String executionMode = ToolExecutionModes.BEAN;

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
                .sourceStatus(sourceStatus)
                .hasCodeBean(hasCodeBean)
                .hasDatabaseConfig(hasDatabaseConfig)
                .executable(executable)
                .resolveError(resolveError)
                .actions(actions)
                .build();
    }

    /**
     * 解析工具启用状态，优先使用数据库配置，其次使用配置属性与代码层默认值。
     *
     * @param toolCode    工具编码
     * @param codeTool    代码层工具描述符
     * @param databaseTool 数据库层工具定义
     * @return 是否启用
     */
    private boolean resolveEnabled(String toolCode, CodeToolDescriptor codeTool, ToolDefinition databaseTool) {
        if (databaseTool != null && databaseTool.getEnabled() != null) {
            return databaseTool.getEnabled();
        }
        if (codeTool != null) {
            return smartCrewProperties.getTools().isEnabled(toolCode) && codeTool.enabledByDefault();
        }
        return true;
    }

    /**
     * 比较配置的 Bean 名称与运行时 Bean 名称是否指向同一实例。
     *
     * @param configuredBeanName 数据库配置的 Bean 名称
     * @param runtimeBeanName    代码层注册的 Bean 名称
     * @return 是否为同一 Bean 实例
     */
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

    /**
     * 解析方法参数名称，优先使用 {@link P} 注解值，否则使用反射参数名。
     *
     * @param parameter 方法参数
     * @return 参数名称
     */
    private String resolveParameterName(Parameter parameter) {
        P pAnnotation = parameter.getAnnotation(P.class);
        if (pAnnotation != null && StringUtils.isNotBlank(pAnnotation.value())) {
            return pAnnotation.value().trim();
        }
        return parameter.getName();
    }

    /**
     * 根据参数类型映射为工具参数类型常量。
     *
     * @param parameter 方法参数
     * @return 工具参数类型字符串
     * @see ToolParameterTypes
     */
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

    /**
     * 代码层工具描述符，封装从 {@link SmartCrewTool} Bean 扫描得到的工具信息。
     *
     * @param toolCode        工具编码
     * @param toolName        工具名称
     * @param description     工具描述
     * @param beanName        Spring Bean 名称
     * @param riskLevel       风险等级
     * @param enabledByDefault 是否默认启用
     * @param actions         动作元数据列表
     */
    private record CodeToolDescriptor(String toolCode,
                                      String toolName,
                                      String description,
                                      String beanName,
                                      String riskLevel,
                                      boolean enabledByDefault,
                                      List<ToolActionMetadata> actions) {
    }
}
