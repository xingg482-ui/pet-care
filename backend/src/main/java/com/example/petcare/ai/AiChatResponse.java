package com.example.petcare.ai;

import java.util.List;

public record AiChatResponse(
        Long sessionId,
        String answer,
        String source,
        List<String> suggestedQuestions,
        String riskLevel
) {
}
