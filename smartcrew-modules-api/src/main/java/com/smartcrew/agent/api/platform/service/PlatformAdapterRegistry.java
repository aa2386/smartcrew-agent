package com.smartcrew.agent.api.platform.service;

import java.util.Optional;

/**
 * PlatformAdapterRegistry 接口，负责相关组件的注册、查询与管理。
 */
public interface PlatformAdapterRegistry {

    Optional<PlatformAdapter> getAdapter(String platform);
}
