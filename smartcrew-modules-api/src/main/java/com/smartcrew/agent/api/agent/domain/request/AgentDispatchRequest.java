package com.smartcrew.agent.api.agent.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * ????????????????????????
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
