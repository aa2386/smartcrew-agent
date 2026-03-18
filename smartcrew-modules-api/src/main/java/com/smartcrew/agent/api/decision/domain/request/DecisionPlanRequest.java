package com.smartcrew.agent.api.decision.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * ?????????????????????????
 */
@Data
public class DecisionPlanRequest {

    /**
     * 代理编码。
     */
    @NotBlank
    private String agentCode;

    /**
     * 用户 ID。
     */
    @NotNull
    private Long userId;

    /**
     * 用户输入内容。
     */
    @NotBlank
    private String input;

    /**
     * 附加上下文数据。
     */
    private Map<String, Object> context = new HashMap<>();
}
