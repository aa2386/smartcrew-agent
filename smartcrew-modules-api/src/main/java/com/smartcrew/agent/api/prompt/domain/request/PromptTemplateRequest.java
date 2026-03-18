package com.smartcrew.agent.api.prompt.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ????????????
 */
@Data
public class PromptTemplateRequest {

    /**
     * 模板名称。
     */
    @NotBlank
    private String templateName;

    /**
     * 模板内容。
     */
    @NotBlank
    private String templateContent;

    /**
     * 分类标识。
     */
    @NotBlank
    private String category;

    /**
     * 备注信息。
     */
    private String remark;
}
