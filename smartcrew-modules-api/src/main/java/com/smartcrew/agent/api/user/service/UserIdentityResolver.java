package com.smartcrew.agent.api.user.service;

import com.smartcrew.agent.api.user.domain.entity.ScUser;

/**
 * 用户身份解析服务。
 */
public interface UserIdentityResolver {

    /**
     * 解析或创建平台用户。
     */
    ScUser resolveOrCreatePlatformUser(String provider,
                                       String providerUserId,
                                       String tenantKey,
                                       String profileSnapshotJson);
}
