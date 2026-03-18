package com.smartcrew.agent.api.prompt.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * ??????????
 */
@Data
@Builder
public class PromptTemplateVo {

    /**
     * 主键 ID。
     */
    private Long id;
    /**
     * 模板名称。
     */
    private String templateName;
    /**
     * 模板内容。
     */
    private String templateContent;
    /**
     * 分类标识。
     */
    private String category;
    /**
     * 备注信息。
     */
    private String remark;
}
