package com.example.petcare.ai;

import java.util.List;

public record AiChatSessionView(
        Long id,
        String title,
        String roleScope,
        String sourcePage,
        String createdAt,
        String updatedAt,
        List<AiChatMessageView> messages
) {
    public static AiChatSessionView from(AiChatSession session, List<AiChatMessageView> messages) {
        return new AiChatSessionView(
                session.getId(),
                session.getTitle(),
                session.getRoleScope(),
                session.getSourcePage(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                messages
        );
    }
}
