package com.smartcrew.agent.common.auth;

import lombok.Builder;
import lombok.Data;

/**
 * 当前请求上下文中的认证用户信息。
 */
@Data
@Builder
public class AuthenticatedUser {

    /**
     * 系统用户 ID。
     */
    private Long userId;

    /**
     * 登录会话 ID。
     */
    private String sessionId;

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
