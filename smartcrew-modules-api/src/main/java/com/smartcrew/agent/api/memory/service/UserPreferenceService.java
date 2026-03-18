package com.smartcrew.agent.api.memory.service;

import com.smartcrew.agent.api.memory.domain.request.UserPreferenceUpsertRequest;
import com.smartcrew.agent.api.memory.domain.vo.UserPreferenceVo;

import java.util.List;
import java.util.Optional;

/**
 * UserPreferenceService 接口，定义该领域的业务能力与操作约定。
 */
public interface UserPreferenceService {

    List<UserPreferenceVo> listByUserId(Long userId);

    Optional<UserPreferenceVo> getByUserIdAndKey(Long userId, String prefKey);

    UserPreferenceVo upsert(Long userId, UserPreferenceUpsertRequest request);

    void delete(Long userId, String prefKey);
}
