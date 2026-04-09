package com.smartcrew.agent.api.user.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户身份映射实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sc_user_identity")
public class ScUserIdentity extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 系统用户 ID。
     */
    private Long userId;

    /**
     * 身份提供方。
     */
    private String provider;

    /**
     * 提供方用户标识。
     */
    private String providerUserId;

    /**
     * 租户标识。
     */
    private String tenantKey;

    /**
     * 身份快照 JSON。
     */
    private String profileSnapshotJson;
}
