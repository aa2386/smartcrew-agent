package com.smartcrew.agent.api.platform.service;

import com.smartcrew.agent.api.platform.domain.request.PlatformEventRequest;
import com.smartcrew.agent.api.platform.domain.vo.PlatformDispatchResponse;

/**
 * ??????????????????????
 */
public interface PlatformAdapter {

    String platformCode();

    boolean supports(String platform);

    PlatformDispatchResponse handleEvent(PlatformEventRequest request);
}
