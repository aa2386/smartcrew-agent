package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.List;

/**
 * 鍚戦噺瀛樺偍鏈嶅姟鎺ュ彛銆?
 */
public interface VectorStoreService {

    /**
     * 鍚戞寚瀹氬懡鍚嶇┖闂存坊鍔犲崟鏉″悜閲忋€?     *
     * @param namespace 鍛藉悕绌洪棿銆?     * @param embedding 鍚戦噺鏁版嵁銆?     * @param segment 鍘熷鍒囩墖銆?     * @return 鍚戦噺 ID銆?     */
    String add(String namespace, Embedding embedding, TextSegment segment);

    /**
     * 鍚戞寚瀹氬懡鍚嶇┖闂存壒閲忔坊鍔犲悜閲忋€?     *
     * @param namespace 鍛藉悕绌洪棿銆?     * @param embeddings 鍚戦噺鍒楄〃銆?     * @param segments 鍘熷鍒囩墖鍒楄〃銆?     * @return 鍚戦噺 ID 鍒楄〃銆?     */
    List<String> addAll(String namespace, List<Embedding> embeddings, List<TextSegment> segments);

    /**
     * 浠庢寚瀹氬懡鍚嶇┖闂寸Щ闄ゅ崟鏉″悜閲忋€?     *
     * @param namespace 鍛藉悕绌洪棿銆?     * @param id 鍚戦噺 ID銆?     */
    void remove(String namespace, String id);

    /**
     * 浠庢寚瀹氬懡鍚嶇┖闂存壒閲忕Щ闄ゅ悜閲忋€?     *
     * @param namespace 鍛藉悕绌洪棿銆?     * @param ids 鍚戦噺 ID 鍒楄〃銆?     */
    void removeAll(String namespace, List<String> ids);

    /**
     * 鍦ㄦ寚瀹氬懡鍚嶇┖闂翠腑鎼滅储鐩稿叧鍒囩墖銆?     *
     * @param namespace 鍛藉悕绌洪棿銆?     * @param queryEmbedding 鏌ヨ鍚戦噺銆?     * @param maxResults 杩斿洖鏁伴噺銆?     * @return 鎼滅储缁撴灉銆?     */
    List<EmbeddingMatch<TextSegment>> search(String namespace, Embedding queryEmbedding, int maxResults);

    /**
     * 鍦ㄦ寚瀹氬懡鍚嶇┖闂翠腑鎸夐槇鍊兼悳绱㈢浉鍏冲垏鐗囥€?     *
     * @param namespace 鍛藉悕绌洪棿銆?     * @param queryEmbedding 鏌ヨ鍚戦噺銆?     * @param maxResults 杩斿洖鏁伴噺銆?     * @param minScore 鏈€浣庣浉浼煎害銆?     * @return 鎼滅储缁撴灉銆?     */
    List<EmbeddingMatch<TextSegment>> search(String namespace, Embedding queryEmbedding, int maxResults, double minScore);
}
