package com.smartcrew.agent.api.auth.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录态缓存记录。
 */
@Data
@Builder
public class LoginSessionRecord {

    /**
     * 登录会话 ID。
     */
    private String sessionId;

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

    /**
     * 过期时间。
     */
    private LocalDateTime expireAt;
}
