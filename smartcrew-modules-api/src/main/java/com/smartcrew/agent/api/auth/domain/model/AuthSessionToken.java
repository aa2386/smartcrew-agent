package com.smartcrew.agent.api.auth.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 解析后的 JWT 令牌信息。
 */
@Data
@Builder
public class AuthSessionToken {

    /**
     * 登录会话 ID。
     */
    private String sessionId;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 用户角色。
     */
    private String role;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 过期时间。
     */
    private LocalDateTime expireAt;
}
