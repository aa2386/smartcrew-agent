package com.smartcrew.agent.controller.platform;

import com.smartcrew.agent.api.platform.domain.request.PlatformEventRequest;
import com.smartcrew.agent.api.platform.domain.vo.PlatformDispatchResponse;
import com.smartcrew.agent.api.platform.service.PlatformAdapter;
import com.smartcrew.agent.api.platform.service.PlatformAdapterRegistry;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.common.exception.ServiceException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台网关控制器，负责接收外部平台事件并分发到对应适配器。
 */
@RestController
@RequestMapping("/api/v1/platform")
public class PlatformController {

    /**
     * 平台适配器注册表。
     */
    private final PlatformAdapterRegistry platformAdapterRegistry;

    /**
     * 构造 PlatformController 所需的依赖对象。
     */
    public PlatformController(PlatformAdapterRegistry platformAdapterRegistry) {
        this.platformAdapterRegistry = platformAdapterRegistry;
    }

    /**
     * 处理当前请求。
     */
    @PostMapping("/{platform}/events")
    public R<PlatformDispatchResponse> handle(@PathVariable("platform") String platform,
                                              @Valid @RequestBody PlatformEventRequest request) {
        PlatformAdapter adapter = platformAdapterRegistry.getAdapter(platform)
                .orElseThrow(() -> new ServiceException("Unknown platform: " + platform));
        return R.ok(adapter.handleEvent(request));
    }
}
