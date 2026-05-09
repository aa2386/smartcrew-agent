package com.smartcrew.agent.api.experience.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 推荐工具编码 JSON 数组字符串转换约定。
 */
public final class AgentExperienceToolCodes {

    private AgentExperienceToolCodes() {
    }

    /**
     * 将工具编码列表编码为 JSON 数组字符串。
     *
     * @param toolCodes 工具编码列表
     * @return JSON 数组字符串
     */
    public static String toJson(List<String> toolCodes) {
        if (toolCodes == null || toolCodes.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < toolCodes.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append('"').append(escape(toolCodes.get(index))).append('"');
        }
        builder.append(']');
        return builder.toString();
    }

    /**
     * 将 JSON 数组字符串解码为工具编码列表。
     *
     * @param json JSON 数组字符串
     * @return 工具编码列表
     */
    public static List<String> fromJson(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        String normalized = json.trim();
        if (normalized.length() < 2 || normalized.charAt(0) != '[' || normalized.charAt(normalized.length() - 1) != ']') {
            throw new IllegalArgumentException("recommended_tool_codes 必须是 JSON 数组字符串");
        }
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideString = false;
        boolean escaping = false;
        for (int index = 1; index < normalized.length() - 1; index++) {
            char currentChar = normalized.charAt(index);
            if (!insideString) {
                if (Character.isWhitespace(currentChar) || currentChar == ',') {
                    continue;
                }
                if (currentChar != '"') {
                    throw new IllegalArgumentException("recommended_tool_codes 仅支持字符串数组");
                }
                insideString = true;
                current.setLength(0);
                continue;
            }
            if (escaping) {
                current.append(unescape(currentChar));
                escaping = false;
                continue;
            }
            if (currentChar == '\\') {
                escaping = true;
                continue;
            }
            if (currentChar == '"') {
                values.add(current.toString());
                insideString = false;
                continue;
            }
            current.append(currentChar);
        }
        if (insideString || escaping) {
            throw new IllegalArgumentException("recommended_tool_codes JSON 数组字符串格式不完整");
        }
        return values;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static char unescape(char value) {
        return switch (value) {
            case '\\', '"' -> value;
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> value;
        };
    }
}
