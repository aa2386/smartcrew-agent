package com.smartcrew.agent.core.tool;

import cn.hutool.core.io.FileUtil;
import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import com.smartcrew.agent.common.config.ToolProperties;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 文件工具实现，用于在工具工作目录中读写文件。
 */
@Component
public class FileTools implements SmartCrewTool {

    /**
     * 工具配置属性。
     */
    private final ToolProperties toolProperties;

    /**
     * 构造 FileTools 所需的依赖对象。
     */
    public FileTools(ToolProperties toolProperties) {
        this.toolProperties = toolProperties;
    }

    /**
     * 返回工具编码。
     */
    @Override
    public String toolCode() {
        return "file";
    }

    /**
     * 返回工具名称。
     */
    @Override
    public String toolName() {
        return "File Tools";
    }

    /**
     * 返回当前工具的功能描述。
     */
    @Override
    public String description() {
        return "Read and write workspace tool files";
    }

    /**
     * 读取工作目录中的文件内容。
     */
    @Tool("Read a file from the tool workspace")
    public String readFile(@P("file name") String fileName) {
        return FileUtil.readUtf8String(resolve(fileName));
    }

    /**
     * 将内容写入工作目录中的文件。
     */
    @Tool("Write content to a file in the tool workspace")
    public String writeFile(@P("file name") String fileName, @P("content") String content) {
        File target = resolve(fileName);
        FileUtil.mkParentDirs(target);
        FileUtil.writeUtf8String(content, target);
        return target.getAbsolutePath();
    }

    /**
     * 将文件名解析为工具工作目录下的实际文件。
     */
    private File resolve(String fileName) {
        return new File(toolProperties.getFile().getSaveDir(), fileName);
    }
}
