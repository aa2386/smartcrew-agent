package com.smartcrew.agent.core.platform;

import com.smartcrew.agent.api.platform.domain.request.PlatformEventRequest;
import com.smartcrew.agent.api.platform.domain.vo.PlatformDispatchResponse;
import com.smartcrew.agent.api.platform.service.PlatformAdapter;
import org.springframework.stereotype.Component;

/**
 * 飞书平台适配器占位实现。
 */
@Component
public class FeishuPlatformAdapter implements PlatformAdapter {

    /**
     * 返回平台编码。
     */
    @Override
    public String platformCode() {
        return "feishu";
    }

    /**
     * 判断是否支持指定能力或目标。
     */
    @Override
    public boolean supports(String platform) {
        return platformCode().equalsIgnoreCase(platform);
    }

    /**
     * 处理平台事件。
     */
    @Override
    public PlatformDispatchResponse handleEvent(PlatformEventRequest request) {
        return PlatformDispatchResponse.builder()
                .platform(platformCode())
                .handled(true)
                .message("Feishu adapter placeholder accepted event " + request.getEventType())
                .build();
    }
}
