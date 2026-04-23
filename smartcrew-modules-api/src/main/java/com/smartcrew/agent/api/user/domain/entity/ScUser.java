package com.smartcrew.agent.api.user.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcrew.agent.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统用户实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sc_user")
public class ScUser extends BaseEntity {

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 密码哈希值。
     */
    private String passwordHash;

    /**
     * 显示名称。
     */
    private String displayName;

    /**
     * 头像地址。
     */
    private String avatarUrl;

    /**
     * 用户角色。
     */
    private String role;

    /**
     * 用户状态。
     */
    private String status;

    /**
     * 最后登录时间。
     */
    private LocalDateTime lastLoginAt;
}
