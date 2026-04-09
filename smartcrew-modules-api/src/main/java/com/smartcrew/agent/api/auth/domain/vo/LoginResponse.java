package com.smartcrew.agent.api.auth.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录响应。
 */
@Data
@Builder
public class LoginResponse {

    /**
     * 访问令牌。
     */
    private String token;

    /**
     * 令牌类型。
     */
    private String tokenType;

    /**
     * 登录会话 ID。
     */
    private String sessionId;

    /**
     * 过期时间。
     */
    private LocalDateTime expireAt;

    /**
     * 当前用户。
     */
    private CurrentUserVo user;
}
