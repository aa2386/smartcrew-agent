package com.smartcrew.agent.core.agent.service;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "smartcrew.llm", name = "enabled", havingValue = "true")
public class DefaultInitialAgentChatService implements InitialAgentChatService {

    private final InitialAgentAssistant assistant;

    public DefaultInitialAgentChatService(ChatLanguageModel chatLanguageModel,
                                          ChatMemoryProvider chatMemoryProvider,
                                          ToolProvider toolProvider) {
        this.assistant = AiServices.builder(InitialAgentAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatMemoryProvider)
                .toolProvider(toolProvider)
                .build();
    }

    @Override
    public Result<String> chat(String memoryId, String userMessage, String systemPrompt) {
        return assistant.chat(memoryId, userMessage, systemPrompt);
    }

    interface InitialAgentAssistant {

        @SystemMessage("{{systemPrompt}}")
        Result<String> chat(@MemoryId String memoryId,
                            @UserMessage String userMessage,
                            @V("systemPrompt") String systemPrompt);
    }
}
