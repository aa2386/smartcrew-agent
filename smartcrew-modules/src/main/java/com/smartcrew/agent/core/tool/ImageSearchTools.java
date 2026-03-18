package com.smartcrew.agent.core.tool;

import cn.hutool.http.HttpUtil;
import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import com.smartcrew.agent.common.config.ToolProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 图片搜索工具实现，封装对 Pexels 的查询能力。
 */
@Component
public class ImageSearchTools implements SmartCrewTool {

    /**
     * 工具配置属性。
     */
    private final ToolProperties toolProperties;

    /**
     * 构造 ImageSearchTools 所需的依赖对象。
     */
    public ImageSearchTools(ToolProperties toolProperties) {
        this.toolProperties = toolProperties;
    }

    /**
     * 返回工具编码。
     */
    @Override
    public String toolCode() {
        return "image-search";
    }

    /**
     * 返回工具名称。
     */
    @Override
    public String toolName() {
        return "Image Search Tools";
    }

    /**
     * 返回描述信息。
     */
    @Override
    public String description() {
        return "Search images with Pexels";
    }

    /**
     * 执行搜索操作。
     */
    @Tool("Search images from Pexels")
    public String search(@P("search query") String query) {
        if (toolProperties.getPexels().getApiKey() == null || toolProperties.getPexels().getApiKey().isBlank()) {
            throw new ServiceException("Pexels API is not configured");
        }
        return HttpUtil.createGet(toolProperties.getPexels().getApiUrl())
                .header("Authorization", toolProperties.getPexels().getApiKey())
                .form(Map.of("query", query))
                .execute()
                .body();
    }
}
