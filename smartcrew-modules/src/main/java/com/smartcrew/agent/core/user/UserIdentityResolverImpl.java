package com.smartcrew.agent.core.user;

import com.smartcrew.agent.api.user.domain.entity.ScUser;
import com.smartcrew.agent.api.user.domain.entity.ScUserIdentity;
import com.smartcrew.agent.api.user.mapper.ScUserIdentityMapper;
import com.smartcrew.agent.api.user.service.UserAccountService;
import com.smartcrew.agent.api.user.service.UserIdentityResolver;
import com.smartcrew.agent.common.exception.ServiceException;
import org.springframework.stereotype.Service;

/**
 * 第三方身份解析服务实现。
 */
@Service
public class UserIdentityResolverImpl implements UserIdentityResolver {

    /**
     * 身份映射 Mapper。
     */
    private final ScUserIdentityMapper scUserIdentityMapper;

    /**
     * 用户账户服务。
     */
    private final UserAccountService userAccountService;

    public UserIdentityResolverImpl(ScUserIdentityMapper scUserIdentityMapper,
                                    UserAccountService userAccountService) {
        this.scUserIdentityMapper = scUserIdentityMapper;
        this.userAccountService = userAccountService;
    }

    @Override
    public ScUser resolveOrCreatePlatformUser(String provider,
                                              String providerUserId,
                                              String tenantKey,
                                              String profileSnapshotJson) {
        String normalizedProvider = provider == null ? "UNKNOWN" : provider.trim().toUpperCase();
        String normalizedTenantKey = tenantKey == null ? "" : tenantKey.trim();
        ScUserIdentity identity = scUserIdentityMapper.selectByIdentity(
                normalizedProvider,
                providerUserId,
                normalizedTenantKey
        );
        if (identity != null) {
            return userAccountService.findById(identity.getUserId())
                    .orElseThrow(() -> new ServiceException(404, "身份映射对应的用户不存在"));
        }

        String candidateUsername = normalizedProvider.toLowerCase() + "_" + providerUserId;
        ScUser user = userAccountService.createPlatformUser(candidateUsername, normalizedProvider + "用户");
        ScUserIdentity newIdentity = new ScUserIdentity();
        newIdentity.setUserId(user.getId());
        newIdentity.setProvider(normalizedProvider);
        newIdentity.setProviderUserId(providerUserId);
        newIdentity.setTenantKey(normalizedTenantKey);
        newIdentity.setProfileSnapshotJson(profileSnapshotJson);
        newIdentity.setRemark("第三方平台自动绑定身份");
        scUserIdentityMapper.insert(newIdentity);
        return user;
    }
}
