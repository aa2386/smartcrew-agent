package com.smartcrew.agent.api.tool.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * 工具动作参数元数据，描述工具动作的单个输入参数。
 *
 * <p>包含参数名称、描述、类型及是否必填等信息，
 * 用于在工具注册和调用时进行参数校验与文档生成。</p>
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
     * 参数类型。
     */
    private String type;

    /**
     * 是否必填。
     */
    private boolean required;
}
