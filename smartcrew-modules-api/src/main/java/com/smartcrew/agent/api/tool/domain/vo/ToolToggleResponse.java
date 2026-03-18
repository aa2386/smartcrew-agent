package com.smartcrew.agent.api.tool.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * ?????????
 */
@Data
@Builder
public class ToolToggleResponse {

    /**
     * 工具编码。
     */
    private String toolCode;
    /**
     * 是否启用。
     */
    private boolean enabled;
    /**
     * 消息内容。
     */
    private String message;
}
