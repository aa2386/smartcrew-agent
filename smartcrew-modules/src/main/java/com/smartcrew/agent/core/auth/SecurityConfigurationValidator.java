package com.smartcrew.agent.core.auth;

import com.smartcrew.agent.common.config.SmartCrewSecurityProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 安全配置启动校验，避免仓库以弱默认配置直接运行。
 */
@Component
public class SecurityConfigurationValidator {

    private final SmartCrewSecurityProperties securityProperties;

    public SecurityConfigurationValidator(SmartCrewSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @PostConstruct
    public void validate() {
        SmartCrewSecurityProperties.Auth auth = securityProperties.getAuth();
        if ("LOCAL_JWT".equalsIgnoreCase(auth.getMode()) && !StringUtils.hasText(auth.getTokenSecret())) {
            throw new IllegalStateException(
                    "已启用 LOCAL_JWT 鉴权，请通过 SMARTCREW_TOKEN_SECRET 或 smartcrew.auth.token-secret 配置 JWT 签名密钥。"
            );
        }

        SmartCrewSecurityProperties.BootstrapAdmin bootstrapAdmin = auth.getBootstrapAdmin();
        if (bootstrapAdmin.isEnabled() && !StringUtils.hasText(bootstrapAdmin.getPassword())) {
            throw new IllegalStateException(
                    "已启用默认管理员初始化，请通过 SMARTCREW_BOOTSTRAP_ADMIN_PASSWORD 或 "
                            + "smartcrew.auth.bootstrap-admin.password 配置管理员密码。"
            );
        }
    }
}
