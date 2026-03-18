package com.smartcrew.agent.api.agent.domain.vo;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import lombok.Builder;
import lombok.Data;

/**
 * ???????????????????????????
 */
@Data
@Builder
public class RegisteredAgent {

    /**
     * 代理定义信息。
     */
    private AgentDefinition definition;
}
