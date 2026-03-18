package com.smartcrew.agent.api.memory.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * UserPreferenceUpsertRequest 请求对象，封装接口调用所需的入参数据。
 */
@Data
public class UserPreferenceUpsertRequest {

    /**
     * 偏好键。
     */
    @NotBlank
    private String prefKey;

    /**
     * 偏好值。
     */
    @NotBlank
    private String prefValue;

    /**
     * 偏好类型。
     */
    private String prefType = "TEXT";
    /**
     * 来源标识。
     */
    private String source = "MANUAL";
}
