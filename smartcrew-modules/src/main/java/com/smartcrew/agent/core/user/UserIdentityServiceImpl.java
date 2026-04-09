package com.smartcrew.agent.core.user;

import com.smartcrew.agent.api.admin.domain.request.UserIdentityBindRequest;
import com.smartcrew.agent.api.user.domain.entity.ScUser;
import com.smartcrew.agent.api.user.domain.entity.ScUserIdentity;
import com.smartcrew.agent.api.user.domain.vo.ScUserIdentityVo;
import com.smartcrew.agent.api.user.mapper.ScUserIdentityMapper;
import com.smartcrew.agent.api.user.mapper.ScUserMapper;
import com.smartcrew.agent.api.user.service.UserIdentityService;
import com.smartcrew.agent.common.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户身份管理服务实现。
 */
@Service
public class UserIdentityServiceImpl implements UserIdentityService {

    /**
     * 身份映射 Mapper。
     */
    private final ScUserIdentityMapper scUserIdentityMapper;

    /**
     * 用户 Mapper。
     */
    private final ScUserMapper scUserMapper;

    public UserIdentityServiceImpl(ScUserIdentityMapper scUserIdentityMapper, ScUserMapper scUserMapper) {
        this.scUserIdentityMapper = scUserIdentityMapper;
        this.scUserMapper = scUserMapper;
    }

    @Override
    public List<ScUserIdentityVo> listByUserId(Long userId) {
        return scUserIdentityMapper.selectByUserId(userId).stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public ScUserIdentityVo bind(Long userId, UserIdentityBindRequest request) {
        ScUser user = scUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }
        String provider = normalizeProvider(request.getProvider());
        String tenantKey = normalizeTenantKey(request.getTenantKey());
        ScUserIdentity existedIdentity = scUserIdentityMapper.selectByIdentity(
                provider,
                request.getProviderUserId(),
                tenantKey
        );
        if (existedIdentity != null && !userId.equals(existedIdentity.getUserId())) {
            throw new ServiceException(400, "该第三方身份已绑定到其他用户");
        }
        ScUserIdentity identity = existedIdentity == null ? new ScUserIdentity() : existedIdentity;
        identity.setUserId(userId);
        identity.setProvider(provider);
        identity.setProviderUserId(request.getProviderUserId());
        identity.setTenantKey(tenantKey);
        identity.setProfileSnapshotJson(request.getProfileSnapshotJson());
        if (identity.getId() == null) {
            scUserIdentityMapper.insert(identity);
        } else {
            scUserIdentityMapper.updateById(identity);
        }
        return toVo(identity);
    }

    @Override
    public void unbind(Long userId, Long identityId) {
        ScUserIdentity identity = scUserIdentityMapper.selectById(identityId);
        if (identity == null || !userId.equals(identity.getUserId())) {
            throw new ServiceException(404, "身份映射不存在");
        }
        scUserIdentityMapper.deleteById(identityId);
    }

    /**
     * 统一提供方编码。
     */
    private String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toUpperCase();
    }

    /**
     * 统一租户编码。
     */
    private String normalizeTenantKey(String tenantKey) {
        return tenantKey == null ? "" : tenantKey.trim();
    }

    /**
     * 转换为身份映射视图对象。
     */
    private ScUserIdentityVo toVo(ScUserIdentity identity) {
        return ScUserIdentityVo.builder()
                .id(identity.getId())
                .userId(identity.getUserId())
                .provider(identity.getProvider())
                .providerUserId(identity.getProviderUserId())
                .tenantKey(identity.getTenantKey())
                .profileSnapshotJson(identity.getProfileSnapshotJson())
                .build();
    }
}
