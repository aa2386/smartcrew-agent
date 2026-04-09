package com.smartcrew.agent.api.chat.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送聊天消息请求。
 */
@Data
public class SendChatMessageRequest {

    /**
     * 用户输入内容。
     */
    @NotBlank
    private String message;
}
