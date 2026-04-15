package com.smartcrew.agent.api.rag.service;

import dev.langchain4j.data.document.Document;

import java.nio.file.Path;

/**
 * 文档加载服务接口。
 */
public interface DocumentLoaderService {

    /**
     * 按文件路径加载文档。
     *
     * @param filePath 文件路径。
     * @return 文档对象。
     */
    Document loadDocument(Path filePath);

    /**
     * 指定文件类型加载文档。
     *
     * @param filePath 文件路径。
     * @param fileType 文件类型。
     * @return 文档对象。
     */
    Document loadDocument(Path filePath, String fileType);

    /**
     * 判断是否明确支持当前文件类型。
     *
     * @param fileType 文件类型。
     * @return 支持结果。
     */
    boolean supports(String fileType);
}
