package com.smartcrew.agent.api.tool.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * Tool 动作参数元数据。
 */
@Data
@Builder
public class ToolActionParameter {

    /**
     * 参数名称。
     */
    private String name;

    /**
     * 参数描述。
     */
    private String description;

    /**
     * 是否必填。
     */
    private boolean required;
}
