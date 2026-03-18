package com.smartcrew.agent.api.memory.service;

import com.smartcrew.agent.api.memory.domain.request.UserPreferenceUpsertRequest;
import com.smartcrew.agent.api.memory.domain.vo.UserPreferenceVo;

import java.util.List;
import java.util.Optional;

/**
 * UserPreferenceService 接口，定义该领域的业务能力与操作约定。
 */
public interface UserPreferenceService {

    /**
     * 查询指定用户的偏好列表。
     *
     * @param userId 用户 ID。
     * @return 结果列表。
     */
    List<UserPreferenceVo> listByUserId(Long userId);

    /**
     * 查询指定用户的目标偏好。
     *
     * @param userId 用户 ID。
     * @param prefKey 偏好键。
     * @return 匹配结果；未找到时返回空 `Optional`。
     */
    Optional<UserPreferenceVo> getByUserIdAndKey(Long userId, String prefKey);

    /**
     * 新增或更新用户偏好。
     *
     * @param userId 用户 ID。
     * @param request 请求参数。
     * @return 新增或更新后的偏好信息。
     */
    UserPreferenceVo upsert(Long userId, UserPreferenceUpsertRequest request);

    /**
     * 删除指定用户的偏好项。
     *
     * @param userId 用户 ID。
     * @param prefKey 偏好键。
     */
    void delete(Long userId, String prefKey);
}
