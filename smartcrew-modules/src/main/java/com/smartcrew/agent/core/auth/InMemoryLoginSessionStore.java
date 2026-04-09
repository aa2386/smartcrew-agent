package com.smartcrew.agent.core.auth;

import com.smartcrew.agent.api.auth.domain.model.LoginSessionRecord;
import com.smartcrew.agent.api.auth.service.LoginSessionStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的登录态存储实现。
 */
@Service
public class InMemoryLoginSessionStore implements LoginSessionStore {

    /**
     * 登录态缓存。
     */
    private final Map<String, LoginSessionRecord> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(String sessionId, LoginSessionRecord record, LocalDateTime expireAt) {
        record.setExpireAt(expireAt);
        sessions.put(sessionId, record);
    }

    @Override
    public Optional<LoginSessionRecord> get(String sessionId) {
        LoginSessionRecord record = sessions.get(sessionId);
        if (record == null) {
            return Optional.empty();
        }
        if (record.getExpireAt() != null && record.getExpireAt().isBefore(LocalDateTime.now())) {
            sessions.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    @Override
    public void invalidate(String sessionId) {
        sessions.remove(sessionId);
    }
}
