package com.smartcrew.agent.api.auth.service;

import com.smartcrew.agent.api.auth.domain.model.LoginSessionRecord;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 登录态存储抽象，后续可替换为 Redis。
 */
public interface LoginSessionStore {

    /**
     * 保存登录态。
     */
    void save(String sessionId, LoginSessionRecord record, LocalDateTime expireAt);

    /**
     * 查询登录态。
     */
    Optional<LoginSessionRecord> get(String sessionId);

    /**
     * 使登录态失效。
     */
    void invalidate(String sessionId);
}
