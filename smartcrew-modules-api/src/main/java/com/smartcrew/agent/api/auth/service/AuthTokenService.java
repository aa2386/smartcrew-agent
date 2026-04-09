package com.smartcrew.agent.api.auth.service;

import com.smartcrew.agent.api.auth.domain.model.AuthSessionToken;
import com.smartcrew.agent.api.auth.domain.model.LoginSessionRecord;

/**
 * 访问令牌服务。
 */
public interface AuthTokenService {

    /**
     * 生成 JWT 令牌。
     */
    String createToken(LoginSessionRecord record);

    /**
     * 解析 JWT 令牌。
     */
    AuthSessionToken parseToken(String token);
}
