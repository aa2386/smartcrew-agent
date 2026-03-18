package com.smartcrew.agent.api.agent.domain.model;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * AgentDispatchCommand 领域模型，描述业务流程中的中间数据结构。
 */
@Data
@Builder
public class AgentDispatchCommand {

    /**
     * 调用链追踪 ID。
     */
    private String traceId;
    /**
     * 代理编码。
     */
    private String agentCode;
    /**
     * 用户 ID。
     */
    private Long userId;
    /**
     * 会话 ID。
     */
    private String sessionId;
    /**
     * 消息内容。
     */
    private String message;
    /**
     * 附加上下文数据。
     */
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();
}
