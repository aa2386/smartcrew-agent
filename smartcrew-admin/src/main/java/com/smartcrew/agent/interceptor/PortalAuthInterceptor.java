package com.smartcrew.agent.interceptor;

import com.smartcrew.agent.api.auth.domain.model.AuthSessionToken;
import com.smartcrew.agent.api.auth.domain.model.LoginSessionRecord;
import com.smartcrew.agent.api.auth.service.AuthTokenService;
import com.smartcrew.agent.api.auth.service.LoginSessionStore;
import com.smartcrew.agent.common.auth.AuthContextHolder;
import com.smartcrew.agent.common.auth.AuthenticatedUser;
import com.smartcrew.agent.common.config.SmartCrewSecurityProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Web 与后台接口认证拦截器。
 */
@Component
public class PortalAuthInterceptor implements HandlerInterceptor {

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

    public PortalAuthInterceptor(AuthTokenService authTokenService,
                                 LoginSessionStore loginSessionStore,
                                 SmartCrewSecurityProperties securityProperties) {
        this.authTokenService = authTokenService;
        this.loginSessionStore = loginSessionStore;
        this.securityProperties = securityProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        boolean webApi = uri.startsWith("/api/web/");
        boolean adminApi = uri.startsWith("/api/admin/");
        if (!webApi && !adminApi) {
            return true;
        }
        if ("OFF".equalsIgnoreCase(securityProperties.getAuth().getMode())) {
            return true;
        }
        if (webApi && !securityProperties.getAuth().isRequireWebLogin()) {
            return true;
        }
        if (adminApi && !securityProperties.getAuth().isRequireAdminLogin()) {
            return true;
        }
        if (isPublicPath(uri)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ServiceException(401, "请先登录");
        }
        AuthSessionToken authSessionToken = authTokenService.parseToken(authorization.substring("Bearer ".length()).trim());
        LoginSessionRecord sessionRecord = loginSessionStore.get(authSessionToken.getSessionId())
                .orElseThrow(() -> new ServiceException(401, "登录状态已失效"));
        if (!sessionRecord.getUserId().equals(authSessionToken.getUserId())) {
            throw new ServiceException(401, "登录状态校验失败");
        }
        if (adminApi && !"ADMIN".equalsIgnoreCase(sessionRecord.getRole())) {
            throw new ServiceException(403, "当前账号没有后台权限");
        }
        AuthContextHolder.set(AuthenticatedUser.builder()
                .userId(sessionRecord.getUserId())
                .sessionId(sessionRecord.getSessionId())
                .username(sessionRecord.getUsername())
                .displayName(sessionRecord.getDisplayName())
                .role(sessionRecord.getRole())
                .avatarUrl(sessionRecord.getAvatarUrl())
                .build());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContextHolder.clear();
    }

    /**
     * 判断是否为公开接口。
     */
    private boolean isPublicPath(String uri) {
        return "/api/web/auth/login".equals(uri)
                || "/api/web/auth/register".equals(uri)
                || "/api/admin/auth/login".equals(uri);
    }
}
