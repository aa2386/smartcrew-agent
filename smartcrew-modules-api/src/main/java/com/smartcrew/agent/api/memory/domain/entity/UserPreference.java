package com.smartcrew.agent.api.memory.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ??????????????????????
 */
@Data
@TableName("user_preference")
@EqualsAndHashCode(callSuper = true)
public class UserPreference extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
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
