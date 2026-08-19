package com.example.petcare.support;

import jakarta.validation.constraints.NotBlank;

public record SupportTicketCustomerMessageRequest(
        @NotBlank(message = "不能为空") String contactInfo,
        @NotBlank(message = "不能为空") String content
) {
}
