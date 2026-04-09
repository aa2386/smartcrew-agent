package com.smartcrew.agent.api.auth.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求。
 */
@Data
public class LoginRequest {

    /**
     * 用户名。
     */
    @NotBlank
    private String username;

    /**
     * 密码。
     */
    @NotBlank
    private String password;
}
