package com.smartcrew.agent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * SmartCrew 涓婚厤缃睘鎬х被锛屾槧灏?smartcrew 鍓嶇紑涓嬬殑搴旂敤閰嶇疆銆?
 */
@Data
@Component
@ConfigurationProperties(prefix = "smartcrew")
public class SmartCrewProperties {

    /**
     * 搴旂敤鍚嶇О銆?
     */
    private String name = "smartcrew-agent";
    /**
     * 搴旂敤鐗堟湰銆?
     */
    private String version = "0.0.1-SNAPSHOT";
    /**
     * 澶фā鍨嬮厤缃€?
     */
    private Llm llm = new Llm();
    /**
     * 宸ュ叿寮€鍏抽厤缃€?
     */
    private Tools tools = new Tools();
    /**
     * RAG 鍩虹閰嶇疆銆?
     */
    private Rag rag = new Rag();

    /**
     * 澶фā鍨嬮厤缃」锛屾弿杩版ā鍨嬪紑鍏炽€佷緵搴斿晢鍙婅闂弬鏁般€?
     */
    @Data
    public static class Llm {

        /**
         * 鏄惁鍚敤銆?
         */
        private boolean enabled;
        /**
         * 澶фā鍨嬩緵搴斿晢鏍囪瘑銆?
         */
        private String provider;
        /**
         * 鏈嶅姟鍩虹鍦板潃銆?
         */
        private String baseUrl;
        /**
         * 鎺ュ彛璁块棶瀵嗛挜銆?
         */
        private String apiKey;
        /**
         * 妯″瀷鍚嶇О銆?
         */
        private String model;
    }

    /**
     * 宸ュ叿寮€鍏抽厤缃」锛岀淮鎶ゅ悇宸ュ叿鐨勫惎鐢ㄧ姸鎬併€?
     */
    @Data
    public static class Tools {

        /**
         * 鏄惁鍚敤銆?
         */
        private Map<String, Boolean> enabled = new HashMap<>();

        /**
         * 鍒ゆ柇鎸囧畾宸ュ叿鏄惁鍚敤銆?
         */
        public boolean isEnabled(String toolCode) {
            return enabled.getOrDefault(toolCode, true);
        }
    }

    /**
     * RAG 閰嶇疆椤广€?
     */
    @Data
    public static class Rag {

        /**
         * 鏄惁鍚敤 RAG 鍩虹鑳藉姏銆?
         */
        private boolean enabled;
        /**
         * 宓屽叆妯″瀷閰嶇疆銆?
         */
        private Embedding embedding = new Embedding();
        /**
         * 鍚戦噺瀛樺偍閰嶇疆銆?
         */
        private VectorStore vectorStore = new VectorStore();
        /**
         * 鏂囨。澶勭悊閰嶇疆銆?
         */
        private Document document = new Document();
    }

    /**
     * 宓屽叆妯″瀷閰嶇疆椤广€?
     */
    @Data
    public static class Embedding {

        /**
         * 宓屽叆鏈嶅姟鎻愪緵鍟嗐€?
         */
        private String provider = "dashscope";
        /**
         * 宓屽叆妯″瀷鍚嶇О銆?
         */
        private String model = "text-embedding-v3";
        /**
         * 宓屽叆鏈嶅姟璁块棶瀵嗛挜銆?
         */
        private String apiKey;
        /**
         * 宓屽叆鏈嶅姟鍩虹鍦板潃銆?
         */
        private String baseUrl;
    }

    /**
     * 鍚戦噺瀛樺偍閰嶇疆椤广€?
     */
    @Data
    public static class VectorStore {

        /**
         * 鍚戦噺瀛樺偍瀹炵幇绫诲瀷銆?
         */
        private String type = "chroma";
        /**
         * Chroma 閰嶇疆銆?
         */
        private Chroma chroma = new Chroma();
    }

    /**
     * Chroma 閰嶇疆椤广€?
     */
    @Data
    public static class Chroma {

        /**
         * Chroma 杩滅▼鏈嶅姟鍦板潃銆?
         */
        private String baseUrl = "http://localhost:8000";
        /**
         * 璇锋眰瓒呮椂绉掓暟銆?
         */
        private int timeoutSeconds = 60;
    }

    /**
     * 鏂囨。澶勭悊閰嶇疆椤广€?
     */
    @Data
    public static class Document {

        /**
         * 鏂囨。涓婁紶鐩綍銆?
         */
        private String uploadPath = "./uploads/knowledge";
        /**
         * 鏂囨。鍒嗗壊閰嶇疆銆?
         */
        private Splitter splitter = new Splitter();
    }

    /**
     * 鏂囨。鍒嗗壊閰嶇疆椤广€?
     */
    @Data
    public static class Splitter {

        /**
         * 鍒嗗壊绛栫暐銆?
         */
        private String type = "paragraph";
        /**
         * 鍗曚釜鍒囩墖鏈€澶уぇ灏忋€?
         */
        private int maxChunkSize = 200;
        /**
         * 鍒囩墖閲嶅彔澶у皬銆?
         */
        private int overlapSize = 50;
    }
}
