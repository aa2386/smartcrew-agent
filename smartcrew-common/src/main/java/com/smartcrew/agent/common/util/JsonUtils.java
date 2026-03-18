package com.smartcrew.agent.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcrew.agent.common.exception.ServiceException;

/**
 * JSON 工具类，封装常用的序列化与反序列化能力。
 */
public final class JsonUtils {

    /**
     * 共享的 Jackson 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 私有构造方法，禁止外部实例化。
     */
    private JsonUtils() {
    }

    /**
     * 将对象序列化为 JSON 字符串。
     */
    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ServiceException(500, "Failed to serialize json");
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型对象。
     */
    public static <T> T parse(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException exception) {
            throw new ServiceException(400, "Failed to parse json");
        }
    }

    /**
     * 将 JSON 字符串按泛型类型反序列化。
     */
    public static <T> T parse(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException exception) {
            throw new ServiceException(400, "Failed to parse json");
        }
    }
}
