package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 网页读取工具实现，用于抓取网页内容并提取纯文本。
 */
@Component
public class WebPageTools implements SmartCrewTool {

    /**
     * 访问网页内容所使用的 HTTP 客户端。
     */
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 返回工具编码。
     */
    @Override
    public String toolCode() {
        return "web-page";
    }

    /**
     * 返回工具名称。
     */
    @Override
    public String toolName() {
        return "Web Page Tools";
    }

    /**
     * 返回当前工具的功能描述。
     */
    @Override
    public String description() {
        return "Load and extract web page content";
    }

    /**
     * 加载网页内容并返回纯文本。
     */
    @Tool("Load a web page and return plain text")
    public String load(@P("page url") String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string()
                    .replaceAll("<script[^>]*>[\\s\\S]*?</script>", "")
                    .replaceAll("<style[^>]*>[\\s\\S]*?</style>", "")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        }
    }
}
