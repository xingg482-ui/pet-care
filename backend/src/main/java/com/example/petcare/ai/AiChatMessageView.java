package com.example.petcare.ai;

public record AiChatMessageView(
        Long id,
        String sender,
        String content,
        String source,
        String riskLevel,
        String createdAt
) {
    public static AiChatMessageView from(AiChatMessage message) {
        return new AiChatMessageView(
                message.getId(),
                message.getSender(),
                message.getContent(),
                message.getSource(),
                message.getRiskLevel(),
                message.getCreatedAt()
        );
    }
}
