package com.smartcrew.agent.api.auth.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 当前登录用户信息。
 */
@Data
@Builder
public class CurrentUserVo {

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 显示名称。
     */
    private String displayName;

    /**
     * 用户角色。
     */
    private String role;

    /**
     * 头像地址。
     */
    private String avatarUrl;
}
