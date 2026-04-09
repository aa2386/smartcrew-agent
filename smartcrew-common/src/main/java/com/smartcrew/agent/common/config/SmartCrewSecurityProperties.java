package com.smartcrew.agent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SmartCrew 安全与接口暴露配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "smartcrew")
public class SmartCrewSecurityProperties {

    /**
     * 接口暴露配置。
     */
    private Api api = new Api();

    /**
     * 鉴权配置。
     */
    private Auth auth = new Auth();

    /**
     * 跨域配置。
     */
    private Cors cors = new Cors();

    @Data
    public static class Api {

        /**
         * 现有 v1 接口配置。
         */
        private Endpoint v1 = new Endpoint();

        /**
         * Web 页面服务接口配置。
         */
        private Endpoint web = new Endpoint();

        /**
         * 后台页面服务接口配置。
         */
        private Endpoint admin = new Endpoint();
    }

    @Data
    public static class Endpoint {

        /**
         * 是否启用当前接口分组。
         */
        private boolean enabled = true;
    }

    @Data
    public static class Auth {

        /**
         * 鉴权模式，当前支持 OFF 与 LOCAL_JWT。
         */
        private String mode = "OFF";

        /**
         * Web 页面接口是否要求登录。
         */
        private boolean requireWebLogin = true;

        /**
         * 后台接口是否要求管理员登录。
         */
        private boolean requireAdminLogin = true;

        /**
         * JWT 签名密钥。
         */
        private String tokenSecret = "";

        /**
         * Token 过期分钟数。
         */
        private long tokenExpireMinutes = 720;

        /**
         * 默认管理员引导配置。
         */
        private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
    }

    @Data
    public static class Cors {

        /**
         * 允许的来源列表。
         */
        private java.util.List<String> allowedOrigins = new java.util.ArrayList<>(
                java.util.List.of(
                        "http://localhost:8080",
                        "http://localhost:8081"
                )
        );

        /**
         * 是否允许携带凭证。
         */
        private boolean allowCredentials = true;

        /**
         * 预检缓存秒数。
         */
        private long maxAge = 3600;
    }

    @Data
    public static class BootstrapAdmin {

        /**
         * 是否初始化管理员。
         */
        private boolean enabled = true;

        /**
         * 管理员用户名。
         */
        private String username = "admin";

        /**
         * 管理员密码。
         */
        private String password = "";

        /**
         * 管理员显示名称。
         */
        private String displayName = "系统管理员";
    }
}
