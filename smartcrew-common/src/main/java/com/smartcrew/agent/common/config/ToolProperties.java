package com.smartcrew.agent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工具相关配置属性类，映射 smartcrew.tooling 前缀下的工具配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "smartcrew.tooling")
public class ToolProperties {

    /**
     * Tavily 搜索服务配置。
     */
    private Tavily tavily = new Tavily();
    /**
     * Pexels 图片搜索服务配置。
     */
    private Pexels pexels = new Pexels();
    /**
     * 文件工具配置。
     */
    private FileConfig file = new FileConfig();

    /**
     * Tavily 搜索服务配置项。
     */
    @Data
    public static class Tavily {

        /**
         * 接口访问密钥。
         */
        private String apiKey;
        /**
         * 服务基础地址。
         */
        private String baseUrl = "https://api.tavily.com/search";
    }

    /**
     * Pexels 图片服务配置项。
     */
    @Data
    public static class Pexels {

        /**
         * 接口访问密钥。
         */
        private String apiKey;
        /**
         * 接口地址。
         */
        private String apiUrl = "https://api.pexels.com/v1/search";
    }

    /**
     * 文件工具配置项。
     */
    @Data
    public static class FileConfig {

        /**
         * 文件保存目录。
         */
        private String saveDir = "./tmp";
    }
}
