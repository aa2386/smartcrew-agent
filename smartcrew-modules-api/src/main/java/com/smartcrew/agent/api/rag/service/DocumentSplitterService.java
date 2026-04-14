package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * 鏂囨。鍒嗗壊鏈嶅姟鎺ュ彛銆?
 */
public interface DocumentSplitterService {

    /**
     * 浣跨敤榛樿閰嶇疆鍒嗗壊鏂囨。銆?     *
     * @param document 鏂囨。瀵硅薄銆?     * @return 鍒囩墖鍒楄〃銆?     */
    List<TextSegment> split(Document document);

    /**
     * 鎸囧畾鍙傛暟鍒嗗壊鏂囨。銆?     *
     * @param document 鏂囨。瀵硅薄銆?     * @param maxChunkSize 鍗曚釜鍒囩墖鏈€澶уぇ灏忋€?     * @param overlapSize 鍒囩墖閲嶅彔澶у皬銆?     * @return 鍒囩墖鍒楄〃銆?     */
    List<TextSegment> split(Document document, int maxChunkSize, int overlapSize);
}
