package com.smartcrew.agent.controller.prompt;

import com.smartcrew.agent.api.prompt.domain.request.PromptTemplateRequest;
import com.smartcrew.agent.api.prompt.domain.vo.PromptTemplateVo;
import com.smartcrew.agent.api.prompt.service.PromptTemplateService;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.core.page.TableDataInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提示词模板控制器，提供模板创建、查询和按分类获取接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/prompts")
public class PromptController {

    /**
     * 提示词模板服务。
     */
    private final PromptTemplateService promptTemplateService;

    /**
     * 查询列表数据。
     */
    @GetMapping
    public TableDataInfo<PromptTemplateVo> list() {
        return TableDataInfo.build(promptTemplateService.listAll());
    }

    /**
     * 创建目标资源。
     */
    @PostMapping
    public R<PromptTemplateVo> create(@Valid @RequestBody PromptTemplateRequest request) {
        return R.ok(promptTemplateService.create(request));
    }

    /**
     * 按分类查询数据。
     */
    @GetMapping("/category/{category}")
    public R<PromptTemplateVo> queryByCategory(@PathVariable("category") String category) {
        return R.ok(promptTemplateService.queryByCategory(category)
                .orElseThrow(() -> new ServiceException("Unknown prompt category: " + category)));
    }
}
