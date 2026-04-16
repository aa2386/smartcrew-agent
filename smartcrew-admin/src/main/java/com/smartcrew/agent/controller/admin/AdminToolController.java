package com.smartcrew.agent.controller.admin;

import com.smartcrew.agent.api.tool.domain.entity.ToolDefinition;
import com.smartcrew.agent.api.tool.domain.request.ToolDefinitionRequest;
import com.smartcrew.agent.api.tool.domain.request.ToolExecutionRequest;
import com.smartcrew.agent.api.tool.domain.model.ToolExecutionResult;
import com.smartcrew.agent.api.tool.domain.vo.ToolDefinitionVo;
import com.smartcrew.agent.api.tool.service.ToolDefinitionService;
import com.smartcrew.agent.api.tool.service.ToolExecutor;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.core.page.TableDataInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台 Tool 管理控制器。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tools")
@ConditionalOnProperty(prefix = "smartcrew.api.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminToolController {

    private final ToolRegistry toolRegistry;
    private final ToolDefinitionService toolDefinitionService;
    private final ToolExecutor toolExecutor;

    /**
     * 获取所有工具列表。
     *
     * @return 工具定义列表
     */
    @GetMapping
    public TableDataInfo<ToolDefinitionVo> list() {
        return TableDataInfo.build(toolRegistry.listAll().stream()
                .map(item -> item.toVo())
                .toList());
    }

    /**
     * 获取指定工具详情。
     *
     * @param code 工具编码
     * @return 工具定义详情
     */
    @GetMapping("/{code}")
    public R<ToolDefinitionVo> detail(@PathVariable("code") String code) {
        ToolDefinitionVo vo = toolRegistry.getByCode(code)
                .map(item -> item.toVo())
                .orElseThrow(() -> new ServiceException(404, "Tool 不存在"));
        return R.ok(vo);
    }

    /**
     * 创建工具定义。
     *
     * @param request 工具定义请求
     * @return 创建后的工具定义
     */
    @PostMapping
    public R<ToolDefinitionVo> create(@Valid @RequestBody ToolDefinitionRequest request) {
        if (toolDefinitionService.findByToolCode(request.getToolCode()).isPresent()) {
            throw new ServiceException(400, "Tool 数据库配置已存在");
        }
        ToolDefinition definition = toolDefinitionService.saveOrUpdate(request);
        toolRegistry.refresh();
        return R.ok(loadView(definition.getToolCode()));
    }

    /**
     * 更新工具定义。
     *
     * @param code    工具编码
     * @param request 工具定义请求
     * @return 更新后的工具定义
     */
    @PutMapping("/{code}")
    public R<ToolDefinitionVo> update(@PathVariable("code") String code,
                                      @Valid @RequestBody ToolDefinitionRequest request) {
        boolean hasDatabaseConfig = toolDefinitionService.findByToolCode(code).isPresent();
        boolean hasRuntimeTool = toolRegistry.getByCode(code).isPresent();
        if (!hasDatabaseConfig && !hasRuntimeTool) {
            throw new ServiceException(404, "Tool 不存在");
        }
        request.setToolCode(code);
        ToolDefinition definition = toolDefinitionService.saveOrUpdate(request);
        toolRegistry.refresh();
        return R.ok(loadView(definition.getToolCode()));
    }

    /**
     * 执行工具动作。
     *
     * @param code    工具编码
     * @param request 工具执行请求
     * @return 执行结果
     */
    @PostMapping("/{code}/execute")
    public R<ToolExecutionResult> execute(@PathVariable("code") String code,
                                          @RequestBody(required = false) ToolExecutionRequest request) {
        ToolExecutionRequest safeRequest = request == null ? new ToolExecutionRequest() : request;
        ToolExecutionResult result = toolExecutor.execute(
                code,
                safeRequest.getActionName(),
                safeRequest.getArguments(),
                safeRequest.getExecutionContext()
        );
        return R.ok(result);
    }

    /* 根据工具编码加载视图对象。 */
    private ToolDefinitionVo loadView(String toolCode) {
        return toolRegistry.getByCode(toolCode)
                .map(item -> item.toVo())
                .orElseThrow(() -> new ServiceException(500, "Tool 保存后查询失败"));
    }
}
