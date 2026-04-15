package com.smartcrew.agent.controller.admin;

import com.smartcrew.agent.api.auth.domain.request.LoginRequest;
import com.smartcrew.agent.api.auth.domain.vo.LoginResponse;
import com.smartcrew.agent.api.auth.service.AuthenticationService;
import com.smartcrew.agent.common.auth.AuthContextHolder;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.common.exception.ServiceException;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台认证控制器。
 */
@RestController
@RequestMapping("/api/admin/auth")
@ConditionalOnProperty(prefix = "smartcrew.api.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminAuthController {

    /**
     * 认证服务。
     */
    private final AuthenticationService authenticationService;

    public AdminAuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * 执行后台登录。
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authenticationService.loginAdmin(request));
    }

    /**
     * 执行后台登出。
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        var user = AuthContextHolder.get();
        if (user == null) {
            throw new ServiceException(401, "\u5f53\u524d\u672a\u767b\u5f55");
        }
        authenticationService.logout(user.getSessionId());
        return R.ok("\u9000\u51fa\u6210\u529f", null);
    }
}
