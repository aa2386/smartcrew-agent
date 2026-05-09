package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.memory.domain.request.UserPreferenceUpsertRequest;
import com.smartcrew.agent.api.memory.domain.vo.UserPreferenceVo;
import com.smartcrew.agent.api.memory.service.ConversationMemoryService;
import com.smartcrew.agent.api.memory.service.UserPreferenceService;
import com.smartcrew.agent.api.schedule.service.LifeTaskRecordService;
import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import com.smartcrew.agent.common.util.JsonUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 记忆工具，提供偏好读写、会话记忆管理、任务记录查询等记忆能力。
 *
 * <p>所有操作按 userId 隔离，不接受模型传入的 userId。
 * 敏感信息（密码、密钥、身份证等）写入将被拒绝。</p>
 */
@Component("memoryTools")
public class MemoryTools implements SmartCrewTool {

    private final UserPreferenceService preferenceService;
    private final ConversationMemoryService memoryService;
    private final LifeTaskRecordService taskRecordService;

    /**
     * 敏感 key 前缀/关键词，写入时拒绝。
     */
    private static final java.util.Set<String> SENSITIVE_KEY_PATTERNS = java.util.Set.of(
            "password", "passwd", "secret", "token", "api", "key",
            "id_card", "idCard", "bank_card", "bankCard", "credit_card", "creditCard",
            "ssn", "credential", "cvv", "pin"
    );

    public MemoryTools(UserPreferenceService preferenceService,
                        ConversationMemoryService memoryService,
                        LifeTaskRecordService taskRecordService) {
        this.preferenceService = preferenceService;
        this.memoryService = memoryService;
        this.taskRecordService = taskRecordService;
    }

    @Override
    public String toolCode() {
        return "memory";
    }

    @Override
    public String toolName() {
        return "记忆工具";
    }

    @Override
    public String description() {
        return "提供用户偏好、会话记忆、任务记录的读写能力，所有操作按 userId 隔离。";
    }

    @Override
    public String riskLevel() {
        return "HIGH";
    }

    /**
     * 读取用户指定 key 的偏好。
     */
    @Tool("读取用户的偏好设置，传入偏好键名获取对应的值。")
    public String readPreference(@P("偏好键名，如 language、nickname、tone") String prefKey) {
        Long userId = getUserIdFromContext();
        if (userId == null) return errorResult("无法获取用户身份信息");

        Optional<UserPreferenceVo> opt = preferenceService.getByUserIdAndKey(userId, prefKey);
        return opt.map(vo -> JsonUtils.toJson(Map.of(
                "success", true,
                "prefKey", vo.getPrefKey(),
                "prefValue", vo.getPrefValue(),
                "prefType", vo.getPrefType()
        ))).orElseGet(() -> JsonUtils.toJson(Map.of(
                "success", true,
                "prefKey", prefKey,
                "prefValue", "",
                "message", "未找到该偏好设置"
        )));
    }

    /**
     * 写入或更新用户偏好。
     */
    @Tool("写入或更新用户的偏好设置。敏感信息（密码、密钥、身份证、银行卡等）将被拒绝。")
    public String writePreference(
            @P("偏好键名") String prefKey,
            @P("偏好值") String prefValue) {

        Long userId = getUserIdFromContext();
        if (userId == null) return errorResult("无法获取用户身份信息");

        // 敏感信息检查
        if (isSensitiveKey(prefKey) || isSensitiveValue(prefValue)) {
            return errorResult("写入被拒绝：偏好键或值包含敏感信息，不允许保存");
        }

        UserPreferenceUpsertRequest request = new UserPreferenceUpsertRequest();
        request.setPrefKey(prefKey);
        request.setPrefValue(prefValue);
        request.setPrefType("TEXT");
        request.setSource("AGENT_MEMORY");

        UserPreferenceVo vo = preferenceService.upsert(userId, request);
        return JsonUtils.toJson(Map.of(
                "success", true,
                "prefKey", vo.getPrefKey(),
                "prefValue", vo.getPrefValue()
        ));
    }

    /**
     * 列出用户所有偏好。
     */
    @Tool("列出当前用户已设置的所有偏好。")
    public String listPreferences() {
        Long userId = getUserIdFromContext();
        if (userId == null) return errorResult("无法获取用户身份信息");

        List<UserPreferenceVo> prefs = preferenceService.listByUserId(userId);
        List<Map<String, Object>> result = prefs.stream()
                .map(p -> Map.<String, Object>of(
                        "prefKey", p.getPrefKey(),
                        "prefValue", p.getPrefValue(),
                        "prefType", p.getPrefType()
                ))
                .toList();

        return JsonUtils.toJson(Map.of(
                "success", true,
                "preferences", result,
                "count", result.size()
        ));
    }

    /**
     * 删除指定偏好。
     */
    @Tool("删除用户的指定偏好设置。")
    public String deletePreference(@P("偏好键名") String prefKey) {
        Long userId = getUserIdFromContext();
        if (userId == null) return errorResult("无法获取用户身份信息");

        preferenceService.delete(userId, prefKey);
        return JsonUtils.toJson(Map.of(
                "success", true,
                "prefKey", prefKey,
                "message", "偏好已删除"
        ));
    }

    /**
     * 写入或更新会话记忆。
     */
    @Tool("追加或更新用户的会话记忆项。记忆键值对会被存储在用户偏好中。")
    public String writeMemory(
            @P("记忆键名") String key,
            @P("记忆值") String value) {

        Long userId = getUserIdFromContext();
        if (userId == null) return errorResult("无法获取用户身份信息");

        if (isSensitiveKey(key) || isSensitiveValue(value)) {
            return errorResult("写入被拒绝：记忆包含敏感信息，不允许保存");
        }

        memoryService.appendOrUpdate(userId, key, value);
        return JsonUtils.toJson(Map.of(
                "success", true,
                "key", key,
                "message", "记忆已写入"
        ));
    }

    /**
     * 加载用户所有会话记忆。
     */
    @Tool("加载当前用户的所有会话记忆，返回键值对映射。")
    public String loadMemory() {
        Long userId = getUserIdFromContext();
        if (userId == null) return errorResult("无法获取用户身份信息");

        Map<String, String> memory = memoryService.loadMemory(userId);
        return JsonUtils.toJson(Map.of(
                "success", true,
                "memory", memory,
                "count", memory.size()
        ));
    }

    /**
     * 查询当前用户的任务记录。
     */
    @Tool("查询当前用户保存的生活日程任务记录，用于记忆 Agent 在需要时读取任务历史。")
    public String listTaskRecords() {
        Long userId = getUserIdFromContext();
        if (userId == null) return errorResult("无法获取用户身份信息");

        var records = taskRecordService.listByUserId(userId);
        var result = records.stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "title", r.getTitle(),
                        "status", r.getStatus(),
                        "dueTime", r.getDueTime() != null ? r.getDueTime().toString() : "",
                        "priority", r.getPriority() != null ? r.getPriority() : ""
                ))
                .toList();

        return JsonUtils.toJson(Map.of(
                "success", true,
                "tasks", result,
                "count", result.size()
        ));
    }

    /**
     * 检查 key 是否包含敏感关键词。
     */
    private boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return SENSITIVE_KEY_PATTERNS.stream().anyMatch(lower::contains);
    }

    /**
     * 检查 value 是否可能包含敏感信息（简单启发式检查）。
     */
    private boolean isSensitiveValue(String value) {
        if (value == null) return false;
        // 检查是否包含常见的敏感格式
        if (value.matches(".*\\d{15,19}.*")) return true; // 可能为身份证/银行卡号
        if (value.length() > 100 && value.contains("eyJ")) return true; // 可能为 JWT token
        return false;
    }

    private Long getUserIdFromContext() {
        ToolCallContextHolder.ToolCallContext ctx = ToolCallContextHolder.get();
        if (ctx == null || ctx.context() == null) return null;
        Object userIdObj = ctx.context().get("userId");
        if (userIdObj instanceof Number n) return n.longValue();
        if (userIdObj instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private String errorResult(String message) {
        return JsonUtils.toJson(Map.of("success", false, "errorMessage", message));
    }
}
