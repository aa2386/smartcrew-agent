package com.smartcrew.agent.api.platform.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * PlatformDispatchResponse 视图对象，封装接口返回给调用方的数据。
 */
@Data
@Builder
public class PlatformDispatchResponse {

    /**
     * 平台编码。
     */
    private String platform;
    /**
     * 是否已处理。
     */
    private boolean handled;
    /**
     * 消息内容。
     */
    private String message;
}
