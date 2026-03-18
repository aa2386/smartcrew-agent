package com.smartcrew.agent.api.platform.service;

import java.util.Optional;

/**
 * PlatformAdapterRegistry 接口，负责相关组件的注册、查询与管理。
 */
public interface PlatformAdapterRegistry {

    /**
     * 按平台编码获取适配器。
     *
     * @param platform 平台编码，例如 `wecom`、`feishu`。
     * @return 匹配结果；未找到时返回空 `Optional`。
     */
    Optional<PlatformAdapter> getAdapter(String platform);
}
