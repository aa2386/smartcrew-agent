package com.smartcrew.agent.controller.tool;

import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolMetadata;
import com.smartcrew.agent.api.tool.domain.request.ToolDefinitionRequest;
import com.smartcrew.agent.api.tool.domain.vo.ToolDefinitionVo;
import com.smartcrew.agent.api.tool.domain.vo.ToolToggleResponse;
import com.smartcrew.agent.api.tool.service.ToolDefinitionService;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.domain.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工具管理控制器，提供工具定义维护和启停控制接口。
 */
@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {

    /**
     * 工具注册表。
     */
    private final ToolRegistry toolRegistry;
    /**
     * 工具定义服务。
     */
    private final ToolDefinitionService toolDefinitionService;

    /**
     * 构造 ToolController 所需的依赖对象。
     */
    public ToolController(ToolRegistry toolRegistry, ToolDefinitionService toolDefinitionService) {
        this.toolRegistry = toolRegistry;
        this.toolDefinitionService = toolDefinitionService;
    }

    /**
     * 查询列表数据。
     */
    @GetMapping
    public R<List<ToolMetadata>> list() {
        return R.ok(toolRegistry.listAll());
    }

    /**
     * 保存请求数据。
     */
    @PostMapping
    public R<ToolDefinitionVo> save(@Valid @RequestBody ToolDefinitionRequest request) {
        ToolDefinition definition = toolDefinitionService.saveOrUpdate(request);
        toolRegistry.refresh();
        return R.ok(ToolDefinitionVo.builder()
                .id(definition.getId())
                .toolCode(definition.getToolCode())
                .toolName(definition.getToolName())
                .description(definition.getDescription())
                .beanName(definition.getBeanName())
                .riskLevel(definition.getRiskLevel())
                .enabled(definition.getEnabled())
                .configJson(definition.getConfigJson())
                .build());
    }

    /**
     * 启用指定对象。
     */
    @PostMapping("/{toolCode}/enable")
    public R<ToolToggleResponse> enable(@PathVariable("toolCode") String toolCode) {
        toolDefinitionService.updateEnabledStatus(toolCode, true);
        toolRegistry.setEnabled(toolCode, true);
        return R.ok(ToolToggleResponse.builder()
                .toolCode(toolCode)
                .enabled(true)
                .message("tool enabled")
                .build());
    }

    /**
     * 禁用指定对象。
     */
    @PostMapping("/{toolCode}/disable")
    public R<ToolToggleResponse> disable(@PathVariable("toolCode") String toolCode) {
        toolDefinitionService.updateEnabledStatus(toolCode, false);
        toolRegistry.setEnabled(toolCode, false);
        return R.ok(ToolToggleResponse.builder()
                .toolCode(toolCode)
                .enabled(false)
                .message("tool disabled")
                .build());
    }
}
