package com.smartcrew.agent.api.auth.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册请求。
 */
@Data
public class RegisterRequest {

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

    /**
     * 显示名称。
     */
    @NotBlank
    private String displayName;
}
