package com.smartcrew.agent.api.agent.domain.vo;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import lombok.Builder;
import lombok.Data;

/**
 * RegisteredAgent 视图对象，封装接口返回给调用方的数据。
 */
@Data
@Builder
public class RegisteredAgent {

    /**
     * 代理定义信息。
     */
    private AgentDefinition definition;
}
