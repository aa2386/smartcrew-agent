package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import com.smartcrew.agent.common.config.ToolProperties;
import com.smartcrew.agent.common.exception.ServiceException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 网页搜索工具实现，封装对 Tavily 的查询能力。
 */
@Component
public class WebSearchTools implements SmartCrewTool {

    /**
     * 工具配置属性。
     */
    private final ToolProperties toolProperties;
    /**
     * 访问 Tavily 接口所使用的 HTTP 客户端。
     */
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 构造 WebSearchTools 所需的依赖对象。
     */
    public WebSearchTools(ToolProperties toolProperties) {
        this.toolProperties = toolProperties;
    }

    /**
     * 返回工具编码。
     */
    @Override
    public String toolCode() {
        return "web-search";
    }

    /**
     * 返回工具名称。
     */
    @Override
    public String toolName() {
        return "网络搜索工具";
    }

    /**
     * 返回当前工具的功能描述。
     */
    @Override
    public String description() {
        return "使用Tavily进行网络搜索";
    }

    /**
     * 使用 Tavily 执行网络搜索。
     */
    @Tool("在网络上搜索信息")
    public String search(@P("搜索关键词") String query) throws IOException {
        if (toolProperties.getTavily().getApiKey() == null || toolProperties.getTavily().getApiKey().isBlank()) {
            throw new ServiceException("Tavily API is not configured");
        }
        String payload = "{\"query\":\"" + query.replace("\"", "\\\"") + "\",\"max_results\":5}";
        Request request = new Request.Builder()
                .url(toolProperties.getTavily().getBaseUrl())
                .post(RequestBody.create(payload, MediaType.parse("application/json")))
                .header("Authorization", "Bearer " + toolProperties.getTavily().getApiKey())
                .header("Content-Type", "application/json")
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.body() == null ? "" : response.body().string();
        }
    }
}
