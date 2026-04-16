package com.smartcrew.agent.core.decision;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smartcrew.agent.api.decision.domain.request.DecisionPlanRequest;
import com.smartcrew.agent.api.decision.domain.vo.DecisionPlanResponse;
import com.smartcrew.agent.api.decision.domain.vo.DecisionStep;
import com.smartcrew.agent.api.decision.domain.vo.PlannedToolCall;
import com.smartcrew.agent.api.decision.service.DecisionEngine;
import com.smartcrew.agent.api.tool.domain.model.ResolvedToolDefinition;
import com.smartcrew.agent.api.tool.domain.model.ToolActionMetadata;
import com.smartcrew.agent.api.tool.domain.model.ToolActionParameter;
import com.smartcrew.agent.common.util.JsonUtils;
import com.smartcrew.agent.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 结构化 ReAct 决策引擎实现。
 */
@Service
public class ReActDecisionEngine implements DecisionEngine {

    private static final Pattern EXPLICIT_TOOL_PATTERN = Pattern.compile("(?is)^\\s*(?:tool|工具)\\s*[:：]\\s*([a-zA-Z0-9_-]+)(?:#([a-zA-Z0-9_-]+))?(?:\\s+(\\{.*}))?\\s*$");
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");
    private static final Pattern QUOTED_PATTERN = Pattern.compile("[\"“](.+?)[\"”]");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?i)(?:prefix|前缀)\\s*[:：]?\\s*([a-zA-Z0-9_-]+)");

    /**
     * 根据用户输入和可用工具生成结构化决策计划。
     *
     * @param request 决策请求
     * @return 决策计划响应
     */
    @Override
    public DecisionPlanResponse plan(DecisionPlanRequest request) {
        List<ResolvedToolDefinition> availableTools = extractAvailableTools(request.getContext().get("availableTools"));
        List<PlannedToolCall> plannedToolCalls = new ArrayList<>(resolveExplicitCalls(request.getInput(), availableTools));
        if (plannedToolCalls.isEmpty()) {
            resolveHeuristicCall(request.getInput(), availableTools).ifPresent(plannedToolCalls::add);
        }

        List<String> selectedTools = plannedToolCalls.stream()
                .map(PlannedToolCall::getToolCode)
                .distinct()
                .toList();

        String finalAction = plannedToolCalls.isEmpty()
                ? "未命中可执行 Tool，回退到直接回答链路"
                : "按顺序执行已规划 Tool，并将结果汇总给用户";

        return DecisionPlanResponse.builder()
                .thought(buildThought(request.getInput(), availableTools, plannedToolCalls))
                .steps(List.of(
                        DecisionStep.builder().stage("observe").action("识别用户问题与当前可用 Tool 清单").build(),
                        DecisionStep.builder().stage("think").action("判断是否存在显式 Tool 指令或高置信度匹配").build(),
                        DecisionStep.builder().stage("act").action(plannedToolCalls.isEmpty() ? "未生成 Tool 调用计划" : "生成 " + plannedToolCalls.size() + " 个 Tool 调用计划").build(),
                        DecisionStep.builder().stage("summarize").action(finalAction).build()
                ))
                .selectedTools(selectedTools)
                .plannedToolCalls(plannedToolCalls)
                .finalAction(finalAction)
                .build();
    }

    /* 构建决策思考摘要。 */
    private String buildThought(String input,
                                List<ResolvedToolDefinition> availableTools,
                                List<PlannedToolCall> plannedToolCalls) {
        if (plannedToolCalls.isEmpty()) {
            return "已分析输入“" + input + "”，当前绑定 Tool 数量为 " + availableTools.size() + "，但未找到足够明确的 Tool 调用信号。";
        }
        PlannedToolCall firstCall = plannedToolCalls.get(0);
        return "已分析输入“" + input + "”，匹配到 Tool 调用 " + firstCall.getToolCode() + "#" + firstCall.getActionName() + "。";
    }

    /* 解析用户显式指定的 Tool 调用指令。 */
    private List<PlannedToolCall> resolveExplicitCalls(String input, List<ResolvedToolDefinition> availableTools) {
        Matcher matcher = EXPLICIT_TOOL_PATTERN.matcher(input == null ? "" : input);
        if (!matcher.matches()) {
            return List.of();
        }
        String toolCode = matcher.group(1);
        String actionName = matcher.group(2);
        String argumentsJson = matcher.group(3);
        ResolvedToolDefinition tool = availableTools.stream()
                .filter(item -> item.getToolCode().equals(toolCode))
                .findFirst()
                .orElse(null);
        if (tool == null) {
            return List.of();
        }
        String resolvedActionName = actionName;
        if (StringUtils.isBlank(resolvedActionName) && tool.getActions().size() == 1) {
            resolvedActionName = tool.getActions().get(0).getActionName();
        }
        if (StringUtils.isBlank(resolvedActionName)) {
            return List.of();
        }
        Map<String, Object> arguments = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(argumentsJson)) {
            arguments.putAll(JsonUtils.parse(argumentsJson, new TypeReference<Map<String, Object>>() {
            }));
        }
        return List.of(PlannedToolCall.builder()
                .toolCode(toolCode)
                .actionName(resolvedActionName)
                .arguments(arguments)
                .reason("用户显式指定 Tool 调用")
                .build());
    }

    /* 基于启发式规则推断 Tool 调用。 */
    private Optional<PlannedToolCall> resolveHeuristicCall(String input, List<ResolvedToolDefinition> availableTools) {
        if (StringUtils.isBlank(input) || availableTools.isEmpty()) {
            return Optional.empty();
        }
        String normalizedInput = input.trim();

        Optional<PlannedToolCall> timeCall = buildKnownCall(availableTools, normalizedInput,
                List.of("时间", "几点", "当前时间", "服务器时间"),
                "basic", "currentTime",
                "用户在查询时间");
        if (timeCall.isPresent()) {
            return timeCall;
        }

        Optional<PlannedToolCall> idCall = buildKnownCall(availableTools, normalizedInput,
                List.of("uuid", "随机id", "随机标识", "生成id", "生成标识"),
                "basic", "generateId",
                "用户在请求生成标识");
        if (idCall.isPresent()) {
            Map<String, Object> arguments = new LinkedHashMap<>();
            Matcher prefixMatcher = PREFIX_PATTERN.matcher(normalizedInput);
            if (prefixMatcher.find()) {
                ToolActionMetadata action = findAction(availableTools, "basic", "generateId").orElse(null);
                if (action != null && !action.getParameters().isEmpty()) {
                    arguments.put(action.getParameters().get(0).getName(), prefixMatcher.group(1));
                }
            }
            return Optional.of(PlannedToolCall.builder()
                    .toolCode("basic")
                    .actionName("generateId")
                    .arguments(arguments)
                    .reason("用户在请求生成标识")
                    .build());
        }

        return rankGenericCandidates(normalizedInput, availableTools);
    }

    /* 构建已知类型的 Tool 调用。 */
    private Optional<PlannedToolCall> buildKnownCall(List<ResolvedToolDefinition> availableTools,
                                                     String input,
                                                     List<String> keywords,
                                                     String toolCode,
                                                     String actionName,
                                                     String reason) {
        boolean matched = keywords.stream().anyMatch(keyword -> input.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)));
        if (!matched) {
            return Optional.empty();
        }
        return findAction(availableTools, toolCode, actionName)
                .map(action -> PlannedToolCall.builder()
                        .toolCode(toolCode)
                        .actionName(actionName)
                        .arguments(new LinkedHashMap<>())
                        .reason(reason)
                        .build());
    }

    /* 对通用候选动作进行评分排序，返回最高分调用计划。 */
    private Optional<PlannedToolCall> rankGenericCandidates(String input, List<ResolvedToolDefinition> availableTools) {
        List<ActionCandidate> candidates = new ArrayList<>();
        for (ResolvedToolDefinition tool : availableTools) {
            for (ToolActionMetadata action : tool.getActions()) {
                candidates.add(new ActionCandidate(tool, action));
            }
        }

        return candidates.stream()
                .map(candidate -> candidate.toScored(input, inferArguments(input, candidate)))
                .filter(candidate -> candidate.score() > 0)
                .filter(candidate -> candidate.arguments() != null)
                .max(Comparator.comparingInt(ScoredCandidate::score))
                .map(candidate -> PlannedToolCall.builder()
                        .toolCode(candidate.candidate().tool().getToolCode())
                        .actionName(candidate.candidate().action().getActionName())
                        .arguments(candidate.arguments())
                        .reason("根据输入与 Tool 描述进行高置信度匹配")
                        .build());
    }

    /* 从用户输入中推断工具参数。 */
    private Map<String, Object> inferArguments(String input, ActionCandidate candidate) {
        List<ToolActionParameter> parameters = candidate.action().getParameters();
        if (parameters == null || parameters.isEmpty()) {
            return new LinkedHashMap<>();
        }
        if (parameters.size() > 1) {
            return null;
        }
        ToolActionParameter parameter = parameters.get(0);
        Map<String, Object> arguments = new LinkedHashMap<>();
        String actionName = candidate.action().getActionName().toLowerCase(Locale.ROOT);
        String description = (candidate.action().getDescription() + " " + candidate.tool().getDescription()).toLowerCase(Locale.ROOT);

        Matcher urlMatcher = URL_PATTERN.matcher(input);
        if (urlMatcher.find()) {
            arguments.put(parameter.getName(), urlMatcher.group(1));
            return arguments;
        }

        Matcher quotedMatcher = QUOTED_PATTERN.matcher(input);
        if (quotedMatcher.find()) {
            arguments.put(parameter.getName(), quotedMatcher.group(1));
            return arguments;
        }

        if (actionName.contains("search") || description.contains("搜索") || description.contains("查询")) {
            arguments.put(parameter.getName(), input);
            return arguments;
        }

        if (actionName.contains("generate") && input.toLowerCase(Locale.ROOT).contains("prefix")) {
            Matcher prefixMatcher = PREFIX_PATTERN.matcher(input);
            if (prefixMatcher.find()) {
                arguments.put(parameter.getName(), prefixMatcher.group(1));
                return arguments;
            }
        }

        return null;
    }

    /* 查找指定工具的动作元数据。 */
    private Optional<ToolActionMetadata> findAction(List<ResolvedToolDefinition> availableTools,
                                                    String toolCode,
                                                    String actionName) {
        return availableTools.stream()
                .filter(item -> item.getToolCode().equals(toolCode))
                .flatMap(item -> item.getActions().stream())
                .filter(item -> item.getActionName().equals(actionName))
                .findFirst();
    }

    /* 从上下文中提取可用工具列表。 */
    private List<ResolvedToolDefinition> extractAvailableTools(Object rawAvailableTools) {
        if (!(rawAvailableTools instanceof Collection<?> collection) || collection.isEmpty()) {
            return List.of();
        }
        List<ResolvedToolDefinition> result = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof ResolvedToolDefinition definition) {
                result.add(definition);
                continue;
            }
            if (item instanceof Map<?, ?> map) {
                result.add(JsonUtils.parse(JsonUtils.toJson(map), ResolvedToolDefinition.class));
            }
        }
        return result;
    }

    /* 动作候选记录，用于评分排序。 */
    private record ActionCandidate(ResolvedToolDefinition tool, ToolActionMetadata action) {

        private ScoredCandidate toScored(String input, Map<String, Object> arguments) {
            int score = 0;
            String normalizedInput = input.toLowerCase(Locale.ROOT);
            Set<String> keywords = collectKeywords(tool.getToolCode(), tool.getToolName(), tool.getDescription(),
                    action.getActionName(), action.getDescription());
            for (String keyword : keywords) {
                if (normalizedInput.contains(keyword)) {
                    score += keyword.length() >= 4 ? 3 : 1;
                }
            }
            if (normalizedInput.contains(tool.getToolCode().toLowerCase(Locale.ROOT))) {
                score += 6;
            }
            if (normalizedInput.contains(action.getActionName().toLowerCase(Locale.ROOT))) {
                score += 6;
            }
            return new ScoredCandidate(this, score, arguments);
        }

        private Set<String> collectKeywords(String... texts) {
            Set<String> result = new LinkedHashSet<>();
            for (String text : texts) {
                if (StringUtils.isBlank(text)) {
                    continue;
                }
                for (String item : text.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+")) {
                    if (item.length() >= 2) {
                        result.add(item);
                    }
                }
            }
            return result;
        }
    }

    /* 评分后的候选记录。 */
    private record ScoredCandidate(ActionCandidate candidate, int score, Map<String, Object> arguments) {
    }
}
