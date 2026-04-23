package com.smartcrew.agent.core.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionModes;
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
 * 工具定义服务实现，提供工具的创建/更新、查询及启用状态管理。
 *
 * <p>在数据库层维护工具的治理元数据（名称、描述、风险级别等），
 * 并与运行时注册中心 {@link ToolRegistry} 协同完成 Bean 名称解析与状态同步。</p>
 *
 * @see ToolDefinitionService
 * @see ToolRegistry
 * @see ToolDefinitionMapper
 */
@Service
public class ToolDefinitionServiceImpl implements ToolDefinitionService {

    private final ToolDefinitionMapper toolDefinitionMapper;
    private final ToolRegistry toolRegistry;

    /**
     * 构造工具定义服务实例。
     *
     * @param toolDefinitionMapper 工具定义数据库 Mapper
     * @param toolRegistry         工具注册中心，用于获取运行时工具信息
     */
    public ToolDefinitionServiceImpl(ToolDefinitionMapper toolDefinitionMapper,
                                     ToolRegistry toolRegistry) {
        this.toolDefinitionMapper = toolDefinitionMapper;
        this.toolRegistry = toolRegistry;
    }

    /**
     * 创建或更新工具定义，自动解析 Bean 名称并持久化到数据库。
     *
     * <p>若工具编码已存在则更新，否则新建。Bean 名称按优先级从请求参数、
     * 已有记录、运行时注册中心依次解析。</p>
     *
     * @param request 工具定义请求对象
     * @return 持久化后的工具定义实体
     * @throws ServiceException 当必填字段为空或 BEAN 模式缺少 beanName 时抛出
     */
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
        entity.setExecutionMode(ToolExecutionModes.BEAN);
        entity.setRiskLevel(defaultIfBlank(request.getRiskLevel(), "MEDIUM"));
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        entity.setConfigJson(trimToNull(request.getConfigJson()));

        String beanName = firstNonBlank(
                trimToNull(request.getBeanName()),
                entity.getBeanName(),
                runtimeDefinition == null ? null : runtimeDefinition.getBeanName()
        );
        if (StringUtils.isBlank(beanName)) {
            throw new ServiceException(400, "BEAN 模式必须指定 beanName");
        }
        entity.setBeanName(beanName);

        if (entity.getId() == null) {
            toolDefinitionMapper.insert(entity);
        } else {
            toolDefinitionMapper.updateById(entity);
        }
        return entity;
    }

    /**
     * 查询所有工具定义，将数据库实体转换为视图对象列表。
     *
     * @return 工具定义视图对象列表
     */
    @Override
    public List<ToolDefinitionVo> listAll() {
        return toolDefinitionMapper.selectList(Wrappers.emptyWrapper()).stream()
                .map(this::toVo)
                .toList();
    }

    /**
     * 根据工具编码查询工具定义实体。
     *
     * @param toolCode 工具编码
     * @return 工具定义实体（可能为空）
     */
    @Override
    public Optional<ToolDefinition> findByToolCode(String toolCode) {
        return Optional.ofNullable(toolDefinitionMapper.selectByToolCode(toolCode));
    }

    /**
     * 更新工具启用状态，若数据库中不存在则自动创建记录。
     *
     * <p>当数据库中无对应记录时，从运行时注册中心获取工具信息并创建新记录；
     * 已有记录则直接更新启用状态。</p>
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
            entity.setExecutionMode(ToolExecutionModes.BEAN);
            entity.setRiskLevel(runtimeDefinition == null ? "MEDIUM" : runtimeDefinition.getRiskLevel());
            entity.setEnabled(enabled);
            entity.setConfigJson(runtimeDefinition == null ? null : runtimeDefinition.getConfigJson());
            toolDefinitionMapper.insert(entity);
            return;
        }
        entity.setEnabled(enabled);
        entity.setExecutionMode(ToolExecutionModes.BEAN);
        toolDefinitionMapper.updateById(entity);
    }

    /**
     * 将数据库实体转换为视图对象。
     *
     * @param definition 工具定义实体
     * @return 工具定义视图对象
     */
    private ToolDefinitionVo toVo(ToolDefinition definition) {
        return ToolDefinitionVo.builder()
                .id(definition.getId())
                .toolCode(definition.getToolCode())
                .toolName(definition.getToolName())
                .description(definition.getDescription())
                .beanName(definition.getBeanName())
                .executionMode(ToolExecutionModes.BEAN)
                .riskLevel(definition.getRiskLevel())
                .enabled(definition.getEnabled())
                .configJson(definition.getConfigJson())
                .build();
    }

    /**
     * 校验文本非空，为空时抛出业务异常。
     *
     * @param value   待校验文本
     * @param message 异常提示信息
     * @return 去除首尾空白后的文本
     * @throws ServiceException 当文本为空时抛出
     */
    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (StringUtils.isBlank(normalized)) {
            throw new ServiceException(400, message);
        }
        return normalized;
    }

    /**
     * 返回非空值或默认值。
     *
     * @param value        待检查值
     * @param defaultValue 默认值
     * @return 非空时返回去除空白后的值，否则返回默认值
     */
    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    /**
     * 去除首尾空白，空白字符串转为 null。
     *
     * @param value 待处理字符串
     * @return 去除空白后的字符串或 null
     */
    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    /**
     * 返回第一个非空白字符串值。
     *
     * @param values 候选字符串数组
     * @return 第一个非空白字符串，全部为空时返回 null
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
