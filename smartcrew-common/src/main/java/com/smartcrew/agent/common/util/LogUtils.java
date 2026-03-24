package com.smartcrew.agent.common.util;

/**
 * 日志工具类，提供统一的日志格式化能力。
 */
public final class LogUtils {

    private LogUtils() {
    }

    /**
     * 格式化日志消息
     *
     * @param prefix 日志前缀
     * @param message 日志内容
     * @return 格式化后的日志消息
     */
    public static String format(String prefix, String message) {
        return String.format("%s %s", prefix, message);
    }

    /**
     * 格式化日志消息（支持参数占位符）
     *
     * @param prefix 日志前缀
     * @param message 日志内容（支持 {} 占位符）
     * @param args 参数列表
     * @return 格式化后的日志消息
     */
    public static String format(String prefix, String message, Object... args) {
        String formattedMessage = message;
        for (Object arg : args) {
            formattedMessage = formattedMessage.replaceFirst("\\{}", String.valueOf(arg));
        }
        return String.format("%s %s", prefix, formattedMessage);
    }

    /**
     * 格式化带键值对的日志消息
     *
     * @param prefix 日志前缀
     * @param message 日志内容
     * @param keyValues 键值对（必须成对出现）
     * @return 格式化后的日志消息
     */
    public static String formatWithKv(String prefix, String message, Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return format(prefix, message);
        }
        
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("键值对参数数量必须为偶数");
        }
        
        StringBuilder sb = new StringBuilder(format(prefix, message));
        for (int i = 0; i < keyValues.length; i += 2) {
            sb.append(", ").append(keyValues[i]).append(": ").append(keyValues[i + 1]);
        }
        return sb.toString();
    }
}
