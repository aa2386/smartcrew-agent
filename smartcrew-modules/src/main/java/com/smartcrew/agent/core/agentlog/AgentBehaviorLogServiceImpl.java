package com.smartcrew.agent.core.agentlog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcrew.agent.api.agentlog.entity.AgentBehaviorLog;
import com.smartcrew.agent.api.agentlog.mapper.AgentBehaviorLogMapper;
import com.smartcrew.agent.api.agentlog.service.AgentBehaviorLogService;
import com.smartcrew.agent.common.util.JsonUtils;
import com.smartcrew.agent.common.util.StringUtils;
import com.smartcrew.agent.core.page.PageQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Agent 行为日志服务实现。
 *
 * <p>日志写入失败不阻断主链路，仅记录应用日志（SLF4J）。
 * 查询支持按 traceId、sessionId、userId、agentCode、eventType、eventStatus、时间范围分页。</p>
 */
@Service
public class AgentBehaviorLogServiceImpl implements AgentBehaviorLogService {

    private static final Logger log = LoggerFactory.getLogger(AgentBehaviorLogServiceImpl.class);

    private final AgentBehaviorLogMapper mapper;

    public AgentBehaviorLogServiceImpl(AgentBehaviorLogMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 敏感 key 黑名单，包含这些 key 的数据在写入 metadata 前会被脱敏或移除。
     */
    private static final java.util.Set<String> SENSITIVE_KEYS = java.util.Set.of(
            "password", "passwd", "secret", "token", "apiKey", "api_key",
            "idCard", "id_card", "bankCard", "bank_card", "creditCard", "credit_card",
            "ssn", "phone", "mobile", "email", "address"
    );

    @Override
    public void write(AgentBehaviorLog entry) {
        try {
            // 脱敏 metadata
            if (StringUtils.isNotBlank(entry.getMetadataJson())) {
                entry.setMetadataJson(sanitizeMetadata(entry.getMetadataJson()));
            }
            // 脱敏 eventSummary
            if (StringUtils.isNotBlank(entry.getEventSummary())) {
                entry.setEventSummary(sanitizeSummary(entry.getEventSummary()));
            }
            entry.setCreateTime(LocalDateTime.now());
            mapper.insert(entry);
        } catch (Exception e) {
            log.warn("Agent 行为日志写入失败 (traceId={}, eventType={}): {}",
                    entry.getTraceId(), entry.getEventType(), e.getMessage());
        }
    }

    @Override
    public Page<AgentBehaviorLog> query(String traceId, String sessionId, Long userId,
                                         String agentCode, String eventType, String eventStatus,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         PageQuery pageQuery) {
        LambdaQueryWrapper<AgentBehaviorLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(traceId)) {
            wrapper.eq(AgentBehaviorLog::getTraceId, traceId);
        }
        if (StringUtils.isNotBlank(sessionId)) {
            wrapper.eq(AgentBehaviorLog::getSessionId, sessionId);
        }
        if (userId != null) {
            wrapper.eq(AgentBehaviorLog::getUserId, userId);
        }
        if (StringUtils.isNotBlank(agentCode)) {
            wrapper.eq(AgentBehaviorLog::getAgentCode, agentCode);
        }
        if (StringUtils.isNotBlank(eventType)) {
            wrapper.eq(AgentBehaviorLog::getEventType, eventType);
        }
        if (StringUtils.isNotBlank(eventStatus)) {
            wrapper.eq(AgentBehaviorLog::getEventStatus, eventStatus);
        }
        if (startTime != null) {
            wrapper.ge(AgentBehaviorLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AgentBehaviorLog::getCreateTime, endTime);
        }
        wrapper.orderByDesc(AgentBehaviorLog::getCreateTime);

        Page<AgentBehaviorLog> page = mapper.selectPage(pageQuery.build(), wrapper);
        return page;
    }

    @Override
    public List<AgentBehaviorLog> queryByTraceId(String traceId) {
        return mapper.selectList(new LambdaQueryWrapper<AgentBehaviorLog>()
                .eq(AgentBehaviorLog::getTraceId, traceId)
                .orderByAsc(AgentBehaviorLog::getCreateTime));
    }

    @Override
    public AgentBehaviorLog buildLog(String traceId, Long userId, String sessionId,
                                      String agentCode, String eventType, String eventStatus,
                                      String eventSummary, Map<String, Object> metadata) {
        AgentBehaviorLog entry = new AgentBehaviorLog();
        entry.setTraceId(traceId);
        entry.setUserId(userId);
        entry.setSessionId(sessionId);
        entry.setAgentCode(agentCode);
        entry.setEventType(eventType);
        entry.setEventStatus(eventStatus);
        entry.setEventSummary(eventSummary);
        entry.setMetadataJson(metadata != null && !metadata.isEmpty() ? sanitizeMetadata(JsonUtils.toJson(metadata)) : null);
        return entry;
    }

    /**
     * 脱敏 metadata_json，移除敏感 key。
     */
    private String sanitizeMetadata(String json) {
        if (StringUtils.isBlank(json)) return json;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = JsonUtils.parse(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            boolean modified = false;
            for (String key : map.keySet()) {
                String lower = key.toLowerCase();
                for (String sensitive : SENSITIVE_KEYS) {
                    if (lower.contains(sensitive)) {
                        map.put(key, "***");
                        modified = true;
                        break;
                    }
                }
            }
            return modified ? JsonUtils.toJson(map) : json;
        } catch (Exception e) {
            // 解析失败时返回摘要，不保存原文
            return "{\"sanitized\":true}";
        }
    }

    /**
     * 脱敏摘要文本，截断过长的内容（防止完整 Prompt 泄露）。
     */
    private String sanitizeSummary(String summary) {
        if (summary == null) return null;
        // 限制摘要长度，防止完整 Prompt 被保存
        int maxLen = 200;
        if (summary.length() > maxLen) {
            return summary.substring(0, maxLen) + "...";
        }
        return summary;
    }
}
