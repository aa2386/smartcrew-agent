package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * 宓屽叆鏈嶅姟鎺ュ彛銆?
 */
public interface EmbeddingService {

    /**
     * 瀵瑰崟鏉℃枃鏈繘琛屽悜閲忓寲銆?     *
     * @param text 鏂囨湰鍐呭銆?     * @return 鍚戦噺缁撴灉銆?     */
    Embedding embed(String text);

    /**
     * 瀵规壒閲忓垏鐗囪繘琛屽悜閲忓寲銆?     *
     * @param segments 鍒囩墖鍒楄〃銆?     * @return 鍚戦噺鍒楄〃銆?     */
    List<Embedding> embedAll(List<TextSegment> segments);

    /**
     * 杩斿洖褰撳墠浣跨敤鐨勫祵鍏ユā鍨嬪悕绉般€?     *
     * @return 妯″瀷鍚嶇О銆?     */
    String modelName();
}
