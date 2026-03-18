package com.smartcrew.agent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * SmartCrew 主配置属性类，映射 smartcrew 前缀下的应用配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "smartcrew")
public class SmartCrewProperties {

    /**
     * 应用名称。
     */
    private String name = "smartcrew-agent";
    /**
     * 应用版本。
     */
    private String version = "0.0.1-SNAPSHOT";
    /**
     * 大模型配置。
     */
    private Llm llm = new Llm();
    /**
     * 工具开关配置。
     */
    private Tools tools = new Tools();

    /**
     * 大模型配置项，描述模型开关、供应商及访问参数。
     */
    @Data
    public static class Llm {

        /**
         * 是否启用。
         */
        private boolean enabled;
        /**
         * 大模型供应商标识。
         */
        private String provider;
        /**
         * 服务基础地址。
         */
        private String baseUrl;
        /**
         * 接口访问密钥。
         */
        private String apiKey;
        /**
         * 模型名称。
         */
        private String model;
    }

    /**
     * 工具开关配置项，维护各工具的启用状态。
     */
    @Data
    public static class Tools {

        /**
         * 是否启用。
         */
        private Map<String, Boolean> enabled = new HashMap<>();

        /**
         * 判断指定工具是否启用。
         */
        public boolean isEnabled(String toolCode) {
            return enabled.getOrDefault(toolCode, true);
        }
    }
}
