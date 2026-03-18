package com.smartcrew.agent.common.util;

/**
 * 字符串工具类，提供空白判断等基础能力。
 */
public final class StringUtils {

    /**
     * 私有构造方法，禁止外部实例化。
     */
    private StringUtils() {
    }

    /**
     * 判断字符串是否为空白。
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 判断字符串是否非空白。
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }
}
