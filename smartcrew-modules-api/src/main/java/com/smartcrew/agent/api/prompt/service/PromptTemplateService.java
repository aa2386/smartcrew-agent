package com.smartcrew.agent.api.prompt.service;

import com.smartcrew.agent.api.prompt.domain.request.PromptTemplateRequest;
import com.smartcrew.agent.api.prompt.domain.vo.PromptTemplateVo;

import java.util.List;
import java.util.Optional;

/**
 * ??????????
 */
public interface PromptTemplateService {

    PromptTemplateVo create(PromptTemplateRequest request);

    List<PromptTemplateVo> listAll();

    Optional<PromptTemplateVo> queryByCategory(String category);
}
