package com.smartcrew.agent.api.tool.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * ToolToggleResponse 视图对象，封装接口返回给调用方的数据。
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
