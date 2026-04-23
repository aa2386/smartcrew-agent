package com.smartcrew.agent.core.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.request.ToolDefinitionRequest;
import com.smartcrew.agent.api.tool.domain.vo.ToolDefinitionVo;
import com.smartcrew.agent.api.tool.mapper.ToolDefinitionMapper;
import com.smartcrew.agent.api.tool.service.ToolDefinitionService;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Tool 元数据服务实现。
 */
@Service
public class ToolDefinitionServiceImpl implements ToolDefinitionService {

    private final ToolDefinitionMapper toolDefinitionMapper;
    private final ToolRegistry toolRegistry;

    public ToolDefinitionServiceImpl(ToolDefinitionMapper toolDefinitionMapper,
                                     ToolRegistry toolRegistry) {
        this.toolDefinitionMapper = toolDefinitionMapper;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public ToolDefinition saveOrUpdate(ToolDefinitionRequest request) {
        String toolCode = requireText(request.getToolCode(), "Tool 编码不能为空");
        ToolDefinition entity = toolDefinitionMapper.selectByToolCode(toolCode);
        if (entity == null) {
            entity = new ToolDefinition();
            entity.setToolCode(toolCode);
        }

        ResolvedToolDefinition runtimeDefinition = toolRegistry.getByCode(toolCode).orElse(null);
        entity.setToolName(requireText(request.getToolName(), "Tool 名称不能为空"));
        entity.setDescription(requireText(request.getDescription(), "Tool 描述不能为空"));
        entity.setRiskLevel(defaultIfBlank(request.getRiskLevel(), "MEDIUM"));
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        entity.setConfigJson(trimToNull(request.getConfigJson()));

        String beanName = firstNonBlank(
                trimToNull(request.getBeanName()),
                entity.getBeanName(),
                runtimeDefinition == null ? null : runtimeDefinition.getBeanName()
        );
        if (StringUtils.isBlank(beanName)) {
            throw new ServiceException(400, "必须指定 Spring Bean 名称");
        }
        entity.setBeanName(beanName);

        if (entity.getId() == null) {
            toolDefinitionMapper.insert(entity);
        } else {
            toolDefinitionMapper.updateById(entity);
        }
        return entity;
    }

    @Override
    public List<ToolDefinitionVo> listAll() {
        return toolDefinitionMapper.selectList(Wrappers.emptyWrapper()).stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public Optional<ToolDefinition> findByToolCode(String toolCode) {
        return Optional.ofNullable(toolDefinitionMapper.selectByToolCode(toolCode));
    }

    @Override
    public void updateEnabledStatus(String toolCode, boolean enabled) {
        ToolDefinition entity = toolDefinitionMapper.selectByToolCode(toolCode);
        ResolvedToolDefinition runtimeDefinition = toolRegistry.getByCode(toolCode).orElse(null);
        if (entity == null) {
            entity = new ToolDefinition();
            entity.setToolCode(toolCode);
            entity.setToolName(runtimeDefinition == null ? toolCode : runtimeDefinition.getToolName());
            entity.setDescription(runtimeDefinition == null ? "Generated during toggle operation" : runtimeDefinition.getDescription());
            entity.setBeanName(runtimeDefinition == null ? toolCode : runtimeDefinition.getBeanName());
            entity.setRiskLevel(runtimeDefinition == null ? "MEDIUM" : runtimeDefinition.getRiskLevel());
            entity.setEnabled(enabled);
            entity.setConfigJson(runtimeDefinition == null ? null : runtimeDefinition.getConfigJson());
            toolDefinitionMapper.insert(entity);
            return;
        }
        entity.setEnabled(enabled);
        toolDefinitionMapper.updateById(entity);
    }

    private ToolDefinitionVo toVo(ToolDefinition definition) {
        return ToolDefinitionVo.builder()
                .id(definition.getId())
                .toolCode(definition.getToolCode())
                .toolName(definition.getToolName())
                .description(definition.getDescription())
                .beanName(definition.getBeanName())
                .riskLevel(definition.getRiskLevel())
                .enabled(definition.getEnabled())
                .configJson(definition.getConfigJson())
                .build();
    }

    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (StringUtils.isBlank(normalized)) {
            throw new ServiceException(400, message);
        }
        return normalized;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
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
