package com.smartcrew.agent.controller.web;

import com.smartcrew.agent.api.auth.domain.request.LoginRequest;
import com.smartcrew.agent.api.auth.domain.request.RegisterRequest;
import com.smartcrew.agent.api.auth.domain.vo.CurrentUserVo;
import com.smartcrew.agent.api.auth.domain.vo.LoginResponse;
import com.smartcrew.agent.api.auth.service.AuthenticationService;
import com.smartcrew.agent.common.auth.AuthContextHolder;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.common.exception.ServiceException;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web 端认证控制器。
 */
@RestController
@RequestMapping("/api/web/auth")
@ConditionalOnProperty(prefix = "smartcrew.api.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebAuthController {

    /**
     * 认证服务。
     */
    private final AuthenticationService authenticationService;

    public WebAuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public R<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authenticationService.loginWeb(request));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        var user = AuthContextHolder.get();
        if (user == null) {
            throw new ServiceException(401, "\u5f53\u524d\u672a\u767b\u5f55");
        }
        authenticationService.logout(user.getSessionId());
        return R.ok("\u9000\u51fa\u6210\u529f", null);
    }

    @GetMapping("/me")
    public R<CurrentUserVo> me() {
        var user = AuthContextHolder.get();
        if (user == null) {
            throw new ServiceException(401, "\u5f53\u524d\u672a\u767b\u5f55");
        }
        return R.ok(authenticationService.currentUser(user.getUserId()));
    }
}
