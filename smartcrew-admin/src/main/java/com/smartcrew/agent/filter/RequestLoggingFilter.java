package com.smartcrew.agent.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 统一请求日志过滤器。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    /* 记录请求日志并继续执行过滤链。 */
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        log.info("收到请求 method={}, uri={}, query={}, clientIp={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString() == null ? "" : request.getQueryString(),
                resolveClientIp(request));
        filterChain.doFilter(request, response);
    }

    /**
     * 提取客户端 IP，优先读取反向代理头。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
