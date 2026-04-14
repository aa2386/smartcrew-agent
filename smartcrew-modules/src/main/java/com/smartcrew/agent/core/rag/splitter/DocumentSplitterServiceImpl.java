package com.smartcrew.agent.core.rag.splitter;

import com.smartcrew.agent.api.rag.service.DocumentSplitterService;
import com.smartcrew.agent.common.config.SmartCrewProperties;
import com.smartcrew.agent.common.util.StringUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * 文档分割服务实现，按配置将长文档切成可向量化片段。
 */
@Service
@ConditionalOnProperty(prefix = "smartcrew.rag", name = "enabled", havingValue = "true")
public class DocumentSplitterServiceImpl implements DocumentSplitterService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSplitterServiceImpl.class);

    private final SmartCrewProperties properties;

    public DocumentSplitterServiceImpl(SmartCrewProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<TextSegment> split(Document document) {
        SmartCrewProperties.Splitter splitter = properties.getRag().getDocument().getSplitter();
        return split(document, splitter.getMaxChunkSize(), splitter.getOverlapSize());
    }

    @Override
    public List<TextSegment> split(Document document, int maxChunkSize, int overlapSize) {
        DocumentSplitter splitter = createSplitter(maxChunkSize, overlapSize);
        List<TextSegment> segments = splitter.split(document);
        log.info("文档分割完成，生成 {} 个切片", segments.size());
        return segments;
    }

    private DocumentSplitter createSplitter(int maxChunkSize, int overlapSize) {
        String splitterType = properties.getRag().getDocument().getSplitter().getType();
        String normalizedType = StringUtils.isBlank(splitterType)
                ? "paragraph"
                : splitterType.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedType) {
            case "sentence" -> new DocumentBySentenceSplitter(maxChunkSize, overlapSize);
            case "paragraph" -> new DocumentByParagraphSplitter(maxChunkSize, overlapSize);
            default -> {
                log.warn("未知文档分割策略: {}，回退为 paragraph", splitterType);
                yield new DocumentByParagraphSplitter(maxChunkSize, overlapSize);
            }
        };
    }
}
