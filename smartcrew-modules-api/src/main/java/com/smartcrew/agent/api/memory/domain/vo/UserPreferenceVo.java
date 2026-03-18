package com.smartcrew.agent.api.memory.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * UserPreferenceVo 视图对象，封装接口返回给调用方的数据。
 */
@Data
@Builder
public class UserPreferenceVo {

    /**
     * 主键 ID。
     */
    private Long id;
    /**
     * 用户 ID。
     */
    private Long userId;
    /**
     * 偏好键。
     */
    private String prefKey;
    /**
     * 偏好值。
     */
    private String prefValue;
    /**
     * 偏好类型。
     */
    private String prefType;
    /**
     * 来源标识。
     */
    private String source;
}
