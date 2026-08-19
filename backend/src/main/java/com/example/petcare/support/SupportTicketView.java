package com.example.petcare.support;

import java.util.List;

public record SupportTicketView(
        Long id,
        String ticketNo,
        String contactName,
        String contactInfo,
        String username,
        String issueType,
        String content,
        String status,
        String createdAt,
        String updatedAt,
        List<SupportTicketReplyView> replies
) {
    public static SupportTicketView from(SupportTicket ticket, List<SupportTicketReplyView> replies) {
        return new SupportTicketView(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getContactName(),
                ticket.getContactInfo(),
                ticket.getUsername(),
                ticket.getIssueType(),
                ticket.getContent(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                replies
        );
    }
}
