package com.smartcrew.agent.core.agent.service;

import com.smartcrew.agent.common.util.StringUtils;

/**
 * 初始智能体会话记忆标识，用于编码和解码会话的唯一记忆 ID。
 *
 * <p>记忆 ID 格式为 {@code agentCode::userId::sessionId}，
 * 用于 LangChain4j 会话管理及持久化存储的键值映射。</p>
 */
public record InitialAgentMemoryId(String agentCode, Long userId, String sessionId) {

    /** 分隔符常量。 */
    private static final String DELIMITER = "::";

    /**
     * 将各组件编码为字符串形式的记忆 ID。
     *
     * @param agentCode 智能体编码
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 编码后的记忆 ID 字符串
     */
    public static String encode(String agentCode, Long userId, String sessionId) {
        return agentCode + DELIMITER + userId + DELIMITER + sessionId;
    }

    /**
     * 将字符串形式的记忆 ID 解析为结构化对象。
     *
     * @param memoryId 记忆 ID 字符串或对象
     * @return 解析后的记忆 ID 结构化对象
     * @throws IllegalArgumentException 当格式非法时抛出
     */
    public static InitialAgentMemoryId parse(Object memoryId) {
        String text = String.valueOf(memoryId);
        String[] parts = text.split(DELIMITER, 3);
        if (parts.length != 3 || StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1]) || StringUtils.isBlank(parts[2])) {
            throw new IllegalArgumentException("非法 memoryId: " + text);
        }
        return new InitialAgentMemoryId(parts[0], Long.parseLong(parts[1]), parts[2]);
    }

    /**
     * 生成持久化存储使用的会话 ID。
     *
     * @return 格式为 {@code agentCode::sessionId} 的持久化会话 ID
     */
    public String persistedSessionId() {
        return agentCode + DELIMITER + sessionId;
    }
}
