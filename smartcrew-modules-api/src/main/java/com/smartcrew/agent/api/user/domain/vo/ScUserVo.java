package com.smartcrew.agent.api.user.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户视图对象。
 */
@Data
@Builder
public class ScUserVo {

    /**
     * 用户 ID。
     */
    private Long id;

    /**
     * 用户名。
     */
    private String username;

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
