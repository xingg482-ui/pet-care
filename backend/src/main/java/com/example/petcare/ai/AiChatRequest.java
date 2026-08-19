package com.example.petcare.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @NotBlank(message = "咨询内容不能为空")
        @Size(max = 1000, message = "咨询内容不能超过 1000 字")
        String message,
        Long sessionId,
        String sourcePage,
        String contextType,
        Long contextId
) {
}
