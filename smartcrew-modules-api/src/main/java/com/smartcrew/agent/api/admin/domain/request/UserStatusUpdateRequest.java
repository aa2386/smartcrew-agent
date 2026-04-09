package com.smartcrew.agent.api.admin.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户状态更新请求。
 */
@Data
public class UserStatusUpdateRequest {

    /**
     * 用户状态。
     */
    @NotBlank
    private String status;
}
