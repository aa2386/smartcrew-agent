package com.smartcrew.agent.core.tool.executable;

import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 基础工具实现，提供随机标识生成和当前时间查询能力。
 */
@Component
public class BasicTools implements SmartCrewTool {

    /**
     * 返回工具编码。
     */
    @Override
    public String toolCode() {
        return "basic";
    }

    /**
     * 返回工具名称。
     */
    @Override
    public String toolName() {
        return "基础工具";
    }

    /**
     * 返回描述信息。
     */
    @Override
    public String description() {
        return "基础工具集，提供随机标识生成和时间查询功能";
    }

    /**
     * 生成随机标识。
     */
    @Tool("生成一个随机标识符，可指定可选前缀")
    public String generateId(@P("标识符前缀") String prefix) {
        return (prefix == null ? "" : prefix) + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取当前服务器时间。
     */
    @Tool("获取当前服务器时间")
    public String currentTime() {
        return LocalDateTime.now().toString();
    }
}
