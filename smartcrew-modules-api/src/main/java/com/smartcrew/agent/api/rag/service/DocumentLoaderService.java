package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.data.document.Document;

import java.nio.file.Path;

/**
 * 鏂囨。鍔犺浇鏈嶅姟鎺ュ彛銆?
 */
public interface DocumentLoaderService {

    /**
     * 鎸夋枃浠惰矾寰勫姞杞芥枃妗ｃ€?     *
     * @param filePath 鏂囦欢璺緞銆?     * @return 鏂囨。瀵硅薄銆?     */
    Document loadDocument(Path filePath);

    /**
     * 鎸囧畾鏂囦欢绫诲瀷鍔犺浇鏂囨。銆?     *
     * @param filePath 鏂囦欢璺緞銆?     * @param fileType 鏂囦欢绫诲瀷銆?     * @return 鏂囨。瀵硅薄銆?     */
    Document loadDocument(Path filePath, String fileType);

    /**
     * 鍒ゆ柇鏄惁鏄庣‘鏀寔褰撳墠鏂囦欢绫诲瀷銆?     *
     * @param fileType 鏂囦欢绫诲瀷銆?     * @return 鏀寔缁撴灉銆?     */
    boolean supports(String fileType);
}
