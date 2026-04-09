package com.smartcrew.agent.core.agent.service;

import com.smartcrew.agent.api.agent.domain.entity.AgentDefinition;
import com.smartcrew.agent.api.agent.service.AgentRegistry;
import com.smartcrew.agent.api.memory.domain.vo.UserPreferenceVo;
import com.smartcrew.agent.api.memory.service.UserPreferenceService;
import com.smartcrew.agent.api.prompt.domain.vo.AgentPromptBindingVo;
import com.smartcrew.agent.api.prompt.service.AgentPromptBindingService;
import org.springframework.stereotype.Service;

/**
 * 初始智能体提示词构建服务实现。
 */
@Service
public class InitialAgentPromptServiceImpl implements InitialAgentPromptService {

    /**
     * 三层提示词都为空时使用的兜底提示词。
     */
    private static final String DEFAULT_PROMPT = """
            你是 SmartCrew 平台的初始智能体。
            你需要使用清晰、专业、友好的中文与用户交流。
            如果用户表达不够完整，请先进行澄清，再继续回答。
            """;

    /**
     * Agent 注册表。
     */
    private final AgentRegistry agentRegistry;

    /**
     * Agent Prompt 绑定服务。
     */
    private final AgentPromptBindingService agentPromptBindingService;

    /**
     * 用户偏好服务。
     */
    private final UserPreferenceService userPreferenceService;

    public InitialAgentPromptServiceImpl(AgentRegistry agentRegistry,
                                         AgentPromptBindingService agentPromptBindingService,
                                         UserPreferenceService userPreferenceService) {
        this.agentRegistry = agentRegistry;
        this.agentPromptBindingService = agentPromptBindingService;
        this.userPreferenceService = userPreferenceService;
    }

    @Override
    public String buildSystemPrompt(String agentCode, Long userId) {
        StringBuilder builder = new StringBuilder();

        // 第一层：Agent 自身的人格、风格与安全边界配置。
        agentRegistry.getDefinition(agentCode)
                .map(AgentDefinition::getSystemPrompt)
                .ifPresent(systemPrompt -> appendSection(builder, systemPrompt));

        // 第二层：按顺序拼接后台绑定的工作流 Prompt 模板。
        agentPromptBindingService.listResolvedByAgentCode(agentCode).stream()
                .map(AgentPromptBindingVo::getTemplateContent)
                .forEach(templateContent -> appendSection(builder, templateContent));

        // 第三层：用户长期偏好。
        appendPreference(builder, userId, "language", "用户偏好语言：");
        appendPreference(builder, userId, "nickname", "用户偏好称呼：");
        appendPreference(builder, userId, "tone", "用户偏好风格：");

        return builder.length() == 0 ? DEFAULT_PROMPT : builder.toString();
    }

    /**
     * 拼接文本段落。
     */
    private void appendSection(StringBuilder builder, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(System.lineSeparator()).append(System.lineSeparator());
        }
        builder.append(content.trim());
    }

    /**
     * 拼接用户偏好信息。
     */
    private void appendPreference(StringBuilder builder, Long userId, String key, String prefix) {
        userPreferenceService.getByUserIdAndKey(userId, key)
                .map(UserPreferenceVo::getPrefValue)
                .filter(value -> value != null && !value.isBlank())
                .ifPresent(value -> {
                    if (builder.length() > 0) {
                        builder.append(System.lineSeparator());
                    }
                    builder.append(prefix).append(value);
                });
    }
}
