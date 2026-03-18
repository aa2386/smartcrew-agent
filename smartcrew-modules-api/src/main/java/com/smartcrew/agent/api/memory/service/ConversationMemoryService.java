package com.smartcrew.agent.api.memory.service;

import java.util.Map;

/**
 * ConversationMemoryService 接口，定义该领域的业务能力与操作约定。
 */
public interface ConversationMemoryService {

    /**
     * 加载指定用户的会话记忆。
     *
     * @param userId 用户 ID。
     * @return 键值对形式的会话记忆。
     */
    Map<String, String> loadMemory(Long userId);

    /**
     * 追加或更新会话记忆项。
     *
     * @param userId 用户 ID。
     * @param key 记忆键。
     * @param value 记忆值。
     */
    void appendOrUpdate(Long userId, String key, String value);
}
