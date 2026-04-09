package com.smartcrew.agent.api.user.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 用户身份映射视图对象。
 */
@Data
@Builder
public class ScUserIdentityVo {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 系统用户 ID。
     */
    private Long userId;

    /**
     * 身份提供方。
     */
    private String provider;

    /**
     * 提供方用户标识。
     */
    private String providerUserId;

    /**
     * 租户标识。
     */
    private String tenantKey;

    /**
     * 快照 JSON。
     */
    private String profileSnapshotJson;
}
