package com.smartcrew.agent.api.platform.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * ???????????????????????
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
