package com.smartcrew.agent.core.platform;

import com.smartcrew.agent.api.platform.service.PlatformAdapter;
import com.smartcrew.agent.api.platform.service.PlatformAdapterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 基于内存的平台适配器注册表实现。
 */
@Component
public class InMemoryPlatformAdapterRegistry implements PlatformAdapterRegistry {

    /**
     * adapters 的业务字段。
     */
    private final List<PlatformAdapter> adapters;

    /**
     * 构造 InMemoryPlatformAdapterRegistry 所需的依赖对象。
     */
    public InMemoryPlatformAdapterRegistry(List<PlatformAdapter> adapters) {
        this.adapters = adapters;
    }

    /**
     * 按平台编码获取适配器。
     */
    @Override
    public Optional<PlatformAdapter> getAdapter(String platform) {
        return adapters.stream().filter(adapter -> adapter.supports(platform)).findFirst();
    }
}
