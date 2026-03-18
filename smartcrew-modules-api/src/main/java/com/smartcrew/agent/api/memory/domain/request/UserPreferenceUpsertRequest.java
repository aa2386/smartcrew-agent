package com.smartcrew.agent.api.memory.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ???????????????????????
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
