package com.smartcrew.agent.api.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartcrew.agent.api.auth.domain.model.LoginSessionRecord;
import com.smartcrew.agent.api.auth.domain.request.RegisterRequest;
import com.smartcrew.agent.api.user.domain.entity.ScUser;
import com.smartcrew.agent.api.user.domain.vo.ScUserVo;
import com.smartcrew.agent.core.page.PageQuery;

import java.util.List;
import java.util.Optional;

/**
 * 用户账户服务。
 */
public interface UserAccountService {

    /**
     * 创建本地注册用户。
     */
    ScUser createLocalUser(RegisterRequest request);

    /**
     * 校验用户名密码。
     */
    LoginSessionRecord authenticate(String username, String password);

    /**
     * 按用户名查询用户。
     */
    Optional<ScUser> findByUsername(String username);

    /**
     * 按 ID 查询用户。
     */
    Optional<ScUser> findById(Long userId);

    /**
     * 查询全部用户。
     */
    List<ScUserVo> listAll();

    /**
     * 按条件分页查询用户。
     */
    IPage<ScUserVo> listPage(PageQuery pageQuery, String keyword);

    /**
     * 更新用户状态。
     */
    ScUserVo updateStatus(Long userId, String status);

    /**
     * 创建平台默认用户。
     */
    ScUser createPlatformUser(String username, String displayName);

    /**
     * 保存用户实体。
     */
    ScUser save(ScUser user);
}
