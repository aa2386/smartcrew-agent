package com.smartcrew.agent.api.memory.service;

import java.util.Map;

/**
 * ConversationMemoryService 接口，定义该领域的业务能力与操作约定。
 */
public interface ConversationMemoryService {

    Map<String, String> loadMemory(Long userId);

    void appendOrUpdate(Long userId, String key, String value);
}
