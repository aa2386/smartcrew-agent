package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import com.smartcrew.agent.common.config.ToolProperties;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 终端工具实现，用于在工具工作目录中执行命令。
 */
@Component
public class TerminalTools implements SmartCrewTool {

    /**
     * 工具配置属性。
     */
    private final ToolProperties toolProperties;

    /**
     * 构造 TerminalTools 所需的依赖对象。
     */
    public TerminalTools(ToolProperties toolProperties) {
        this.toolProperties = toolProperties;
    }

    /**
     * 返回工具编码。
     */
    @Override
    public String toolCode() {
        return "terminal";
    }

    /**
     * 返回工具名称。
     */
    @Override
    public String toolName() {
        return "终端工具";
    }

    /**
     * 返回描述信息。
     */
    @Override
    public String description() {
        return "在工具工作目录中执行终端命令";
    }

    /**
     * 返回风险等级。
     */
    @Override
    public String riskLevel() {
        return "HIGH";
    }

    /**
     * 返回是否默认启用。
     */
    @Override
    public boolean enabledByDefault() {
        return false;
    }

    /**
     * 执行目标操作。
     */
    @Tool("执行终端命令")
    public String execute(@P("命令行") String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("/bin/sh", "-c", command);
        }
        File workDir = new File(toolProperties.getFile().getSaveDir());
        if (!workDir.exists()) {
            workDir.mkdirs();
        }
        processBuilder.directory(workDir);
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        process.waitFor();
        return output.toString();
    }
}
