package com.smartcrew.agent.core.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.request.ToolDefinitionRequest;
import com.smartcrew.agent.api.tool.domain.vo.ToolDefinitionVo;
import com.smartcrew.agent.api.tool.mapper.ToolDefinitionMapper;
import com.smartcrew.agent.api.tool.service.ToolDefinitionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 工具定义服务实现，负责工具配置的保存、查询和启停状态维护。
 */
@Service
public class ToolDefinitionServiceImpl implements ToolDefinitionService {

    /**
     * 工具定义数据访问对象。
     */
    private final ToolDefinitionMapper toolDefinitionMapper;

    /**
     * 构造 ToolDefinitionServiceImpl 所需的依赖对象。
     */
    public ToolDefinitionServiceImpl(ToolDefinitionMapper toolDefinitionMapper) {
        this.toolDefinitionMapper = toolDefinitionMapper;
    }

    /**
     * 保存或更新数据。
     */
    @Override
    public ToolDefinition saveOrUpdate(ToolDefinitionRequest request) {
        ToolDefinition entity = toolDefinitionMapper.selectByToolCode(request.getToolCode());
        if (entity == null) {
            entity = new ToolDefinition();
        }
        BeanUtils.copyProperties(request, entity);
        if (entity.getId() == null) {
            toolDefinitionMapper.insert(entity);
        } else {
            toolDefinitionMapper.updateById(entity);
        }
        return entity;
    }

    /**
     * 查询全部数据。
     */
    @Override
    public List<ToolDefinitionVo> listAll() {
        return toolDefinitionMapper.selectList(Wrappers.emptyWrapper()).stream()
                .map(this::toVo)
                .toList();
    }

    /**
     * 根据工具编码查询数据。
     */
    @Override
    public Optional<ToolDefinition> findByToolCode(String toolCode) {
        return Optional.ofNullable(toolDefinitionMapper.selectByToolCode(toolCode));
    }

    /**
     * 更新启用状态。
     */
    @Override
    public void updateEnabledStatus(String toolCode, boolean enabled) {
        ToolDefinition entity = toolDefinitionMapper.selectByToolCode(toolCode);
        if (entity == null) {
            entity = new ToolDefinition();
            entity.setToolCode(toolCode);
            entity.setToolName(toolCode);
            entity.setDescription("Generated during toggle operation");
            entity.setBeanName(toolCode);
            entity.setRiskLevel("MEDIUM");
            entity.setEnabled(enabled);
            toolDefinitionMapper.insert(entity);
            return;
        }
        entity.setEnabled(enabled);
        toolDefinitionMapper.updateById(entity);
    }

    /**
     * 将工具定义实体转换为视图对象。
     */
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
}
