package com.smartcrew.agent.api.platform.service;

import com.smartcrew.agent.api.platform.domain.request.PlatformEventRequest;
import com.smartcrew.agent.api.platform.domain.vo.PlatformDispatchResponse;

/**
 * PlatformAdapter 接口，统一不同平台接入的能力抽象。
 */
public interface PlatformAdapter {

    String platformCode();

    boolean supports(String platform);

    PlatformDispatchResponse handleEvent(PlatformEventRequest request);
}
