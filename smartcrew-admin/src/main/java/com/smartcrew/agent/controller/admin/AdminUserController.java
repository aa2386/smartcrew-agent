package com.smartcrew.agent.controller.admin;

import com.smartcrew.agent.api.admin.domain.request.UserIdentityBindRequest;
import com.smartcrew.agent.api.admin.domain.request.UserStatusUpdateRequest;
import com.smartcrew.agent.api.user.domain.entity.ScUser;
import com.smartcrew.agent.api.user.domain.vo.ScUserIdentityVo;
import com.smartcrew.agent.api.user.domain.vo.ScUserVo;
import com.smartcrew.agent.api.user.service.UserAccountService;
import com.smartcrew.agent.api.user.service.UserIdentityService;
import com.smartcrew.agent.common.domain.R;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.core.page.PageQuery;
import com.smartcrew.agent.core.page.TableDataInfo;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台用户管理控制器。
 */
@RestController
@RequestMapping("/api/admin/users")
@ConditionalOnProperty(prefix = "smartcrew.api.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserController {

    /**
     * 用户账户服务。
     */
    private final UserAccountService userAccountService;

    /**
     * 身份映射服务。
     */
    private final UserIdentityService userIdentityService;

    public AdminUserController(UserAccountService userAccountService,
                               UserIdentityService userIdentityService) {
        this.userAccountService = userAccountService;
        this.userIdentityService = userIdentityService;
    }

    @GetMapping
    public TableDataInfo<ScUserVo> list(PageQuery pageQuery,
                                        @RequestParam(value = "keyword", required = false) String keyword) {
        if (pageQuery.hasPaging()) {
            return TableDataInfo.build(userAccountService.listPage(pageQuery, keyword));
        }
        return TableDataInfo.build(userAccountService.listAll());
    }

    @GetMapping("/{id}")
    public R<ScUserVo> detail(@PathVariable("id") Long id) {
        ScUser user = userAccountService.findById(id)
                .orElseThrow(() -> new ServiceException(404, "用户不存在"));
        return R.ok(toVo(user));
    }

    @PutMapping("/{id}/status")
    public R<ScUserVo> updateStatus(@PathVariable("id") Long id,
                                    @Valid @RequestBody UserStatusUpdateRequest request) {
        return R.ok(userAccountService.updateStatus(id, request.getStatus()));
    }

    @GetMapping("/{id}/identities")
    public R<java.util.List<ScUserIdentityVo>> listIdentities(@PathVariable("id") Long id) {
        return R.ok(userIdentityService.listByUserId(id));
    }

    @PostMapping("/{id}/identities")
    public R<ScUserIdentityVo> bindIdentity(@PathVariable("id") Long id,
                                            @Valid @RequestBody UserIdentityBindRequest request) {
        return R.ok(userIdentityService.bind(id, request));
    }

    @DeleteMapping("/{id}/identities/{identityId}")
    public R<Void> unbindIdentity(@PathVariable("id") Long id, @PathVariable("identityId") Long identityId) {
        userIdentityService.unbind(id, identityId);
        return R.ok("解绑成功", null);
    }

    /**
     * 转换为用户视图对象。
     */
    private ScUserVo toVo(ScUser user) {
        return ScUserVo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
