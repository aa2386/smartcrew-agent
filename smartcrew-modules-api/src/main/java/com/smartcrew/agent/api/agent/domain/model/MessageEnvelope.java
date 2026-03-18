package com.smartcrew.agent.api.agent.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ?????????????????????????????
 */
@Data
@Builder
public class MessageEnvelope {

    /**
     * 调用链追踪 ID。
     */
    private String traceId;
    /**
     * 来源代理编码。
     */
    private String sourceAgent;
    /**
     * 目标代理编码。
     */
    private String targetAgent;
    /**
     * 用户 ID。
     */
    private Long userId;
    /**
     * 会话 ID。
     */
    private String sessionId;
    /**
     * 消息载荷。
     */
    private Object payload;
    /**
     * 扩展元数据。
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    /**
     * 消息创建时间。
     */
    private LocalDateTime createdAt;
}
