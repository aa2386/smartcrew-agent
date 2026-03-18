package com.smartcrew.agent.api.platform.service;

import com.smartcrew.agent.api.platform.domain.request.PlatformEventRequest;
import com.smartcrew.agent.api.platform.domain.vo.PlatformDispatchResponse;

/**
 * PlatformAdapter 接口，统一不同平台接入的能力抽象。
 */
public interface PlatformAdapter {

    /**
     * 返回平台编码。
     *
     * @return 对应编码。
     */
    String platformCode();

    /**
     * 判断当前适配器是否支持指定平台。
     *
     * @param platform 平台编码，例如 `wecom`、`feishu`。
     * @return `true` 表示支持，`false` 表示不支持。
     */
    boolean supports(String platform);

    /**
     * 处理平台事件并返回派发结果。
     *
     * @param request 请求参数。
     * @return 平台事件处理结果。
     */
    PlatformDispatchResponse handleEvent(PlatformEventRequest request);
}
