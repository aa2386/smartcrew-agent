package com.smartcrew.agent.core.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionModes;
import com.smartcrew.agent.api.tool.domain.model.ToolFlowDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolFlowStep;
import com.smartcrew.agent.api.tool.domain.request.ToolDefinitionRequest;
import com.smartcrew.agent.api.tool.domain.vo.ToolDefinitionVo;
import com.smartcrew.agent.api.tool.mapper.ToolDefinitionMapper;
import com.smartcrew.agent.api.tool.service.ToolDefinitionService;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.JsonUtils;
import com.smartcrew.agent.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Tool 定义服务实现，负责数据库配置的保存与校验。
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

    /**
     * 保存或更新工具定义。
     *
     * @param request 工具定义请求
     * @return 保存后的实体
     */
    @Override
    public ToolDefinition saveOrUpdate(ToolDefinitionRequest request) {
        String toolCode = requireText(request.getToolCode(), "Tool 编码不能为空");
        String executionMode = normalizeExecutionMode(request.getExecutionMode());
        ToolDefinition entity = toolDefinitionMapper.selectByToolCode(toolCode);
        if (entity == null) {
            entity = new ToolDefinition();
            entity.setToolCode(toolCode);
        }

        ResolvedToolDefinition runtimeDefinition = toolRegistry.getByCode(toolCode).orElse(null);
        entity.setToolName(requireText(request.getToolName(), "Tool 名称不能为空"));
        entity.setDescription(requireText(request.getDescription(), "Tool 描述不能为空"));
        entity.setExecutionMode(executionMode);
        entity.setRiskLevel(defaultIfBlank(request.getRiskLevel(), "MEDIUM"));
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        entity.setConfigJson(trimToNull(request.getConfigJson()));

        if (ToolExecutionModes.BEAN.equals(executionMode)) {
            String beanName = firstNonBlank(
                    trimToNull(request.getBeanName()),
                    entity.getBeanName(),
                    runtimeDefinition == null ? null : runtimeDefinition.getBeanName()
            );
            if (StringUtils.isBlank(beanName)) {
                throw new ServiceException(400, "BEAN 模式必须指定 beanName");
            }
            entity.setBeanName(beanName);
            entity.setFlowDefinitionJson(trimToNull(request.getFlowDefinitionJson()));
        } else {
            String flowDefinitionJson = trimToNull(request.getFlowDefinitionJson());
            if (StringUtils.isBlank(flowDefinitionJson)) {
                throw new ServiceException(400, "FLOW 模式必须提供 flowDefinitionJson");
            }
            validateFlowDefinition(flowDefinitionJson);
            entity.setBeanName(trimToNull(request.getBeanName()));
            entity.setFlowDefinitionJson(flowDefinitionJson);
        }

        if (entity.getId() == null) {
            toolDefinitionMapper.insert(entity);
        } else {
            toolDefinitionMapper.updateById(entity);
        }
        return entity;
    }

    /**
     * 获取所有工具定义视图。
     *
     * @return 工具定义视图列表
     */
    @Override
    public List<ToolDefinitionVo> listAll() {
        return toolDefinitionMapper.selectList(Wrappers.emptyWrapper()).stream()
                .map(this::toVo)
                .toList();
    }

    /**
     * 根据编码查询工具定义。
     *
     * @param toolCode 工具编码
     * @return 工具定义（可选）
     */
    @Override
    public Optional<ToolDefinition> findByToolCode(String toolCode) {
        return Optional.ofNullable(toolDefinitionMapper.selectByToolCode(toolCode));
    }

    /**
     * 更新工具启用状态。
     *
     * @param toolCode 工具编码
     * @param enabled  是否启用
     */
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
            entity.setExecutionMode(runtimeDefinition == null ? ToolExecutionModes.BEAN : runtimeDefinition.getExecutionMode());
            entity.setRiskLevel(runtimeDefinition == null ? "MEDIUM" : runtimeDefinition.getRiskLevel());
            entity.setEnabled(enabled);
            entity.setConfigJson(runtimeDefinition == null ? null : runtimeDefinition.getConfigJson());
            entity.setFlowDefinitionJson(runtimeDefinition == null ? null : runtimeDefinition.getFlowDefinitionJson());
            toolDefinitionMapper.insert(entity);
            return;
        }
        entity.setEnabled(enabled);
        toolDefinitionMapper.updateById(entity);
    }

    /* 转换为视图对象。 */
    private ToolDefinitionVo toVo(ToolDefinition definition) {
        return ToolDefinitionVo.builder()
                .id(definition.getId())
                .toolCode(definition.getToolCode())
                .toolName(definition.getToolName())
                .description(definition.getDescription())
                .beanName(definition.getBeanName())
                .executionMode(definition.getExecutionMode())
                .riskLevel(definition.getRiskLevel())
                .enabled(definition.getEnabled())
                .configJson(definition.getConfigJson())
                .flowDefinitionJson(definition.getFlowDefinitionJson())
                .build();
    }

    /* 校验 FLOW 模式的步骤定义。 */
    private void validateFlowDefinition(String flowDefinitionJson) {
        ToolFlowDefinition definition = JsonUtils.parse(flowDefinitionJson, ToolFlowDefinition.class);
        if (definition.getSteps() == null || definition.getSteps().isEmpty()) {
            throw new ServiceException(400, "FLOW 模式至少需要一个步骤");
        }
        boolean hasReturnStep = false;
        for (ToolFlowStep step : definition.getSteps()) {
            if (step == null || StringUtils.isBlank(step.getType())) {
                throw new ServiceException(400, "FLOW 步骤必须指定 type");
            }
            String type = step.getType().trim().toLowerCase(Locale.ROOT);
            switch (type) {
                case "template" -> {
                    if (StringUtils.isBlank(step.getOutput())) {
                        throw new ServiceException(400, "template 步骤必须指定 output");
                    }
                }
                case "tool_call" -> {
                    if (StringUtils.isBlank(step.getToolCode())) {
                        throw new ServiceException(400, "tool_call 步骤必须指定 toolCode");
                    }
                }
                case "return" -> hasReturnStep = true;
                default -> throw new ServiceException(400, "暂不支持的 FLOW 步骤类型: " + step.getType());
            }
        }
        if (!hasReturnStep) {
            throw new ServiceException(400, "FLOW 定义必须包含 return 步骤");
        }
    }

    /* 校验文本非空。 */
    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (StringUtils.isBlank(normalized)) {
            throw new ServiceException(400, message);
        }
        return normalized;
    }

    /* 规范化执行模式。 */
    private String normalizeExecutionMode(String executionMode) {
        String normalized = defaultIfBlank(executionMode, ToolExecutionModes.BEAN)
                .trim()
                .toUpperCase(Locale.ROOT);
        if (!ToolExecutionModes.BEAN.equals(normalized) && !ToolExecutionModes.FLOW.equals(normalized)) {
            throw new ServiceException(400, "executionMode 仅支持 BEAN 或 FLOW");
        }
        return normalized;
    }

    /* 返回默认值（如果原值为空）。 */
    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    /* 去除首尾空白，空字符串返回 null。 */
    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
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
}
