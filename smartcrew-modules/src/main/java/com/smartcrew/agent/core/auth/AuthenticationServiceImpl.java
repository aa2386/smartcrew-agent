package com.smartcrew.agent.core.auth;

import com.smartcrew.agent.api.auth.domain.model.LoginSessionRecord;
import com.smartcrew.agent.api.auth.domain.request.LoginRequest;
import com.smartcrew.agent.api.auth.domain.request.RegisterRequest;
import com.smartcrew.agent.api.auth.domain.vo.CurrentUserVo;
import com.smartcrew.agent.api.auth.domain.vo.LoginResponse;
import com.smartcrew.agent.api.auth.service.AuthTokenService;
import com.smartcrew.agent.api.auth.service.AuthenticationService;
import com.smartcrew.agent.api.auth.service.LoginSessionStore;
import com.smartcrew.agent.api.user.domain.entity.ScUser;
import com.smartcrew.agent.api.user.service.UserAccountService;
import com.smartcrew.agent.common.config.SmartCrewSecurityProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 认证服务实现。
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    /**
     * 用户账户服务。
     */
    private final UserAccountService userAccountService;

    /**
     * Token 服务。
     */
    private final AuthTokenService authTokenService;

    /**
     * 登录态存储。
     */
    private final LoginSessionStore loginSessionStore;

    /**
     * 安全配置。
     */
    private final SmartCrewSecurityProperties securityProperties;

    public AuthenticationServiceImpl(UserAccountService userAccountService,
                                     AuthTokenService authTokenService,
                                     LoginSessionStore loginSessionStore,
                                     SmartCrewSecurityProperties securityProperties) {
        this.userAccountService = userAccountService;
        this.authTokenService = authTokenService;
        this.loginSessionStore = loginSessionStore;
        this.securityProperties = securityProperties;
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        userAccountService.createLocalUser(request);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(request.getUsername());
        loginRequest.setPassword(request.getPassword());
        return loginWeb(loginRequest);
    }

    @Override
    public LoginResponse loginWeb(LoginRequest request) {
        LoginSessionRecord record = userAccountService.authenticate(request.getUsername(), request.getPassword());
        return createLoginResponse(record, false);
    }

    @Override
    public LoginResponse loginAdmin(LoginRequest request) {
        LoginSessionRecord record = userAccountService.authenticate(request.getUsername(), request.getPassword());
        return createLoginResponse(record, true);
    }

    @Override
    public void logout(String sessionId) {
        loginSessionStore.invalidate(sessionId);
    }

    @Override
    public CurrentUserVo currentUser(Long userId) {
        ScUser user = userAccountService.findById(userId)
                .orElseThrow(() -> new ServiceException(404, "用户不存在"));
        return toCurrentUserVo(user);
    }

    /**
     * 构建登录响应。
     */
    private LoginResponse createLoginResponse(LoginSessionRecord record, boolean adminOnly) {
        if (adminOnly && !"ADMIN".equalsIgnoreCase(record.getRole())) {
            throw new ServiceException(403, "当前账号不是管理员");
        }
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime expireAt = LocalDateTime.now()
                .plusMinutes(securityProperties.getAuth().getTokenExpireMinutes());
        LoginSessionRecord currentRecord = LoginSessionRecord.builder()
                .sessionId(sessionId)
                .userId(record.getUserId())
                .username(record.getUsername())
                .displayName(record.getDisplayName())
                .role(record.getRole())
                .avatarUrl(record.getAvatarUrl())
                .expireAt(expireAt)
                .build();
        loginSessionStore.save(sessionId, currentRecord, expireAt);
        return LoginResponse.builder()
                .token(authTokenService.createToken(currentRecord))
                .tokenType("Bearer")
                .sessionId(sessionId)
                .expireAt(expireAt)
                .user(CurrentUserVo.builder()
                        .userId(record.getUserId())
                        .username(record.getUsername())
                        .displayName(record.getDisplayName())
                        .role(record.getRole())
                        .avatarUrl(record.getAvatarUrl())
                        .build())
                .build();
    }

    /**
     * 转换为当前用户视图对象。
     */
    private CurrentUserVo toCurrentUserVo(ScUser user) {
        return CurrentUserVo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
