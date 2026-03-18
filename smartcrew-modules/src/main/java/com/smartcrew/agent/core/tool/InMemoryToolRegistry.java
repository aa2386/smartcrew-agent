package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolMetadata;
import com.smartcrew.agent.api.tool.mapper.ToolDefinitionMapper;
import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.beans.Introspector;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的工具注册表实现，负责在应用启动时收集工具元数据。
 */
@Component
public class InMemoryToolRegistry implements ToolRegistry {

    /**
     * 已发现的工具 Bean 列表。
     */
    private final List<SmartCrewTool> toolBeans;
    /**
     * 工具定义数据访问对象。
     */
    private final ToolDefinitionMapper toolDefinitionMapper;
    /**
     * SmartCrew 配置属性。
     */
    private final SmartCrewProperties smartCrewProperties;
    /**
     * 工具元数据缓存。
     */
    private final ConcurrentHashMap<String, ToolMetadata> metadataMap = new ConcurrentHashMap<>();

    /**
     * 构造 InMemoryToolRegistry 所需的依赖对象。
     */
    public InMemoryToolRegistry(List<SmartCrewTool> toolBeans,
                                ToolDefinitionMapper toolDefinitionMapper,
                                SmartCrewProperties smartCrewProperties) {
        this.toolBeans = toolBeans;
        this.toolDefinitionMapper = toolDefinitionMapper;
        this.smartCrewProperties = smartCrewProperties;
    }

    /**
     * 刷新运行期缓存数据。
     */
    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void refresh() {
        metadataMap.clear();
        toolBeans.forEach(tool -> {
            ToolDefinition definition = toolDefinitionMapper.selectByToolCode(tool.toolCode());
            boolean enabled = definition != null && definition.getEnabled() != null
                    ? definition.getEnabled()
                    : smartCrewProperties.getTools().isEnabled(tool.toolCode()) && tool.enabledByDefault();
            ToolMetadata metadata = ToolMetadata.builder()
                    .toolCode(tool.toolCode())
                    .toolName(tool.toolName())
                    .description(tool.description())
                    .beanName(Introspector.decapitalize(tool.getClass().getSimpleName()))
                    .riskLevel(tool.riskLevel())
                    .enabled(enabled)
                    .build();
            ToolMetadata previous = metadataMap.putIfAbsent(tool.toolCode(), metadata);
            if (previous != null) {
                throw new ServiceException("Duplicate tool code found: " + tool.toolCode());
            }
        });
    }

    /**
     * 查询全部数据。
     */
    @Override
    public List<ToolMetadata> listAll() {
        return metadataMap.values().stream()
                .sorted(Comparator.comparing(ToolMetadata::getToolCode))
                .toList();
    }

    /**
     * 根据编码获取对象。
     */
    @Override
    public Optional<ToolMetadata> getByCode(String toolCode) {
        return Optional.ofNullable(metadataMap.get(toolCode));
    }

    /**
     * 设置启用状态。
     */
    @Override
    public void setEnabled(String toolCode, boolean enabled) {
        ToolMetadata metadata = metadataMap.get(toolCode);
        if (metadata == null) {
            throw new ServiceException("Unknown tool: " + toolCode);
        }
        metadata.setEnabled(enabled);
    }
}
