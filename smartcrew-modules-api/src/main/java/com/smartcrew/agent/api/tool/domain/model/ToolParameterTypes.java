package com.smartcrew.agent.api.tool.domain.model;

/**
 * 工具参数类型常量，定义工具参数支持的数据类型。
 *
 * <p>支持的类型：</p>
 * <ul>
 *   <li>{@link #STRING} - 字符串类型</li>
 *   <li>{@link #INTEGER} - 整数类型</li>
 *   <li>{@link #NUMBER} - 数值类型（含浮点数）</li>
 *   <li>{@link #BOOLEAN} - 布尔类型</li>
 * </ul>
 */
public final class ToolParameterTypes {

    /** 字符串类型。 */
    public static final String STRING = "STRING";
    /** 整数类型。 */
    public static final String INTEGER = "INTEGER";
    /** 数值类型，含浮点数。 */
    public static final String NUMBER = "NUMBER";
    /** 布尔类型。 */
    public static final String BOOLEAN = "BOOLEAN";

    /**
     * 私有构造，防止实例化。
     */
    private ToolParameterTypes() {
    }
}
