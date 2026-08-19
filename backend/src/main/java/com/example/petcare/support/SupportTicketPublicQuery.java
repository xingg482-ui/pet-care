package com.example.petcare.support;

import jakarta.validation.constraints.NotBlank;

public record SupportTicketPublicQuery(
        @NotBlank(message = "不能为空") String ticketNo,
        @NotBlank(message = "不能为空") String contactInfo
) {
}
