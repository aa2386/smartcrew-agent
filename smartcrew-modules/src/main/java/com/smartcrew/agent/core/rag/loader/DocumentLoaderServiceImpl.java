package com.smartcrew.agent.core.rag.loader;

import com.smartcrew.agent.api.rag.service.DocumentLoaderService;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 文档加载服务实现，统一将不同格式文档转换为 LangChain4j 文档对象。
 */
@Service
@ConditionalOnProperty(prefix = "smartcrew.rag", name = "enabled", havingValue = "true")
public class DocumentLoaderServiceImpl implements DocumentLoaderService {

    private static final Logger log = LoggerFactory.getLogger(DocumentLoaderServiceImpl.class);

    private final Map<String, DocumentParser> parserMap = new HashMap<>();
    private final DocumentParser fallbackParser = new ApacheTikaDocumentParser();

    public DocumentLoaderServiceImpl() {
        DocumentParser textParser = new TextDocumentParser();
        DocumentParser pdfParser = new ApachePdfBoxDocumentParser();
        DocumentParser tikaParser = fallbackParser;

        parserMap.put("txt", textParser);
        parserMap.put("md", textParser);
        parserMap.put("pdf", pdfParser);
        parserMap.put("doc", tikaParser);
        parserMap.put("docx", tikaParser);
        parserMap.put("html", tikaParser);
        parserMap.put("json", tikaParser);
    }

    @Override
    public Document loadDocument(Path filePath) {
        return loadDocument(filePath, resolveFileType(filePath));
    }

    @Override
    public Document loadDocument(Path filePath, String fileType) {
        String normalizedType = normalizeFileType(fileType);
        DocumentParser parser = parserMap.getOrDefault(normalizedType, fallbackParser);
        if (!parserMap.containsKey(normalizedType)) {
            log.info("文档类型 {} 未显式配置解析器，回退 Apache Tika，file: {}", normalizedType, filePath);
        }
        return FileSystemDocumentLoader.loadDocument(filePath, parser);
    }

    @Override
    public boolean supports(String fileType) {
        return parserMap.containsKey(normalizeFileType(fileType));
    }

    /* 根据文件路径解析文件类型。 */
    private String resolveFileType(Path filePath) {
        String fileName = filePath.getFileName() == null ? "" : filePath.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "txt";
        }
        return fileName.substring(lastDot + 1);
    }

    /* 规范化文件类型字符串。 */
    private String normalizeFileType(String fileType) {
        if (StringUtils.isBlank(fileType)) {
            return "txt";
        }
        return fileType.trim().toLowerCase(Locale.ROOT);
    }
}
