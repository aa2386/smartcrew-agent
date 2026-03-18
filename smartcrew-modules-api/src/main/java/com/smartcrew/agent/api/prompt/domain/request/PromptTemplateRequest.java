package com.smartcrew.agent.api.prompt.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * PromptTemplateRequest 请求对象，封装接口调用所需的入参数据。
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
