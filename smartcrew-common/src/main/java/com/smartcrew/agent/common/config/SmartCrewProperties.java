package com.smartcrew.agent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * SmartCrew 主配置属性类，映射 `smartcrew` 前缀下的应用配置。
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
     * RAG 基础配置。
     */
    private Rag rag = new Rag();

    /**
     * 大模型配置项，描述模型开关、提供商及访问参数。
     */
    @Data
    public static class Llm {

        /**
         * 是否启用。
         */
        private boolean enabled;
        /**
         * 大模型提供商标识。
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

    /**
     * RAG 配置项。
     */
    @Data
    public static class Rag {

        /**
         * 是否启用 RAG 基础能力。
         */
        private boolean enabled;
        /**
         * 嵌入模型配置。
         */
        private Embedding embedding = new Embedding();
        /**
         * 向量存储配置。
         */
        private VectorStore vectorStore = new VectorStore();
        /**
         * 文档处理配置。
         */
        private Document document = new Document();
    }

    /**
     * 嵌入模型配置项。
     */
    @Data
    public static class Embedding {

        /**
         * 嵌入服务提供商。
         */
        private String provider = "dashscope";
        /**
         * 嵌入模型名称。
         */
        private String model = "text-embedding-v3";
        /**
         * 嵌入服务访问密钥。
         */
        private String apiKey;
        /**
         * 嵌入服务基础地址。
         */
        private String baseUrl;
    }

    /**
     * 向量存储配置项。
     */
    @Data
    public static class VectorStore {

        /**
         * 向量存储实现类型。
         */
        private String type = "chroma";
        /**
         * Chroma 配置。
         */
        private Chroma chroma = new Chroma();
    }

    /**
     * Chroma 配置项。
     */
    @Data
    public static class Chroma {

        /**
         * Chroma 远程服务地址。
         */
        private String baseUrl = "http://localhost:8000";
        /**
         * 请求超时秒数。
         */
        private int timeoutSeconds = 60;
        /**
         * Chroma API 版本（V1 或 V2）。
         * Chroma 0.7.0+ 仅支持 V2 API。
         */
        private String apiVersion = "V2";
        /**
         * 租户名称（V2 API 必需）。
         */
        private String tenantName = "default_tenant";
        /**
         * 数据库名称（V2 API 必需）。
         */
        private String databaseName = "default_database";
    }

    /**
     * 文档处理配置项。
     */
    @Data
    public static class Document {

        /**
         * 文档上传目录。
         */
        private String uploadPath = "./uploads/knowledge";
        /**
         * 文档分割配置。
         */
        private Splitter splitter = new Splitter();
    }

    /**
     * 文档分割配置项。
     */
    @Data
    public static class Splitter {

        /**
         * 分割策略。
         */
        private String type = "paragraph";
        /**
         * 单个切片最大大小。
         */
        private int maxChunkSize = 200;
        /**
         * 切片重叠大小。
         */
        private int overlapSize = 50;
    }
}
