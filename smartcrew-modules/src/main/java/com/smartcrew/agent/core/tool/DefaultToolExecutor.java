package com.smartcrew.agent.core.tool;

import com.smartcrew.agent.api.tool.domain.model.ToolMetadata;
import com.smartcrew.agent.api.tool.service.ToolExecutor;
import com.smartcrew.agent.api.tool.service.ToolRegistry;
import com.smartcrew.agent.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 默认工具执行器实现，负责校验工具状态并定位目标 Bean。
 */
@RequiredArgsConstructor
@Service
public class DefaultToolExecutor implements ToolExecutor {

    /**
     * 工具注册表。
     */
    private final ToolRegistry toolRegistry;
    /**
     * Spring 应用上下文。
     */
    private final ApplicationContext applicationContext;

    /**
     * 执行目标操作。
     */
    @Override
    public Object execute(String toolCode, Map<String, Object> arguments) {
        ToolMetadata metadata = toolRegistry.getByCode(toolCode)
                .orElseThrow(() -> new ServiceException("Unknown tool: " + toolCode));
        if (!metadata.isEnabled()) {
            throw new ServiceException("Tool is disabled: " + toolCode);
        }
        Object bean = applicationContext.getBean(metadata.getBeanName());
        return "Tool " + toolCode + " is ready on bean " + bean.getClass().getSimpleName() + " with args " + arguments;
    }
}
