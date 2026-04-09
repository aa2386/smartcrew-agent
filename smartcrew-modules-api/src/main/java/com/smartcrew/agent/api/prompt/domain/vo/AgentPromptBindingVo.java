package com.smartcrew.agent.api.prompt.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * Agent Prompt 绑定视图对象。
 */
@Data
@Builder
public class AgentPromptBindingVo {

    /**
     * 绑定记录主键 ID。
     */
    private Long id;

    /**
     * Agent 编码。
     */
    private String agentCode;

    /**
     * Prompt 模板主键 ID。
     */
    private Long promptTemplateId;

    /**
     * 模板名称。
     */
    private String templateName;

    /**
     * 模板分类。
     */
    private String category;

    /**
     * 模板内容。
     */
    private String templateContent;

    /**
     * 绑定顺序。
     */
    private Integer sortOrder;
}
