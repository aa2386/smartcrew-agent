package com.smartcrew.agent.controller.tool;

import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolMetadata;
import com.smartcrew.agent.api.tool.domain.request.ToolDefinitionRequest;
import com.smartcrew.agent.api.tool.domain.vo.ToolDefinitionVo;
import com.smartcrew.agent.api.tool.domain.vo.ToolToggleResponse;
import com.smartcrew.agent.api.tool.service.ToolDefinitionService;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.common.exception.ServiceException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 兼容旧版 `/api/v1/tools` 的 Tool 管理控制器。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {

    private final ToolRegistry toolRegistry;
    private final ToolDefinitionService toolDefinitionService;

    /**
     * 获取工具元数据列表（兼容旧版接口）。
     *
     * @return 工具元数据列表
     */
    @GetMapping
    public R<List<ToolMetadata>> list() {
        return R.ok(toolRegistry.listLegacyMetadata());
    }

    /**
     * 保存工具定义（创建或更新）。
     *
     * @param request 工具定义请求
     * @return 保存后的工具定义
     */
    @PostMapping
    public R<ToolDefinitionVo> save(@Valid @RequestBody ToolDefinitionRequest request) {
        ToolDefinition definition = toolDefinitionService.saveOrUpdate(request);
        toolRegistry.refresh();
        ToolDefinitionVo vo = toolRegistry.getByCode(definition.getToolCode())
                .map(item -> item.toVo())
                .orElseThrow(() -> new ServiceException(500, "Tool 保存后查询失败"));
        return R.ok(vo);
    }

    /**
     * 启用指定工具。
     *
     * @param toolCode 工具编码
     * @return 启用结果
     */
    @PostMapping("/{toolCode}/enable")
    public R<ToolToggleResponse> enable(@PathVariable("toolCode") String toolCode) {
        toolDefinitionService.updateEnabledStatus(toolCode, true);
        toolRegistry.refresh();
        return R.ok(ToolToggleResponse.builder()
                .toolCode(toolCode)
                .enabled(true)
                .message("tool enabled")
                .build());
    }

    /**
     * 禁用指定工具。
     *
     * @param toolCode 工具编码
     * @return 禁用结果
     */
    @PostMapping("/{toolCode}/disable")
    public R<ToolToggleResponse> disable(@PathVariable("toolCode") String toolCode) {
        toolDefinitionService.updateEnabledStatus(toolCode, false);
        toolRegistry.refresh();
        return R.ok(ToolToggleResponse.builder()
                .toolCode(toolCode)
                .enabled(false)
                .message("tool disabled")
                .build());
    }
}
