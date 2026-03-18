package com.smartcrew.agent.api.platform.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * PlatformEventRequest 请求对象，封装接口调用所需的入参数据。
 */
@Data
public class PlatformEventRequest {

    /**
     * 平台侧用户 ID。
     */
    @NotBlank
    private String platformUserId;

    /**
     * 平台事件类型。
     */
    @NotBlank
    private String eventType;

    /**
     * 事件内容。
     */
    @NotBlank
    private String content;

    /**
     * 扩展元数据。
     */
    private Map<String, Object> metadata = new HashMap<>();
}
