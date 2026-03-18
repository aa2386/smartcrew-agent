package com.smartcrew.agent.api.memory.service;

import java.util.Map;

/**
 * ??????????????????????????
 */
public interface ConversationMemoryService {

    Map<String, String> loadMemory(Long userId);

    void appendOrUpdate(Long userId, String key, String value);
}
