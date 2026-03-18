package com.smartcrew.agent.core.memory;

import com.smartcrew.agent.api.memory.domain.request.UserPreferenceUpsertRequest;
import com.smartcrew.agent.api.memory.service.ConversationMemoryService;
import com.smartcrew.agent.api.memory.service.UserPreferenceService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会话记忆服务实现，基于用户偏好服务读写会话记忆。
 */
@Service
public class ConversationMemoryServiceImpl implements ConversationMemoryService {

    /**
     * 用户偏好服务。
     */
    private final UserPreferenceService userPreferenceService;

    /**
     * 构造 ConversationMemoryServiceImpl 所需的依赖对象。
     */
    public ConversationMemoryServiceImpl(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    /**
     * 加载指定用户的会话记忆。
     */
    @Override
    public Map<String, String> loadMemory(Long userId) {
        return userPreferenceService.listByUserId(userId).stream()
                .collect(Collectors.toMap(item -> item.getPrefKey(), item -> item.getPrefValue()));
    }

    /**
     * 追加或更新会话记忆。
     */
    @Override
    public void appendOrUpdate(Long userId, String key, String value) {
        UserPreferenceUpsertRequest request = new UserPreferenceUpsertRequest();
        request.setPrefKey(key);
        request.setPrefValue(value);
        request.setPrefType("TEXT");
        request.setSource("CONVERSATION");
        userPreferenceService.upsert(userId, request);
    }
}
