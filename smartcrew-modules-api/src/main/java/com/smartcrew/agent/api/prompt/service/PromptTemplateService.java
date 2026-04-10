package com.smartcrew.agent.api.prompt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartcrew.agent.api.prompt.domain.request.PromptTemplateRequest;
import com.smartcrew.agent.api.prompt.domain.vo.PromptTemplateVo;
import com.smartcrew.agent.core.page.PageQuery;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 模板服务接口，定义模板维护与查询能力。
 */
public interface PromptTemplateService {

    /**
     * 创建提示词模板。
     */
    PromptTemplateVo create(PromptTemplateRequest request);

    /**
     * 按主键更新提示词模板。
     */
    PromptTemplateVo update(Long id, PromptTemplateRequest request);

    /**
     * 按主键删除提示词模板。
     */
    void deleteById(Long id);

    /**
     * 查询并返回全部记录。
     */
    List<PromptTemplateVo> listAll();

    /**
     * 分页查询按分类聚合后的最新 Prompt。
     */
    IPage<PromptTemplateVo> listLatestCategories(PageQuery pageQuery);

    /**
     * 按分类查询提示词模板。
     */
    Optional<PromptTemplateVo> queryByCategory(String category);
}
