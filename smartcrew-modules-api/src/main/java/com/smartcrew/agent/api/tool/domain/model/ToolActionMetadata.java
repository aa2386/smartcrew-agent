package com.smartcrew.agent.api.tool.domain.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool 动作元数据。
 */
@Data
@Builder
public class ToolActionMetadata {

    /**
     * 所属 Tool 编码。
     */
    private String toolCode;

    /**
     * 动作名称。
     */
    private String actionName;

    /**
     * 动作描述。
     */
    private String description;

    /**
     * 参数定义列表。
     */
    @Builder.Default
    private List<ToolActionParameter> parameters = new ArrayList<>();
}
