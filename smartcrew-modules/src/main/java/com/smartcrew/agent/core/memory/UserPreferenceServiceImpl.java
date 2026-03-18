package com.smartcrew.agent.core.memory;

import com.smartcrew.agent.api.memory.domain.entity.UserPreference;
import com.smartcrew.agent.api.memory.domain.request.UserPreferenceUpsertRequest;
import com.smartcrew.agent.api.memory.domain.vo.UserPreferenceVo;
import com.smartcrew.agent.api.memory.mapper.UserPreferenceMapper;
import com.smartcrew.agent.api.memory.service.UserPreferenceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 用户偏好服务实现，负责用户偏好的查询、写入和删除。
 */
@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    /**
     * 用户偏好数据访问对象。
     */
    private final UserPreferenceMapper userPreferenceMapper;

    /**
     * 构造 UserPreferenceServiceImpl 所需的依赖对象。
     */
    public UserPreferenceServiceImpl(UserPreferenceMapper userPreferenceMapper) {
        this.userPreferenceMapper = userPreferenceMapper;
    }

    /**
     * 查询指定用户的全部偏好配置。
     */
    @Override
    public List<UserPreferenceVo> listByUserId(Long userId) {
        return userPreferenceMapper.selectByUserId(userId).stream()
                .map(this::toVo)
                .toList();
    }

    /**
     * 查询指定用户下某个偏好键的配置。
     */
    @Override
    public Optional<UserPreferenceVo> getByUserIdAndKey(Long userId, String prefKey) {
        return Optional.ofNullable(userPreferenceMapper.selectByUserIdAndPrefKey(userId, prefKey))
                .map(this::toVo);
    }

    /**
     * 新增或更新数据。
     */
    @Override
    public UserPreferenceVo upsert(Long userId, UserPreferenceUpsertRequest request) {
        UserPreference entity = userPreferenceMapper.selectByUserIdAndPrefKey(userId, request.getPrefKey());
        if (entity == null) {
            entity = new UserPreference();
            entity.setUserId(userId);
            entity.setPrefKey(request.getPrefKey());
        }
        entity.setPrefValue(request.getPrefValue());
        entity.setPrefType(request.getPrefType());
        entity.setSource(request.getSource());
        if (entity.getId() == null) {
            userPreferenceMapper.insert(entity);
        } else {
            userPreferenceMapper.updateById(entity);
        }
        return toVo(entity);
    }

    /**
     * 删除指定数据。
     */
    @Override
    public void delete(Long userId, String prefKey) {
        userPreferenceMapper.deleteByUserIdAndPrefKey(userId, prefKey);
    }

    /**
     * 将用户偏好实体转换为视图对象。
     */
    private UserPreferenceVo toVo(UserPreference entity) {
        return UserPreferenceVo.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .prefKey(entity.getPrefKey())
                .prefValue(entity.getPrefValue())
                .prefType(entity.getPrefType())
                .source(entity.getSource())
                .build();
    }
}
