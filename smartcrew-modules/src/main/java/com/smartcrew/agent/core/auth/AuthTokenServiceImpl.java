package com.smartcrew.agent.core.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smartcrew.agent.api.auth.domain.model.AuthSessionToken;
import com.smartcrew.agent.api.auth.domain.model.LoginSessionRecord;
import com.smartcrew.agent.api.auth.service.AuthTokenService;
import com.smartcrew.agent.common.config.SmartCrewSecurityProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.common.util.JsonUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 HS256 的本地 JWT 服务。
 */
@Service
public class AuthTokenServiceImpl implements AuthTokenService {

    /**
     * JWT 签名算法。
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * 安全配置。
     */
    private final SmartCrewSecurityProperties securityProperties;

    public AuthTokenServiceImpl(SmartCrewSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public String createToken(LoginSessionRecord record) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sid", record.getSessionId());
        payload.put("uid", record.getUserId());
        payload.put("role", record.getRole());
        payload.put("username", record.getUsername());
        payload.put("exp", toEpochMillis(record.getExpireAt()));

        String headerPart = encodeBase64Url(JsonUtils.toJson(header));
        String payloadPart = encodeBase64Url(JsonUtils.toJson(payload));
        String unsignedToken = headerPart + "." + payloadPart;
        return unsignedToken + "." + sign(unsignedToken);
    }

    @Override
    public AuthSessionToken parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ServiceException(401, "缺少访问令牌");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new ServiceException(401, "访问令牌格式不正确");
        }
        String unsignedToken = parts[0] + "." + parts[1];
        if (!sign(unsignedToken).equals(parts[2])) {
            throw new ServiceException(401, "访问令牌校验失败");
        }

        Map<String, Object> payload = JsonUtils.parse(
                decodeBase64Url(parts[1]),
                new TypeReference<Map<String, Object>>() {
                }
        );
        long expireAt = Long.parseLong(String.valueOf(payload.get("exp")));
        if (expireAt <= System.currentTimeMillis()) {
            throw new ServiceException(401, "访问令牌已过期");
        }
        return AuthSessionToken.builder()
                .sessionId(String.valueOf(payload.get("sid")))
                .userId(Long.parseLong(String.valueOf(payload.get("uid"))))
                .role(String.valueOf(payload.get("role")))
                .username(String.valueOf(payload.get("username")))
                .expireAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(expireAt), ZoneId.systemDefault()))
                .build();
    }

    /**
     * 执行签名。
     */
    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    securityProperties.getAuth().getTokenSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new ServiceException(500, "生成访问令牌失败");
        }
    }

    /**
     * Base64 URL 编码。
     */
    private String encodeBase64Url(String content) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 URL 解码。
     */
    private String decodeBase64Url(String content) {
        return new String(Base64.getUrlDecoder().decode(content), StandardCharsets.UTF_8);
    }

    /**
     * 转换为毫秒时间戳。
     */
    private long toEpochMillis(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
