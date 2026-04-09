package com.smartcrew.agent.api.user.service;

import com.smartcrew.agent.api.admin.domain.request.UserIdentityBindRequest;
import com.smartcrew.agent.api.user.domain.vo.ScUserIdentityVo;

import java.util.List;

/**
 * 用户身份管理服务。
 */
public interface UserIdentityService {

    /**
     * 查询用户身份映射。
     */
    List<ScUserIdentityVo> listByUserId(Long userId);

    /**
     * 手动绑定身份。
     */
    ScUserIdentityVo bind(Long userId, UserIdentityBindRequest request);

    /**
     * 删除身份映射。
     */
    void unbind(Long userId, Long identityId);
}
