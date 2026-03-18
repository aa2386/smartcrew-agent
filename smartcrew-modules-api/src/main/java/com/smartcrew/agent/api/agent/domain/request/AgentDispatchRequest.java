package com.smartcrew.agent.api.agent.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * AgentDispatchRequest 请求对象，封装接口调用所需的入参数据。
 */
@Data
public class AgentDispatchRequest {

    /**
     * 用户 ID。
     */
    @NotNull
    private Long userId;

    /**
     * 会话 ID。
     */
    @NotBlank
    private String sessionId;

    /**
     * 消息内容。
     */
    @NotBlank
    private String message;

    /**
     * 附加上下文数据。
     */
    private Map<String, Object> context = new HashMap<>();
}
