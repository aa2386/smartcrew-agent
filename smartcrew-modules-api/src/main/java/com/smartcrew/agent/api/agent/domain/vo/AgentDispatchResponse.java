package com.smartcrew.agent.api.agent.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * AgentDispatchResponse 视图对象，封装接口返回给调用方的数据。
 */
@Data
@Builder
public class AgentDispatchResponse {

    /**
     * 调用链追踪 ID。
     */
    private String traceId;
    /**
     * 代理编码。
     */
    private String agentCode;
    /**
     * accepted 的业务字段。
     */
    private boolean accepted;
    /**
     * 消息内容。
     */
    private String message;
    /**
     * 附加元数据。
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
