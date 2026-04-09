package com.smartcrew.agent.api.auth.service;

import com.smartcrew.agent.api.auth.domain.request.LoginRequest;
import com.smartcrew.agent.api.auth.domain.request.RegisterRequest;
import com.smartcrew.agent.api.auth.domain.vo.CurrentUserVo;
import com.smartcrew.agent.api.auth.domain.vo.LoginResponse;

/**
 * 认证服务。
 */
public interface AuthenticationService {

    /**
     * 注册普通用户并自动登录。
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 普通用户登录。
     */
    LoginResponse loginWeb(LoginRequest request);

    /**
     * 管理员登录。
     */
    LoginResponse loginAdmin(LoginRequest request);

    /**
     * 退出登录。
     */
    void logout(String sessionId);

    /**
     * 构建当前用户信息。
     */
    CurrentUserVo currentUser(Long userId);
}
