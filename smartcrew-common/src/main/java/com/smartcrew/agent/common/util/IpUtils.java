package com.smartcrew.agent.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 工具类，用于从请求头中解析客户端真实 IP。
 */
public final class IpUtils {

    /**
     * 私有构造方法，禁止外部实例化。
     */
    private IpUtils() {
    }

    /**
     * 解析请求中的客户端真实 IP。
     */
    public static String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String value = request.getHeader(header);
            if (StringUtils.isNotBlank(value) && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
