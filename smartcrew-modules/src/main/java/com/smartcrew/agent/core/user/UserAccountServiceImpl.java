package com.smartcrew.agent.core.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcrew.agent.api.auth.domain.model.LoginSessionRecord;
import com.smartcrew.agent.api.auth.domain.request.RegisterRequest;
import com.smartcrew.agent.api.user.domain.entity.ScUser;
import com.smartcrew.agent.api.user.domain.entity.ScUserIdentity;
import com.smartcrew.agent.api.user.domain.vo.ScUserVo;
import com.smartcrew.agent.api.user.mapper.ScUserIdentityMapper;
import com.smartcrew.agent.api.user.mapper.ScUserMapper;
import com.smartcrew.agent.api.user.service.UserAccountService;
import com.smartcrew.agent.common.exception.ServiceException;
import com.smartcrew.agent.core.page.PageQuery;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户账户服务实现。
 */
@Service
public class UserAccountServiceImpl implements UserAccountService {

    /**
     * 密码编码器。
     */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 用户 Mapper。
     */
    private final ScUserMapper scUserMapper;

    /**
     * 身份映射 Mapper。
     */
    private final ScUserIdentityMapper scUserIdentityMapper;

    public UserAccountServiceImpl(ScUserMapper scUserMapper, ScUserIdentityMapper scUserIdentityMapper) {
        this.scUserMapper = scUserMapper;
        this.scUserIdentityMapper = scUserIdentityMapper;
    }

    @Override
    public ScUser createLocalUser(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (scUserMapper.selectByUsername(username) != null) {
            throw new ServiceException(400, "用户名已存在");
        }
        ScUser user = new ScUser();
        user.setUsername(username);
        user.setPasswordHash(PASSWORD_ENCODER.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName().trim());
        user.setRole("USER");
        user.setStatus("ENABLED");
        user.setRemark("本地注册用户");
        scUserMapper.insert(user);

        ScUserIdentity identity = new ScUserIdentity();
        identity.setUserId(user.getId());
        identity.setProvider("LOCAL");
        identity.setProviderUserId(user.getUsername());
        identity.setTenantKey("");
        identity.setRemark("本地注册身份");
        scUserIdentityMapper.insert(identity);
        return user;
    }

    @Override
    public LoginSessionRecord authenticate(String username, String password) {
        ScUser user = scUserMapper.selectByUsername(username.trim());
        if (user == null) {
            throw new ServiceException(400, "用户名或密码错误");
        }
        if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
            throw new ServiceException(403, "当前账号已被禁用");
        }
        if (user.getPasswordHash() == null || !PASSWORD_ENCODER.matches(password, user.getPasswordHash())) {
            throw new ServiceException(400, "用户名或密码错误");
        }
        user.setLastLoginAt(LocalDateTime.now());
        scUserMapper.updateById(user);
        return LoginSessionRecord.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    @Override
    public Optional<ScUser> findByUsername(String username) {
        return Optional.ofNullable(scUserMapper.selectByUsername(username));
    }

    @Override
    public Optional<ScUser> findById(Long userId) {
        return Optional.ofNullable(scUserMapper.selectById(userId));
    }

    @Override
    public List<ScUserVo> listAll() {
        return scUserMapper.selectList(Wrappers.lambdaQuery(ScUser.class).orderByDesc(ScUser::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    public IPage<ScUserVo> listPage(PageQuery pageQuery, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        LambdaQueryWrapper<ScUser> queryWrapper = Wrappers.lambdaQuery(ScUser.class)
                .orderByDesc(ScUser::getId);
        if (!normalizedKeyword.isBlank()) {
            queryWrapper.and(wrapper -> wrapper.like(ScUser::getUsername, normalizedKeyword)
                    .or()
                    .like(ScUser::getDisplayName, normalizedKeyword)
                    .or()
                    .like(ScUser::getRole, normalizedKeyword)
                    .or()
                    .like(ScUser::getStatus, normalizedKeyword));
        }
        Page<ScUser> page = scUserMapper.selectPage(pageQuery.build(), queryWrapper);
        Page<ScUserVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVo).toList());
        return result;
    }

    @Override
    public ScUserVo updateStatus(Long userId, String status) {
        ScUser user = scUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }
        user.setStatus(status);
        scUserMapper.updateById(user);
        return toVo(user);
    }

    @Override
    public ScUser createPlatformUser(String username, String displayName) {
        ScUser user = new ScUser();
        user.setUsername(uniqueUsername(username));
        user.setDisplayName(displayName);
        user.setRole("USER");
        user.setStatus("ENABLED");
        user.setRemark("第三方平台自动创建用户");
        scUserMapper.insert(user);
        return user;
    }

    @Override
    public ScUser save(ScUser user) {
        if (user.getId() == null) {
            scUserMapper.insert(user);
        } else {
            scUserMapper.updateById(user);
        }
        return user;
    }

    /**
     * 生成唯一用户名。
     */
    private String uniqueUsername(String rawUsername) {
        String sanitized = rawUsername == null ? "user" : rawUsername.trim().toLowerCase()
                .replaceAll("[^a-z0-9_\\-]", "_");
        if (sanitized.isBlank()) {
            sanitized = "user";
        }
        if (sanitized.length() > 48) {
            sanitized = sanitized.substring(0, 48);
        }
        String candidate = sanitized;
        int index = 1;
        while (scUserMapper.selectByUsername(candidate) != null) {
            candidate = sanitized + "_" + index++;
        }
        return candidate;
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
