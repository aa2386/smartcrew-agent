package com.smartcrew.agent.api.agent.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * ?????????????????????????
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
}
