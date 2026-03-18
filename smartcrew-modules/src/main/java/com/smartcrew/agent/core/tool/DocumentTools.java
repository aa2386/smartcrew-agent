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
 * 文档工具实现，用于从指定 URL 读取文档文本内容。
 */
@Component
public class DocumentTools implements SmartCrewTool {

    /**
     * 访问文档内容所使用的 HTTP 客户端。
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
        return "document";
    }

    /**
     * 返回工具名称。
     */
    @Override
    public String toolName() {
        return "Document Tools";
    }

    /**
     * 返回当前工具的功能描述。
     */
    @Override
    public String description() {
        return "Load simple document text from a URL";
    }

    /**
     * 解析目标内容。
     */
    @Tool("Read document content from a URL")
    public String parse(@P("document url") String fileUrl) throws IOException {
        Request request = new Request.Builder().url(fileUrl).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.body() == null ? "" : response.body().string();
        }
    }
}
