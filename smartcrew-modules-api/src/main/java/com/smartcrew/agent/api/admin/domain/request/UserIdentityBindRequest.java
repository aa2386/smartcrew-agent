package com.smartcrew.agent.api.admin.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 手动绑定身份请求。
 */
@Data
public class UserIdentityBindRequest {

    /**
     * 身份提供方。
     */
    @NotBlank
    private String provider;

    /**
     * 提供方用户标识。
     */
    @NotBlank
    private String providerUserId;

    /**
     * 租户标识。
     */
    private String tenantKey = "";

    /**
     * 快照 JSON。
     */
    private String profileSnapshotJson;
}
