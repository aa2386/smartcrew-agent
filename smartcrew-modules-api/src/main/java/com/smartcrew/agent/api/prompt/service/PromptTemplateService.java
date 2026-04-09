package com.smartcrew.agent.api.prompt.service;

import com.smartcrew.agent.api.prompt.domain.request.PromptTemplateRequest;
import com.smartcrew.agent.api.prompt.domain.vo.PromptTemplateVo;

import java.util.List;
import java.util.Optional;

/**
 * PromptTemplateService 接口，定义该领域的业务能力与操作约定。
 */
public interface PromptTemplateService {

    /**
     * 创建提示词模板。
     *
     * @param request 请求参数。
     * @return 创建后的模板信息。
     */
    PromptTemplateVo create(PromptTemplateRequest request);

    /**
     * 按主键更新提示词模板。
     *
     * @param id      模板主键
     * @param request 更新参数
     * @return 更新后的模板信息
     */
    PromptTemplateVo update(Long id, PromptTemplateRequest request);

    /**
     * 按主键删除提示词模板。
     *
     * @param id 模板主键
     */
    void deleteById(Long id);

    /**
     * 查询并返回全部记录。
     *
     * @return 结果列表。
     */
    List<PromptTemplateVo> listAll();

    /**
     * 按分类查询提示词模板。
     *
     * @param category 模板分类。
     * @return 匹配结果；未找到时返回空 `Optional`。
     */
    Optional<PromptTemplateVo> queryByCategory(String category);
}
