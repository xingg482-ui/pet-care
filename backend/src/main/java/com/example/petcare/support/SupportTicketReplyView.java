package com.example.petcare.support;

public record SupportTicketReplyView(
        Long id,
        Long ticketId,
        Long replierAccountId,
        String replierRole,
        String content,
        String createdAt
) {
    public static SupportTicketReplyView from(SupportTicketReply reply) {
        return new SupportTicketReplyView(reply.getId(), reply.getTicketId(), reply.getReplierAccountId(), reply.getReplierRole(), reply.getContent(), reply.getCreatedAt());
    }
}
