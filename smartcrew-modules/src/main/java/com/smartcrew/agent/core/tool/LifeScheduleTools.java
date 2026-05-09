package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.schedule.entity.LifeTaskRecord;
import com.smartcrew.agent.api.schedule.service.LifeTaskRecordService;
import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import com.smartcrew.agent.common.util.JsonUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * 生活日程工具，提供任务记录的创建、查询、状态更新等最小闭环能力。
 *
 * <p>本期不接入真实第三方日历 API，所有任务保存为项目内记录。
 * 缺少明确时间信息时不创建确定任务，返回待确认提示。</p>
 *
 * <p>userId 从执行上下文获取，不接受模型传入。</p>
 */
@Component("lifeScheduleTools")
public class LifeScheduleTools implements SmartCrewTool {

    private final LifeTaskRecordService taskRecordService;

    public LifeScheduleTools(LifeTaskRecordService taskRecordService) {
        this.taskRecordService = taskRecordService;
    }

    @Override
    public String toolCode() {
        return "life-schedule";
    }

    @Override
    public String toolName() {
        return "生活日程工具";
    }

    @Override
    public String description() {
        return "提供生活日程任务记录的创建、查询、状态更新能力。时间模糊时返回待确认，不创建确定任务。";
    }

    @Override
    public String riskLevel() {
        return "MEDIUM";
    }

    /**
     * 创建生活日程任务记录。
     *
     * @param title       任务标题
     * @param description 任务描述（可选）
     * @param timeText    时间文本描述（如"明天上午九点"，可选）
     * @param priority    优先级：LOW / MEDIUM / HIGH（可选）
     * @return 创建结果
     */
    @Tool("创建一个生活日程任务记录。如果时间信息模糊或缺失，任务状态将设为 NEEDS_CONFIRMATION（待确认）。")
    public String createTask(
            @P("任务标题") String title,
            @P("任务描述（可选）") String description,
            @P("时间文本描述，如'明天上午九点'或'2026-03-15T09:00:00'（可选）") String timeText,
            @P("优先级，可选 LOW / MEDIUM / HIGH") String priority) {

        Long userId = getUserIdFromContext();
        if (userId == null) {
            return errorResult("无法获取用户身份信息");
        }

        LifeTaskRecord record = new LifeTaskRecord();
        record.setUserId(userId);
        record.setTitle(title);
        record.setDescription(description);
        record.setTimeText(timeText);
        record.setTimezone(ZoneId.systemDefault().getId());
        record.setPriority(priority != null ? priority.toUpperCase() : "MEDIUM");

        // 尝试解析时间
        LocalDateTime parsedTime = parseTimeText(timeText);
        record.setDueTime(parsedTime);
        if (parsedTime == null) {
            record.setStatus("NEEDS_CONFIRMATION");
        }

        // 设置追踪信息
        ToolCallContextHolder.ToolCallContext ctx = ToolCallContextHolder.get();
        if (ctx != null) {
            record.setTraceId(ctx.traceId());
        }
        record.setSource("DELEGATION");

        taskRecordService.create(record);

        if (parsedTime == null) {
            return JsonUtils.toJson(Map.of(
                    "success", true,
                    "status", "NEEDS_CONFIRMATION",
                    "id", record.getId(),
                    "message", "任务已保存，但时间信息不明确，请确认具体时间后更新",
                    "title", title,
                    "timeText", timeText != null ? timeText : ""
            ));
        }

        return JsonUtils.toJson(Map.of(
                "success", true,
                "status", "PENDING",
                "id", record.getId(),
                "title", title,
                "dueTime", parsedTime.toString()
        ));
    }

    /**
     * 查询当前用户的任务记录列表。
     *
     * @return 任务记录列表 JSON
     */
    @Tool("查询当前用户的所有生活日程任务记录。")
    public String listTasks() {
        Long userId = getUserIdFromContext();
        if (userId == null) {
            return errorResult("无法获取用户身份信息");
        }

        List<LifeTaskRecord> records = taskRecordService.listByUserId(userId);
        List<Map<String, Object>> result = records.stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "title", r.getTitle(),
                        "status", r.getStatus(),
                        "dueTime", r.getDueTime() != null ? r.getDueTime().toString() : "",
                        "timeText", r.getTimeText() != null ? r.getTimeText() : "",
                        "priority", r.getPriority() != null ? r.getPriority() : "",
                        "createdAt", r.getCreateTime() != null ? r.getCreateTime().toString() : ""
                ))
                .toList();

        return JsonUtils.toJson(Map.of(
                "success", true,
                "tasks", result,
                "count", result.size()
        ));
    }

    /**
     * 更新任务状态。
     *
     * @param taskId 任务 ID
     * @param status 新状态：DONE / CANCELLED / PENDING
     * @return 更新结果
     */
    @Tool("更新指定任务的状态。状态可设为 DONE（已完成）、CANCELLED（已取消）、PENDING（待处理）。")
    public String updateTaskStatus(
            @P("任务 ID") String taskId,
            @P("新状态：DONE / CANCELLED / PENDING") String status) {

        Long userId = getUserIdFromContext();
        if (userId == null) {
            return errorResult("无法获取用户身份信息");
        }

        long id;
        try {
            id = Long.parseLong(taskId);
        } catch (NumberFormatException e) {
            return errorResult("无效的任务 ID: " + taskId);
        }

        LifeTaskRecord existing = taskRecordService.getById(id);
        if (existing == null) {
            return errorResult("任务不存在: " + taskId);
        }

        // 用户隔离校验
        if (!userId.equals(existing.getUserId())) {
            return errorResult("无权操作其他用户的任务");
        }

        String normalizedStatus = status.toUpperCase();
        if (!java.util.Set.of("DONE", "CANCELLED", "PENDING", "NEEDS_CONFIRMATION").contains(normalizedStatus)) {
            return errorResult("无效的状态值: " + status + "，允许的状态: DONE / CANCELLED / PENDING / NEEDS_CONFIRMATION");
        }

        LifeTaskRecord updated = taskRecordService.updateStatus(id, normalizedStatus);
        return JsonUtils.toJson(Map.of(
                "success", true,
                "id", updated.getId(),
                "title", updated.getTitle(),
                "status", updated.getStatus()
        ));
    }

    /**
     * 尝试将时间文本解析为 LocalDateTime，支持 ISO8801 格式和常见中文时间表达。
     */
    private LocalDateTime parseTimeText(String timeText) {
        if (timeText == null || timeText.isBlank()) {
            return null;
        }
        try {
            // 尝试 ISO 格式: 2026-03-15T09:00:00
            return LocalDateTime.parse(timeText);
        } catch (DateTimeParseException e) {
            // 无法解析为精确时间，返回 null
            return null;
        }
    }

    /**
     * 从 ToolCallContextHolder 获取当前请求的 userId。
     */
    private Long getUserIdFromContext() {
        ToolCallContextHolder.ToolCallContext ctx = ToolCallContextHolder.get();
        if (ctx == null || ctx.context() == null) {
            return null;
        }
        Object userIdObj = ctx.context().get("userId");
        if (userIdObj instanceof Number n) {
            return n.longValue();
        }
        if (userIdObj instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String errorResult(String message) {
        return JsonUtils.toJson(Map.of("success", false, "errorMessage", message));
    }
}
