package com.smartcrew.agent.config;

import com.smartcrew.agent.common.config.SmartCrewSecurityProperties;
import com.smartcrew.agent.interceptor.PortalAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 页面端 MVC 配置。
 */
@Configuration
public class WebMvcPortalConfig implements WebMvcConfigurer {

    /**
     * 页面端鉴权拦截器。
     */
    private final PortalAuthInterceptor portalAuthInterceptor;

    /**
     * 安全与跨域配置。
     */
    private final SmartCrewSecurityProperties securityProperties;

    public WebMvcPortalConfig(PortalAuthInterceptor portalAuthInterceptor,
                              SmartCrewSecurityProperties securityProperties) {
        this.portalAuthInterceptor = portalAuthInterceptor;
        this.securityProperties = securityProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(portalAuthInterceptor)
                .addPathPatterns("/api/web/**", "/api/admin/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(securityProperties.getCors().getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(securityProperties.getCors().isAllowCredentials())
                .maxAge(securityProperties.getCors().getMaxAge());
    }
}
