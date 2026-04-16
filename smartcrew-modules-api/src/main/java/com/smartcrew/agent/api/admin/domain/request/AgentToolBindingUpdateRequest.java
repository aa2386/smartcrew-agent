package com.smartcrew.agent.api.admin.domain.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent Tool 绑定替换请求。
 */
@Data
public class AgentToolBindingUpdateRequest {

    /**
     * 目标 Tool 编码列表。
     */
    private List<String> toolCodes = new ArrayList<>();
}
