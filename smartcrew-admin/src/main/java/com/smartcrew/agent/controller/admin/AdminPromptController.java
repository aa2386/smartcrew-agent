package com.smartcrew.agent.controller.admin;

import com.smartcrew.agent.api.prompt.domain.request.PromptTemplateRequest;
import com.smartcrew.agent.api.prompt.domain.vo.PromptTemplateVo;
import com.smartcrew.agent.api.prompt.service.PromptTemplateService;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.core.page.PageQuery;
import com.smartcrew.agent.core.page.TableDataInfo;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.Map;

/**
 * 后台 Prompt 管理控制器。
 */
@RestController
@RequestMapping("/api/admin/prompts")
@ConditionalOnProperty(prefix = "smartcrew.api.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminPromptController {

    /**
     * Prompt 模板服务。
     */
    private final PromptTemplateService promptTemplateService;

    public AdminPromptController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    @GetMapping
    public TableDataInfo<PromptTemplateVo> list() {
        return TableDataInfo.build(promptTemplateService.listAll());
    }

    @GetMapping("/categories")
    public TableDataInfo<PromptTemplateVo> listCategories(PageQuery pageQuery) {
        if (pageQuery.hasPaging()) {
            return TableDataInfo.build(promptTemplateService.listLatestCategories(pageQuery));
        }
        return TableDataInfo.build(promptTemplateService.listAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        PromptTemplateVo::getCategory,
                        item -> item,
                        (left, right) -> left.getId() >= right.getId() ? left : right,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(PromptTemplateVo::getCategory))
                .toList());
    }

    @GetMapping("/category/{category}")
    public R<PromptTemplateVo> detail(@PathVariable("category") String category) {
        return R.ok(promptTemplateService.queryByCategory(category)
                .orElseThrow(() -> new ServiceException(404, "Prompt 分类不存在")));
    }

    @PostMapping
    public R<PromptTemplateVo> create(@Valid @RequestBody PromptTemplateRequest request) {
        return R.ok(promptTemplateService.create(request));
    }

    @PutMapping("/{id}")
    public R<PromptTemplateVo> update(@PathVariable("id") Long id,
                                      @Valid @RequestBody PromptTemplateRequest request) {
        return R.ok(promptTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable("id") Long id) {
        promptTemplateService.deleteById(id);
        return R.ok("删除成功", null);
    }
}
