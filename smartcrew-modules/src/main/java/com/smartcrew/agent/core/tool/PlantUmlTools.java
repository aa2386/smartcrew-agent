package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.tool.service.SmartCrewTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * PlantUML 工具实现，用于将 UML 源码转换为 SVG。
 */
@Component
public class PlantUmlTools implements SmartCrewTool {

    /**
     * 返回工具编码。
     */
    @Override
    public String toolCode() {
        return "plantuml";
    }

    /**
     * 返回工具名称。
     */
    @Override
    public String toolName() {
        return "PlantUML工具";
    }

    /**
     * 返回描述信息。
     */
    @Override
    public String description() {
        return "生成PlantUML图表";
    }

    /**
     * 将 PlantUML 源码转换为 SVG。
     */
    @Tool("将PlantUML源码转换为SVG图片")
    public String generateSvg(@P("PlantUML源码") String umlCode) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new SourceStringReader(umlCode).generateImage(outputStream, new FileFormatOption(FileFormat.SVG));
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
